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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentStateListener;
import org.osgi.framework.ServiceRegistration;

/**
 * This class allows to filter a Service interface. All Aspect/Adapters extends this class
 * in order to add functionality to the default Service implementation.
 */
public class FilterService implements Component
{
    protected ComponentImpl m_service;
    protected List m_stateListeners = new ArrayList();
    protected String m_init = "init";
    protected String m_start = "start";
    protected String m_stop = "stop";
    protected String m_destroy = "destroy";
    protected Object m_callbackObject;
    protected Object m_compositionInstance;
    protected String m_compositionMethod;
    protected String[] m_serviceInterfaces;
    protected Object m_serviceImpl;
    protected Object m_factory;
    protected String m_factoryCreateMethod;
    protected Dictionary m_serviceProperties;

    public FilterService(Component service)
    {
        m_service = (ComponentImpl) service;
    }

    public Component add(Dependency dependency)
    {
        m_service.add(dependency);
        // Add the dependency (if optional) to all already instantiated services.
        // If the dependency is required, our internal service will be stopped/restarted, so in this case
        // we have nothing to do.
        if (! dependency.isRequired()) {
            AbstractDecorator ad = (AbstractDecorator) m_service.getService();
            if (ad != null)
            {
                ad.addDependency(dependency);
            }
        }
        return this;
    }

    public Component add(List dependencies)
    {
        m_service.add(dependencies);
        // Add the dependencies to all already instantiated services.
        // If one dependency from the list is required, we have nothing to do, since our internal
        // service will be stopped/restarted.
        Iterator it = dependencies.iterator();
        while (it.hasNext()) {
            if (((Dependency) it.next()).isRequired()) {
                return this;
            }
        }
        // Ok, the list contains no required dependencies: add optionals dependencies in already instantiated
        // services.
        AbstractDecorator ad = (AbstractDecorator) m_service.getService();
        if (ad != null)
        {
            ad.addDependencies(dependencies);
        }
        return this;
    }

    public void addStateListener(ComponentStateListener listener)
    {
        synchronized (this)
        {
            m_stateListeners.add(listener);
        }
        // Add the listener to all already instantiated services.
        AbstractDecorator ad = (AbstractDecorator) m_service.getService();
        if (ad != null)
        {
            ad.addStateListener(listener);
        }
    }

    public List getDependencies()
    {
        return m_service.getDependencies();
    }

    public Object getService()
    {
        return m_service.getService();
    }

    public synchronized Dictionary getServiceProperties()
    {
        return m_serviceProperties;
    }

    public ServiceRegistration getServiceRegistration()
    {
        return m_service.getServiceRegistration();
    }

    public Component remove(Dependency dependency)
    {
        m_service.remove(dependency);
        // Remove the dependency (if optional) from all already instantiated services.
        // If the dependency is required, our internal service will be stopped, so in this case
        // we have nothing to do.
        if (!dependency.isRequired())
        {
            AbstractDecorator ad = (AbstractDecorator) m_service.getService();
            if (ad != null)
            {
                ad.removeDependency(dependency);
            }
        }
        return this;
    }

    public void removeStateListener(ComponentStateListener listener)
    {
        synchronized (this)
        {
            m_stateListeners.remove(listener);
        }
        // Remove the listener from all already instantiated services.
        AbstractDecorator ad = (AbstractDecorator) m_service.getService();
        if (ad != null)
        {
            ad.removeStateListener(listener);
        }
    }

    public synchronized Component setCallbacks(Object instance, String init, String start, String stop,
                                             String destroy)
    {
        m_service.ensureNotActive();
        m_callbackObject = instance;
        m_init = init;
        m_start = start;
        m_stop = stop;
        m_destroy = destroy;
        return this;
    }

    public Component setCallbacks(String init, String start, String stop, String destroy)
    {
        setCallbacks(null, init, start, stop, destroy);
        return this;
    }

    public synchronized Component setComposition(Object instance, String getMethod)
    {
        m_service.ensureNotActive();
        m_compositionInstance = instance;
        m_compositionMethod = getMethod;
        return this;
    }

    public synchronized Component setComposition(String getMethod)
    {
        m_service.ensureNotActive();
        m_compositionMethod = getMethod;
        return this;
    }

    public synchronized Component setFactory(Object factory, String createMethod)
    {
        m_service.ensureNotActive();
        m_factory = factory;
        m_factoryCreateMethod = createMethod;
        return this;
    }

    public Component setFactory(String createMethod)
    {
        return setFactory(null, createMethod);
    }

    public synchronized Component setImplementation(Object implementation)
    {
        m_service.ensureNotActive();
        m_serviceImpl = implementation;
        return this;
    }

    public Component setInterface(String serviceName, Dictionary properties)
    {
        return setInterface(new String[] { serviceName }, properties);
    }

    public synchronized Component setInterface(String[] serviceInterfaces, Dictionary properties) {
        m_service.ensureNotActive();
        if (serviceInterfaces != null) {
            m_serviceInterfaces = new String[serviceInterfaces.length];
            System.arraycopy(serviceInterfaces, 0, m_serviceInterfaces, 0, serviceInterfaces.length);
            m_serviceProperties = properties;
        }
        return this;
    }

    public Component setServiceProperties(Dictionary serviceProperties)
    {
        synchronized (this)
        {
            m_serviceProperties = serviceProperties;
        }
        // Set the properties to all already instantiated services.
        if (serviceProperties != null) {
            AbstractDecorator ad = (AbstractDecorator) m_service.getService();
            if (ad != null)
            {
                ad.setServiceProperties(serviceProperties);
            }
        }
        return this;
    }

    public void start()
    {
        m_service.start();
    }

    public void stop()
    {
        m_service.stop();
    }
    
    public Object[] getCompositionInstances() {
        return m_service.getCompositionInstances();
    }
    
    public DependencyManager getDependencyManager() {
        return m_service.getDependencyManager();
    }

    public Component setAutoConfig(Class clazz, boolean autoConfig) {
        m_service.setAutoConfig(clazz, autoConfig);
        return this;
    }

    public Component setAutoConfig(Class clazz, String instanceName) {
        m_service.setAutoConfig(clazz, instanceName);
        return this;
    }
    
    public boolean getAutoConfig(Class clazz) {
        return m_service.getAutoConfig(clazz);
    }
    
    public String getAutoConfigInstance(Class clazz) {
        return m_service.getAutoConfigInstance(clazz);
    };
}