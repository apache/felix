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
package org.apache.felix.dm;

/**
 * Interface used to configure the various parameters needed when defining 
 * a Dependency Manager factory component.
 * A factory component is a component which can be instantiated multiple times using standard 
 * OSGi Factory Configurations.<p>
 * 
 * When a factory configuration is created, an instance of the component is created and the configuration
 * is injected by default in the "updated" callback, which supports the following signatures:
 * 
 * <ul>
 * <li>callback(Dictionary) 
 * <li>callback(Component, Dictionary) 
 * <li>callback(Component, Configuration ... configTypes) // type safe configuration interface(s)
 * <li>callback(Configuration ... configTypes) // type safe configuration interface(s)
 * <li>callback(Dictionary, Configuration ... configTypes) // type safe configuration interfaces(s)
 * <li>callback(Component, Dictionary, Configuration ... configTypes) // type safe configuration interfaces(s)
 * </ul>
 * 
 * <h3>Usage Examples</h3>
 * 
 * Here is a sample showing a Hello component, which can be instantiated multiple times using a factory configuration:
 * 
 * <blockquote><pre>
 * {@code
 * public class Activator extends DependencyActivatorBase {
 *     &Override
 *     public void init(BundleContext context, DependencyManager dm) throws Exception {
 *         Component factoryComponent = createFactoryComponent()
 *             .setFactoryPid("my.factory.pid")
 *             .setInterface(MySevice.class.getName(), null)
 *             .setImplementation(MyComponent.class)
 *             .setConfigType(MyConfig.class);
 *         dm.add(factoryComponent);
 *     }
 * }
 * 
 * public interface MyConfig {
 *     int getPort();
 *     String getAddress();
 * }
 * 
 * public class MyComponent implements MyService {
 *     void updated(MyConfig cnf) {
 *         int port = cnf.getPort();
 *         String addr = cnf.getAddress();
 *         ...
 *     }
 * }
 * } </pre></blockquote>
 * 
 * @see DependencyManager#createFactoryComponent()
 */
public interface FactoryComponent extends Component<FactoryComponent> {
    
    /**
     * Sets the pid matching the factory configuration
     * @param factoryPid the pid matching the factory configuration
     */
    FactoryComponent setFactoryPid(String factoryPid);

    /**
     * Sets the pid matching the factory configuration using the specified class.
     * The FQDN of the specified class will be used as the factory pid.
     * @param clazz the class whose FQDN name will be used for the factory pid
     */
    FactoryComponent setFactoryPid(Class<?> clazz);

    /**
     * Sets the method name that will be notified when the factory configuration is created/updated.
     * By default, the callback name used is <code>updated</code>
     * TODO describe supported signatures
     * @param update the method name that will be notified when the factory configuration is created/updated.
     */
    FactoryComponent setUpdated(String update);

    /**
     * Sets the object on which the updated callback will be invoked. 
     * By default, the callback is invoked on the component instance.
     * @param updatedCallbackInstance the object on which the updated callback will be invoked.
     */
    FactoryComponent setUpdateInstance(Object updatedCallbackInstance);

    /**
     * Sets the propagate flag (true means all public configuration properties are propagated to service properties).
     * By default, public configurations are not propagated.
     * @param propagate the propagate flag (false, by default; true means all public configuration properties are propagated to service properties).
     */
    FactoryComponent setPropagate(boolean propagate);

   /**
     * Sets the configuration type to use instead of a dictionary. The updated callback is assumed to take
     * as arguments the specified configuration types in the same order they are provided in this method. 
     * Optionally, the callback may define a Dictionary as the first argument, in order to also get the raw configuration.
     * @param configTypes the configuration type to use instead of a dictionary
     * @see ConfigurationDependency
     */
    FactoryComponent setConfigType(Class<?> ... configTypes);

    /**
     * Sets the metatype label used to display the tab name (or section) where the properties are displayed. 
     * Example: "Printer Service"
     * @param heading the label used to display the tab name (or section) where the properties are displayed.
     */
    FactoryComponent setHeading(String heading);

    /**
     * A metatype human readable description of the factory PID this configuration is associated with.
     * @param desc
     */
    FactoryComponent setDesc(String desc);

    /**
     * Points to the metatype basename of the Properties file that can localize the Meta Type informations.
     * The default localization base name for the properties is OSGI-INF/l10n/bundle, but can
     * be overridden by the manifest Bundle-Localization header (see core specification, in section Localization 
     * on page 68). You can specify a specific localization basename file using this parameter 
     * (e.g. <code>"person"</code> will match person_du_NL.properties in the root bundle directory).
     * @param localization 
     */
    FactoryComponent setLocalization(String localization);
        
    /**
     * Sets metatype MetaData regarding configuration properties.
     * @param metaData the metadata regarding configuration properties 
     */
    FactoryComponent add(PropertyMetaData ... metaData) ;

}
