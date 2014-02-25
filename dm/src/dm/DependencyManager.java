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
package dm;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

import dm.context.ComponentContext;
import dm.impl.ComponentImpl;
import dm.impl.ConfigurationDependencyImpl;
import dm.impl.Logger;
import dm.impl.ServiceDependencyImpl;
import dm.impl.index.AdapterFilterIndex;
import dm.impl.index.AspectFilterIndex;
import dm.impl.index.ServiceRegistryCache;
import dm.impl.index.multiproperty.MultiPropertyFilterIndex;

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
public class DependencyManager {
    public static final String ASPECT = "org.apache.felix.dependencymanager.aspect";
    public static final String SERVICEREGISTRY_CACHE_INDICES = "org.apache.felix.dependencymanager.filterindex";
    public static final String METHOD_CACHE_SIZE = "org.apache.felix.dependencymanager.methodcache";
    private final BundleContext m_context;
    private final Logger m_logger;
    private final List<Component> m_components = new CopyOnWriteArrayList<>();

    // service registry cache
    private static ServiceRegistryCache m_serviceRegistryCache;
    private static final Set /* WeakReference<DependencyManager> */ m_dependencyManagers = new HashSet();
    static {
        String index = System.getProperty(SERVICEREGISTRY_CACHE_INDICES);
        if (index != null) {
            Bundle bundle = FrameworkUtil.getBundle(DependencyManager.class);
            try {
                if (bundle.getState() != Bundle.ACTIVE) {
                    bundle.start();
                }
                BundleContext bundleContext = bundle.getBundleContext();
                
                m_serviceRegistryCache = new ServiceRegistryCache(bundleContext);
                m_serviceRegistryCache.open(); // TODO close it somewhere
                String[] props = index.split(";");
                for (int i = 0; i < props.length; i++) {
                    if (props[i].equals("*aspect*")) {
                        m_serviceRegistryCache.addFilterIndex(new AspectFilterIndex());
                    }
                    else if (props[i].equals("*adapter*")) {
                        m_serviceRegistryCache.addFilterIndex(new AdapterFilterIndex());
                    }
                    else {
                        m_serviceRegistryCache.addFilterIndex(new MultiPropertyFilterIndex(props[i]));
                    }
                }
            }
            catch (BundleException e) {
                // if we cannot start ourselves, we cannot use the indices
                // TODO we might want to warn people about this
            }
        }
    }
    
    public DependencyManager(BundleContext context) {
        this(context, new Logger(context));
    }
    
    DependencyManager(BundleContext context, Logger logger) {
        m_context = createContext(context);
        m_logger = logger;
        synchronized (m_dependencyManagers) {
            m_dependencyManagers.add(new WeakReference(this));
        }
    }
    
    public static List getDependencyManagers() {
        List /* DependencyManager */ result = new ArrayList();
        synchronized (m_dependencyManagers) {
            Iterator iterator = m_dependencyManagers.iterator();
            while (iterator.hasNext()) {
                WeakReference reference = (WeakReference) iterator.next();
                DependencyManager manager = (DependencyManager) reference.get();
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
    
    public BundleContext getBundleContext() {
        return m_context;
    }

    public void add(Component service) {
        m_components.add(service);
        ((ComponentContext) service).start();
    }

    public void remove(Component service) {
        ((ComponentContext) service).stop();
        m_components.remove(service);
    }

    public Component createComponent() {
        return new ComponentImpl(m_context, this, m_logger);
    }
    
    public ServiceDependency createServiceDependency() {
        return new ServiceDependencyImpl(m_context, m_logger);
    }
    
    public ConfigurationDependencyImpl createConfigurationDependency() {
        return new ConfigurationDependencyImpl(); // TODO pass context and logger in the constructor
    }
        
    public List getComponents() {
        return m_components;
    }

    public void clear() {
        for (Component component : m_components) {
            remove(component);
        }
        m_components.clear();
    }
    
    private BundleContext createContext(BundleContext context) {
        if (m_serviceRegistryCache != null) {
            return m_serviceRegistryCache.createBundleContextInterceptor(context);
        }
        else {
            return context;
        }
    }
}
