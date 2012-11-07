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

import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceReference
import org.osgi.framework.Constants.OBJECTCLASS

import org.apache.felix.servicediagnostics._

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
     * This method gathers components information from all plugins
     * and filters all intermediate known unregistered services
     * to keep only missing "leaf" dependencies
     */
    override def notavail :Map[String, List[String]] = 
    {
        val unavail :List[Comp] = for {
                          plugin <- plugins.toList
                          comp <- plugin.components
                          if (! comp.registered)
                      } yield comp
        (for {
            comp <- unavail
            dep <- comp.deps.filterNot(_.available)
            if (! unavail.exists(c => dep.matchedBy(c)))
        } yield comp.toString -> comp.deps.filterNot(_.available).map(_.toString) ) toMap

    }
    
    class Node(val comp:Comp, val edges:mSet[Node] = mSet[Node]()) {
      def name = comp.toString
      override def toString = name + " -> " + edges.map(_.name)
    }

    /**
     * returns a map of (component.name -> list(component.name)) of unresolvable services, if any
     */
    override def unresolved :Map[String, List[String]] = 
    {
        // first build a traversable graph from all found component and dependencies
        def buildGraph(link:(Node,Node)=>Unit) = {
            // concatenate component nodes from all plugins
            val allnodes = for ( p <- plugins; comp <- p.components ) yield new Node(comp)

            // and connect the nodes according to component dependencies
            // the 'link' method gives the direction of the link
            for ( node <- allnodes; dep <- node.comp.deps )
            {
                allnodes.filter(n => dep.matchedBy(n.comp)).foreach(n => link(node, n) )
            }

            allnodes.toList //return the graph
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
            node <- graph.find(_.name == border.name)
        } yield resolve(node)).flatten.toSet

        // finally filter the original graph by removing all resolved nodes
        // and format the result
        (for (node <- graph.filterNot(n => resolved.contains(n)))
          yield (node.name -> node.edges.map(_.name).toList)).toMap
    }

    /**
    * Implements ServiceDiagnostics.allServices.
    */
    override def allServices:Map[String,List[String]] = 
    {
        val allrefs = bc.getAllServiceReferences(null, null)
        if (allrefs == null) return Map()

        /*
         * inner method used to return all the interface names a ServiceReference was registered under
         */
        def names(ref:ServiceReference):Array[String] = 
        {
            val n = ref.getProperty(OBJECTCLASS)
            if (n != null) n.asInstanceOf[Array[String]] else Array()
        }

        /*
         * inner method used to return all the bundles using a given ServiceReference
         */
        def using(ref:ServiceReference):List[String] = 
        {
            val u = ref.getUsingBundles
            if (u != null) u.toList.map(_ toString) else List()
        }

        //scan all service references to build a map of service name to list of using bundles
        (for(ref <- bc.getAllServiceReferences(null, null);
            name <- names(ref);
            u = using(ref);
            if (u.nonEmpty))
            // yield (key,value) accumulates a list of (key,value) pairs
            // the resulting list is transformed to a map and returned
            yield (name, u)) toMap
    }
}
