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

    def components:List[Comp] = 
    {
        // this involves a bit of type casting gymnastics because the underlying 
        // API uses mutables and no generic types
        // Option is used to avoid null pointers
        /*val comps = scrService.getComponents
        if (comps == null) Nil
        else */
        (for {
            comp <- Option[Array[Component]](scrService.getComponents).getOrElse(Array())
            service <- Option[Array[String]](comp.getServices).getOrElse(Array())
            deps = Option[Array[Reference]](comp.getReferences).getOrElse(Array())
                          .map(dep => new Dependency(dep.getServiceName,
                                                     Option(dep.getTarget),
                                                     dep.isSatisfied || dep.isOptional)).toList
          }
            // yield Comp builds a list of Comp out of the for comprehension
            yield new Comp(comp.getClassName.trim,
                           service.trim,
                           comp.getProperties,
                           comp.getState != Component.STATE_UNSATISFIED,
                           deps)) toList
    }
}

