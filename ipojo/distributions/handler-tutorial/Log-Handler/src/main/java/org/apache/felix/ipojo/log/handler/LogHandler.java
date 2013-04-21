/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.log.handler;


import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.service.log.LogService;

import java.util.Dictionary;

// Declare a handler.
@Handler(name = "Log", namespace = LogHandler.NAMESPACE)
public class LogHandler extends PrimitiveHandler {

    public static final String NAMESPACE = "org.apache.felix.ipojo.log.handler";

    // Handlers are iPOJO components, so can use service dependencies
    @Requires(optional = true, nullable = false)
    LogService log;
    private InstanceManager instanceManager;
    private int logLevel;

    /**
     * Parses the component's metadata to retrieve the log level in which we log messages.
     *
     * @param metadata      component's metadata
     * @param configuration instance configuration (unused in this example)
     * @throws ConfigurationException the configuration is inconsistent
     */
    @Override
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
        // First parse the metadata to check if the log handler logLevel

        // Get all Namespace:log element from the metadata
        Element[] log_elements = metadata.getElements("log", NAMESPACE);

        // If an element match, parse the logLevel attribute of the first found element
        if (log_elements[0].containsAttribute("level")) {
            String l = log_elements[0].getAttribute("level");
            if (l.equalsIgnoreCase("info")) {
                logLevel = LogService.LOG_INFO;
            } else if (l.equalsIgnoreCase("error")) {
                logLevel = LogService.LOG_ERROR;
            } else if (l.equalsIgnoreCase("warning")) {
                logLevel = LogService.LOG_WARNING;
            }
        }

        instanceManager = getInstanceManager();
    }

    /**
     * The instance is starting.
     */
    public void start() {
        if (log != null) {
            log.log(logLevel, "The component instance " + instanceManager.getInstanceName() + " is starting");
        }
    }

    /**
     * The instance is stopping.
     */
    public void stop() {
        if (log != null) {
            log.log(logLevel, "The component instance " + instanceManager.getInstanceName() + " is stopping");
        }
    }

    /**
     * Logging messages when the instance state is changing
     *
     * @param state the new state
     */
    public void stateChanged(int state) {
        if (log != null) {
            if (state == InstanceManager.VALID) {
                System.out.println("The component instance " + instanceManager.getInstanceName() + " becomes valid");
                log.log(logLevel, "The component instance " + instanceManager.getInstanceName() + " becomes valid");
            }
            if (state == InstanceManager.INVALID) {
                System.out.println("The component instance " + instanceManager.getInstanceName() + " becomes invalid");
                log.log(logLevel, "The component instance " + instanceManager.getInstanceName() + " becomes invalid");
            }
        }
    }
}
