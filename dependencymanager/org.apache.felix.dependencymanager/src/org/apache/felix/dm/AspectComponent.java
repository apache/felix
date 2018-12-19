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
 * a Dependency Manager aspect component.
 * 
 * Aspects allow you to define an interceptor, or chain of interceptors 
 * for a service (to add features like caching or logging, etc ...). The dependency manager intercepts 
 * the original service, and allows you to execute some code before invoking the original service ...
 * The aspect will be applied to any service that matches the specified interface and filter and 
 * will be registered with the same interface and properties as the original service, plus any 
 * extra properties you supply here. If you declare the original service as a member it will be injected.
 * 
 * <h3>Usage Examples</h3>
 * 
 * Here is a sample showing a DatabaseCache aspect which is created each time a Database interface is registered in the registry.
 * 
 * <blockquote><pre>
 * {@code
 * public class Activator extends DependencyActivatorBase {
 *     &Override
 *     public void init(BundleContext context, DependencyManager dm) throws Exception {
 *         AspectComponent aspectComponent = createAspectComponent()
 *             .setAspect(Database.class, null, 10)
 *             .setImplementation(DatabaseCache.class);
 *         dm.add(aspectComponent);
 *     }
 * }
 * 
 * interface Database {
 *     String get(String key);
 * }
 * 
 * class DatabaseCache implements Database {
 *     volatile Database originalDatabase; // injected
 *     
 *     String get(String key) {
 *         String value = cache.get(key);
 *         if (value == null) {
 *             value = this.originalDatabase.get(key);
 *             store(key, value);
 *         }
 *         return value;
 *     }
 *     ... 
 * }
 * } </pre></blockquote>
 * 
 * <p> For "add", "change", "remove" callbacks, the following method signatures are supported:
 * 
 * <pre>{@code
 * (Component comp, ServiceReference ref, Service service)
 * (Component comp, ServiceReference ref, Object service)
 * (Component comp, ServiceReference ref)
 * (Component comp, Service service)
 * (Component comp, Object service)
 * (Component comp)
 * (Component comp, Map properties, Service service)
 * (ServiceReference ref, Service service)
 * (ServiceReference ref, Object service)
 * (ServiceReference ref)
 * (Service service)
 * (Service service, Map propeerties)
 * (Map properties, Service, service)
 * (Service service, Dictionary properties)
 * (Dictionary properties, Service service)
 * (Object service)
 * }</pre>
 * 
 * <p> For "swap" callbacks, the following method signatures are supported:
 * 
 * <pre>{@code
 * (Service old, Service replace)
 * (Object old, Object replace)
 * (ServiceReference old, Service old, ServiceReference replace, Service replace)
 * (ServiceReference old, Object old, ServiceReference replace, Object replace)
 * (Component comp, Service old, Service replace)
 * (Component comp, Object old, Object replace)
 * (Component comp, ServiceReference old, Service old, ServiceReference replace, Service replace)
 * (Component comp, ServiceReference old, Object old, ServiceReference replace, Object replace)
 * (ServiceReference old, ServiceReference replace)
 * (Component comp, ServiceReference old, ServiceReference replace)
 * }</pre>
 */
public interface AspectComponent extends Component {

	/**
	 * Sets the component scope.
	 * @param scope the component scope (default=SINGLETON)
	 * 
	 * @return this component
	 */
    AspectComponent setScope(ServiceScope scope);
    
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
	AspectComponent setImplementation(Object implementation);

    /**
     * Adds dependency(ies) to this component, atomically. If the component is already active or if you add
     * dependencies from the init method, then you should add all the dependencies in one single add method call 
     * (using the varargs argument), because this method may trigger component activation (like
     * the ServiceTracker.open() method does).
     * 
     * @param dependencies the dependencies to add.
     * @return this component
     */
	AspectComponent add(Dependency ... dependencies);
	
	/**
	 * Removes a dependency from the component.
	 * @param d the dependency to remove
	 * @return this component
	 */
	AspectComponent remove(Dependency d);

    /**
     * Adds a component state listener to this component.
     * 
     * @param listener the state listener
     */
	AspectComponent add(ComponentStateListener listener);

    /**
     * Removes a component state listener from this component.
     * 
     * @param listener the state listener
     */
	AspectComponent remove(ComponentStateListener listener);

    /**
     * Sets the public interface under which this component should be registered
     * in the OSGi service registry.
     *  
     * @param serviceName the name of the service interface
     * @param properties the properties for this service
     * @return this component
     */
	AspectComponent setInterface(String serviceName, Dictionary<?,?> properties);

    /**
     * Sets the public interfaces under which this component should be registered
     * in the OSGi service registry.
     *  
     * @param serviceNames the names of the service interface
     * @param properties the properties for these services
     * @return this component
     */
	AspectComponent setInterface(String[] serviceNames, Dictionary<?, ?> properties);

    /**
     * Sets the public interface under which this component should be registered
     * in the OSGi service registry.
     *  
     * @param serviceName the name of the service interface
     * @param properties the properties for this service
     * @return this component
     */
	AspectComponent setInterface(Class<?> serviceName, Dictionary<?,?> properties);

    /**
     * Sets the public interfaces under which this component should be registered
     * in the OSGi service registry.
     *  
     * @param serviceNames the names of the service interface
     * @param properties the properties for these services
     * @return this component
     */
	AspectComponent setInterface(Class<?>[] serviceNames, Dictionary<?, ?> properties);

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
	AspectComponent setAutoConfig(Class<?> clazz, boolean autoConfig);

    /**
     * Configures auto configuration of injected classes in the component instance.
     * 
     * @param clazz the class (from the list above)
     * @param instanceName the name of the instance to inject the class into
     * @see #setAutoConfig(Class, boolean)
     */
	AspectComponent setAutoConfig(Class<?> clazz, String instanceName);

    /**
     * Sets the service properties associated with the component. If the service
     * was already registered, it will be updated.
     * 
     * @param serviceProperties the properties
     */
	AspectComponent setServiceProperties(Dictionary<?, ?> serviceProperties);

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
	AspectComponent setCallbacks(String init, String start, String stop, String destroy);

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
	AspectComponent setCallbacks(Object instance, String init, String start, String stop, String destroy);

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
	AspectComponent setFactory(Object factory, String createMethod);

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
	AspectComponent setFactory(String createMethod);

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
	AspectComponent setComposition(Object instance, String getMethod);

    /**
     * Sets the method to invoke on the service implementation to get back all
     * instances that are part of a composition and need dependencies injected.
     * All of them will be searched for any of the dependencies. The method that
     * is invoked must return an <code>Object[]</code>.
     * 
     * @param getMethod the method to invoke
     * @return this component
     */
	AspectComponent setComposition(String getMethod);

	/**
	 * Activate debug for this component. Informations related to dependency processing will be displayed
	 * using osgi log service, our to standard output if no log service is currently available.
	 * @param label
	 */
	AspectComponent setDebug(String label);

    /**
     * Sets the service interface to apply the aspect to (required parameter)
     * @param service the service interface to apply the aspect to
     * @param filter the filter condition to use with the service aspect interface (null if no filter)
     * @param ranking  the level used to organize the aspect chain ordering
     * @return this aspect parameter instance
     */
	AspectComponent setAspect(Class<?> service, String filter, int ranking);
    
    /**
     * Sets the aspect implementation field name where to inject original service (optional parameter).
     * If not set or null, any field matching the original service will be injected.
     * @param autoConfig the aspect implementation field name where to inject original service
     * @return this aspect parameter instance
     */
	AspectComponent setAspectField(String autoConfig);

    /**
     * Sets name of the callbacks method to invoke on add,change,remove, or swap callbacks (optional parameter).
     * @param add name of the callback method to invoke on add
     * @param change name of the callback method to invoke on change
     * @param remove name of the callback method to invoke on remove
     * @param swap name of the callback method to invoke on swap
     * @return this aspect parameter instance
     */
	AspectComponent setAspectCallbacks(String add, String change, String remove, String swap);
    
    /**
     * Sets the instance to invoke the callbacks on (optional parameter). 
     * null means the callbacks will be invoked on the aspect implementation object.
     * @param callbackInstance the instance to invoke the callbacks on
     * @return this aspect parameter instance
     */
	AspectComponent setAspectCallbackInstance(Object callbackInstance);

}
