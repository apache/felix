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

import org.osgi.framework.BundleContext
import org.apache.felix.dm.DependencyActivatorBase
import org.apache.felix.dm.DependencyManager
import org.apache.felix.servicediagnostics.ServiceDiagnostics

/**
 * This class is a basic DependencyManager based demonstration
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class TestDM extends DependencyActivatorBase
{
    var diagnostics:ServiceDiagnostics = _ //injected

    override def init(bc:BundleContext, dm:DependencyManager) 
    {
        // register a callback in this class to try the engine alone
        // in text mode, when everything is started
        dm.add(createComponent
            .setImplementation(this)
            .add(createServiceDependency
                .setService(classOf[TestDS]) // also wait for DS initialization
                .setAutoConfig(false)
                .setRequired(true))
            .add(createServiceDependency
                .setService(classOf[ServiceDiagnostics])
                .setAutoConfig("diagnostics")
                .setRequired(true)))

        // initialize some sample services for testing purpose (see also TestDS)
        dm.add(createComponent
            .setInterface(classOf[DM1].getName, null)
            .setImplementation(classOf[DM1])
            .add(createServiceDependency
                .setService(classOf[DM2])
                .setAutoConfig(false)
                .setCallbacks(null, null, null)
                .setRequired(true)))

        dm.add(createComponent
            .setInterface(classOf[DM2].getName, null)
            .setImplementation(classOf[DM2])
            .add(createServiceDependency
                .setService(classOf[DM3])
                .setAutoConfig(false)
                .setCallbacks(null, null, null)
                .setRequired(true)))

        dm.add(createComponent
            .setInterface(classOf[DM3].getName, null)
            .setImplementation(classOf[DM3])
            .add(createServiceDependency
                .setService(classOf[Runnable]) //missing dependency
                .setAutoConfig(false)
                .setCallbacks(null, null, null)
                .setRequired(true)))
    }

    override def destroy(bc:BundleContext, dm:DependencyManager) = {}

    def start = 
    {
        println("unavail="+diagnostics.notavail)
        println("all="+diagnostics.allServices)
    }
}

//sample service classes
class DM1
class DM2
class DM3
