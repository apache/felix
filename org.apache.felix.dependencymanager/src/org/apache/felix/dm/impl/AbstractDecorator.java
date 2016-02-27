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
package org.apache.felix.dm.impl;

import java.net.URL;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.context.ComponentContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class AbstractDecorator  {
    protected volatile DependencyManager m_manager;
    private final Map<Object, Component> m_services = new ConcurrentHashMap<>();
    private volatile ComponentContext m_decoratorComponent;

    public abstract Component createService(Object[] properties) throws Exception;
    
    /**
     * Catches our DependencyManager handle from our component init method.
     */
    public void init(Component c) {
        m_manager = c.getDependencyManager();
        m_decoratorComponent = (ComponentContext) c;
    }
    
    /**
     * Extra method, which may be used by sub-classes, when adaptee has changed.
     * For now, it's only used by the FactoryConfigurationAdapterImpl class, 
     * but it might also make sense to use this for Resource Adapters ...
     */
    public void updateService(Object[] properties) throws Exception {
        throw new NoSuchMethodException("Method updateService not implemented");
    }
    
    /**
     * Set some service properties to all already instantiated services.
     */
    public void setServiceProperties(Dictionary<?,?> serviceProperties) {
        for (Component component : m_services.values()) {
            component.setServiceProperties(serviceProperties);
        }
    }
    
    /**
     * Remove a StateListener from all already instantiated services.
     */
    public void addStateListener(ComponentStateListener listener) {
        for (Component component : m_services.values()) {
            component.add(listener);
        }
    }

    /**
     * Remove a StateListener from all already instantiated services.
     */
    public void removeStateListener(ComponentStateListener listener) {
        for (Component component : m_services.values()) {
            component.remove(listener);
        }
    }
    
    /**
     * Add a Dependency to all already instantiated services.
     */
    public void addDependency(Dependency ... dependencies) {
        for (Component component : m_services.values()) {
            component.add(dependencies);
        }
    }
    
    /**
     * Remove a Dependency from all instantiated services.
     */
    public void removeDependency(Dependency d) {
        for (Component component : m_services.values()) {
            component.remove(d);
        }
    }
    
    // callbacks for FactoryConfigurationAdapterImpl from the ConfigAdmin thread
    @SuppressWarnings("rawtypes")
    public void updated(String pid, Dictionary properties) throws ConfigurationException {
        // FELIX-5193: invoke the updated callback in the internal decorator component queue, in order
        // to safely detect if the component is still active or not.
        InvocationUtil.invokeUpdated(m_decoratorComponent.getExecutor(), () -> updatedSafe(pid, properties));
    }
    
    @SuppressWarnings("rawtypes")
    private void updatedSafe(String pid, Dictionary properties) throws Exception {
        if (!m_decoratorComponent.isActive()) {
            // Our decorator component has been removed: ignore the configuration update.
            return;
        }
        Component service = m_services.get(pid);
        if (service == null) {
            service = createService(new Object[] { properties });
            m_services.put(pid, service);
            m_manager.add(service);
        } else {
            updateService(new Object[] { properties, service });
        }
    }

    public void deleted(String pid) {
        Component service = m_services.remove(pid);
        if (service != null) {
            m_manager.remove(service);
        }
    }

    // callbacks for resources
    public void added(URL resource) throws Exception {
        Component newService = createService(new Object[] { resource });
        m_services.put(resource, newService);
        m_manager.add(newService);
    }

    public void removed(URL resource) {
        Component newService = m_services.remove(resource);
        if (newService == null) {
            throw new IllegalStateException("Service should not be null here.");
        }
        m_manager.remove(newService);
    }
    
    // callbacks for services
    public void added(ServiceReference ref, Object service) throws Exception {
        Component newService = createService(new Object[] { ref, service });
        m_services.put(ref, newService);
        m_manager.add(newService);
    }
    
    public void removed(ServiceReference ref, Object service) {
        Component newService;
        newService = (Component) m_services.remove(ref);
        if (newService == null) {
            throw new IllegalStateException("Service should not be null here.");
        }
        m_manager.remove(newService);
    }
    
    public void swapped(ServiceReference oldRef, Object oldService, ServiceReference newRef, Object newService) {
        Component service = (Component) m_services.remove(oldRef);
        if (service == null) {
            throw new IllegalStateException("Service should not be null here.");
        }           
        m_services.put(newRef, service);
    }
    
    // callbacks for bundles
    public void added(Bundle bundle) throws Exception {
        Component newService = createService(new Object[] { bundle });
        m_services.put(bundle, newService);
        m_manager.add(newService);
    }
    
    public void removed(Bundle bundle) {
        Component newService;
        newService = (Component) m_services.remove(bundle);
        if (newService == null) {
            throw new IllegalStateException("Service should not be null here.");
        }
        m_manager.remove(newService);
    }
      
    public void stop() {
        for (Component component : m_services.values()) {
            m_manager.remove(component);
        }
        m_services.clear();
    }    
    
    public void configureAutoConfigState(Component target, ComponentContext source) {
        configureAutoConfigState(target, source, BundleContext.class);
        configureAutoConfigState(target, source, ServiceRegistration.class);
        configureAutoConfigState(target, source, DependencyManager.class);
        configureAutoConfigState(target, source, Component.class);
    }
    
    public Map<Object, Component> getServices() {
        return m_services;
    }

    private void configureAutoConfigState(Component target, ComponentContext source, Class<?> clazz) {
        String name = source.getAutoConfigInstance(clazz);
        if (name != null) {
            target.setAutoConfig(clazz, name);
        }
        else {
            target.setAutoConfig(clazz, source.getAutoConfig(clazz));
        }
    }    
}
