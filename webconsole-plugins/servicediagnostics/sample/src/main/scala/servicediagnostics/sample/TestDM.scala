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

import java.util. { Hashtable => jHT }

/**
 * This class is a basic DependencyManager based demonstration
 * 
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

        //
        // test1:
        //  DM1 -> DS1 -> DM2 -> Unavail

        dm.add(createComponent
            .setInterface(classOf[DM1].getName, null)
            .setImplementation(classOf[DM1])
            .add(createServiceDependency
                .setService(classOf[DS1])
                .setAutoConfig(false)
                .setCallbacks(null, null, null)
                .setRequired(true)))

        dm.add(createComponent
            .setInterface(classOf[DM2].getName, null)
            .setImplementation(classOf[DM2])
            .add(createServiceDependency
                .setService(classOf[Runnable]) //Unavail
                .setAutoConfig(false)
                .setCallbacks(null, null, null)
                .setRequired(true)))

        // test2: properties
        // DMP(0,1) -> DSP(0) -> DMP(2) -> Unavail

        dm.add(createComponent
            .setInterface(classOf[DMP].getName, new jHT[String,String]() {{
              put("p", "0")
              put("q", "1")
            }})
            .setImplementation(classOf[DMP])
            .add(createServiceDependency
                .setService(classOf[DSP], "(p=0)")
                .setAutoConfig(false)
                .setCallbacks(null, null, null)
                .setRequired(true)))

        dm.add(createComponent
            .setInterface(classOf[DMP].getName, new jHT[String,String]() {{
              put("p", "2")
            }})
            .setImplementation(classOf[DMP])
            .add(createServiceDependency
                .setService(classOf[Runnable]) // Unavail
                .setAutoConfig(false)
                .setCallbacks(null, null, null)
                .setRequired(true)))

        // test3: loop (required)
        // DML1 -> DSL1 -> DML2 -> DML1

        dm.add(createComponent
            .setInterface(classOf[DML1].getName, new jHT[String,String]() {{
              put("p", "1")
              put("q", "1")
            }})
            .setImplementation(classOf[DML1])
            .add(createServiceDependency
                .setService(classOf[DSL1], "(q=1)")
                .setAutoConfig(false)
                .setCallbacks(null, null, null)
                .setRequired(true)))

        dm.add(createComponent
            .setInterface(classOf[DML2].getName, new jHT[String,String]() {{
              put("p", "2")
              put("q", "2")
            }})
            .setImplementation(classOf[DML2])
            .add(createServiceDependency
                .setService(classOf[DML1]) //Loop
                .setAutoConfig(false)
                .setCallbacks(null, null, null)
                .setRequired(true))
            .add(createServiceDependency
                .setService(classOf[TestDS]) //Available with no deps
                .setAutoConfig(false)
                .setCallbacks(null, null, null)
                .setRequired(true)))

        // test4: loop (optional)
        // DSL2 -(opt)-> DML3 --> DSL3 -> DSL2

        dm.add(createComponent
            .setInterface(classOf[DML3].getName, new jHT[String,String]() {{
              put("p", "3")
              put("q", "3")
            }})
            .setImplementation(classOf[DML3])
            .add(createServiceDependency
                .setService(classOf[DSL3], "(q=3)")
                .setAutoConfig(false)
                .setCallbacks(null, null, null)
                .setRequired(true)))

    }

    override def destroy(bc:BundleContext, dm:DependencyManager) = {}

    def start = try 
    {
        println("unavail="+diagnostics.notavail)
    }
    catch 
    {
      case (e:Exception) => e.printStackTrace
    }
}

//sample service classes
class DM1
class DM2
class DMP
class DML1
class DML2
class DML3
