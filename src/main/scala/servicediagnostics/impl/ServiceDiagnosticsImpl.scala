/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.servicediagnostics.impl

import scala.collection.mutable.Buffer
import scala.collection.mutable.{Set => mSet}
import scala.collection.JavaConversions._

import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceReference
import org.osgi.framework.Constants.OBJECTCLASS

import org.apache.felix.servicediagnostics._
import org.apache.felix.servicediagnostics.Util._

/**
 * This is the ServiceDiagnostics implementation. 
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class ServiceDiagnosticsImpl(val bc:BundleContext) extends ServiceDiagnostics 
{
    val plugins:Buffer[ServiceDiagnosticsPlugin] = Buffer()

    def addPlugin(p:ServiceDiagnosticsPlugin) = plugins += p

    /**
     * Implements ServiceDiagnostics.notavail.
     * 
     * This method aggregates unregistered components from all plugins
     * and filters all intermediate known unregistered services
     * to keep only missing "leaf" dependencies
     */
    override def notavail :Map[String, Set[String]] = 
    {
        val unavail = plugins.flatMap(_.components).filterNot(_.registered)
        unavail.foldLeft(Map[String,Set[String]]()) { (map,comp) =>
            val missing = comp.deps.toSet.filterNot { d =>
                  d.available || unavail.exists(c => d.matchedBy(c))
                }.map(_.toString) 
            if (missing isEmpty) map else map + (shorten(comp.impl) -> missing)
        }
    }
    
    class Node(val comp:Comp, val edges:mSet[Node] = mSet[Node]()) {
      def name = comp.impl
      override def toString = name + " -> " + edges.map(_.name)
      override def equals(o:Any) = o != null && o.getClass == getClass && o.asInstanceOf[Node].comp == comp
    }

    //debug helper
    def json(l:Iterable[Node]) = l.toList.sortWith { (n1,n2) => 
        n1.name < n2.name
    }.foldLeft(new org.json.JSONArray()) { (j,n) => 
        j.put(new org.json.JSONObject(new java.util.HashMap[String,java.util.List[String]] {{
            put(n.name, new java.util.ArrayList[String] {{ addAll(n.edges.map(_.name)) }})
          }}))
    }.toString(2)

    /**
     * Implements ServiceDiagnostics.unresolved.
     * 
     * Returns a map of (component.name -> list(component.name)) of unresolvable services, if any.
     * 
     * This methods first attempts to resolve all possible paths by traversing the graph 
     * of components dependencies, entering the graph from its outer nodes. 
     * Then it returns the list of unresolvable components by subtraction of the resolved components
     * from the original graph. This is done because "perfect loops" have no border node and are 
     * therefore "invisible" to the traversing algorithm.
     */
    override def unresolved(optionals:Boolean) :Map[String, Set[String]] = 
    {
        // first build a traversable graph from all found components and dependencies
        def buildGraph(link:(Node,Node)=>Unit) = {
            // concatenate component nodes from all plugins
            val allnodes = plugins.flatMap(_.components).map(new Node(_))

            // and connect the nodes according to component dependencies
            // the 'link' method gives the direction of the link
            // note that all dependencies not pointing to a known component are dropped from the graph
            for {
              node <- allnodes 
              dep <- node.comp.deps 
              if (optionals || !dep.available)
            }
            {
                allnodes.filter(n => dep.matchedBy(n.comp)).foreach(n => link(node, n) )
            }

            allnodes.toSet //return the graph
        }

        // a "forward" graph of who depends on who
        val graph = buildGraph((n1,n2) => n1.edges += n2)
        // and the reverse graph of who "triggers" who
        val triggers = buildGraph((n1,n2) => n2.edges += n1)

        // recursive helper method used to traverse the graph and detect loops
        def resolve(node:Node, visited:List[Node] = Nil) :List[Node] = 
        {
            // if a node has no dependency, it is resolved
            if (node.edges isEmpty) node::visited
            else // replace ("map") each dependency with its resolution
            {
                val resolved = node.edges.map { e => 
                    if (visited contains e) 
                    { 
                        println("!!!LOOP {"+node.name+" -> "+e+"} in "+visited)
                        Nil // return an empty list
                    } 
                    else resolve(e, node::visited)
                }
                if (resolved.contains(Nil)) Nil // there were some loops; resolution failed
                else resolved.flatten.toList
            }
        }

        // now traverse the graph starting from border nodes (nodes not pointed by anyone)
        val resolved:Set[Node] = (for { 
            border <- triggers filter (_.edges.size == 0)
            node <- graph.find(_ == border) // graph and triggers contain different Node instances; this uses the overriden equals methods
        } yield resolve(node)).flatten.toSet

        // finally filter the original graph by removing all resolved nodes
        // and format the result (keeping only the names)
        (for (node <- graph.filterNot(n => n.edges.isEmpty || resolved.contains(n)))
          yield (node.name -> node.edges.map{ n => n.name }.toSet)).toMap
    }

    /**
     * Implements ServiceDiagnostics.usingBundles.
     */
    override def usingBundles:Map[String,Set[String]] = 
        allServices.foldLeft(Map[String,Set[String]]()) { case (result, (name, ref)) =>
            Option(ref.getUsingBundles).map { _.toList.map(_.toString) }.getOrElse(Nil) match {
                case using @ h::t => result + (name -> using.toSet)
                case Nil => result
            }
        }

    /**
     * Implements ServiceDiagnostics.serviceProviders.
     */
    override def serviceProviders:Map[String, Set[String]] = 
        allServices.foldLeft(Map[String,Set[String]]()) { case (result, (name, ref)) =>
            val b = ref.getBundle.toString
            result.updated(b, result.getOrElse(b, Set()) + name)
        }

    override def b2b:Map[String,Set[String]] = 
        allServices.foldLeft(Map[String,Set[String]]()) { case (result, (name, ref)) =>
            Option(ref.getUsingBundles).map { _.toList.map(_.toString) }.getOrElse(Nil) match {
                case using @ h::t => 
                    val b = ref.getBundle.toString
                    val filteredUsers = using.filter(_ != b)
                    if (filteredUsers isEmpty) result
                    else result.updated(b, result.getOrElse(b, Set()) ++ using)
                case Nil => result
            }
        }

    /**
     * returns map(service name -> service reference)
     */
    def allServices:Map[String,ServiceReference] = 
    {
        val allrefs = bc.getAllServiceReferences(null, null)
        if (allrefs == null) return Map()

        // scan all service references to build a map of service name to list of using bundles
        // yield (key,value) accumulates a list of (key,value) pairs
        // the resulting list is transformed to a map and returned
        (for {
            ref <- bc.getAllServiceReferences(null, null)
            name <- Option(ref.getProperty(OBJECTCLASS)).map(_.asInstanceOf[Array[String]]).getOrElse(Array())
        } yield (name, ref)) toMap
    }
}
