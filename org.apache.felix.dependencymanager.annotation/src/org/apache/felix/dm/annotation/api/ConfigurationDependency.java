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
package org.apache.felix.dm.annotation.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotates a method for injecting a Configuration Dependency. A configuration dependency 
 * is always required, and allows you to depend on the availability of a valid configuration 
 * for your component. This dependency requires the OSGi Configuration Admin Service.
 * 
 * <h3>Usage Examples</h3>
 * 
 * <p> In the following example, the "Printer" component depends on a configuration
 * whose PID name is "org.apache.felix.sample.Printer". This service will initialize
 * its ip/port number from the provided configuration:
 * <p>
 * <blockquote>
 * <pre>
 * package org.apache.felix.sample;
 * 
 * &#64;Component
 * public class Printer {
 *     &#64;ConfigurationDependency
 *     void updated(Dictionary config) {
 *         // load printer ip/port from the provided dictionary.
 *     }
 * }
 * </pre>
 * </blockquote>
 * 
 * <p> This other example shows how to specify a configuration dependency, as well as meta data
 * used to customize the WebConsole GUI. Using these meta data, you can specify for example the
 * default value for your configurations data, some descriptions, the cardinality of configuration 
 * values, etc ... 
 * <p>
 * <blockquote>
 * <pre>
 * package org.apache.felix.sample;
 * 
 * &#64;Component
 * public class Printer {
 *     &#64;ConfigurationDependency(
 *         heading = "Printer Service",
 *         description = "Declare here parameters used to configure the Printer service", 
 *         metadata = { 
 *             &#64;PropertyMetaData(heading = "Ip Address", 
 *                               description = "Enter the ip address for the Printer service",
 *                               defaults = { "127.0.0.1" }, 
 *                               type = String.class,
 *                               id = "IPADDR", 
 *                               cardinality = 0),
 *             &#64;PropertyMetaData(heading = "Port Number", 
 *                               description = "Enter the port number for the Printer service",
 *                               defaults = { "4444" }, 
 *                               type = Integer.class,
 *                               id = "PORTNUM", 
 *                               cardinality = 0) 

 *         }
 *     )
 *     void updated(Dictionary config) {
 *         // load configuration from the provided dictionary.
 *     }
 * }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface ConfigurationDependency
{
    /**
     * Returns the pid for a given service (by default, the pid is the service class name).
     * @return the pid for a given service (default = Service class name)
     */
    String pid() default "";

    /**
     * Returns true if the configuration properties must be published along with the service. 
     * Any additional service properties specified directly are merged with these.
     * @return true if configuration must be published along with the service, false if not.
     */
    boolean propagate() default false;
    
    /**
     * The label used to display the tab name (or section) where the properties are displayed. Example: "Printer Service".
     * @return The label used to display the tab name where the properties are displayed.
     */
    String heading() default "";

    /**
     * A human readable description of the PID this annotation is associated with. Example: "Configuration for the PrinterService bundle".
     * @return A human readable description of the PID this annotation is associated with.
     */
    String description() default "";

    /**
     * The list of properties types used to expose properties in web console. 
     * @return The list of properties types used to expose properties in web console. 
     */
    PropertyMetaData[] metadata() default {};
}
