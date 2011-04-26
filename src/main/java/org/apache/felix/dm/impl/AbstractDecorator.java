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
import java.util.Iterator;
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
        Map services = new HashMap();
        synchronized (this) {
            services.putAll(m_services);
        }
        Iterator i = services.values().iterator();
        while (i.hasNext()) {
            ((Component) i.next()).setServiceProperties(serviceProperties);
        }
    }
    
    /**
     * Remove a StateListener from all already instantiated services.
     */
    public void addStateListener(ComponentStateListener listener) {
        Map services = new HashMap();
        synchronized (this) {
            services.putAll(m_services);
        }
        Iterator i = services.values().iterator();
        while (i.hasNext()) {
            ((Component) i.next()).addStateListener(listener);
        } 
    }

    /**
     * Remove a StateListener from all already instantiated services.
     */
    public void removeStateListener(ComponentStateListener listener) {
        Map services = new HashMap();
        synchronized (this) {
            services.putAll(m_services);
        }
        Iterator i = services.values().iterator();
        while (i.hasNext()) {
            ((Component) i.next()).removeStateListener(listener);
        } 
    }
    
    /**
     * Add a Dependency to all already instantiated services.
     */
    public void addDependency(Dependency d) {
        Map services = new HashMap();
        synchronized (this) {
            services.putAll(m_services);
        }
        Iterator i = services.values().iterator();
        while (i.hasNext()) {
            ((Component) i.next()).add(d);
        } 
    }
    
    /**
     * Add a Dependency to all already instantiated services.
     */
    public void addDependencies(List dependencies) {
        Map services = new HashMap();
        synchronized (this) {
            services.putAll(m_services);
        }
        Iterator i = services.values().iterator();
        while (i.hasNext()) {
            ((Component) i.next()).add(dependencies);
        } 
    }

    /**
     * Remove a Dependency from all instantiated services.
     */
    public void removeDependency(Dependency d) {
        Map services = new HashMap();
        synchronized (this) {
            services.putAll(m_services);
        }
        Iterator i = services.values().iterator();
        while (i.hasNext()) {
            ((Component) i.next()).remove(d);
        } 
    }
    
    // callbacks for FactoryConfigurationAdapterImpl
    public void updated(String pid, Dictionary properties) throws ConfigurationException {
        try {
            Component service;
            synchronized (this) {
                service = (Component) m_services.get(pid);
            }
            if (service == null) { 
                service = createService(new Object[] { properties });
                synchronized (this) {
                    m_services.put(pid, service);
                }
                m_manager.add(service);
            } else {
                updateService(new Object[] { properties, service });
            }
        }
        
        catch (Throwable t) {
            if (t instanceof ConfigurationException) {
                throw (ConfigurationException) t;
            } else if (t.getCause() instanceof ConfigurationException) {
                throw (ConfigurationException) t.getCause();
            } else {
                throw new ConfigurationException(null, "Could not create service for ManagedServiceFactory Pid " + pid, t);
            }
        }
    }

    public void deleted(String pid) {
        Component service = null;
        synchronized (this) {
            service = (Component) m_services.remove(pid);
        }
        if (service != null)
        {
            m_manager.remove(service);
        }
    }

    // callbacks for resources
    public void added(URL resource) {
        Component newService = createService(new Object[] { resource });
        m_services.put(resource, newService);
        m_manager.add(newService);
    }

    public void removed(URL resource) {
        Component newService = (Component) m_services.remove(resource);
        if (newService == null) {
            throw new IllegalStateException("Service should not be null here.");
        }
        else {
            m_manager.remove(newService);
        }
    }
    
    // callbacks for services
    public void added(ServiceReference ref, Object service) {
        Component newService = createService(new Object[] { ref, service });
        m_services.put(ref, newService);
        m_manager.add(newService);
    }
    
    public void removed(ServiceReference ref, Object service) {
        Component newService = (Component) m_services.remove(ref);
        if (newService == null) {
            throw new IllegalStateException("Service should not be null here.");
        }
        else {
            m_manager.remove(newService);
        }
    }
    
    // callbacks for bundles
    public void added(Bundle bundle) {
        Component newService = createService(new Object[] { bundle });
        m_services.put(bundle, newService);
        m_manager.add(newService);
    }
    
    public void removed(Bundle bundle) {
        Component newService = (Component) m_services.remove(bundle);
        if (newService == null) {
            throw new IllegalStateException("Service should not be null here.");
        }
        else {
            m_manager.remove(newService);
        }
    }
    
    public void stop() { 
        Iterator i = m_services.values().iterator();
        while (i.hasNext()) {
            m_manager.remove((Component) i.next());
        }
        m_services.clear();
    }    
    
    public void configureAutoConfigState(Component target, Component source) {
        configureAutoConfigState(target, source, BundleContext.class);
        configureAutoConfigState(target, source, ServiceRegistration.class);
        configureAutoConfigState(target, source, DependencyManager.class);
        configureAutoConfigState(target, source, Component.class);
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
