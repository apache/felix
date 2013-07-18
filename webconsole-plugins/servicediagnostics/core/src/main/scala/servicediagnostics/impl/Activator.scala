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

import java.util. { Hashtable => jHT }

import org.osgi.framework.BundleContext

import org.apache.felix.shell.Command
import org.apache.felix.service.command.CommandProcessor

import org.apache.felix.dm.DependencyActivatorBase
import org.apache.felix.dm.DependencyManager

import org.apache.felix.servicediagnostics.ServiceDiagnostics
import org.apache.felix.servicediagnostics.ServiceDiagnosticsPlugin
import org.apache.felix.servicediagnostics.webconsole.WebConsolePlugin
import org.apache.felix.servicediagnostics.shell.CLI

/**
 * Activator class for the service diagnostics core implementation
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class Activator extends DependencyActivatorBase
{
    override def init(bc:BundleContext, dm:DependencyManager) =
    {
        // register our diagnostics service and its plugins
        dm.add(createComponent
            .setInterface(classOf[ServiceDiagnosticsPlugin].getName, null)
            .setImplementation(new DMNotAvail(bc)))

        dm.add(createComponent
            .setInterface(classOf[ServiceDiagnosticsPlugin].getName, null)
            .setImplementation(classOf[DSNotAvail])
            .add(createServiceDependency
                .setService(classOf[org.apache.felix.scr.ScrService])
                .setAutoConfig("scrService")
                setRequired(true)))

        dm.add(createComponent
            .setInterface(classOf[ServiceDiagnostics].getName, null)
            .setImplementation(new ServiceDiagnosticsImpl(bc))
            .add(createServiceDependency
                .setService(classOf[ServiceDiagnosticsPlugin])
                .setCallbacks("addPlugin", null, null)
                .setRequired(false)))

        try // if the engine is used alone, the webconsole may just not be there 
        { 
            // register the webconsole plugin 
            dm.add(createComponent
                .setInterface(classOf[javax.servlet.Servlet].getName, new jHT[String,String]() {{
                      put("felix.webconsole.label", "servicegraph")
                  }})
                .setImplementation(classOf[WebConsolePlugin])
                .add(createServiceDependency
                    .setService(classOf[ServiceDiagnostics])
                    .setRequired(true)
                    .setAutoConfig("engine")))
        }
        catch 
        {
            case t:Throwable => println("failed to register the servicediagnostics webconsole plugin")
        }

        try // if the engine is used alone, the shell may just not be there 
        { 
            // register the shell command
            dm.add(createComponent
                .setInterface(classOf[Command].getName, new jHT[String,Any]() {{
                      put(CommandProcessor.COMMAND_FUNCTION, CLI.usage.split("|"))
                      put(CommandProcessor.COMMAND_SCOPE, CLI.scope)
                  }})
                .setImplementation(classOf[CLI])
                .add(createServiceDependency
                    .setService(classOf[ServiceDiagnostics])
                    .setRequired(true)
                    .setAutoConfig("engine")))
        }
        catch 
        {
            case t:Throwable => println("failed to register the servicediagnostics shell plugin")
        }
    }

    override def destroy(bc:BundleContext, dm:DependencyManager) = {}
}
