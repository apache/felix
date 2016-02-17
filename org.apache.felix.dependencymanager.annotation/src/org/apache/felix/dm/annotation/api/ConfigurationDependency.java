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
import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;


/**
 * Annotates a method for injecting a Configuration Dependency. A configuration dependency 
 * is always required, and allows you to depend on the availability of a valid configuration 
 * for your component. This dependency requires the OSGi Configuration Admin Service.
 * 
 * The annotation can be applied on a callback method which accepts the following parameters:
 * 
 * <p><ul>
 * <li>callback(Dictionary) 
 * <li>callback(Component, Dictionary) 
 * <li>callback(Configuration interface) // type safe configuration
 * <li>callback(Component, Configuration interface) // type safe configuration
 * </ul>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <p> In the following example, the "Printer" component depends on a configuration
 * whose PID name is "sample.PrinterConfiguration". This service will initialize
 * its ip/port number from the provided configuration.
 * 
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
 *     &#64;ConfigurationDependency // Will use the fqdn of the  PrinterConfiguration interface as the pid.
 *     void updated(PrinterConfiguration cnf) {
 *         String ip = cnf.ipAddress();
 *         int port = cnf.portNumber();
 *         ...
 *     }
 * }
 * </pre>
 * </blockquote>
 * 
 * In the above example, the updated callback accepts a type-safe configuration type (and its fqdn is used as the pid).
 * <p> Configuration type is a new feature that allows you to specify an interface that is implemented 
 * by DM and such interface is then injected to your callback instead of the actual Dictionary.
 * Using such configuration interface provides a way for creating type-safe configurations from a actual {@link Dictionary} that is
 * normally injected by Dependency Manager.
 * The callback accepts in argument an interface that you have to provide, and DM will inject a proxy that converts
 * method calls from your configuration-type to lookups in the actual map or dictionary. The results of these lookups are then
 * converted to the expected return type of the invoked configuration method.<br>
 * As proxies are injected, no implementations of the desired configuration-type are necessary!
 * </p>
 * <p>
 * The lookups performed are based on the name of the method called on the configuration type. The method names are
 * "mangled" to the following form: <tt>[lower case letter] [any valid character]*</tt>. Method names starting with
 * <tt>get</tt> or <tt>is</tt> (JavaBean convention) are stripped from these prefixes. For example: given a dictionary
 * with the key <tt>"foo"</tt> can be accessed from a configuration-type using the following method names:
 * <tt>foo()</tt>, <tt>getFoo()</tt> and <tt>isFoo()</tt>.
 * </p>
 * <p>
 * The return values supported are: primitive types (or their object wrappers), strings, enums, arrays of
 * primitives/strings, {@link Collection} types, {@link Map} types, {@link Class}es and interfaces. When an interface is
 * returned, it is treated equally to a configuration type, that is, it is returned as a proxy.
 * </p>
 * <p>
 * Arrays can be represented either as comma-separated values, optionally enclosed in square brackets. For example:
 * <tt>[ a, b, c ]</tt> and <tt>a, b,c</tt> are both considered an array of length 3 with the values "a", "b" and "c".
 * Alternatively, you can append the array index to the key in the dictionary to obtain the same: a dictionary with
 * "arr.0" =&gt; "a", "arr.1" =&gt; "b", "arr.2" =&gt; "c" would result in the same array as the earlier examples.
 * </p>
 * <p>
 * Maps can be represented as single string values similarly as arrays, each value consisting of both the key and value
 * separated by a dot. Optionally, the value can be enclosed in curly brackets. Similar to array, you can use the same
 * dot notation using the keys. For example, a dictionary with 
 * 
 * <pre>{@code "map" => "{key1.value1, key2.value2}"}</pre> 
 * 
 * and a dictionary with <p>
 * 
 * <pre>{@code "map.key1" => "value1", "map2.key2" => "value2"}</pre> 
 * 
 * result in the same map being returned.
 * Instead of a map, you could also define an interface with the methods <tt>getKey1()</tt> and <tt>getKey2</tt> and use
 * that interface as return type instead of a {@link Map}.
 * </p>
 * <p>
 * In case a lookup does not yield a value from the underlying map or dictionary, the following rules are applied:
 * <ol>
 * <li>primitive types yield their default value, as defined by the Java Specification;
 * <li>string, {@link Class}es and enum values yield <code>null</code>;
 * <li>for arrays, collections and maps, an empty array/collection/map is returned;
 * <li>for other interface types that are treated as configuration type a null-object is returned.
 * </ol>
 * </p>
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
     * You can use this method when you use an interface annotated with standard bndtols metatype annotations.
     * (see http://www.aqute.biz/Bnd/MetaType).
     * @return the pid class
     * @deprecated just define an updated callback which accepts as argument a configuration type.
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
     *  @return the dependency name used to configure the dependency dynamically from init callback
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
