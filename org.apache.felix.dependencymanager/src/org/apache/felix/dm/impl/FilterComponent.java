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

import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDeclaration;
import org.apache.felix.dm.ComponentDependencyDeclaration;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.Logger;
import org.apache.felix.dm.context.ComponentContext;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;
import org.apache.felix.dm.context.EventType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * This class allows to filter a Component interface. All Aspect/Adapters extend this class
 * in order to add functionality to the default Component implementation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FilterComponent implements Component, ComponentContext, ComponentDeclaration {
    protected volatile ComponentImpl m_component;
    protected volatile List<ComponentStateListener> m_stateListeners = new CopyOnWriteArrayList<>();
    protected volatile String m_init = "init";
    protected volatile String m_start = "start";
    protected volatile String m_stop = "stop";
    protected volatile String m_destroy = "destroy";
    protected volatile Object m_callbackObject;
    protected volatile Object m_compositionInstance;
    protected volatile String m_compositionMethod;
    protected volatile String[] m_serviceInterfaces;
    protected volatile Object m_serviceImpl;
    protected volatile Object m_factory;
    protected volatile String m_factoryCreateMethod;
    protected volatile Dictionary<String, Object> m_serviceProperties;

    public FilterComponent(Component service) {
        m_component = (ComponentImpl) service;
    }
    
    @Override
    public <T> T createConfigurationType(Class<T> type, Dictionary<?, ?> config) {
        return m_component.createConfigurationType(type, config);
    }
    
    @Override
    public Executor getExecutor() {
        return m_component.getExecutor();
    }

    @Override
    public String toString() {
        return m_component.toString();
    }

    public Component add(Dependency ... dependencies) {
        m_component.add(dependencies);
        // Add the dependencies to all already instantiated services.
        // If one dependency from the list is required, we have nothing to do, since our internal
        // service will be stopped/restarted.
        for (Dependency dependency : dependencies) {
            if (((DependencyContext) dependency).isRequired()) {
                return this;
            }
        }
        // Ok, the list contains no required dependencies: add optionals dependencies in already instantiated services.
        Object[] instances = m_component.getInstances();
        if (instances.length > 0) {
            AbstractDecorator ad = (AbstractDecorator) instances[0];
            if (ad != null) {
                ad.addDependency(dependencies);
            }
        }
        return this;
    }

    public Component add(ComponentStateListener listener) {
        m_stateListeners.add(listener);
        // Add the listener to all already instantiated services.
        Object[] instances = m_component.getInstances();
        if (instances.length > 0) {
            AbstractDecorator ad = (AbstractDecorator) instances[0];
            if (ad != null) {
                ad.addStateListener(listener);
            }
        }
        return this;
    }

    public List<DependencyContext> getDependencies() {
        return m_component.getDependencies();
    }

    public String getClassName() {
        return m_component.getClassName();
    }
    
    @SuppressWarnings("unchecked")
	public Dictionary<String, Object> getServiceProperties() {
        return m_serviceProperties;
    }

    public ServiceRegistration getServiceRegistration() {
        return m_component.getServiceRegistration();
    }

    public Component remove(Dependency dependency) {
        m_component.remove(dependency);
        // Remove the dependency (if optional) from all already instantiated services.
        // If the dependency is required, our internal service will be stopped, so in this case
        // we have nothing to do.
        if (!((DependencyContext) dependency).isRequired())
        {
            Object[] instances = m_component.getInstances();
            if (instances.length > 0) {
                AbstractDecorator ad = (AbstractDecorator) instances[0];
                if (ad != null) {
                    ad.removeDependency(dependency);
                }
            }
        }
        return this;
    }

    public Component remove(ComponentStateListener listener) {
        m_stateListeners.remove(listener);
        // Remove the listener from all already instantiated services.
        Object[] instances = m_component.getInstances();
        if (instances.length > 0) {
            AbstractDecorator ad = (AbstractDecorator) instances[0];
            if (ad != null) {
                ad.removeStateListener(listener);
            }
        }
        return this;
    }

    public Component setCallbacks(Object instance, String init, String start, String stop, String destroy) {
        m_component.ensureNotActive();
        m_callbackObject = instance;
        m_init = init;
        m_start = start;
        m_stop = stop;
        m_destroy = destroy;
        return this;
    }

    public Component setCallbacks(String init, String start, String stop, String destroy) {
        setCallbacks(null, init, start, stop, destroy);
        return this;
    }

    public Component setComposition(Object instance, String getMethod) {
        m_component.ensureNotActive();
        m_compositionInstance = instance;
        m_compositionMethod = getMethod;
        return this;
    }

    public Component setComposition(String getMethod) {
        m_component.ensureNotActive();
        m_compositionMethod = getMethod;
        return this;
    }

    public Component setFactory(Object factory, String createMethod) {
        m_component.ensureNotActive();
        m_factory = factory;
        m_factoryCreateMethod = createMethod;
        return this;
    }

    public Component setFactory(String createMethod) {
        return setFactory(null, createMethod);
    }

    public Component setImplementation(Object implementation) {
        m_component.ensureNotActive();
        m_serviceImpl = implementation;
        return this;
    }

    public Component setInterface(String serviceName, Dictionary<?, ?> properties) {
        return setInterface(new String[] { serviceName }, properties);
    }

    @SuppressWarnings("unchecked")
    public Component setInterface(String[] serviceInterfaces, Dictionary<?, ?> properties) {
        m_component.ensureNotActive();
        if (serviceInterfaces != null) {
            m_serviceInterfaces = new String[serviceInterfaces.length];
            System.arraycopy(serviceInterfaces, 0, m_serviceInterfaces, 0, serviceInterfaces.length);
            m_serviceProperties = (Dictionary<String, Object>) properties;
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public Component setServiceProperties(Dictionary<?, ?> serviceProperties) {
        m_serviceProperties = (Dictionary<String, Object>) serviceProperties;
        // Set the properties to all already instantiated services.
        if (serviceProperties != null) {
            Object[] instances = m_component.getInstances();
            if (instances.length > 0) {
                AbstractDecorator ad = (AbstractDecorator) instances[0];
                if (ad != null) {
                    ad.setServiceProperties(serviceProperties);
                }
            }
        }
        return this;
    }

    public void start() {
        m_component.start();
    }

    public void stop() {
        m_component.stop();
    }
    
    public void invokeCallbackMethod(Object[] instances, String methodName, Class<?>[][] signatures, Object[][] parameters) {
        m_component.invokeCallbackMethod(instances, methodName, signatures, parameters);
    }
    
    public void invokeCallbackMethod(Object[] instances, String methodName, Class<?>[][] signatures, Object[][] parameters, boolean logIfNotFound) {
        m_component.invokeCallbackMethod(instances, methodName, signatures, parameters, logIfNotFound);
    }
            
    public DependencyManager getDependencyManager() {
        return m_component.getDependencyManager();
    }

    public Component setAutoConfig(Class<?> clazz, boolean autoConfig) {
        m_component.setAutoConfig(clazz, autoConfig);
        return this;
    }

    public Component setAutoConfig(Class<?> clazz, String instanceName) {
        m_component.setAutoConfig(clazz, instanceName);
        return this;
    }
    
    public boolean getAutoConfig(Class<?> clazz) {
        return m_component.getAutoConfig(clazz);
    }
    
    public String getAutoConfigInstance(Class<?> clazz) {
        return m_component.getAutoConfigInstance(clazz);
    }

    public ComponentDependencyDeclaration[] getComponentDependencies() {
        return m_component.getComponentDependencies();
    }

    public String getName() {
        return m_component.getName();
    }

    public int getState() {
        return m_component.getState();
    }
    
    public long getId() {
        return m_component.getId();
    }

    public String[] getServices() {
        return m_component.getServices();
    }
    
    public BundleContext getBundleContext() {
        return m_component.getBundleContext();
    }
        
    @Override
    public boolean isActive() {
        return m_component.isActive();
    }

    @Override
    public boolean isAvailable() {
        return m_component.isAvailable();
    }

    @Override
    public void handleEvent(DependencyContext dc, EventType type, Event ... e) {
        m_component.handleEvent(dc, type, e);
    }
    
    @Override
    public <T> T getInstance() {
        return m_component.getInstance();
    }

    @Override
    public Object[] getInstances() {
        return m_component.getInstances();
    }
    
    @Override
    public Event getDependencyEvent(DependencyContext dc) {
        return m_component.getDependencyEvent(dc);
    }
    
    @Override
    public Set<Event> getDependencyEvents(DependencyContext dc) {
        return m_component.getDependencyEvents(dc);
    }
    
    public ComponentDeclaration getComponentDeclaration() {
        return this;
    }

	@Override
	public Component setDebug(String label) {
		m_component.setDebug(label);
		return this;
	}
	
    @Override
    public void setThreadPool(Executor threadPool) {
        m_component.setThreadPool(threadPool);
    }

    @Override
    public Map<String, Long> getCallbacksTime() {
        return m_component.getCallbacksTime();
    }

    @Override
    public Bundle getBundle() {
        return m_component.getBundle();
    }

    @Override
    public Logger getLogger() {
        return m_component.getLogger();
    }
    
    protected void copyDependencies(List<DependencyContext> dependencies, Component component) {
        for (DependencyContext dc : dependencies) {
            DependencyContext copy = dc.createCopy();

            component.add(copy);
        }
    }
}