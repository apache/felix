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
     * This method gathers "notavail" information from all plugins
     * and performs the final merge by removing known unregistered services
     */
    override def notavail = 
    {
        // merge all notavails from plugins 
        // (kv stands for each key/value pair in a map)
        val merged = (for(p <- plugins; kv <- p.getUnresolvedDependencies) yield kv) toMap

        // remove remaining intermediates. ex: unresolved in DS -> unavailable in DM
        // and return the resulting map
        (for(kv <- merged; dep <- kv._2; if (merged.get(dep.name) isEmpty)) yield kv) toMap
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
