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
 * whose PID name is "sample.PrinterConfiguration". This service will initialize
 * its ip/port number from the provided configuration.
 * <p> First, we define the configuration metadata, using standard bndtools metatatype annotations 
 * (see http://www.aqute.biz/Bnd/MetaType):
 * 
 * <blockquote>
 * <pre>
 * package sample;
 * import aQute.bnd.annotation.metatype.Meta.AD;
 * import aQute.bnd.annotation.metatype.Meta.OCD;
 *
 * &#64;OCD(description = "Declare here the Printer Configuration.")
 * public interface PrinterConfiguration {
 *     &#64;AD(description = "Enter the printer ip address")
 *     String ipAddress();
 *
 *     &#64;AD(description = "Enter the printer address port number.")
 *     int portNumber();
 * }
 * </pre>
 * </blockquote>
 * 
 * Next, we define our Printer service which depends on the PrinterConfiguration:
 * 
 * <blockquote>
 * <pre>
 * package sample;
 * import aQute.bnd.annotation.metatype.*;
 *
 * &#64;Component
 * public class Printer {
 *     &#64;ConfigurationDependency(pidClass = PrinterConfiguration.class) // Will use pid "sample.PrinterConfiguration"
 *     void updated(Dictionary props) {
 *         // load configuration from the provided dictionary, or throw an exception of any configuration error.
 *         PrinterConfig cnf = Configurable.createConfigurable(PrinterConfig.class, props);
 *         String ip = cnf.ipAddress();
 *         int port = cnf.portNumber();
 *         ...
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
     * Returns the pid from a class name. The full class name will be used as the configuration PID.
     * You can use this method when you use an interface annoted with standard bndtols metatype annotations.
     * (see http://www.aqute.biz/Bnd/MetaType).
     */
    Class<?> pidClass() default Object.class;

    /**
     * Returns true if the configuration properties must be published along with the service. 
     * Any additional service properties specified directly are merged with these.
     * @return true if configuration must be published along with the service, false if not.
     */
    boolean propagate() default false;
    
    /**
     * The name for this configuration dependency. When you give a name a dependency, it won't be evaluated
     * immediately, but after the component's init method has been called, and from the init method, you can then return 
     * a map in order to dynamically configure the configuration dependency (the map has to contain a "pid" and/or "propagate" 
     * flag, prefixed with the dependency name). Then the dependency will be evaluated after the component init method, and will
     * be injected before the start method.
     * 
     * <p> Usage example of a Configuration dependency whose pid and propagate flag is configured dynamically from init method:
     * 
     * <blockquote><pre>
     *  &#47;**
     *    * A Service that dynamically defines an extra dynamic configuration dependency from its init method. 
     *    *&#47;
     *  &#64;Component
     *  class X {
     *      private Dictionary m_config;
     *      
     *      // Inject initial Configuration (injected before any other required dependencies)
     *      &#64;ConfigurationDependency
     *      void componentConfiguration(Dictionary config) {
     *           // you must throw an exception if the configuration is not valid
     *           m_config = config;
     *      }
     *      
     *      &#47;**
     *       * All unnamed dependencies are injected: we can now configure our dynamic configuration whose dependency name is "global".
     *       *&#47;
     *      &#64;Init
     *      Map init() {
     *          return new HashMap() {{
     *              put("global.pid", m_config.get("globalConfig.pid"));
     *              put("global.propagate", m_config.get("globalConfig.propagate"));
     *          }};
     *      } 
     * 
     *      // Injected after init, and dynamically configured by the init method.
     *      &#64;ConfigurationDependency(name="global")
     *      void globalConfiguration(Dictionary globalConfig) {
     *           // you must throw an exception if the configuration is not valid
     *      }
     * 
     *      &#47;**
     *       * All dependencies are injected and our service is now ready to be published.
     *       *&#47;
     *      &#64;Start
     *      void start() {
     *      }
     *  }
     *  </pre></blockquote>
     */
    String name() default "";
    
   /**
     * The label used to display the tab name (or section) where the properties are displayed. Example: "Printer Service".
     * @return The label used to display the tab name where the properties are displayed.
     * @deprecated use standard bndtools metatype annotations instead (see http://www.aqute.biz/Bnd/MetaType)
     */
    String heading() default "";

    /**
     * A human readable description of the PID this annotation is associated with. Example: "Configuration for the PrinterService bundle".
     * @return A human readable description of the PID this annotation is associated with.
     * @deprecated use standard bndtools metatype annotations instead (see http://www.aqute.biz/Bnd/MetaType)
     */
    String description() default "";

    /**
     * The list of properties types used to expose properties in web console. 
     * @return The list of properties types used to expose properties in web console.
     * @deprecated use standard bndtools metatype annotations instead (see http://www.aqute.biz/Bnd/MetaType)
     */
    PropertyMetaData[] metadata() default {};
}
