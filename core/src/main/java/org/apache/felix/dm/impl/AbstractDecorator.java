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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
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
    private final Map m_services = new HashMap();
    
    public abstract Component createService(Object[] properties);
    
    /**
     * Catches our DependencyManager handle from our component init method.
     */
    public void init(Component c) {
        m_manager = c.getDependencyManager();
    }
    
    /**
     * Extra method, which may be used by sub-classes, when adaptee has changed.
     * For now, it's only used by the FactoryConfigurationAdapterImpl class, 
     * but it might also make sense to use this for Resource Adapters ...
     */
    public void updateService(Object[] properties) {
        throw new NoSuchMethodError("Method updateService not implemented");
    }
    
    /**
     * Set some service properties to all already instantiated services.
     */
    public void setServiceProperties(Dictionary serviceProperties) {
        Object[] components;
        synchronized (m_services) {
            components = m_services.values().toArray();
        }
        for (int i = 0; i < components.length; i++) {
            ((Component) components[i]).setServiceProperties(serviceProperties);
        }
    }
    
    /**
     * Remove a StateListener from all already instantiated services.
     */
    public void addStateListener(ComponentStateListener listener) {
        Object[] components;
        synchronized (m_services) {
            components = m_services.values().toArray();
        }
        for (int i = 0; i < components.length; i++) {
            ((Component) components[i]).addStateListener(listener);
        }
    }

    /**
     * Remove a StateListener from all already instantiated services.
     */
    public void removeStateListener(ComponentStateListener listener) {
        Object[] components;
        synchronized (m_services) {
            components = m_services.values().toArray();
        }
        for (int i = 0; i < components.length; i++) {
            ((Component) components[i]).removeStateListener(listener);
        }
    }
    
    /**
     * Add a Dependency to all already instantiated services.
     */
    public void addDependency(Dependency d) {
        Object[] components;
        synchronized (m_services) {
            components = m_services.values().toArray();
        }
        for (int i = 0; i < components.length; i++) {
            ((Component) components[i]).add(d);
        }
    }
    
    /**
     * Add a Dependency to all already instantiated services.
     */
    public void addDependencies(List dependencies) {
        Object[] components;
        synchronized (m_services) {
            components = m_services.values().toArray();
        }
        for (int i = 0; i < components.length; i++) {
            ((Component) components[i]).add(dependencies);
        }
    }

    /**
     * Remove a Dependency from all instantiated services.
     */
    public void removeDependency(Dependency d) {
        Object[] components;
        synchronized (m_services) {
            components = m_services.values().toArray();
        }
        for (int i = 0; i < components.length; i++) {
            ((Component) components[i]).remove(d);
        }
    }
    
    // callbacks for FactoryConfigurationAdapterImpl
    public void updated(String pid, Dictionary properties) throws ConfigurationException {
        try {
            Component service;
            synchronized (m_services) {
                service = (Component) m_services.get(pid);
            }
            if (service == null) { 
                service = createService(new Object[] { properties });
                synchronized (m_services) {
                    m_services.put(pid, service);
                }
                m_manager.add(service);
            }
            else {
                updateService(new Object[] { properties, service });
            }
        }
        catch (Throwable t) {
            if (t instanceof ConfigurationException) {
                throw (ConfigurationException) t;
            }
            else if (t.getCause() instanceof ConfigurationException) {
                throw (ConfigurationException) t.getCause();
            }
            else {
                throw new ConfigurationException(null, "Could not create service for ManagedServiceFactory Pid " + pid, t);
            }
        }
    }

    public void deleted(String pid) {
        Component service = null;
        synchronized (m_services) {
            service = (Component) m_services.remove(pid);
        }
        if (service != null) {
            m_manager.remove(service);
        }
    }

    // callbacks for resources
    public void added(URL resource) {
        Component newService = createService(new Object[] { resource });
        synchronized (m_services) {
            m_services.put(resource, newService);
        }
        m_manager.add(newService);
    }

    public void removed(URL resource) {
        Component oldService;
        synchronized (m_services) {
            oldService = (Component) m_services.remove(resource);
        }
        if (oldService == null) {
            throw new IllegalStateException("Service should not be null here.");
        }
        m_manager.remove(oldService);
    }
    
    // callbacks for services
    public void added(ServiceReference ref, Object service) {
        Component newService = createService(new Object[] { ref, service });
        synchronized (m_services) {
            m_services.put(ref, newService);
        }
        m_manager.add(newService);
    }
    
    public void removed(ServiceReference ref, Object service) { 
        Component oldService;
        synchronized (m_services) {
            oldService = (Component) m_services.remove(ref);
        }
        if (oldService == null) {
            throw new IllegalStateException("Service should not be null here.");  
        }
        m_manager.remove(oldService);
    }
    
    public void swapped(ServiceReference oldRef, Object oldService, ServiceReference newRef, Object newService) {
        synchronized (m_services) {
        	Component service = (Component) m_services.remove(oldRef);
            if (service == null) {
                throw new IllegalStateException("Service should not be null here.");
            }        	
        	m_services.put(newRef, service);
        } 
    }
    
    // callbacks for bundles
    public void added(Bundle bundle) {
        Component newService = createService(new Object[] { bundle });
        synchronized (m_services) {
            m_services.put(bundle, newService);
        }
        m_manager.add(newService);
    }
    
    public void removed(Bundle bundle) {
        Component newService;
        synchronized (m_services) {
            newService = (Component) m_services.remove(bundle);
        }
        if (newService == null) {
            throw new IllegalStateException("Service should not be null here.");
        }
        m_manager.remove(newService);
    }
      
    public void stop() { 
        Object[] components;
        synchronized (m_services) {
            components = m_services.values().toArray();
        }
        for (int i = 0; i < components.length; i++) {
            m_manager.remove((Component) components[i]); 
        }
    }    
    
    public void configureAutoConfigState(Component target, Component source) {
        configureAutoConfigState(target, source, BundleContext.class);
        configureAutoConfigState(target, source, ServiceRegistration.class);
        configureAutoConfigState(target, source, DependencyManager.class);
        configureAutoConfigState(target, source, Component.class);
    }
    
    public Map getServices() {
        synchronized (m_services) {
            return new HashMap(m_services);
        }
    }

    private void configureAutoConfigState(Component target, Component source, Class clazz) {
        String name = source.getAutoConfigInstance(clazz);
        if (name != null) {
            target.setAutoConfig(clazz, name);
        }
        else {
            target.setAutoConfig(clazz, source.getAutoConfig(clazz));
        }
    }    
}
