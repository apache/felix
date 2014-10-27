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
package org.apache.felix.dm.context;

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.felix.dm.ComponentDependencyDeclaration;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.impl.AbstractDecorator;
import org.apache.felix.dm.impl.AdapterServiceImpl;
import org.apache.felix.dm.impl.EventImpl;
import org.apache.felix.dm.impl.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * Abstract class for implementing Dependencies
 *
 * @param <T> The type of the interface representing a Dependency Manager Dependency (must extends the Dependency interface).
 */
public abstract class AbstractDependency<T extends Dependency> implements Dependency, DependencyContext, ComponentDependencyDeclaration
{
    protected volatile ComponentContext m_component;
    protected volatile boolean m_available; // volatile because accessed by getState method
    protected boolean m_instanceBound;
    protected volatile boolean m_required; // volatile because accessed by getState method
    protected String m_add;
    protected String m_change;
    protected String m_remove;
    protected boolean m_autoConfig;
    protected String m_autoConfigInstance;
    protected boolean m_autoConfigInvoked;
    protected volatile boolean m_isStarted; // volatile because accessed by getState method
    protected Object m_callbackInstance;
    protected volatile boolean m_propagate;
    protected volatile Object m_propagateCallbackInstance;
    protected volatile String m_propagateCallbackMethod;
    protected final BundleContext m_context;
    protected final Bundle m_bundle;
    protected final static Dictionary<String, Object> EMPTY_PROPERTIES = new Hashtable<>(0);
    protected final Logger m_logger;
    
    public AbstractDependency() {
        this(true, null);
    }

    public AbstractDependency(boolean autoConfig, BundleContext bc) {
        this(autoConfig, bc, null);
    }
    
    public AbstractDependency(boolean autoConfig, BundleContext bc, Logger logger) {
        m_autoConfig = autoConfig;
        m_context = bc;
        m_bundle = m_context != null ? m_context.getBundle() : null;
        m_logger = logger;
    }

    public AbstractDependency(AbstractDependency<T> prototype) {
        m_component = prototype.m_component;
        m_instanceBound = prototype.m_instanceBound;
        m_required = prototype.m_required;
        m_add = prototype.m_add;
        m_change = prototype.m_change;
        m_remove = prototype.m_remove;
        m_autoConfig = prototype.m_autoConfig;
        m_autoConfigInstance = prototype.m_autoConfigInstance;
        m_autoConfigInvoked = prototype.m_autoConfigInvoked;
        m_callbackInstance = prototype.m_callbackInstance;
        m_propagate = prototype.m_propagate;
        m_propagateCallbackInstance = prototype.m_propagateCallbackInstance;
        m_propagateCallbackMethod = prototype.m_propagateCallbackMethod;
        m_context = prototype.m_context;
        m_bundle = prototype.m_bundle;
        m_logger = prototype.m_logger;
    }

    // ----------------------- Dependency interface -----------------------------

    @Override
    public boolean isRequired() {
        return m_required;
    }

    @Override
    public boolean isAvailable() {
        return m_available;
    }

    @Override
    public boolean isAutoConfig() {
        return m_autoConfig;
    }

    @Override
    public String getAutoConfigName() {
        return m_autoConfigInstance;
    }

    @Override
    public boolean isPropagated() {
        return m_propagate;
    }

    @Override
    public Dictionary<String, Object> getProperties() {
        return EMPTY_PROPERTIES;
    }

    // -------------- DependencyContext -----------------------------------------------

    @Override
    public void invokeAdd(Event e) {
        if (m_add != null) {
            // If the add callback is not found and if the component instance is not an abstract decorator, then
            // log a warn message. (AbstractDecorator, like Aspect or Adapter are not interested in user dependency callbacks).
            if (! invoke(m_add, e) && ! (m_component.getInstance() instanceof AbstractDecorator)) {
                callbackNotFound(m_add);
            }
        }
    }

    @Override
    public void invokeChange(Event e) {
        if (m_change != null) {
            // If the change callback is not found and if the component instance is not an abstract decorator, then
            // log a warn message. (AbstractDecorator, like Aspect or Adapter are not interested in user dependency callbacks).
            if (! invoke(m_change, e) && ! (m_component.getInstance() instanceof AbstractDecorator)) {
                callbackNotFound(m_change);
            }
        }
    }

    @Override
    public void invokeRemove(Event e) {
        if (m_remove != null) {
            // If the remove callback is not found and if the component instance is not an abstract decorator, then
            // log a warn message. (AbstractDecorator, like Aspect or Adapter are not interested in user dependency callbacks).
            if (! invoke(m_remove, e) && ! (m_component.getInstance() instanceof AbstractDecorator)) {
                callbackNotFound(m_remove);
            }
        }
    }

    @Override
    public void invokeSwap(Event event, Event newEvent) {
        // Has to be implemented by sub classes
        throw new IllegalStateException("This method must be implemented by the class " + getClass().getName());
    }

    public void add(final Event e) {
        // since this method can be invoked by anyone from any thread, we need to
        // pass on the event to a runnable that we execute using the component's
        // executor
        m_component.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                m_component.handleAdded(AbstractDependency.this, e);
            }
        });
    }

    public void change(final Event e) {
        // since this method can be invoked by anyone from any thread, we need to
        // pass on the event to a runnable that we execute using the component's
        // executor
        m_component.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                m_component.handleChanged(AbstractDependency.this, e);
            }
        });
    }

    public void remove(final Event e) {
        // since this method can be invoked by anyone from any thread, we need to
        // pass on the event to a runnable that we execute using the component's
        // executor
        m_component.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    m_component.handleRemoved(AbstractDependency.this, e);
                } finally {
                    e.close();
                }
            }
        });
    }

    public void swap(final Event event, final Event newEvent) {
        // since this method can be invoked by anyone from any thread, we need to
        // pass on the event to a runnable that we execute using the component's
        // executor
        m_component.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    m_component.handleSwapped(AbstractDependency.this, event, newEvent);
                } finally {
                    event.close();
                }
            }
        });
    }

    @Override
    public void setComponentContext(ComponentContext component) {
        m_component = component;
    }

    @Override
    public void start() {
        if (!m_isStarted) {
            startTracking();
            m_isStarted = true;
        }
    }

    @Override
    public void stop() {
        if (m_isStarted) {
            stopTracking();
            m_isStarted = false;
        }
    }

    @Override
    public boolean isStarted() {
        return m_isStarted;
    }

    @Override
    public void setAvailable(boolean available) {
        m_available = available;
    }

    @SuppressWarnings("unchecked")
    public T setPropagate(boolean propagate) {
        ensureNotActive();
        m_propagate = propagate;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setPropagate(Object instance, String method) {
        setPropagate(instance != null && method != null);
        m_propagateCallbackInstance = instance;
        m_propagateCallbackMethod = method;
        return (T) this;
    }

    public boolean isInstanceBound() {
        return m_instanceBound;
    }

    public void setInstanceBound(boolean instanceBound) {
        m_instanceBound = instanceBound;
    }

    public T setCallbacks(String add, String remove) {
        return setCallbacks(add, null, remove);
    }

    public T setCallbacks(String add, String change, String remove) {
        return setCallbacks(null, add, change, remove);
    }

    public T setCallbacks(Object instance, String add, String remove) {
        return setCallbacks(instance, add, null, remove);
    }

    @SuppressWarnings("unchecked")
    public T setCallbacks(Object instance, String add, String change, String remove) {
        if ((add != null || change != null || remove != null) && !m_autoConfigInvoked) {
            setAutoConfig(false);
        }
        m_callbackInstance = instance;
        m_add = add;
        m_change = change;
        m_remove = remove;
        return (T) this;
    }

    public Object[] getInstances() {
        if (m_callbackInstance == null) {
            return m_component.getInstances();
        } else {
            return new Object[] { m_callbackInstance };
        }
    }

    public boolean invoke(String method, Event e, Object[] instances) {
        // specific for this type of dependency
        return m_component.invokeCallbackMethod(instances, method, new Class[][] { {} }, new Object[][] { {} });
    }

    public boolean invoke(String method, Event e) {
        return invoke(method, e, getInstances());
    }

    @SuppressWarnings("unchecked")
    public T setRequired(boolean required) {
        m_required = required;
        return (T) this;
    }

    @Override
    public boolean needsInstance() {
        return false;
    }

    @Override
    public Class<?> getAutoConfigType() {
        return null; // must be implemented by subclasses if autoconfig mode is enabled
    }

    @SuppressWarnings("unchecked")
    public T setAutoConfig(boolean autoConfig) {
        m_autoConfig = autoConfig;
        m_autoConfigInvoked = true;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setAutoConfig(String instanceName) {
        m_autoConfig = (instanceName != null);
        m_autoConfigInstance = instanceName;
        m_autoConfigInvoked = true;
        return (T) this;
    }

    @Override
    public Event getService() {
        Event event = m_component.getDependencyEvent(this);
        if (event == null) {
            Object defaultService = getDefaultService();
            if (defaultService != null) {
                event = new EventImpl(0, defaultService);
            }
        }
        return event;
    }

    @Override
    public void copyToCollection(Collection<Object> services) {
        Set<Event> events = m_component.getDependencyEvents(this);
        if (events.size() > 0) {
            for (Event e : events) {
                services.add(e.getEvent());
            }
        } else {
            Object defaultService = getDefaultService();
            if (defaultService != null) {
                services.add(defaultService);
            }
        }
    }

    @Override
    public void copyToMap(Map<Object, Dictionary<String, ?>> map) {
        Set<Event> events = m_component.getDependencyEvents(this);
        if (events.size() > 0) {
            for (Event e : events) {
                map.put(e.getEvent(), e.getProperties());
            }
        } else {
            Object defaultService = getDefaultService();
            if (defaultService != null) {
                map.put(defaultService, EMPTY_PROPERTIES);
            }
        }
    }

    public ComponentContext getComponentContext() {
        return m_component;
    }

    protected Object getDefaultService() {
        return null;
    }

    protected void ensureNotActive() {
        if (isStarted()) {
            throw new IllegalStateException("Cannot modify state while active.");
        }
    }

    protected void startTracking() {
    }

    protected void stopTracking() {
    }

    @Override
    public abstract DependencyContext createCopy();

    // -------------- DependencyContext -----------------------------------------------

    @Override
    public abstract String getName();

    @Override
    public abstract String getType();

    @Override
    public int getState() { // Can be called from any threads, but our class attributes are volatile
        if (m_isStarted) {
            return (isAvailable() ? 1 : 0) + (isRequired() ? 2 : 0);
        } else {
            return isRequired() ? ComponentDependencyDeclaration.STATE_REQUIRED
                : ComponentDependencyDeclaration.STATE_OPTIONAL;
        }
    }
    
    // -------------- Private methods ---------------------------------------------------
    
    private void callbackNotFound(String callback) {
        if (m_logger == null) {
        System.out.println("Dependency \"" + callback + "\" callback not found on componnent instances "
            + Arrays.toString(m_component.getInstances()));
        } else {
            m_logger.log(LogService.LOG_WARNING, "Dependency \"" + callback + "\" callback not found on componnent instances "
                + Arrays.toString(m_component.getInstances()));
        }
    }
}
