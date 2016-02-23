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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDeclaration;
import org.apache.felix.dm.ServiceDependency;
import org.apache.felix.dm.context.AbstractDependency;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;
import org.apache.felix.dm.context.EventType;
import org.apache.felix.dm.tracker.ServiceTracker;
import org.apache.felix.dm.tracker.ServiceTrackerCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceDependencyImpl extends AbstractDependency<ServiceDependency> implements ServiceDependency, ServiceTrackerCustomizer {
	protected volatile ServiceTracker m_tracker;
    protected volatile String m_swap;
    protected volatile Class<?> m_trackedServiceName;
    private volatile String m_trackedServiceFilter;
    private volatile String m_trackedServiceFilterUnmodified;
    private volatile ServiceReference m_trackedServiceReference;
    private volatile Object m_defaultImplementation;
    private volatile Object m_defaultImplementationInstance;
    private volatile Object m_nullObject;
    private volatile boolean m_debug = false;
    private volatile String m_debugKey;
    private volatile long m_trackedServiceReferenceId;
    
    public ServiceDependency setDebug(String debugKey) {
    	m_debugKey = debugKey;
    	m_debug = true;
    	return this;
    }

    /**
     * Entry to wrap service properties behind a Map.
     */
    private static final class ServicePropertiesMapEntry implements Map.Entry<String, Object> {
        private final String m_key;
        private Object m_value;

        public ServicePropertiesMapEntry(String key, Object value) {
            m_key = key;
            m_value = value;
        }

        public String getKey() {
            return m_key;
        }

        public Object getValue() {
            return m_value;
        }

        public String toString() {
            return m_key + "=" + m_value;
        }

        public Object setValue(Object value) {
            Object oldValue = m_value;
            m_value = value;
            return oldValue;
        }

        @SuppressWarnings("unchecked")
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<String, Object> e = (Map.Entry<String, Object>) o;
            return eq(m_key, e.getKey()) && eq(m_value, e.getValue());
        }

        public int hashCode() {
            return ((m_key == null) ? 0 : m_key.hashCode()) ^ ((m_value == null) ? 0 : m_value.hashCode());
        }

        private static final boolean eq(Object o1, Object o2) {
            return (o1 == null ? o2 == null : o1.equals(o2));
        }
    }

    /**
     * Wraps service properties behind a Map.
     */
    private final static class ServicePropertiesMap extends AbstractMap<String, Object> {
        private final ServiceReference m_ref;

        public ServicePropertiesMap(ServiceReference ref) {
            m_ref = ref;
        }

        public Object get(Object key) {
            return m_ref.getProperty(key.toString());
        }

        public int size() {
            return m_ref.getPropertyKeys().length;
        }

        public Set<Map.Entry<String, Object>> entrySet() {
            Set<Map.Entry<String, Object>> set = new HashSet<>();
            String[] keys = m_ref.getPropertyKeys();
            for (int i = 0; i < keys.length; i++) {
                set.add(new ServicePropertiesMapEntry(keys[i], m_ref.getProperty(keys[i])));
            }
            return set;
        }
    }
    
	public ServiceDependencyImpl() {
	}
	
	public ServiceDependencyImpl(ServiceDependencyImpl prototype) {
		super(prototype);
        m_trackedServiceName = prototype.m_trackedServiceName;
        m_nullObject = prototype.m_nullObject;
        m_trackedServiceFilter = prototype.m_trackedServiceFilter;
        m_trackedServiceFilterUnmodified = prototype.m_trackedServiceFilterUnmodified;
        m_trackedServiceReference = prototype.m_trackedServiceReference;
        m_autoConfigInstance = prototype.m_autoConfigInstance;
        m_defaultImplementation = prototype.m_defaultImplementation;
        m_autoConfig = prototype.m_autoConfig;
	}
	    	    
    // --- CREATION
    			
    public ServiceDependency setCallbacks(Object instance, String added, String changed, String removed, String swapped) {
        setCallbacks(instance, added, changed, removed);
        m_swap = swapped;
        return this;
    }
    
    public ServiceDependency setCallbacks(String added, String changed, String removed, String swapped) {
        setCallbacks(added, changed, removed);
        m_swap = swapped;
        return this;
    }
		
    @Override
    public ServiceDependency setDefaultImplementation(Object implementation) {
        ensureNotActive();
        m_defaultImplementation = implementation;
        return this;
    }   

    @Override
   	public ServiceDependency setService(Class<?> serviceName) {
        setService(serviceName, null, null);
        return this;
    }

    public ServiceDependency setService(Class<?> serviceName, String serviceFilter) {
        setService(serviceName, null, serviceFilter);
        return this;
    }

    public ServiceDependency setService(String serviceFilter) {
        if (serviceFilter == null) {
            throw new IllegalArgumentException("Service filter cannot be null.");
        }
        setService(null, null, serviceFilter);
        return this;
    }

    public ServiceDependency setService(Class<?> serviceName, ServiceReference serviceReference) {
        setService(serviceName, serviceReference, null);
        return this;
    }

	@Override
	public void start() {
        if (m_trackedServiceName != null) {
            BundleContext ctx = m_component.getBundleContext();
            if (m_trackedServiceFilter != null) {
                try {
                    m_tracker = new ServiceTracker(ctx, ctx.createFilter(m_trackedServiceFilter), this);
                } catch (InvalidSyntaxException e) {
                    throw new IllegalStateException("Invalid filter definition for dependency: "
                        + m_trackedServiceFilter);
                }
            } else if (m_trackedServiceReference != null) {
                m_tracker = new ServiceTracker(ctx, m_trackedServiceReference, this);
            } else {
                m_tracker = new ServiceTracker(ctx, m_trackedServiceName.getName(), this);
            }
        } else {
            throw new IllegalStateException("Could not create tracker for dependency, no service name specified.");
        }
        if (m_debug) {
            m_tracker.setDebug(m_debugKey);
        }
        m_tracker.open();
        super.start();
	}
	
	@Override
	public void stop() {
	    m_tracker.close();
	    m_tracker = null;
	    super.stop();
	}

	@Override
	public Object addingService(ServiceReference reference) {
		try {
		    return m_component.getBundleContext().getService(reference);
		} catch (IllegalStateException e) {
		    // most likely our bundle is being stopped. Only log an exception if our component is enabled.
		    if (m_component.isActive()) {
                m_component.getLogger().warn("could not handle service dependency for component %s", e,
                    m_component.getComponentDeclaration().getClassName());
		    }
		    return null;		    
		}
	}

	@Override
	public void addedService(ServiceReference reference, Object service) {
		if (m_debug) {
			System.out.println(m_debugKey + " addedService: ref=" + reference + ", service=" + service);
		}
        m_component.handleEvent(this, EventType.ADDED,
            new ServiceEventImpl(m_component.getBundle(), m_component.getBundleContext(), reference, service));
	}

	@Override
	public void modifiedService(ServiceReference reference, Object service) {
        m_component.handleEvent(this, EventType.CHANGED,
            new ServiceEventImpl(m_component.getBundle(), m_component.getBundleContext(), reference, service));
	}

	@Override
	public void removedService(ServiceReference reference, Object service) {
        m_component.handleEvent(this, EventType.REMOVED,
            new ServiceEventImpl(m_component.getBundle(), m_component.getBundleContext(), reference, service));
	}
	
    @Override
    public void invokeCallback(EventType type, Event ... events) {
        switch (type) {
        case ADDED:
            if (m_add != null) {
                invoke (m_add, events[0], getInstances());
            }
            break;
        case CHANGED:
            if (m_change != null) {
                invoke (m_change, events[0], getInstances());
            }
            break;
        case REMOVED:
            if (m_remove != null) {
                invoke (m_remove, events[0], getInstances());
            }
            break;
        case SWAPPED:
            if (m_swap != null) {
                ServiceEventImpl oldE = (ServiceEventImpl) events[0];
                ServiceEventImpl newE = (ServiceEventImpl) events[1];
                invokeSwap(m_swap, oldE.getReference(), oldE.getEvent(), newE.getReference(), newE.getEvent(),
                    getInstances());
            }
            break;
        }
    }

	@Override
    public Class<?> getAutoConfigType() {
        return m_trackedServiceName;
    }
	
    @Override
    public DependencyContext createCopy() {
        return new ServiceDependencyImpl(this);
    }
    
    @Override
    public String getName() {
        StringBuilder sb = new StringBuilder();
        if (m_trackedServiceName != null) {
            sb.append(m_trackedServiceName.getName());
            if (m_trackedServiceFilterUnmodified != null) {
                sb.append(' ');
                sb.append(m_trackedServiceFilterUnmodified);
            }
        }
        if (m_trackedServiceReference != null) {
            sb.append("{service.id=" + m_trackedServiceReference.getProperty(Constants.SERVICE_ID) + "}");
        }
        return sb.toString();
    }
    
    @Override
    public String getSimpleName() {
        if (m_trackedServiceName != null) {
            return m_trackedServiceName.getName();
        }
        return null;
    }

    @Override
    public String getFilter() {
        if (m_trackedServiceFilterUnmodified != null) {
            return m_trackedServiceFilterUnmodified;
        } else if (m_trackedServiceReference != null) {
            return new StringBuilder("(").append(Constants.SERVICE_ID).append("=").append(
                String.valueOf(m_trackedServiceReferenceId)).append(")").toString();
        } else {
            return null;
        }
    }
    
    @Override
    public String getType() {
        return "service";
    }

	@SuppressWarnings("unchecked")
    @Override
    public Dictionary<String, Object> getProperties() {
        ServiceEventImpl se = (ServiceEventImpl) m_component.getDependencyEvent(this);
        if (se != null) {
            if (m_propagateCallbackInstance != null && m_propagateCallbackMethod != null) {
                try {
                    CallbackTypeDef callbackInfo = new CallbackTypeDef(new Class[][] { { ServiceReference.class, Object.class }, { ServiceReference.class } },
                        new Object[][] { { se.getReference(), se.getEvent() }, { se.getReference() } });
                    return (Dictionary<String, Object>) InvocationUtil.invokeCallbackMethod(m_propagateCallbackInstance, m_propagateCallbackMethod, callbackInfo.m_sigs, callbackInfo.m_args);
                } catch (InvocationTargetException e) {
                    m_component.getLogger().warn("Exception while invoking callback method", e.getCause());
                } catch (Throwable e) {
                    m_component.getLogger().warn("Exception while trying to invoke callback method", e);
                }
                throw new IllegalStateException("Could not invoke callback");
            } else {
                Hashtable<String, Object> props = new Hashtable<>();
                String[] keys = se.getReference().getPropertyKeys();
                for (int i = 0; i < keys.length; i++) {
                    if (!(keys[i].equals(Constants.SERVICE_ID) || keys[i].equals(Constants.SERVICE_PID))) {
                        props.put(keys[i], se.getReference().getProperty(keys[i]));
                    }
                }
                return props;
            }
        } else {
            throw new IllegalStateException("cannot find service reference");
        }
    }	
        
    /** Internal method to set the name, service reference and/or filter. */
    private void setService(Class<?> serviceName, ServiceReference serviceReference, String serviceFilter) {
        ensureNotActive();
        if (serviceName == null) {
            m_trackedServiceName = Object.class;
        }
        else {
            m_trackedServiceName = serviceName;
        }
        if (serviceFilter != null) {
            m_trackedServiceFilterUnmodified = serviceFilter;
            if (serviceName == null) {
                m_trackedServiceFilter = serviceFilter;
            }
            else {
                m_trackedServiceFilter = "(&(" + Constants.OBJECTCLASS + "=" + serviceName.getName() + ")"
                    + serviceFilter + ")";
            }
        }
        else {
            m_trackedServiceFilterUnmodified = null;
            m_trackedServiceFilter = null;
        }
        if (serviceReference != null) {
            m_trackedServiceReference = serviceReference;
            if (serviceFilter != null) {
                throw new IllegalArgumentException("Cannot specify both a filter and a service reference.");
            }
            m_trackedServiceReferenceId = (Long) m_trackedServiceReference.getProperty(Constants.SERVICE_ID);
        }
        else {
            m_trackedServiceReference = null;
        }
    }
    
    @Override
    public Object getDefaultService(boolean nullObject) {
        Object service = null;
        if (isAutoConfig()) {
            service = getDefaultImplementation();
            if (service == null && nullObject) {
                service = getNullObject();
            }
        }
        return service;
    }
        
    private Object getNullObject() {
        if (m_nullObject == null) {
            Class<?> trackedServiceName;
            trackedServiceName = m_trackedServiceName;
            try {
                m_nullObject = Proxy.newProxyInstance(trackedServiceName.getClassLoader(),
                    new Class[] { trackedServiceName }, new DefaultNullObject());
            }
            catch (Throwable err) {
                m_component.getLogger().err("Could not create null object for %s.", err, trackedServiceName);
            }
        }
        return m_nullObject;
    }

    private Object getDefaultImplementation() {
        if (m_defaultImplementation != null) {
            if (m_defaultImplementation instanceof Class) {
                try {
                    m_defaultImplementationInstance = ((Class<?>) m_defaultImplementation).newInstance();
                }
                catch (Throwable e) {
                    m_component.getLogger().err("Could not create default implementation instance of class %s.", e,
                        m_defaultImplementation);
                }
            }
            else {
                m_defaultImplementationInstance = m_defaultImplementation;
            }
        }
        return m_defaultImplementationInstance;
    }

    public void invoke(String method, Event e, Object[] instances) {
        ServiceEventImpl se = (ServiceEventImpl) e;
        ServicePropertiesMap propertiesMap = new ServicePropertiesMap(se.getReference());
        Dictionary<?,?> properties = se.getProperties();
        m_component.invokeCallbackMethod(instances, method,
            new Class[][]{
                {Component.class, ServiceReference.class, m_trackedServiceName},
                {Component.class, ServiceReference.class, Object.class}, 
                {Component.class, ServiceReference.class},
                {Component.class, m_trackedServiceName}, 
                {Component.class, Object.class}, 
                {Component.class},
                {Component.class, Map.class, m_trackedServiceName},
                {ServiceReference.class, m_trackedServiceName},
                {ServiceReference.class, Object.class}, 
                {ServiceReference.class},
                {m_trackedServiceName}, 
                {m_trackedServiceName, Map.class}, 
                {Map.class, m_trackedServiceName}, 
                {m_trackedServiceName, Dictionary.class}, 
                {Dictionary.class, m_trackedServiceName}, 
                {Object.class}, 
                {}},
            
            new Object[][]{
                {m_component, se.getReference(), se.getEvent()},
                {m_component, se.getReference(), se.getEvent()}, 
                {m_component, se.getReference()}, 
                {m_component, se.getEvent()},
                {m_component, se.getEvent()},
                {m_component},
                {m_component, propertiesMap, se.getEvent()},
                {se.getReference(), se.getEvent()},
                {se.getReference(), se.getEvent()}, 
                {se.getReference()}, 
                {se.getEvent()}, 
                {se.getEvent(), propertiesMap},
                {propertiesMap, se.getEvent()},
                {se.getEvent(), properties},
                {properties, se.getEvent()},
                {se.getEvent()}, 
                {}}
        );
    }
        
    public void invokeSwap(String swapMethod, ServiceReference previousReference, Object previous,
			ServiceReference currentReference, Object current, Object[] instances) {
    	if (m_debug) {
    		System.out.println("invoke swap: " + swapMethod + " on component " + m_component + ", instances: " + Arrays.toString(instances) + " - " + ((ComponentDeclaration)m_component).getState());
    	}
    	try {
		m_component.invokeCallbackMethod(instances, swapMethod,
				new Class[][]{
            		{m_trackedServiceName, m_trackedServiceName}, 
            		{Object.class, Object.class},
            		{ServiceReference.class, m_trackedServiceName, ServiceReference.class, m_trackedServiceName},
            		{ServiceReference.class, Object.class, ServiceReference.class, Object.class},
            		{Component.class, m_trackedServiceName, m_trackedServiceName}, 
            		{Component.class, Object.class, Object.class},
            		{Component.class, ServiceReference.class, m_trackedServiceName, ServiceReference.class, m_trackedServiceName},
            		{Component.class, ServiceReference.class, Object.class, ServiceReference.class, Object.class}}, 
	            
            	new Object[][]{
                    {previous, current}, 
                    {previous, current}, 
                    {previousReference, previous, currentReference, current},
                    {previousReference, previous, currentReference, current}, {m_component, previous, current},
                    {m_component, previous, current}, {m_component, previousReference, previous, currentReference, current},
                    {m_component, previousReference, previous, currentReference, current}}
			);
    	} catch (Throwable e) {
    	    m_component.getLogger().err("Could not invoke swap callback", e);
    	}
	}

	@Override
	public void swappedService(final ServiceReference reference, final Object service,
			final ServiceReference newReference, final Object newService) {
		if (m_swap != null) {
			// it will not trigger a state change, but the actual swap should be scheduled to prevent things
			// getting out of order.		    		    
		    // We delegate the swap handling to the ComponentImpl, which is the class responsible for state management.
		    // The ComponentImpl will first check if the component is in the proper state so the swap method can be invoked.		    
            m_component.handleEvent(this, EventType.SWAPPED,
                new ServiceEventImpl(m_component.getBundle(), m_component.getBundleContext(), reference, service),
                new ServiceEventImpl(m_component.getBundle(), m_component.getBundleContext(), newReference, newService));
		} else {
			addedService(newReference, newService);
			removedService(reference, service);
		}
	}	
}
