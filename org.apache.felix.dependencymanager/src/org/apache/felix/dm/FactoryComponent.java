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

import java.util.Dictionary;

import org.apache.felix.dm.Component.ServiceScope;

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
 *         FactoryComponent factoryComponent = createFactoryComponent()
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
public interface FactoryComponent extends Component {
    
	/**
	 * Sets the component scope.
	 * @param scope the component scope (default=SINGLETON)
	 * 
	 * @return this component
	 */
    FactoryComponent setScope(ServiceScope scope);
    
   /**
     * Sets the implementation for this component. You can actually specify
     * an instance you have instantiated manually, or a <code>Class</code>
     * that will be instantiated using its default constructor when the
     * required dependencies are resolved, effectively giving you a lazy
     * instantiation mechanism.
     * 
     * There are four special methods that are called when found through
     * reflection to give you life cycle management options:
     * <ol>
     * <li><code>init()</code> is invoked after the instance has been
     * created and dependencies have been resolved, and can be used to
     * initialize the internal state of the instance or even to add more
     * dependencies based on runtime state</li>
     * <li><code>start()</code> is invoked right before the service is 
     * registered</li>
     * <li><code>stop()</code> is invoked right after the service is
     * unregistered</li>
     * <li><code>destroy()</code> is invoked after all dependencies are
     * removed</li>
     * </ol>
     * In short, this allows you to initialize your instance before it is
     * registered, perform some post-initialization and pre-destruction code
     * as well as final cleanup. If a method is not defined, it simply is not
     * called, so you can decide which one(s) you need. If you need even more
     * fine-grained control, you can register as a service state listener too.
     * 
     * @param implementation the implementation
     * @return this component
     * @see ComponentStateListener
     */
	FactoryComponent setImplementation(Object implementation);

    /**
     * Adds dependency(ies) to this component, atomically. If the component is already active or if you add
     * dependencies from the init method, then you should add all the dependencies in one single add method call 
     * (using the varargs argument), because this method may trigger component activation (like
     * the ServiceTracker.open() method does).
     * 
     * @param dependencies the dependencies to add.
     * @return this component
     */
	FactoryComponent add(Dependency ... dependencies);
	
	/**
	 * Removes a dependency from the component.
	 * @param d the dependency to remove
	 * @return this component
	 */
	FactoryComponent remove(Dependency d);

    /**
     * Adds a component state listener to this component.
     * 
     * @param listener the state listener
     */
	FactoryComponent add(ComponentStateListener listener);

    /**
     * Removes a component state listener from this component.
     * 
     * @param listener the state listener
     */
	FactoryComponent remove(ComponentStateListener listener);

    /**
     * Sets the public interface under which this component should be registered
     * in the OSGi service registry.
     *  
     * @param serviceName the name of the service interface
     * @param properties the properties for this service
     * @return this component
     */
	FactoryComponent setInterface(String serviceName, Dictionary<?,?> properties);

    /**
     * Sets the public interfaces under which this component should be registered
     * in the OSGi service registry.
     *  
     * @param serviceNames the names of the service interface
     * @param properties the properties for these services
     * @return this component
     */
	FactoryComponent setInterface(String[] serviceNames, Dictionary<?, ?> properties);

    /**
     * Sets the public interface under which this component should be registered
     * in the OSGi service registry.
     *  
     * @param serviceName the name of the service interface
     * @param properties the properties for this service
     * @return this component
     */
	FactoryComponent setInterface(Class<?> serviceName, Dictionary<?,?> properties);

    /**
     * Sets the public interfaces under which this component should be registered
     * in the OSGi service registry.
     *  
     * @param serviceNames the names of the service interface
     * @param properties the properties for these services
     * @return this component
     */
	FactoryComponent setInterface(Class<?>[] serviceNames, Dictionary<?, ?> properties);

	/**
     * Configures auto configuration of injected classes in the component instance.
     * The following injections are currently performed, unless you explicitly
     * turn them off:
     * <dl>
     * <dt>BundleContext</dt><dd>the bundle context of the bundle</dd>
     * <dt>ServiceRegistration</dt><dd>the service registration used to register your service</dd>
     * <dt>DependencyManager</dt><dd>the dependency manager instance</dd>
     * <dt>Component</dt><dd>the component instance of the dependency manager</dd>
     * </dl>
     * 
     * @param clazz the class (from the list above)
     * @param autoConfig <code>false</code> to turn off auto configuration
     */
	FactoryComponent setAutoConfig(Class<?> clazz, boolean autoConfig);

    /**
     * Configures auto configuration of injected classes in the component instance.
     * 
     * @param clazz the class (from the list above)
     * @param instanceName the name of the instance to inject the class into
     * @see #setAutoConfig(Class, boolean)
     */
	FactoryComponent setAutoConfig(Class<?> clazz, String instanceName);

    /**
     * Sets the service properties associated with the component. If the service
     * was already registered, it will be updated.
     * 
     * @param serviceProperties the properties
     */
	FactoryComponent setServiceProperties(Dictionary<?, ?> serviceProperties);

    /**
     * Sets the names of the methods used as callbacks. These methods, when found, are
     * invoked as part of the life cycle management of the component implementation. The
     * dependency manager will look for a method of this name with the following signatures,
     * in this order:
     * <ol>
     * <li>method(Component component)</li>
     * <li>method()</li>
     * </ol>
     * 
     * @param init the name of the init method
     * @param start the name of the start method
     * @param stop the name of the stop method
     * @param destroy the name of the destroy method
     * @return the component
     */
	FactoryComponent setCallbacks(String init, String start, String stop, String destroy);

    /**
     * Sets the names of the methods used as callbacks. These methods, when found, are
     * invoked on the specified instance as part of the life cycle management of the component
     * implementation.
     * <p>
     * See setCallbacks(String init, String start, String stop, String destroy) for more
     * information on the signatures. Specifying an instance means you can create a manager
     * that will be invoked whenever the life cycle of a component changes and this manager
     * can then decide how to expose this life cycle to the actual component, offering an
     * important indirection when developing your own component models.
     *
     * @return this component
     */
	FactoryComponent setCallbacks(Object instance, String init, String start, String stop, String destroy);

    /**
     * Sets the factory to use to create the implementation. You can specify
     * both the factory class and method to invoke. The method should return
     * the implementation, and can use any method to create it. Actually, this
     * can be used together with <code>setComposition</code> to create a
     * composition of instances that work together to implement a component. The
     * factory itself can also be instantiated lazily by not specifying an
     * instance, but a <code>Class</code>.
     * 
     * @param factory the factory instance or class
     * @param createMethod the name of the create method
     * @return this component
     */
	FactoryComponent setFactory(Object factory, String createMethod);

    /**
     * Sets the factory to use to create the implementation. You specify the
     * method to invoke. The method should return the implementation, and can
     * use any method to create it. Actually, this can be used together with
     * <code>setComposition</code> to create a composition of instances that
     * work together to implement a component.
     * <p>
     * Note that currently, there is no default for the factory, so please use
     * <code>setFactory(factory, createMethod)</code> instead.
     * 
     * @param createMethod the name of the create method
     * @return this component
     */
	FactoryComponent setFactory(String createMethod);

    /**
     * Sets the instance and method to invoke to get back all instances that
     * are part of a composition and need dependencies injected. All of them
     * will be searched for any of the dependencies. The method that is
     * invoked must return an <code>Object[]</code>.
     * 
     * @param instance the instance that has the method
     * @param getMethod the method to invoke
     * @return this component
     */
	FactoryComponent setComposition(Object instance, String getMethod);

    /**
     * Sets the method to invoke on the service implementation to get back all
     * instances that are part of a composition and need dependencies injected.
     * All of them will be searched for any of the dependencies. The method that
     * is invoked must return an <code>Object[]</code>.
     * 
     * @param getMethod the method to invoke
     * @return this component
     */
	FactoryComponent setComposition(String getMethod);

	/**
	 * Activate debug for this component. Informations related to dependency processing will be displayed
	 * using osgi log service, our to standard output if no log service is currently available.
	 * @param label
	 */
	FactoryComponent setDebug(String label);

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
