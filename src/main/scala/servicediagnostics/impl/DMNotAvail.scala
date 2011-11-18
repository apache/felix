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

// this import of implicit methods makes java to scala collections conversions transparent (lists, maps)
import scala.collection.JavaConversions._

import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceReference

import org.apache.felix.dm.DependencyManager
import org.apache.felix.dm.ComponentDeclaration
import org.apache.felix.dm.ComponentDeclaration._
import org.apache.felix.dm.ComponentDependencyDeclaration
import org.apache.felix.dm.ComponentDependencyDeclaration._

import org.apache.felix.servicediagnostics._

/**
 * This class is the ServiceDiagnosticsPlugin implementation for org.apache.felix.dm.DependencyManager.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class DMNotAvail(val bc:BundleContext) extends ServiceDiagnosticsPlugin 
{

    /**
     * implements ServiceDiagnosticsPlugin.getUnresolvedDependencies
     */
    override def getUnresolvedDependencies:Map[String, List[Dependency]] = 
    {
        // build a map(name -> comp) of all ComponentDeclarations known to DM, from each DependencyManager instance
        val comps:Map[String, ComponentDeclaration] = 
        {
            (for(dm <- DependencyManager.getDependencyManagers;
                comp <- dm.asInstanceOf[DependencyManager].getComponents; 
                cd = comp.asInstanceOf[ComponentDeclaration]) 
                // yield (k,v) builds a list of (k,v) pairs, the resulting list is then turned into a map
                // notice that toMap applies to the complete for-comprehension block
                // k is the ComponentDeclaration name stripped of its filter part: everything before the '('
                yield (cd.getName.takeWhile(_ != '('), cd)) toMap
        }

        val compNames = comps.keySet

        // build and return a map(name -> List(unavail)) of unavailable dependencies:
        // filter out registered services from the known ComponentDeclarations
        (for(kv <- comps.filter(kv => kv._2.getState == STATE_UNREGISTERED);
            // kv._1 is the name, kv._2 is the component
            // here, we lookup the component's list of dependencies
            // filtering out the ones available and the ones already known to DM, to keep only leafs
            // (view/force added for performance)
            unavail = kv._2.getComponentDependencies.view
                .filter(dep => dep.getState == STATE_UNAVAILABLE_REQUIRED)
                .filterNot(dep => compNames.contains(dep.getName))
                .map(dep => new Dependency(dep.getName, ""/*dep.getFilter*/)).force.toList;
            if (unavail nonEmpty)) //this 'if' is part of the for comprehension
            // again yield (k,v) builds a list of (k,v), the final result is then turned into a map
            // notice that toMap applies to the complete for-comprehension block
            // toMap being the last instruction, the resulting map is the return value of the method
            yield (kv._1, unavail)) toMap
    }

}
