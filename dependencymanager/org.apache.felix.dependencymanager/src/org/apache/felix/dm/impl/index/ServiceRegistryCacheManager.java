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
package org.apache.felix.dm.impl.index;

import java.lang.reflect.Constructor;
import java.util.function.Consumer;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.FilterIndex;
import org.apache.felix.dm.impl.index.multiproperty.MultiPropertyFilterIndex;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

/**
 * This class manages the service registry cache creation.
 */
public class ServiceRegistryCacheManager {
	
	/**
	 * The Service Registry cache, which is created if you specify the "org.apache.felix.dependencymanager.filterindex" system property
	 * or if you register as a service a FilterIndex in the service registry.
	 */
	private static volatile ServiceRegistryCache m_cache;
	
	/**
	 * Backdoor only used by tests: a Consumer<String> is added in system properties using the following key and 
	 * when tests invoke it, the cache is reintialized as if the JVM would have been restarted.
	 */
	private final static String RESET = "org.apache.felix.dependencymanager.filterindex.reset";
	
	/**
	 * the DependendencyManager bundle context used by the ServiceRegistryCache.
	 */
	private static volatile BundleContext m_context;
	
	/**
	 * Boolean used to check if we are already initialized
	 */
	private static volatile boolean m_init = false;
	
	/**
	 * Static initializer: we create the registry cache in case "org.apache.felix.dependencymanager.filterindex" system property is configured.
	 */
	static {
		init();
	}
	
	/**
	 * Gets the service registry cache, if enabled.
	 * @return the service registry cache, if enabled, or null
	 */
	public static ServiceRegistryCache getCache() {
		return m_cache;
	}
	
	/**
	 * Registers a FilterIndex into the service registry cache. The cache is created if necessary.
	 * @param index a new FilterIndex to be registered in the cache
	 * @param context the bundle context used by the cache
	 */
	public static void registerFilterIndex(FilterIndex index, BundleContext context) {
		ServiceRegistryCache cache = createCache(context);
		cache.addFilterIndex(index);
	}
	
	/**
	 * Unregister a FilterIndex from the cache, and possibly close the cache in case it becomes empty.
	 * @param index the FilterIndex to unregister
	 */
	public static void unregisterFilterIndex(FilterIndex index) {
		ServiceRegistryCache cache = ServiceRegistryCacheManager.getCache();
		if (cache != null) {
			cache.removeFilterIndex(index);
			boolean close = false;
			synchronized (ServiceRegistryCacheManager.class) {
				if (cache.getSize() == 0) {
					m_cache = null;
					close = true;
				}				
			}
			if (close) {
				cache.close();
			}
		}
	}
	
	/**
	 * Creates the cache if it does not exist.
	 * @param context the bundle context that will be used by the case
	 * @return the created cache (or the existing cache)
	 */
	private static ServiceRegistryCache createCache(BundleContext context) {
		ServiceRegistryCache cache = null;
		boolean open = false;
		synchronized (ServiceRegistryCacheManager.class) {
			if (m_cache == null) {
				m_cache = new ServiceRegistryCache(context);
				open = true;
			}
			cache = m_cache;
		}
		if (open) {
			cache.open();
		}
		return cache;
	}
	
	/**
	 * Initialize the service registry cache.
	 */
	public static void init() {
		try {
			if (m_init) {
				return;
			}
			Bundle bundle = FrameworkUtil.getBundle(ServiceRegistryCacheManager.class);
			if (bundle != null) {
				if (bundle.getState() != Bundle.STARTING && bundle.getState() != Bundle.ACTIVE) {
					bundle.start(); // take care: may callback our registerFilterIndex method 
				}
				m_context = bundle.getBundleContext();
				String index = m_context.getProperty(DependencyManager.SERVICEREGISTRY_CACHE_INDICES);
				if (index != null) {
					resetIndices(index);
				}
			}
			Consumer<String> reset = ServiceRegistryCacheManager::reset;
			System.getProperties().put(RESET, reset);	
			m_init = true;
		}

		catch (BundleException e) {
			// if we cannot start ourselves, we cannot use the indices
			e.printStackTrace();
		}				
	}
		
	private static void resetIndices(String index) {
		try {
			if (index != null) {
				ServiceRegistryCache cache = createCache(m_context); // may already exist, in case the Activator has called back our registerFilterIndex method
					
				String[] props = index.split(";");
				for (int i = 0; i < props.length; i++) {
					// Check if a classname is specified for the current index.
					// (syntax is "full_class_name:index")
					int colon = props[i].indexOf(":");
					if (colon != -1) {
						String className = props[i].substring(0, colon).trim();
						String indexDefinition = props[i].substring(colon + 1);
						cache.addFilterIndex(createCustomIndex(className, indexDefinition));
					} else if (props[i].equals("*aspect*")) {
						cache.addFilterIndex(new AspectFilterIndex());
					} else if (props[i].equals("*adapter*")) {
						cache.addFilterIndex(new AdapterFilterIndex());
					} else {
						cache.addFilterIndex(new MultiPropertyFilterIndex(props[i]));
					}
				}
			}			
		}

		catch (Exception e) {
			e.printStackTrace();
		}				
	}
		
	/**
	 * Creates a custom index using its classname, that has been specified in the org.apache.felix.dependencymanager.filterindex system property.
	 */
	private static FilterIndex createCustomIndex(String className, String indexDefinition) {
		try {
			Class<?> customIndexClass = Class.forName(className);
			Constructor<?> ctor = customIndexClass.getConstructor(String.class);
			FilterIndex index = (FilterIndex) ctor.newInstance(indexDefinition);
			return index;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Reinitialize the cache. Only called by tests. This method is registered in the system properties in the form of a Runnable,
	 * with key=ServiceRegistryCacheManager.RESET.
	 */
	private static void reset(String indices) {
		if (m_cache != null) {
			for (FilterIndex index : m_cache.getFilterIndices()) {
				index.close();
			}
			m_cache.close();
		}
		m_cache = null;
		resetIndices(indices);		
	}

}
