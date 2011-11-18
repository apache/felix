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
package org.apache.felix.servicediagnostics.sample

import java.util.HashMap
import java.util.ServiceLoader

import scala.io.Source

import org.osgi.framework.launch.FrameworkFactory
import org.apache.felix.main.AutoProcessor

/**
 * This is a simple launcher for the framework, that doesn't require a properties file. 
 * Bundles to be started are simply given in the command line.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
object FelixLauncher
{
    def main(bundles:Array[String]) = 
    {
        // load the FrameworkFactory implementation
        val factory:FrameworkFactory = ServiceLoader.load(classOf[FrameworkFactory],
            getClass.getClassLoader).iterator.next
        // set our hardcoded felix.properties
        val felixProps = new HashMap[String, String]
        felixProps.put("org.osgi.framework.storage", ".cache")
        felixProps.put("org.osgi.framework.storage.clean", "onFirstInit")
        felixProps.put("felix.auto.install", bundles.map("file:"+_).mkString(" "))
        felixProps.put("felix.auto.start", bundles.map("file:"+_).mkString(" "))
        felixProps.put("org.osgi.framework.bootdelegation", "sun.*,com.sun.*")

        //println("Launch Felix using "+factory+" and "+felixProps)
        val felix = factory.newFramework(felixProps)
        felix.init
        // let the AutoProcessor process the properties (auto.install and auto.start)
        AutoProcessor.process(felixProps, felix.getBundleContext)
        // launch felix
        felix.start
        //println("Felix started")
    }
}
