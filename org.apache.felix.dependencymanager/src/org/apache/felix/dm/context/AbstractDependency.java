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

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.felix.dm.ComponentDependencyDeclaration;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.ServiceDependency;

/**
 * Abstract class for implementing Dependencies.
 * You can extends this class in order to supply your own custom dependencies to any Dependency Manager Component.
 *
 * @param <T> The type of the interface representing a Dependency Manager Dependency (must extends the Dependency interface).
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class AbstractDependency<T extends Dependency> implements
    Dependency, DependencyContext, ComponentDependencyDeclaration {

    /**
     * The Component implementation is exposed to Dependencies through this interface.
     */
    protected ComponentContext m_component;

    /**
     * Is this Dependency available ? Volatile because the getState method (part of the 
     * {@link ComponentDependencyDeclaration} interface) may be called by any thread, at any time.
     */
    protected volatile boolean m_available;

    /**
     * Is this Dependency "instance bound" ? A dependency is "instance bound" if it is defined within the component's 
     * init method, meaning that it won't deactivate the component if it is not currently available when being added
     * from the component's init method.
     */
    protected boolean m_instanceBound;

    /**
     * Is this dependency required (false by default) ?
     */
    protected volatile boolean m_required;

    /**
     * Component callback used to inject an added dependency.
     */
    protected volatile String m_add;

    /**
     * Component callback invoked when the dependency has changed.
     */
    protected volatile String m_change;

    /**
     * Component callback invoked when the dependency becomes unavailable.
     */
    protected volatile String m_remove;

    /**
     * Can this Dependency be auto configured in the component instance fields ?
     */
    protected volatile boolean m_autoConfig = true;

    /**
     * The Component field name where the Dependency can be injected (null means any field with a compatible type
     * will be injected).
     */
    protected volatile String m_autoConfigInstance;

    /**
     * Indicates if the setAutoConfig method has been invoked. This flag is used to force autoconfig to "false" 
     * when the setCallbacks method is invoked, unless the setAutoConfig method has been called.
     */
    protected volatile boolean m_autoConfigInvoked;

    /**
     * Has this Dependency been started by the Component implementation ? Volatile because the getState method 
     * (part of the {@link ComponentDependencyDeclaration} interface) may be called by any thread, at any time.
     */
    protected volatile boolean m_isStarted;

    /**
     * The object instance on which the dependency callbacks are invoked on. Null means the dependency will be
     * injected to the Component implementation instance(s).
     */
    protected volatile Object m_callbackInstance;

    /**
     * Tells if the dependency service properties have to be propagated to the Component service properties.
     */
    protected volatile boolean m_propagate;

    /**
     * The propagate callback instance that is invoked in order to supply dynamically some dependency service properties.
     */
    protected volatile Object m_propagateCallbackInstance;

    /**
     * The propagate callback method that is invoked in order to supply dynamically some dependency service properties.
     * @see {@link #m_propagateCallbackInstance}
     */
    protected volatile String m_propagateCallbackMethod;

    /**
     * Default empty dependency properties.
     */
    protected final static Dictionary<Object, Object> EMPTY_PROPERTIES = new Hashtable<>(0);

    /**
     * Creates a new Dependency. By default, the dependency is optional and autoconfig.
     */
    public AbstractDependency() {
    }

    /**
     * Create a clone of a given Dependency.
     * @param prototype all the fields of the prototype will be copied to this dependency.
     */
    public AbstractDependency(AbstractDependency<T> prototype) {
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
    }
    
    @Override
    public String toString() {
        return new StringBuilder(getType()).append(" dependency [").append(getName()).append("]").toString();
    }

    // ----------------------- Dependency interface -----------------------------

    /**
     * Is this Dependency required (false by default) ?
     */
    @Override
    public boolean isRequired() {
        return m_required;
    }

    /**
     * Is this Dependency satisfied and available ?
     */
    @Override
    public boolean isAvailable() {
        return m_available;
    }

    /**
     * Can this dependency be injected in a component class field (by reflexion, true by default) ?
     */
    @Override
    public boolean isAutoConfig() {
        return m_autoConfig;
    }

    /**
     * Returns the field name when the dependency can be injected to.
     */
    @Override
    public String getAutoConfigName() {
        return m_autoConfigInstance;
    }

    /**
     * Returns the propagate callback method that is invoked in order to supply dynamically some dependency service properties.
     * @see {@link #m_propagateCallbackInstance}
     */
    @Override
    public boolean isPropagated() {
        return m_propagate;
    }

    /**
     * Returns the dependency service properties (empty by default).
     */
    @SuppressWarnings("unchecked")
    @Override
    public <K,V> Dictionary<K, V> getProperties() {
        return (Dictionary<K, V>) EMPTY_PROPERTIES;
    }

    // -------------- DependencyContext interface -----------------------------------------------

    /**
     * Called by the Component implementation before the Dependency can be started.
     */
    @Override
    public void setComponentContext(ComponentContext component) {
        m_component = component;
    }

    /**
     * A Component callback must be invoked with dependency event(s).
     * @param type the dependency event type
     * @param events the dependency service event to inject in the component. 
     * The number of events depends on the dependency event type: ADDED/CHANGED/REMOVED types only has one event parameter, 
     * but the SWAPPED type has two event parameters: the first one is the old event which must be replaced by the second one.
     */
    @Override
    public void invokeCallback(EventType type, Event ... events) {
    }

    /**
     * Starts this dependency. Subclasses can override this method but must then call super.start().
     */
    @Override
    public void start() {
        m_isStarted = true;
    }

    /**
     * Starts this dependency. Subclasses can override this method but must then call super.stop().
     */
    @Override
    public void stop() {
        m_isStarted = false;
    }

    /**
     * Indicates if this dependency has been started by the Component implementation.
     */
    @Override
    public boolean isStarted() {
        return m_isStarted;
    }

    /**
     * Called by the Component implementation when the dependency is considered to be available.
     */
    @Override
    public void setAvailable(boolean available) {
        m_available = available;
    }

    /**
     * Is this Dependency "instance bound" (has been defined within the component's init method) ?
     */
    public boolean isInstanceBound() {
        return m_instanceBound;
    }

    /**
     * Called by the Component implementation when the dependency is declared within the Component's init method.
     */
    public void setInstanceBound(boolean instanceBound) {
        m_instanceBound = instanceBound;
    }

    /**
     * Tells if the Component must be first instantiated before starting this dependency (false by default).
     */
    @Override
    public boolean needsInstance() {
        return false;
    }

    /**
     * Returns the type of the field where this dependency can be injected (auto config), or return null
     * if autoconfig is not supported.
     */
    @Override
    public abstract Class<?> getAutoConfigType();

    /**
     * Get the highest ranked available dependency service, or null.
     */
    @Override
    public Event getService() {
        Event event = m_component.getDependencyEvent(this);
        if (event == null) {
            Object defaultService = getDefaultService(true);
            if (defaultService != null) {
                event = new Event(defaultService);
            }
        }
        return event;
    }

    /**
     * Copy all dependency service instances to the given collection.
     */
    @Override
    public void copyToCollection(Collection<Object> services) {
        Set<Event> events = m_component.getDependencyEvents(this);
        if (events.size() > 0) {
            for (Event e : events) {
                services.add(e.getEvent());
            }
        } else {
            Object defaultService = getDefaultService(false);
            if (defaultService != null) {
                services.add(defaultService);
            }
        }
    }

    /**
     * Copy all dependency service instances to the given map (key = dependency service, value = dependency service properties.
     */
    @Override
    public void copyToMap(Map<Object, Dictionary<?, ?>> map) {
        Set<Event> events = m_component.getDependencyEvents(this);
        if (events.size() > 0) {
            for (Event e : events) {
                map.put(e.getEvent(), e.getProperties());
            }
        } else {
            Object defaultService = getDefaultService(false);
            if (defaultService != null) {
                map.put(defaultService, EMPTY_PROPERTIES);
            }
        }
    }

    /**
     * Creates a copy of this Dependency.
     */
    @Override
    public abstract DependencyContext createCopy();

    // -------------- ComponentDependencyDeclaration -----------------------------------------------

    /**
     * Returns a description of this dependency (like the dependency service class name with associated filters)
     */
    @Override
    public String getName() {
        return getSimpleName();
    }

    /**
     * Returns a simple name for this dependency (like the dependency service class name).
     */
    @Override
    public abstract String getSimpleName();

    /**
     * Returns the dependency symbolic type.
     */
    @Override
    public abstract String getType();

    /**
     * Returns the dependency filter, if any.
     */
    @Override
    public String getFilter() {
        return null;
    }

    /**
     * Returns this dependency state.
     */
    @Override
    public int getState() { // Can be called from any threads, but our class attributes are volatile
        if (m_isStarted) {
            return (isAvailable() ? 1 : 0) + (isRequired() ? 2 : 0);
        } else {
            return isRequired() ? ComponentDependencyDeclaration.STATE_REQUIRED
                : ComponentDependencyDeclaration.STATE_OPTIONAL;
        }
    }

    // -------------- Methods common to sub interfaces of Dependendency

    /**
     * Activates Dependency service properties propagation (to the service properties of the component to which this
     * dependency is added).
     * 
     * @param propagate true if the dependency service properties must be propagated to the service properties of 
     * the component to which this dependency is added. 
     * @return this dependency instance
     */
    @SuppressWarnings("unchecked")
    public T setPropagate(boolean propagate) {
        ensureNotActive();
        m_propagate = propagate;
        return (T) this;
    }

    /**
     * Sets a callback instance which can ba invoked with the given method in order to dynamically retrieve the 
     * dependency service properties. 
     * 
     * @param instance the callback instance
     * @param method the method to invoke on the callback instance
     * @return this dependency instance
     */
    @SuppressWarnings("unchecked")
    public T setPropagate(Object instance, String method) {
        setPropagate(instance != null && method != null);
        m_propagateCallbackInstance = instance;
        m_propagateCallbackMethod = method;
        return (T) this;
    }

    /**
     * Sets the add/remove callbacks.
     * @param add the callback to invoke when a dependency is added
     * @param remove the callback to invoke when a dependency is removed
     * @return this dependency instance
     */
    public T setCallbacks(String add, String remove) {
        return setCallbacks(add, null, remove);
    }

    /**
     * Sets the add/change/remove callbacks.
     * @param add the callback to invoke when a dependency is added
     * @param change the callback to invoke when a dependency has changed
     * @param remove the callback to invoke when a dependency is removed
     * @return this dependency instance
     */
    public T setCallbacks(String add, String change, String remove) {
        return setCallbacks(null, add, change, remove);
    }

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added or removed. They are called on the instance you provide. When you
     * specify callbacks, the auto configuration feature is automatically turned off, because
     * we're assuming you don't need it in this case.
     * 
     * @param instance the instance to call the callbacks on
     * @param add the method to call when a service was added
     * @param remove the method to call when a service was removed
     * @return this service dependency
     */
    public T setCallbacks(Object instance, String add, String remove) {
        return setCallbacks(instance, add, null, remove);
    }

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added, changed or removed. They are called on the instance you provide. When you
     * specify callbacks, the auto configuration feature is automatically turned off, because
     * we're assuming you don't need it in this case.
     * 
     * @param instance the instance to call the callbacks on
     * @param add the method to call when a service was added
     * @param change the method to call when a service was changed
     * @param remove the method to call when a service was removed
     * @return this service dependency
     */
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

    /**
     * Returns the dependency callback instances
     * @return the dependency callback instances
     */
    public Object[] getInstances() {
        if (m_callbackInstance == null) {
            return m_component.getInstances();
        } else {
            return new Object[] { m_callbackInstance };
        }
    }

    /**
     * @see {@link ServiceDependency#setRequired(boolean)}
     */
    @SuppressWarnings("unchecked")
    public T setRequired(boolean required) {
        m_required = required;
        return (T) this;
    }

    /**
     * @see {@link ServiceDependency#setAutoConfig(boolean)}
     */
    @SuppressWarnings("unchecked")
    public T setAutoConfig(boolean autoConfig) {
        if (autoConfig && getAutoConfigType() == null) {
            throw new IllegalStateException("Dependency does not support auto config mode");
        }
        m_autoConfig = autoConfig;
        m_autoConfigInvoked = true;
        return (T) this;
    }

    /**
     * @see {@link ServiceDependency#setAutoConfig(String instanceName)}
     */
    @SuppressWarnings("unchecked")
    public T setAutoConfig(String instanceName) {
        if (instanceName != null && getAutoConfigType() == null) {
            throw new IllegalStateException("Dependency does not support auto config mode");
        }
        m_autoConfig = (instanceName != null);
        m_autoConfigInstance = instanceName;
        m_autoConfigInvoked = true;
        return (T) this;
    }

    /**
     * Returns the component implementation context
     * @return the component implementation context
     */
    public ComponentContext getComponentContext() {
        return m_component;
    }

    /**
     * Returns the default service, or null.
     * @param nullObject if true, a null object may be returned.
     * @return the default service
     */
    protected Object getDefaultService(boolean nullObject) {
        return null;
    }

    /**
     * Checks if the component dependency is not started.
     */
    protected void ensureNotActive() {
        if (isStarted()) {
            throw new IllegalStateException("Cannot modify state while active.");
        }
    }
}
