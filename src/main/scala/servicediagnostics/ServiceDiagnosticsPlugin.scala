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
package org.apache.felix.servicediagnostics

import org.osgi.framework.FrameworkUtil
import collection.JavaConversions._

import Util._

/**
 * This is the interface to be implemented by participating service injection frameworks
 * such as SCR or DependencyManager. 
 * Each plugin implementation is responsible for returning its own set of components.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
trait ServiceDiagnosticsPlugin 
{
    /**
     * returns a list of all known components for this plugin
     */
    def components:List[Comp]
}

/**
 * This class represents a service component.
 * @param name the service interface name 
 *   (use different instances for objects registering multiple services)
 * @param props the service properties
 * @param registered true if the component is already registered in the Service Registry
 * @param deps the list of declared dependencies
 */
class Comp(val name:String, val props:java.util.Dictionary[_,_], val registered:Boolean, val deps:List[Dependency])
{
    override def toString = {if (registered) "[registered]" else "[unregistered]"}+shorten(name)+{
        if (props != null && !props.isEmpty) " "+props else ""}
}

/**
 * This class represents a service dependency.
 * @param name the service interface name
 * @param filter the optional service filter
 * @param available true if the dependency is already available in the Service Registry
 */
class Dependency(val name:String, val filter:String, val available:Boolean = false) 
{
    private val compiled = if (filter != null && !filter.isEmpty) FrameworkUtil.createFilter(filter) else null

    def matchedBy(comp:Comp):Boolean = comp.name == name && 
        !(compiled != null && comp.props == null) && //filter and no props, doesn't match
        (compiled == null || compiled.`match`(comp.props))
    
    override def toString = shorten(name)+{if (filter != null) filter else ""}
}

/** 
 * utility methods
 */
object Util
{
    /**
     * shorten "org.apache.felix.servicediagnostics.ServiceDiagnostics" to "o.a.f.s.ServiceDiagnostics"
     */
    def shorten(classname:String) :String = { 
        val l = classname.split('.').toList
        l.map(_.take(1)).mkString(".") + l.last.drop(1)
    }
}
