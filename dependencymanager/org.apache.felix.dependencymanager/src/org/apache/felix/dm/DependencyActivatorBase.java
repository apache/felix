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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Base bundle activator class. Subclass this activator if you want to use dependency
 * management in your bundle. There are two methods you should implement:
 * <code>init()</code> and <code>destroy()</code>. Both methods take two arguments,
 * the bundle context and the dependency manager. The dependency manager can be used
 * to define all the dependencies.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class DependencyActivatorBase implements BundleActivator {
    private BundleContext m_context;
    private DependencyManager m_manager;
    private Logger m_logger;
    
    /**
     * Initialize the dependency manager. Here you can add all components and their dependencies.
     * If something goes wrong and you do not want your bundle to be started, you can throw an
     * exception. This exception will be passed on to the <code>start()</code> method of the
     * bundle activator, causing the bundle not to start.
     * 
     * @param context the bundle context
     * @param manager the dependency manager
     * @throws Exception if the initialization fails
     */
    public abstract void init(BundleContext context, DependencyManager manager) throws Exception;
    
    /**
     * Destroy the dependency manager. Here you can remove all components and their dependencies.
     * Actually, the base class will clean up your dependencies anyway, so most of the time you
     * don't need to do anything here.
     * <p>
     * If something goes wrong and you do not want your bundle to be stopped, you can throw an
     * exception. This exception will be passed on to the <code>stop()</code> method of the
     * bundle activator, causing the bundle not to stop.
     * 
     * @param context the bundle context
     * @param manager the dependency manager
     * @throws Exception if the destruction fails
     */
    public void destroy(BundleContext context, DependencyManager manager) throws Exception { }

    /**
     * Start method of the bundle activator. Initializes the dependency manager
     * and calls <code>init()</code>.
     * 
     * @param context the bundle context
     */
    public void start(BundleContext context) throws Exception {
        m_context = context;
        m_logger = new Logger(context);
        m_manager = new DependencyManager(context, m_logger);
        init(m_context, m_manager);
    }

    /**
     * Stop method of the bundle activator. Calls the <code>destroy()</code> method
     * and cleans up all left over dependencies.
     * 
     * @param context the bundle context
     */
    public void stop(BundleContext context) throws Exception {
        destroy(m_context, m_manager);
        m_manager.clear();
        m_manager = null;
        m_context = null;
    }
    
    /**
     * Returns the bundle context that is associated with this bundle.
     * 
     * @return the bundle context
     */
    public BundleContext getBundleContext() {
        return m_context;
    }

    /**
     * Returns the dependency manager that is associated with this bundle.
     * 
     * @return the dependency manager
     */
    public DependencyManager getDependencyManager() {
        return m_manager;
    }
    
    /**
     * Returns the logger that is associated with this bundle. A logger instance
     * is a proxy that will log to a real OSGi logservice if available and standard
     * out if not.
     * 
     * @return the logger
     */
    public Logger getLogger() {
        return m_logger;
    }
    
    /**
     * Creates a new component.
     * 
     * @return the new component
     */
    public Component createComponent() {
        return m_manager.createComponent();
    }
    
    /**
     * Creates a new service dependency.
     * 
     * @return the service dependency
     */
    public ServiceDependency createServiceDependency() {
        return m_manager.createServiceDependency();
    }
    
    /**
     * Creates a new temporal service dependency.
     * 
     * @param timeout the max number of milliseconds to wait for a service availability.
     * @return the service dependency
     */
    public ServiceDependency createTemporalServiceDependency(long timeout) {
        return m_manager.createTemporalServiceDependency(timeout);
    }
    
    /**
     * Creates a new configuration dependency.
     * 
     * @return the configuration dependency
     */
    public ConfigurationDependency createConfigurationDependency() {
    	return m_manager.createConfigurationDependency();
    }
    
    /**
     * Creates a new configuration property metadata.
     * 
     * @return the configuration property metadata
     */
    public PropertyMetaData createPropertyMetaData() {
        return m_manager.createPropertyMetaData();
    }

    /**
     * Creates a new bundle dependency.
     * 
     * @return the bundle dependency
     */
    public BundleDependency createBundleDependency() {
        return m_manager.createBundleDependency();
    }

    /**
     * Creates a new resource dependency.
     * 
     * @return the resource dependency
     */
    public ResourceDependency createResourceDependency() {
        return m_manager.createResourceDependency();
    }

    /**
     * Creates a new aspect service.
     * 
     * @return the aspect service
     * @see DependencyManager#createAspectService(Class, String, int, String)
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking, String attributeName) {
        return m_manager.createAspectService(serviceInterface, serviceFilter, ranking, attributeName);
    }
    
    /**
     * Creates a new aspect service.
     * 
     * @return the aspect service
     * @see DependencyManager#createAspectService(Class, String, int)
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking) {
        return m_manager.createAspectService(serviceInterface, serviceFilter, ranking);
    }
    
    /**
     * Creates a new aspect service.
     * 
     * @return the aspect service
     * @see DependencyManager#createAspectService(Class, String, int, String, String, String)
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking, String add, String change, String remove) {
        return m_manager.createAspectService(serviceInterface, serviceFilter, ranking, add, change, remove);
    }

    /**
     * Creates a new aspect service.
     * 
     * @return the aspect service
     * @see DependencyManager#createAspectService(Class, String, int, String, String, String, String)
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking, String add, String change, String remove, String swap) {    
        return m_manager.createAspectService(serviceInterface, serviceFilter, ranking, add, change, remove, swap);
    }
    	
    /**
     * Creates a new aspect service.
     * 
     * @return the aspect service
     * @see DependencyManager#createAspectService(Class, String, int, Object, String, String, String, String)
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking, Object callbackInstance, String add, String change, String remove, String swap) {    
        return m_manager.createAspectService(serviceInterface, serviceFilter, ranking, callbackInstance, add, change, remove, swap);
    }
        
    /**
     * Creates a new adapter service.
     * 
     * @return the adapter service
     * @see DependencyManager#createAdapterService(Class, String)
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter) {
        return m_manager.createAdapterService(serviceInterface, serviceFilter);
    }
    
    /**
     * Creates a new adapter service.
     * 
     * @return the adapter service
     * @see DependencyManager#createAdapterService(Class, String, String)
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, String autoConfig) {
        return m_manager.createAdapterService(serviceInterface, serviceFilter, autoConfig);
    }
    
    /**
     * Creates a new adapter service.
     * 
     * @return the adapter service
     * @see DependencyManager#createAdapterService(Class, String, String, String, String)
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, String add, String change, String remove) {
        return m_manager.createAdapterService(serviceInterface, serviceFilter, add, change, remove);
    }
    
    /**
     * Creates a new adapter service.
     * @return the adapter service
     * @see DependencyManager#createAdapterService(Class, String, String, String, String, String)
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, String add, String change, String remove, String swap) {
        return m_manager.createAdapterService(serviceInterface, serviceFilter, add, change, remove, swap);
    }  
    
    /**
     * Creates a new adapter service.
     * @return the adapter service
     * @see DependencyManager#createAdapterService(Class, String, String, Object, String, String, String, String, boolean)
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, 
        String autoConfig, Object callbackInstance, String add, String change, String remove, String swap) {
       return m_manager.createAdapterService(serviceInterface, serviceFilter, autoConfig, callbackInstance, add, change, remove, swap, true);
    }

    /**
     * Creates a new adapter service.
     * @return the adapter service
     * @see DependencyManager#createAdapterService(Class, String, String, Object, String, String, String, String, boolean)
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, 
        String autoConfig, Object callbackInstance, String add, String change, String remove, String swap, boolean propagate) {
       return m_manager.createAdapterService(serviceInterface, serviceFilter, autoConfig, callbackInstance, add, change, remove, swap, propagate);
    }

   /**
     * Creates a new resource adapter service.
     * 
     * @return the resource adapter service
     */
    public Component createResourceAdapter(String resourceFilter, boolean propagate, Object callbackInstance, String callbackChanged) {
        return m_manager.createResourceAdapterService(resourceFilter, propagate, callbackInstance, callbackChanged);
    }

    /**
     * Creates a new resource adapter service.
     * 
     * @return the resource adapter service
     */
    public Component createResourceAdapter(String resourceFilter, boolean propagate, Object callbackInstance, String callbackSet, String callbackChanged) {
        return m_manager.createResourceAdapterService(resourceFilter, propagate, callbackInstance, callbackSet, callbackChanged);
    }
    
    /**
     * Creates a new resource adapter service.
     * 
     * @return the resource adapter service
     */
    public Component createResourceAdapter(String resourceFilter, Object propagateCallbackInstance, String propagateCallbackMethod, Object callbackInstance, String callbackChanged) {
        return m_manager.createResourceAdapterService(resourceFilter, propagateCallbackInstance, propagateCallbackMethod, callbackInstance, null, callbackChanged);
    }
    
    /**
     * Creates a new resource adapter service.
     * 
     * @return the resource adapter service
     */
    public Component createResourceAdapter(String resourceFilter, Object propagateCallbackInstance, String propagateCallbackMethod, Object callbackInstance, String callbackSet, String callbackChanged) {
        return m_manager.createResourceAdapterService(resourceFilter, propagateCallbackInstance, propagateCallbackMethod, callbackInstance, callbackSet, callbackChanged);
    }
    
    /**
     * Creates a new bundle adapter service.
     * 
     * @return the bundle adapter service
     */
    public Component createBundleAdapterService(int bundleStateMask, String bundleFilter, boolean propagate) {
        return m_manager.createBundleAdapterService(bundleStateMask, bundleFilter, propagate);
    }

    /**
     * Creates a new bundle adapter service, using a specific callback instance
     * 
     * @return the bundle adapter service
     */
    public Component createBundleAdapterService(int bundleStateMask, String bundleFilter, boolean propagate,
    		Object callbackInstance, String add, String change, String remove) {
        return m_manager.createBundleAdapterService(bundleStateMask, bundleFilter, propagate, callbackInstance, add, change, remove);
    }

    /**
     * Creates a new factory configuration adapter service.
     * 
     * @return the factory configuration adapter service
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate) {
        return m_manager.createFactoryConfigurationAdapterService(factoryPid, update, propagate);
    }
    
    /**
     * Creates a new factory configuration adapter service, using a specific callback instance
     * 
     * @return the factory configuration adapter service
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate, Object callbackInstance) {
        return m_manager.createFactoryConfigurationAdapterService(factoryPid, update, propagate, callbackInstance);
    }
  
    /**
     * Creates a new factory configuration adapter service, using a specific callback instance
     * 
     * @return the factory configuration adapter service
     * @see DependencyManager#createFactoryConfigurationAdapterService(String, String, boolean, Class)
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate, Class<?> configType) {
        return m_manager.createFactoryConfigurationAdapterService(factoryPid, update, propagate, configType);
    }

    /**
     * Creates a new factory configuration adapter service, using a specific callback instance
     * 
     * @return the factory configuration adapter service
     * @see DependencyManager#createFactoryConfigurationAdapterService(String, String, boolean, Object, Class)
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate, Object callbackInstance, Class<?> configType) {
        return m_manager.createFactoryConfigurationAdapterService(factoryPid, update, propagate, callbackInstance, configType);
    }

    /**
     * Creates a new factory configuration adapter service.
     * 
     * @return the factory configuration adapter service
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate, String heading, String desc, String localization, PropertyMetaData[] propertiesMetaData) {
        return m_manager.createAdapterFactoryConfigurationService(factoryPid, update, propagate, heading, desc, localization, propertiesMetaData);
    }
}
