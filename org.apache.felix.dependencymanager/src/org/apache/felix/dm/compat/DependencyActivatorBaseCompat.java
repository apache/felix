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
package org.apache.felix.dm.compat;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.PropertyMetaData;

/**
 * This class contains some methods which have been deprecated from the DependencyActivatorBase class.
 */
public abstract class DependencyActivatorBaseCompat {
	private DependencyManager m_manager;

	protected void setDependencyManager(DependencyManager manager) {
		m_manager = manager;
	}
	
    /**
     * Creates a new aspect service.
     * 
     * @return the aspect service
     * @see DependencyManager#createAspectService(Class, String, int, String)
     * @deprecated use {@link DependencyActivatorBase#createAspectComponent()}
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking, String attributeName) {
        return m_manager.createAspectService(serviceInterface, serviceFilter, ranking, attributeName);
    }

    /**
     * Creates a new aspect service.
     * 
     * @return the aspect service
     * @see DependencyManager#createAspectService(Class, String, int)
     * @deprecated use {@link DependencyActivatorBase#createAspectComponent()}
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking) {
        return m_manager.createAspectService(serviceInterface, serviceFilter, ranking);
    }
    
    /**
     * Creates a new aspect service.
     * 
     * @return the aspect service
     * @see DependencyManager#createAspectService(Class, String, int, String, String, String)
     * @deprecated use {@link DependencyActivatorBase#createAspectComponent()}
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking, String add, String change, String remove) {
        return m_manager.createAspectService(serviceInterface, serviceFilter, ranking, add, change, remove);
    }

    /**
     * Creates a new aspect service.
     * 
     * @return the aspect service
     * @see DependencyManager#createAspectService(Class, String, int, String, String, String, String)
     * @deprecated use {@link DependencyActivatorBase#createAspectComponent()}
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking, String add, String change, String remove, String swap) {    
        return m_manager.createAspectService(serviceInterface, serviceFilter, ranking, add, change, remove, swap);
    }
    	
    /**
     * Creates a new aspect service.
     * 
     * @return the aspect service
     * @see DependencyManager#createAspectService(Class, String, int, Object, String, String, String, String)
     * @deprecated use {@link DependencyActivatorBase#createAspectComponent()}
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking, Object callbackInstance, String add, String change, String remove, String swap) {    
        return m_manager.createAspectService(serviceInterface, serviceFilter, ranking, callbackInstance, add, change, remove, swap);
    }
       
    /**
     * Creates a new adapter service.
     * 
     * @return the adapter service
     * @see DependencyManager#createAdapterService(Class, String)
     * @deprecated use {@link DependencyActivatorBase#createAdapterComponent()}
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter) {
        return m_manager.createAdapterService(serviceInterface, serviceFilter);
    }
    
    /**
     * Creates a new adapter service.
     * 
     * @return the adapter service
     * @see DependencyManager#createAdapterService(Class, String, String)
     * @deprecated use {@link DependencyActivatorBase#createAdapterComponent()}
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, String autoConfig) {
        return m_manager.createAdapterService(serviceInterface, serviceFilter, autoConfig);
    }
    
    /**
     * Creates a new adapter service.
     * 
     * @return the adapter service
     * @see DependencyManager#createAdapterService(Class, String, String, String, String)
     * @deprecated use {@link DependencyActivatorBase#createAdapterComponent()}
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, String add, String change, String remove) {
        return m_manager.createAdapterService(serviceInterface, serviceFilter, add, change, remove);
    }
    
    /**
     * Creates a new adapter service.
     * @return the adapter service
     * @see DependencyManager#createAdapterService(Class, String, String, String, String, String)
     * @deprecated use {@link DependencyActivatorBase#createAdapterComponent()}
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, String add, String change, String remove, String swap) {
        return m_manager.createAdapterService(serviceInterface, serviceFilter, add, change, remove, swap);
    }  
    
    /**
     * Creates a new adapter service.
     * @return the adapter service
     * @see DependencyManager#createAdapterService(Class, String, String, Object, String, String, String, String, boolean)
     * @deprecated use {@link DependencyActivatorBase#createAdapterComponent()}
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, 
        String autoConfig, Object callbackInstance, String add, String change, String remove, String swap) {
       return m_manager.createAdapterService(serviceInterface, serviceFilter, autoConfig, callbackInstance, add, change, remove, swap, true);
    }

    /**
     * Creates a new adapter service.
     * @return the adapter service
     * @see DependencyManager#createAdapterService(Class, String, String, Object, String, String, String, String, boolean)
     * @deprecated use {@link DependencyActivatorBase#createAdapterComponent()}
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, 
        String autoConfig, Object callbackInstance, String add, String change, String remove, String swap, boolean propagate) {
       return m_manager.createAdapterService(serviceInterface, serviceFilter, autoConfig, callbackInstance, add, change, remove, swap, propagate);
    }

    /**
     * Creates a new factory configuration adapter service.
     * 
     * @return the factory configuration adapter service
     * @deprecated use {@link DependencyActivatorBase#createFactoryComponent()}
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate) {
        return m_manager.createFactoryConfigurationAdapterService(factoryPid, update, propagate);
    }
    
    /**
     * Creates a new factory configuration adapter service, using a specific callback instance
     * 
     * @return the factory configuration adapter service
     * @deprecated use {@link DependencyActivatorBase#createFactoryComponent()}
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate, Object callbackInstance) {
        return m_manager.createFactoryConfigurationAdapterService(factoryPid, update, propagate, callbackInstance);
    }
  
    /**
     * Creates a new factory configuration adapter service, using a specific callback instance
     * 
     * @return the factory configuration adapter service
     * @see DependencyManager#createFactoryConfigurationAdapterService(String, String, boolean, Class)
     * @deprecated use {@link DependencyActivatorBase#createFactoryComponent()}
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate, Class<?> configType) {
        return m_manager.createFactoryConfigurationAdapterService(factoryPid, update, propagate, configType);
    }

    /**
     * Creates a new factory configuration adapter service, using a specific callback instance
     * 
     * @return the factory configuration adapter service
     * @see DependencyManager#createFactoryConfigurationAdapterService(String, String, boolean, Object, Class)
     * @deprecated use {@link DependencyActivatorBase#createFactoryComponent()}
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate, Object callbackInstance, Class<?> configType) {
        return m_manager.createFactoryConfigurationAdapterService(factoryPid, update, propagate, callbackInstance, configType);
    }
    
    /**
     * Creates a new factory configuration adapter service.
     * 
     * @return the factory configuration adapter service
     * @deprecated use {@link DependencyActivatorBase#createFactoryComponent()}
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate, String heading, String desc, String localization, PropertyMetaData[] propertiesMetaData) {
        return m_manager.createAdapterFactoryConfigurationService(factoryPid, update, propagate, heading, desc, localization, propertiesMetaData);
    }

    /**
     * Creates a new bundle adapter service.
     * 
     * @return the bundle adapter service
     * @deprecated use {@link DependencyActivatorBase#createBundleComponent()}
     */
    public Component createBundleAdapterService(int bundleStateMask, String bundleFilter, boolean propagate) {
        return m_manager.createBundleAdapterService(bundleStateMask, bundleFilter, propagate);
    }

    /**
     * Creates a new bundle adapter service, using a specific callback instance
     * 
     * @return the bundle adapter service
     * @deprecated use {@link DependencyActivatorBase#createBundleComponent()}
     */
    public Component createBundleAdapterService(int bundleStateMask, String bundleFilter, boolean propagate,
    		Object callbackInstance, String add, String change, String remove) {
        return m_manager.createBundleAdapterService(bundleStateMask, bundleFilter, propagate, callbackInstance, add, change, remove);
    }
    
    /**
     * Creates a new resource adapter service.
     * 
     * @return the resource adapter service
     * @deprecated use {@link DependencyActivatorBase#createResourceComponent()}
     */
    public Component createResourceAdapter(String resourceFilter, boolean propagate, Object callbackInstance, String callbackChanged) {
        return m_manager.createResourceAdapterService(resourceFilter, propagate, callbackInstance, callbackChanged);
    }

    /**
     * Creates a new resource adapter service.
     * 
     * @return the resource adapter service
     * @deprecated use {@link DependencyActivatorBase#createResourceComponent()}
     */
    public Component createResourceAdapter(String resourceFilter, boolean propagate, Object callbackInstance, String callbackSet, String callbackChanged) {
        return m_manager.createResourceAdapterService(resourceFilter, propagate, callbackInstance, callbackSet, callbackChanged);
    }
    
    /**
     * Creates a new resource adapter service.
     * 
     * @return the resource adapter service
     * @deprecated use {@link DependencyActivatorBase#createResourceComponent()}
     */
    public Component createResourceAdapter(String resourceFilter, Object propagateCallbackInstance, String propagateCallbackMethod, Object callbackInstance, String callbackChanged) {
        return m_manager.createResourceAdapterService(resourceFilter, propagateCallbackInstance, propagateCallbackMethod, callbackInstance, null, callbackChanged);
    }
    
    /**
     * Creates a new resource adapter service.
     * 
     * @return the resource adapter service
     * @deprecated use {@link DependencyActivatorBase#createResourceComponent()}
     */
    public Component createResourceAdapter(String resourceFilter, Object propagateCallbackInstance, String propagateCallbackMethod, Object callbackInstance, String callbackSet, String callbackChanged) {
        return m_manager.createResourceAdapterService(resourceFilter, propagateCallbackInstance, propagateCallbackMethod, callbackInstance, callbackSet, callbackChanged);
    }

}
