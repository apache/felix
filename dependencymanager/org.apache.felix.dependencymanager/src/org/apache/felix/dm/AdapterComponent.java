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
 * a Dependency Manager adapter component.
 * 
 * Adapters, like {@link AspectComponent}, are used to "extend" 
 * existing services, and can publish different services based on the existing one. 
 * An example would be implementing a management interface for an existing service, etc .... 
 * <p>When you create an adapter component, it will be applied 
 * to any service that matches the implemented interface and filter. The adapter will be registered 
 * with the specified interface and existing properties from the original service plus any extra 
 * properties you supply here. If you declare the original service as a member it will be injected.
 * 
 * <h3>Usage Examples</h3>
 * 
 * Here is a sample showing a HelloServlet adapter component which creates a servlet each time a HelloService is registered in the
 * osgi service registry with the "foo=bar" service property.
 * 
 * <blockquote><pre>
 * {@code
 * public class Activator extends DependencyActivatorBase {
 *     &Override
 *     public void init(BundleContext context, DependencyManager dm) throws Exception {
 *         AdapterComponent adapterComponent = createAdapterComponent()
 *             .setAdaptee(HelloService.class, "(foo=bar)")
 *             .setInterface(HttpServlet.class.getName(), null)
 *             .setImplementation(HelloServlet.class);
 *         dm.add(adapterComponent);
 *     }
 * }
 * 
 * public interface HelloService {
 *     String sayHello();
 * }
 * 
 * public class HelloServlet extends HttpServlet {
 *     volatile HelloService adatpee; // injected
 *     
 *     void doGet(HttpServletRequest req, HttpServletResponse resp) {
 *         ...
 *         resp.getWriter().println(adaptee.sayHello());
 *     }
 * }
 * } </pre></blockquote>
 * 
 * <p> When you use callbacks to get injected with the adaptee service, the "add", "change", "remove" callbacks
 * support the following method signatures:
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
 * 
 * @see DependencyManager#createAdapterComponent()
 */
public interface AdapterComponent extends Component {
    
	/**
	 * Sets the component scope.
	 * @param scope the component scope (default=SINGLETON)
	 * 
	 * @return this component
	 */
    AdapterComponent setScope(ServiceScope scope);
    
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
	AdapterComponent setImplementation(Object implementation);

    /**
     * Adds dependency(ies) to this component, atomically. If the component is already active or if you add
     * dependencies from the init method, then you should add all the dependencies in one single add method call 
     * (using the varargs argument), because this method may trigger component activation (like
     * the ServiceTracker.open() method does).
     * 
     * @param dependencies the dependencies to add.
     * @return this component
     */
	AdapterComponent add(Dependency ... dependencies);
	
	/**
	 * Removes a dependency from the component.
	 * @param d the dependency to remove
	 * @return this component
	 */
	AdapterComponent remove(Dependency d);

    /**
     * Adds a component state listener to this component.
     * 
     * @param listener the state listener
     */
	AdapterComponent add(ComponentStateListener listener);

    /**
     * Removes a component state listener from this component.
     * 
     * @param listener the state listener
     */
	AdapterComponent remove(ComponentStateListener listener);

    /**
     * Sets the public interface under which this component should be registered
     * in the OSGi service registry.
     *  
     * @param serviceName the name of the service interface
     * @param properties the properties for this service
     * @return this component
     */
	AdapterComponent setInterface(String serviceName, Dictionary<?,?> properties);

    /**
     * Sets the public interfaces under which this component should be registered
     * in the OSGi service registry.
     *  
     * @param serviceNames the names of the service interface
     * @param properties the properties for these services
     * @return this component
     */
	AdapterComponent setInterface(String[] serviceNames, Dictionary<?, ?> properties);

    /**
     * Sets the public interface under which this component should be registered
     * in the OSGi service registry.
     *  
     * @param serviceName the name of the service interface
     * @param properties the properties for this service
     * @return this component
     */
	AdapterComponent setInterface(Class<?> serviceName, Dictionary<?,?> properties);

    /**
     * Sets the public interfaces under which this component should be registered
     * in the OSGi service registry.
     *  
     * @param serviceNames the names of the service interface
     * @param properties the properties for these services
     * @return this component
     */
	AdapterComponent setInterface(Class<?>[] serviceNames, Dictionary<?, ?> properties);

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
	AdapterComponent setAutoConfig(Class<?> clazz, boolean autoConfig);

    /**
     * Configures auto configuration of injected classes in the component instance.
     * 
     * @param clazz the class (from the list above)
     * @param instanceName the name of the instance to inject the class into
     * @see #setAutoConfig(Class, boolean)
     */
	AdapterComponent setAutoConfig(Class<?> clazz, String instanceName);

    /**
     * Sets the service properties associated with the component. If the service
     * was already registered, it will be updated.
     * 
     * @param serviceProperties the properties
     */
	AdapterComponent setServiceProperties(Dictionary<?, ?> serviceProperties);

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
	AdapterComponent setCallbacks(String init, String start, String stop, String destroy);

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
	AdapterComponent setCallbacks(Object instance, String init, String start, String stop, String destroy);

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
	AdapterComponent setFactory(Object factory, String createMethod);

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
	AdapterComponent setFactory(String createMethod);

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
	AdapterComponent setComposition(Object instance, String getMethod);

    /**
     * Sets the method to invoke on the service implementation to get back all
     * instances that are part of a composition and need dependencies injected.
     * All of them will be searched for any of the dependencies. The method that
     * is invoked must return an <code>Object[]</code>.
     * 
     * @param getMethod the method to invoke
     * @return this component
     */
	AdapterComponent setComposition(String getMethod);

	/**
	 * Activate debug for this component. Informations related to dependency processing will be displayed
	 * using osgi log service, our to standard output if no log service is currently available.
	 * @param label
	 */
	AdapterComponent setDebug(String label);
		
    /**
     * Sets the service interface to apply the adapter to
     * @param service the service interface to apply the adapter to
     * @param filter the filter condition to use with the service interface
     * @return this adapter parameter instance
     */
	AdapterComponent setAdaptee(Class<?> service, String filter);
        
    /**
     * Sets the name of the member to inject the service into
     * @param autoConfig the name of the member to inject the service into
     * @return this adapter parameter instance
     */
	AdapterComponent setAdapteeField(String autoConfig);
    
    /**
     * Sets the callbacks to invoke when injecting the adaptee service into the adapter component.
     * @param add name of the callback method to invoke on add
     * @param change name of the callback method to invoke on change
     * @param remove name of the callback method to invoke on remove
     * @param swap name of the callback method to invoke on swap
     * @return this adapter parameter instance
     */
	AdapterComponent setAdapteeCallbacks(String add, String change, String remove, String swap);
    
    /**
     * Sets the instance to invoke the callbacks on (null by default, meaning the callbacks have to be invoked on the adapter itself)
     * @param callbackInstance the instance to invoke the callbacks on (null by default, meaning the callbacks have to be invoked on the adapter itself)
     * @return this adapter parameter instance
     */
	AdapterComponent setAdapteeCallbackInstance(Object callbackInstance);

    /**
     * Sets if the adaptee service properties should be propagated to the adapter service consumer (true by default)
     * @param propagate true if the adaptee service properties should be propagated to the adapter service consumers.
     * The provided adapter service properties take precedence over the propagated adaptee service properties. 
     * It means an adaptee service property won't override an adapter service property having the same name.
     * 
     * @return this adapter parameter instance
     */
	AdapterComponent setPropagate(boolean propagate);

}
