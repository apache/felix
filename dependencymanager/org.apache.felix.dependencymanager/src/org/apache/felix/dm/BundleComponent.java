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
 * a Dependency Manager bundle adapter component.
 * 
 * Bundle Adapters, like {@link AdapterComponent}, are used to "extend" 
 * existing bundles, and can publish an adapter services based on the existing bundle. 
 * An example would be implementing a video player which adapters a resource bundle having 
 * some specific headers. 
 * <p>When you create a bundle adapter component, it will be applied 
 * to any bundle that matches the specified bundle state mask as well as the specified ldap filter
 * used to match the bundle manifest headers. The bundle adapter will be registered 
 * with the specified bundle manifest headers as service properties, plus any extra 
 * properties you suppl. If you declare a bundle field in your bundle adapter class, 
 * it will be injected it will be injected with the original bundle.
 * 
 * <h3>Usage Examples</h3>
 * 
 * Here is a sample showing a VideoPlayer adapter component which plays a video found from 
 * a bundle having a Video-Path manifest header.
 * 
 * <blockquote><pre>
 * {@code
 * public class Activator extends DependencyActivatorBase {
 *     &Override
 *     public void init(BundleContext context, DependencyManager dm) throws Exception {
 *         BundleComponent bundleComponent = createBundleComponent()
 *             .setFilter(Bundle.ACTIVE, "(Video-Path=*)")
 *             .setInterface(VideoPlayer.class.getName(), null)
 *             .setImplementation(VideoPlayerImpl.class);
 *         dm.add(bundleComponent);
 *     }
 * }
 * 
 * public interface VideoPlayer {
 *     void play();
 * }
 * 
 * public class VideoPlayerImpl implements VideoPlayer {
 *     volatile Bundle bundle; // injected
 *     String path;
 *     
 *     void start() {
 *        path = bundle.getHeaders().get("Video-Path");
 *     }
 *     
 *     void play() {
 *         ...
 *     }
 * }
 * } </pre></blockquote>
 * 
 * <p> When you use callbacks to get injected with the bundle, the "add", "change", "remove" callbacks
 * support the following method signatures:
 * 
 * <pre>{@code
 * (Bundle)
 * (Object)
 * (COmponent, Bundle)
 * }</pre>
 * 
 * @see DependencyManager#createBundleComponent()
 */
public interface BundleComponent extends Component {
    
	/**
	 * Sets the component scope.
	 * @param scope the component scope (default=SINGLETON)
	 * 
	 * @return this component
	 */
    BundleComponent setScope(ServiceScope scope);
    
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
	BundleComponent setImplementation(Object implementation);

    /**
     * Adds dependency(ies) to this component, atomically. If the component is already active or if you add
     * dependencies from the init method, then you should add all the dependencies in one single add method call 
     * (using the varargs argument), because this method may trigger component activation (like
     * the ServiceTracker.open() method does).
     * 
     * @param dependencies the dependencies to add.
     * @return this component
     */
	BundleComponent add(Dependency ... dependencies);
	
	/**
	 * Removes a dependency from the component.
	 * @param d the dependency to remove
	 * @return this component
	 */
	BundleComponent remove(Dependency d);

    /**
     * Adds a component state listener to this component.
     * 
     * @param listener the state listener
     */
	BundleComponent add(ComponentStateListener listener);

    /**
     * Removes a component state listener from this component.
     * 
     * @param listener the state listener
     */
	BundleComponent remove(ComponentStateListener listener);

    /**
     * Sets the public interface under which this component should be registered
     * in the OSGi service registry.
     *  
     * @param serviceName the name of the service interface
     * @param properties the properties for this service
     * @return this component
     */
	BundleComponent setInterface(String serviceName, Dictionary<?,?> properties);

    /**
     * Sets the public interfaces under which this component should be registered
     * in the OSGi service registry.
     *  
     * @param serviceNames the names of the service interface
     * @param properties the properties for these services
     * @return this component
     */
	BundleComponent setInterface(String[] serviceNames, Dictionary<?, ?> properties);

    /**
     * Sets the public interface under which this component should be registered
     * in the OSGi service registry.
     *  
     * @param serviceName the name of the service interface
     * @param properties the properties for this service
     * @return this component
     */
	BundleComponent setInterface(Class<?> serviceName, Dictionary<?,?> properties);

    /**
     * Sets the public interfaces under which this component should be registered
     * in the OSGi service registry.
     *  
     * @param serviceNames the names of the service interface
     * @param properties the properties for these services
     * @return this component
     */
	BundleComponent setInterface(Class<?>[] serviceNames, Dictionary<?, ?> properties);

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
	BundleComponent setAutoConfig(Class<?> clazz, boolean autoConfig);

    /**
     * Configures auto configuration of injected classes in the component instance.
     * 
     * @param clazz the class (from the list above)
     * @param instanceName the name of the instance to inject the class into
     * @see #setAutoConfig(Class, boolean)
     */
	BundleComponent setAutoConfig(Class<?> clazz, String instanceName);

    /**
     * Sets the service properties associated with the component. If the service
     * was already registered, it will be updated.
     * 
     * @param serviceProperties the properties
     */
	BundleComponent setServiceProperties(Dictionary<?, ?> serviceProperties);

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
	BundleComponent setCallbacks(String init, String start, String stop, String destroy);

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
	BundleComponent setCallbacks(Object instance, String init, String start, String stop, String destroy);

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
	BundleComponent setFactory(Object factory, String createMethod);

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
	BundleComponent setFactory(String createMethod);

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
	BundleComponent setComposition(Object instance, String getMethod);

    /**
     * Sets the method to invoke on the service implementation to get back all
     * instances that are part of a composition and need dependencies injected.
     * All of them will be searched for any of the dependencies. The method that
     * is invoked must return an <code>Object[]</code>.
     * 
     * @param getMethod the method to invoke
     * @return this component
     */
	BundleComponent setComposition(String getMethod);

	/**
	 * Activate debug for this component. Informations related to dependency processing will be displayed
	 * using osgi log service, our to standard output if no log service is currently available.
	 * @param label
	 */
	BundleComponent setDebug(String label);

	/**
     * Sets the bundle state mask and bundle manifest headers filter.
     * 
     * @param bundleStateMask the bundle state mask to apply
     * @param bundleFilter the filter to apply to the bundle manifest
     * @return this BundleComponent
     */
	BundleComponent setBundleFilter(int bundleStateMask, String bundleFilter);
            
    /**
     * Sets the callbacks to invoke when injecting the bundle into the adapter component.
     * 
     * @param add name of the callback method to invoke on add
     * @param change name of the callback method to invoke on change
     * @param remove name of the callback method to invoke on remove
     * @return this BundleComponent
     */
	BundleComponent setBundleCallbacks(String add, String change, String remove);
    
    /**
     * Sets the instance to invoke the callbacks on (null by default, meaning the callbacks have to be invoked on the adapter itself)
     * 
     * @param callbackInstance the instance to invoke the callbacks on (null by default, meaning the callbacks have to be invoked on the adapter itself)
     * @return this BundleComponent
     */
	BundleComponent setBundleCallbackInstance(Object callbackInstance);

    /**
     * Sets if the bundle manifest headers should be propagated to the bundle component adapter service consumer (true by default).
     * The component service properties take precedence over the propagated bundle manifest headers.
     * @param propagate true if the bundle manifest headers should be propagated to the adapter service consumers
     * @return this BundleComponent
     */
	BundleComponent setPropagate(boolean propagate);
    
}
