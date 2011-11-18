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

import scala.collection.JavaConversions._

import org.apache.felix.scr.Component
import org.apache.felix.scr.Reference
import org.apache.felix.scr.ScrService

import org.apache.felix.servicediagnostics._

/**
 * This class is the ServiceDiagnosticsPlugin implementation for org.apache.felix.scr.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class DSNotAvail extends ServiceDiagnosticsPlugin 
{

    var scrService:ScrService = _ //dependency injection

    /**
     * implements ServiceDiagnosticsPlugin.getUnresolvedDependencies.
     */
    override def getUnresolvedDependencies:Map[String, List[Dependency]] = 
    {
        /*
         * Same algorithm as DMNotAvail with the SCR API. 
         * Please refer to DMNotAvail for comments.
         */
        val comps:Map[String, Component] = 
            (for (c <- Option(scrService.getComponents).flatten;
            s <- Option(c.getServices).flatten)
        yield (s, c)).toMap

        val compNames = comps.keySet

        (for (kv <- comps.filter(kv => kv._2.getState == Component.STATE_UNSATISFIED);
            unavail = kv._2.getReferences.view
                .filterNot(_ isSatisfied)
                .filterNot(_ isOptional)
                .filterNot(ref => compNames.contains(ref.getName))
                .map(ref => new Dependency(ref.getServiceName, ref.getTarget)).force.toList;
            if (unavail nonEmpty))
            yield (kv._1, unavail)).toMap
    }
}

