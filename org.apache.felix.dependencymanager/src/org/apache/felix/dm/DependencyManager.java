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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.dm.compat.DependencyManagerCompat;
import org.apache.felix.dm.impl.AdapterServiceImpl;
import org.apache.felix.dm.impl.AspectServiceImpl;
import org.apache.felix.dm.impl.BundleAdapterImpl;
import org.apache.felix.dm.impl.BundleDependencyImpl;
import org.apache.felix.dm.impl.ComponentImpl;
import org.apache.felix.dm.impl.ComponentScheduler;
import org.apache.felix.dm.impl.ConfigurationDependencyImpl;
import org.apache.felix.dm.impl.FactoryConfigurationAdapterImpl;
import org.apache.felix.dm.impl.ResourceAdapterImpl;
import org.apache.felix.dm.impl.ResourceDependencyImpl;
import org.apache.felix.dm.impl.ServiceDependencyImpl;
import org.apache.felix.dm.impl.TemporalServiceDependencyImpl;
import org.apache.felix.dm.impl.index.ServiceRegistryCache;
import org.apache.felix.dm.impl.index.ServiceRegistryCacheManager;
import org.apache.felix.dm.impl.metatype.PropertyMetaDataImpl;
import org.osgi.framework.BundleContext;

/**
 * The dependency manager manages all components and their dependencies. Using 
 * this API you can declare all components and their dependencies. Under normal
 * circumstances, you get passed an instance of this class through the
 * <code>DependencyActivatorBase</code> subclass you use as your
 * <code>BundleActivator</code>, but it is also possible to create your
 * own instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DependencyManager extends DependencyManagerCompat {    
    /**
     * The DependencyManager Activator will wait for a threadpool before creating any DM components if the following
     * OSGi system property is set to true.
     */
    public final static String PARALLEL = "org.apache.felix.dependencymanager.parallel";

    public static final String ASPECT = "org.apache.felix.dependencymanager.aspect";
    public static final String SERVICEREGISTRY_CACHE_INDICES = "org.apache.felix.dependencymanager.filterindex";
    public static final String METHOD_CACHE_SIZE = "org.apache.felix.dependencymanager.methodcache";
    
    private final BundleContext m_context;
    private final Logger m_logger;
    private final ConcurrentHashMap<Component, Component> m_components = new ConcurrentHashMap<>();

    private static final Set<WeakReference<DependencyManager>> m_dependencyManagers = new HashSet<>();
    
    /**
     * Creates a new dependency manager. You need to supply the
     * <code>BundleContext</code> to be used by the dependency
     * manager to register services and communicate with the 
     * framework.
     * 
     * @param context the bundle context
     */
    public DependencyManager(BundleContext context) {
        this(context, new Logger(context));
    }

    DependencyManager(BundleContext context, Logger logger) {
        m_context = createContext(context);
        m_logger = logger;
        synchronized (m_dependencyManagers) {
            m_dependencyManagers.add(new WeakReference<DependencyManager>(this));
        }
    }
    
    public Logger getLogger() {
        return m_logger;
    }
    
    /**
     * Returns the list of currently created dependency managers.
     * @return the list of currently created dependency managers
     */
    public static List<DependencyManager> getDependencyManagers() {
        List<DependencyManager> result = new ArrayList<>();
        synchronized (m_dependencyManagers) {
            Iterator<WeakReference<DependencyManager>> iterator = m_dependencyManagers.iterator();
            while (iterator.hasNext()) {
                WeakReference<DependencyManager> reference = iterator.next();
                DependencyManager manager = reference.get();
                if (manager != null) {
                    try {
                        manager.getBundleContext().getBundle();
                        result.add(manager);
                        continue;
                    }
                    catch (IllegalStateException e) {
                    }
                }
                iterator.remove();
            }
        }
        return result;
    }

    /**
     * Returns the bundle context associated with this dependency manager.
     * @return the bundle context associated with this dependency manager.
     */
    public BundleContext getBundleContext() {
        return m_context;
    }

    /**
     * Adds a new component to the dependency manager. After the service is added
     * it will be started immediately.
     * 
     * @param c the service to add
     */
    public void add(Component c) {
        m_components.put(c, c);
        ComponentScheduler.instance().add(c);
    }

    /**
     * Removes a service from the dependency manager. Before the service is removed
     * it is stopped first.
     * 
     * @param c the component to remove
     */
    public void remove(Component c) {
        ComponentScheduler.instance().remove(c);
        m_components.remove(c);
    }

    /**
     * Creates a new component.
     * 
     * @return the new component
     */
    public Component<?> createComponent() {
        return new ComponentImpl(m_context, this, m_logger);
    }

    /**
     * Creates a new service dependency.
     * 
     * @return the service dependency
     */
    public ServiceDependency createServiceDependency() {
        return new ServiceDependencyImpl();
    }

    /**
     * Creates a new configuration dependency.
     * 
     * @return the configuration dependency
     */
    public ConfigurationDependency createConfigurationDependency() {
        return new ConfigurationDependencyImpl(m_context, m_logger);
    }

    /**
     * Creates a new bundle dependency.
     * 
     * @return a new BundleDependency instance.
     */
    public BundleDependency createBundleDependency() {
        return new BundleDependencyImpl();
    }

    /**
     * Creates a new resource dependency.
     * 
     * @return the resource dependency
     */
    public ResourceDependency createResourceDependency() {
        return new ResourceDependencyImpl();
    }

    /**
     * Creates a new timed required service dependency. A timed dependency blocks the invoker thread is the required dependency
     * is currently unavailable, until it comes up again. 
     * 
     * @return a new timed service dependency
     */
    public ServiceDependency createTemporalServiceDependency(long timeout) {
        return new TemporalServiceDependencyImpl(m_context, timeout);
    }

    /**
     * Creates a new adapter component. The adapter will be applied to any service that
     * matches the specified interface and filter. For each matching service
     * an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface and existing properties
     * from the original service plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * @return an adapter component
     */
    public AdapterComponent createAdapterComponent() {
        return new AdapterServiceImpl(this);
    }
    
    /**
     * Creates a new Factory Component. For each new factory configuration matching
     * the factoryPid, a component will be created based on the component implementation class.
     * The component will be registered with the specified interface, and with the specified service properties.
     * Depending on the <code>propagate</code> parameter, every public factory configuration properties 
     * (which don't start with ".") will be propagated along with the adapter service properties. 
     * It will also inherit all dependencies.
     * 
     * @return a factory pid component
     */    
    public FactoryComponent createFactoryComponent() {
    	return new FactoryConfigurationAdapterImpl(this);
    }

    /**
     * Creates a new bundle adapter. The adapter will be applied to any bundle that
     * matches the specified bundle state mask and filter condition. For each matching
     * bundle an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface
     * 
     * TODO and existing properties from the original resource plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * @return a service that acts as a factory for generating bundle adapters
     */
    public BundleComponent createBundleComponent() {
        return new BundleAdapterImpl(this);
    }

    /**
     * Creates a new resource adapter component. The adapter will be applied to any resource that
     * matches the specified filter condition. For each matching resource
     * an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface and existing properties
     * from the original resource plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * @return a Resource Adapter Component
     */
    public ResourceComponent createResourceComponent() {
    	return new ResourceAdapterImpl(this);
    }
    
    /**
     * Returns a list of components.
     * 
     * @return a list of components
     */
    public List<Component> getComponents() {
        return Collections.list(m_components.elements());
    }

    /**
     * Creates a new aspect component. The aspect will be applied to any service that
     * matches the specified interface and filter. For each matching service
     * an aspect will be created based on the aspect implementation class.
     * The aspect will be registered with the same interface and properties
     * as the original service, plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * @return an aspect component
     */
    public AspectComponent createAspectComponent() {
        return new AspectServiceImpl(this);
    }

    /**
     * Removes all components and their dependencies.
     */
    public void clear() {
        for (Component<?> component : m_components.keySet()) {
            remove(component);
        }
        m_components.clear();
    }

    /**
     * Creates a new configuration property metadata.
     * 
     * @return the configuration property metadata.
     */
    public PropertyMetaData createPropertyMetaData() {
        return new PropertyMetaDataImpl();
    }

    private BundleContext createContext(BundleContext context) {
    	ServiceRegistryCache cache = ServiceRegistryCacheManager.getCache();
        if (cache != null) {
            return cache.createBundleContextInterceptor(context);
        }
        else {
            return context;
        }
    }
}
