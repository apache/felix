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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
public abstract class FilterComponent<T extends Component<T>> implements Component<T>, ComponentContext<T>, ComponentDeclaration {
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
    protected volatile ServiceScope m_scope = ServiceScope.SINGLETON;
    private boolean m_started;
    private final List<Dependency> m_dependencies = new ArrayList<>();

    public FilterComponent(Component service) {
        m_component = (ComponentImpl) service;
    }
    
    @Override
    public boolean injectionDisabled() {
    	return m_component.injectionDisabled();
    }
    
    @Override
    public <U> U createConfigurationType(Class<U> type, Dictionary<?, ?> config) {
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
    
    public T setScope(ServiceScope scope) {
    	m_scope = scope;
        return (T) this;
    }
    
    public T add(Dependency ... dependencies) {
    	m_component.getExecutor().execute(() -> {
    		if (! m_started) {
    			Stream.of(dependencies).forEach(m_dependencies::add);
    			return;
    		}
    		m_component.add(dependencies);
    		Object instance = m_component.getInstance();
    		if (instance instanceof AbstractDecorator) {
    			AbstractDecorator ad = (AbstractDecorator) instance;
    			ad.addDependency(dependencies); // will clone the dependencies for each component instance
    		}
    	});
        return (T) this;
    }
    
    public T remove(Dependency dependency) {
    	m_component.getExecutor().execute(() -> {
    		m_component.remove(dependency);
    		Object instance = m_component.getInstance();
            if (instance != null && instance instanceof AbstractDecorator) {
            	((AbstractDecorator) instance).removeDependency(dependency); // will remove the previously cloned dependency
            }
        });
        return (T) this;
    }

    public T add(ComponentStateListener listener) {
    	m_component.getExecutor().execute(() -> {
    		m_stateListeners.add(listener);
    		// Add the listener to all already instantiated services.
    		Object instance = m_component.getInstance();
    		if (instance instanceof AbstractDecorator) {
    			((AbstractDecorator) instance).addStateListener(listener);
    		}
    	});
        return (T) this;
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

    public ServiceRegistration<?> getServiceRegistration() {
        return m_component.getServiceRegistration();
    }

    public T remove(ComponentStateListener listener) {
        m_stateListeners.remove(listener);
        // Remove the listener from all already instantiated services.
        Object instance = m_component.getInstance();
        if (instance != null && instance instanceof AbstractDecorator) {
        	((AbstractDecorator) instance).removeStateListener(listener);
        }
        return (T) this;
    }

    public T setCallbacks(Object instance, String init, String start, String stop, String destroy) {
        m_component.ensureNotActive();
        m_callbackObject = instance;
        m_init = init;
        m_start = start;
        m_stop = stop;
        m_destroy = destroy;
        return (T) this;
    }

    public T setCallbacks(String init, String start, String stop, String destroy) {
        setCallbacks(null, init, start, stop, destroy);
        return (T) this;
    }

    public T setComposition(Object instance, String getMethod) {
        m_component.ensureNotActive();
        m_compositionInstance = instance;
        m_compositionMethod = getMethod;
        return (T) this;
    }

    public T setComposition(String getMethod) {
        m_component.ensureNotActive();
        m_compositionMethod = getMethod;
        return (T) this;
    }

    public T setFactory(Object factory, String createMethod) {
        m_component.ensureNotActive();
        m_factory = factory;
        m_factoryCreateMethod = createMethod;
        return (T) this;
    }

    public T setFactory(String createMethod) {
        return setFactory(null, createMethod);
    }

    public T setImplementation(Object implementation) {
        m_component.ensureNotActive();
        m_serviceImpl = implementation;
        return (T) this;
    }

    public T setInterface(String serviceName, Dictionary<?, ?> properties) {
        return setInterface(new String[] { serviceName }, properties);
    }

    @SuppressWarnings("unchecked")
    public T setInterface(String[] serviceInterfaces, Dictionary<?, ?> properties) {
        m_component.ensureNotActive();
        if (serviceInterfaces != null) {
            m_serviceInterfaces = new String[serviceInterfaces.length];
            System.arraycopy(serviceInterfaces, 0, m_serviceInterfaces, 0, serviceInterfaces.length);
            m_serviceProperties = (Dictionary<String, Object>) properties;
        }
        return (T) this;
    }
    
    public T setInterface(Class<?> serviceName, Dictionary<?, ?> properties) {
    	return setInterface(serviceName.getName(), properties);
    }
    
    public T setInterface(Class<?>[] serviceInterfaces, Dictionary<?, ?> properties) {
    	String[] ifaces = Stream.of(serviceInterfaces).map(clazz -> clazz.getName()).toArray(String[]::new);
    	return setInterface(ifaces, properties);
    }

    @SuppressWarnings("unchecked")
    public T setServiceProperties(Dictionary<?, ?> serviceProperties) {
        m_serviceProperties = (Dictionary<String, Object>) serviceProperties;
        // Set the properties to all already instantiated services.
        if (serviceProperties != null) {
            Object instance = m_component.getInstance();
            if (instance instanceof AbstractDecorator) {
            	((AbstractDecorator) instance).setServiceProperties(serviceProperties);
            }
        }
        return (T) this;
    }

    public void start() {
    	m_component.getExecutor().execute(() -> {
    		if (! m_started) {
    			m_started = true;
    			// first initialize concrete adapters, which need to add
    			// their internal dependencies first.
    			startInitial();
    			// Now, add extra dependencies
    			for (Dependency dep : m_dependencies) {
    				m_component.add(dep);
    			}
				m_dependencies.clear();
    		}
            m_component.start();
    	});
    }
    
    protected abstract void startInitial();

    public void stop() {
        m_component.stop();
    }
    
    public void invokeCallbackMethod(Object[] instances, String methodName, Class<?>[][] signatures, Object[][] parameters) {
        m_component.invokeCallbackMethod(instances, methodName, signatures, parameters);
    }
    
    public void invokeCallbackMethod(Object[] instances, String methodName, Class<?>[][] signatures, Object[][] parameters, boolean logIfNotFound) {
        m_component.invokeCallbackMethod(instances, methodName, signatures, parameters, logIfNotFound);
    }
    
    public void invokeCallback(Object[] instances, String methodName, Class<?>[][] signatures, Supplier<?>[][] paramsSupplier, boolean logIfNotFound) {
        m_component.invokeCallbackMethod(instances, methodName, signatures, paramsSupplier, logIfNotFound);
    }
            
    public DependencyManager getDependencyManager() {
        return m_component.getDependencyManager();
    }

    public T setAutoConfig(Class<?> clazz, boolean autoConfig) {
        m_component.setAutoConfig(clazz, autoConfig);
        return (T) this;
    }

    public T setAutoConfig(Class<?> clazz, String instanceName) {
        m_component.setAutoConfig(clazz, instanceName);
        return (T) this;
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
    public <U> U getInstance() {
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
	public T setDebug(String label) {
		m_component.setDebug(label);
		return (T) this;
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
    	dependencies.stream().map(dc -> dc.createCopy()).forEach(component::add);
    }
	
	@Override
    public ComponentContext<T> instantiateComponent() {
		return m_component.instantiateComponent();
	}
}