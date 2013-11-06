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
package org.apache.felix.dm.impl.dependencies;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDependencyDeclaration;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyService;
import org.apache.felix.dm.InvocationUtil;
import org.apache.felix.dm.ServiceDependency;
import org.apache.felix.dm.ServiceUtil;
import org.apache.felix.dm.impl.BlockingSerialExecutor;
import org.apache.felix.dm.impl.DefaultNullObject;
import org.apache.felix.dm.impl.Logger;
import org.apache.felix.dm.tracker.ServiceTracker;
import org.apache.felix.dm.tracker.ServiceTrackerCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * Service dependency that can track an OSGi service.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceDependencyImpl extends DependencyBase implements ServiceDependency, ServiceTrackerCustomizer,
        ComponentDependencyDeclaration {
    protected final List m_services = new ArrayList();
    protected volatile ServiceTracker m_tracker;
    protected final BundleContext m_context;
    protected volatile Class m_trackedServiceName;
    private volatile Object m_nullObject;
    private volatile String m_trackedServiceFilter;
    private volatile String m_trackedServiceFilterUnmodified;
    private volatile ServiceReference m_trackedServiceReference;
    private volatile Object m_callbackInstance;
    private volatile String m_callbackAdded;
    private volatile String m_callbackChanged;
    private volatile String m_callbackRemoved;
    private volatile String m_callbackSwapped;
    private boolean m_autoConfig;
    private volatile String m_autoConfigInstance;
    private boolean m_autoConfigInvoked;
    private volatile Object m_defaultImplementation;
    private volatile Object m_defaultImplementationInstance;
    private boolean m_isAvailable;
    private volatile boolean m_propagate;
    private volatile Object m_propagateCallbackInstance;
    private volatile String m_propagateCallbackMethod;
    private final Map m_sr = new HashMap(); /* <DependencyService, Set<Tuple<ServiceReference, Object>> */
    private final Map m_componentByRank = new HashMap(); /* <Component, Map<Long, Map<Integer, Tuple>>> */
    private volatile boolean m_debug = false;
    private volatile String m_debugKey = null;

    /**
     * Executor used to ensure proper synchronization without holding locks. 
     */
    private final BlockingSerialExecutor _serial = new BlockingSerialExecutor();

    // ----------------------- Inner classes --------------------------------------------------------------

    private static final Comparator COMPARATOR = new Comparator() {
        public int getRank(ServiceReference ref) {
            Object ranking = ref.getProperty(Constants.SERVICE_RANKING);
            if (ranking != null && (ranking instanceof Integer)) {
                return ((Integer) ranking).intValue();
            }
            return 0;
        }

        public int compare(Object a, Object b) {
            ServiceReference ra = (ServiceReference) a, rb = (ServiceReference) b;
            int ranka = getRank(ra);
            int rankb = getRank(rb);
            if (ranka < rankb) {
                return -1;
            } else if (ranka > rankb) {
                return 1;
            }
            return 0;
        }
    };

    private static final class Tuple /* <ServiceReference, Object> */{
        private final ServiceReference m_serviceReference;
        private final Object m_service;

        public Tuple(ServiceReference first, Object last) {
            m_serviceReference = first;
            m_service = last;
        }

        public ServiceReference getServiceReference() {
            return m_serviceReference;
        }

        public Object getService() {
            return m_service;
        }

        public boolean equals(Object obj) {
            return ((Tuple) obj).getServiceReference().equals(getServiceReference());
        }

        public int hashCode() {
            return m_serviceReference.hashCode();
        }
    }

    /**
     * Entry to wrap service properties behind a Map.
     */
    private static final class ServicePropertiesMapEntry implements Map.Entry {
        private final String m_key;
        private Object m_value;

        public ServicePropertiesMapEntry(String key, Object value) {
            m_key = key;
            m_value = value;
        }

        public Object getKey() {
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

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry e = (Map.Entry) o;
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
    private final static class ServicePropertiesMap extends AbstractMap {
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

        public Set entrySet() {
            Set set = new HashSet();
            String[] keys = m_ref.getPropertyKeys();
            for (int i = 0; i < keys.length; i++) {
                set.add(new ServicePropertiesMapEntry(keys[i], m_ref.getProperty(keys[i])));
            }
            return set;
        }
    }

    // ----------------------- Public methods -----------------------------------------------------------

    /**
     * Creates a new service dependency.
     * 
     * @param context the bundle context
     * @param logger the logger
     */
    public ServiceDependencyImpl(BundleContext context, Logger logger) {
        super(logger);
        m_context = context;
        m_autoConfig = true;
    }

    /** Copying constructor that clones an existing instance. */
    public ServiceDependencyImpl(ServiceDependencyImpl prototype) {
        super(prototype);
        synchronized (prototype) {
            m_context = prototype.m_context;
            m_autoConfig = prototype.m_autoConfig;
            m_trackedServiceName = prototype.m_trackedServiceName;
            m_nullObject = prototype.m_nullObject;
            m_trackedServiceFilter = prototype.m_trackedServiceFilter;
            m_trackedServiceFilterUnmodified = prototype.m_trackedServiceFilterUnmodified;
            m_trackedServiceReference = prototype.m_trackedServiceReference;
            m_callbackInstance = prototype.m_callbackInstance;
            m_callbackAdded = prototype.m_callbackAdded;
            m_callbackChanged = prototype.m_callbackChanged;
            m_callbackRemoved = prototype.m_callbackRemoved;
            m_callbackSwapped = prototype.m_callbackSwapped;
            m_autoConfigInstance = prototype.m_autoConfigInstance;
            m_defaultImplementation = prototype.m_defaultImplementation;
        }
    }

    //@Override
    public ServiceDependency setDebug(String identifier) {
        this.m_debug = true;
        this.m_debugKey = identifier;
        return this;
    }

    //@Override
    public Dependency createCopy() {
        return new ServiceDependencyImpl(this);
    }

    //@Override
    public synchronized boolean isAutoConfig() {
        return m_autoConfig;
    }

    //@Override
    public synchronized boolean isAvailable() {
        return m_isAvailable;
    }

    //@Override
    public void start(final DependencyService service) {
        _serial.execute(new Runnable() {
            public void run() {
                // this code is executed exclusively and without holding any locks
                _start(service);
            }
        });
    }

    //@Override
    public void stop(final DependencyService service) {
        _serial.execute(new Runnable() {
            public void run() {
                // this code is executed exclusively and without holding any locks
                _stop(service);
            }
        });
    }

    //@Override
    public Object addingService(ServiceReference ref) {
        Object service = m_context.getService(ref);
        // first check to make sure the service is actually an instance of our service
        if (!m_trackedServiceName.isInstance(service)) {
            return null;
        }
        return service;
    }

    //@Override
    public void addedService(final ServiceReference ref, final Object service) {
        _serial.execute(new Runnable() {
            public void run() {
                // this code is executed exclusively and without holding any locks
                _addedService(ref, service);
            }
        });
    }

    //@Override
    public void modifiedService(final ServiceReference ref, final Object service) {
        _serial.execute(new Runnable() {
            public void run() {
                // this code is executed exclusively and without holding any locks
                _modifiedService(ref, service);
            }
        });
    }

    //@Override
    public void removedService(final ServiceReference ref, final Object service) {
        _serial.execute(new Runnable() {
            public void run() {
                // this code is executed exclusively and without holding any locks
                _removedService(ref, service);
            }
        });
    }

    //@Override
    public void invokeAdded(final DependencyService service) {
        _serial.execute(new Runnable() {
            public void run() {
                // this code is executed exclusively and without holding any locks
                _invokeAdded(service);
            }
        });
    }

    //@Override
    public void invokeRemoved(final DependencyService service) {
        _serial.execute(new Runnable() {
            public void run() {
                // this code is executed exclusively and without holding any locks
                _invokeRemoved(service);
            }
        });
    }

    //@Override
    public synchronized String toString() {
        return "ServiceDependency[" + m_trackedServiceName + " " + m_trackedServiceFilterUnmodified + "]";
    }

    //@Override
    public String getAutoConfigName() {
        return m_autoConfigInstance;
    }

    //@Override
    public Object getAutoConfigInstance() {
        return lookupService();
    }

    //@Override
    public Class getAutoConfigType() {
        return getInterface();
    }

    //@Override
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

    //@Override
    public String getType() {
        return "service";
    }

    //@Override
    public Dictionary getProperties() {
        ServiceReference reference = lookupServiceReference();
        Object service = lookupService();
        if (reference != null) {
            if (m_propagateCallbackInstance != null && m_propagateCallbackMethod != null) {
                try {
                    return (Dictionary) InvocationUtil.invokeCallbackMethod(m_propagateCallbackInstance, m_propagateCallbackMethod,
                            new Class[][]{{ServiceReference.class, Object.class}, {ServiceReference.class}}, new Object[][]{
                                    {reference, service}, {reference}});
                } catch (InvocationTargetException e) {
                    m_logger.log(LogService.LOG_WARNING, "Exception while invoking callback method", e.getCause());
                } catch (Exception e) {
                    m_logger.log(LogService.LOG_WARNING, "Exception while trying to invoke callback method", e);
                }
                throw new IllegalStateException("Could not invoke callback");
            } else {
                Properties props = new Properties();
                String[] keys = reference.getPropertyKeys();
                for (int i = 0; i < keys.length; i++) {
                    if (!(keys[i].equals(Constants.SERVICE_ID) || keys[i].equals(Constants.SERVICE_PID))) {
                        props.put(keys[i], reference.getProperty(keys[i]));
                    }
                }
                return props;
            }
        } else {
            throw new IllegalStateException("cannot find service reference");
        }
    }

    //@Override
    public boolean isPropagated() {
        return m_propagate;
    }

    // ----- CREATION

    /**
     * Sets the name of the service that should be tracked. 
     * 
     * @param serviceName the name of the service
     * @return this service dependency
     */
    //@Override
    public synchronized ServiceDependency setService(Class serviceName) {
        setService(serviceName, null, null);
        return this;
    }

    /**
     * Sets the name of the service that should be tracked. You can either specify
     * only the name, only the filter, or the name and a filter.
     * <p>
     * If you specify name and filter, the filter is used
     * to track the service and should only return services of the type that was specified
     * in the name. To make sure of this, the filter is actually extended internally to
     * filter on the correct name.
     * <p>
     * If you specify only the filter, the name is assumed to be a service of type
     * <code>Object</code> which means that, when auto configuration is on, instances
     * of that service will be injected in any field of type <code>Object</code>.
     * 
     * @param serviceName the name of the service
     * @param serviceFilter the filter condition
     * @return this service dependency
     */
    //@Override
    public synchronized ServiceDependency setService(Class serviceName, String serviceFilter) {
        setService(serviceName, null, serviceFilter);
        return this;
    }

    /**
     * Sets the name of the service that should be tracked. The name is assumed to be 
     * a service of type <code>Object</code> which means that, when auto configuration 
     * is on, instances of that service will be injected in any field of type 
     * <code>Object</code>.
     * 
     * @param serviceFilter the filter condition
     * @return this service dependency
     */
    //@Override
    public synchronized ServiceDependency setService(String serviceFilter) {
        if (serviceFilter == null) {
            throw new IllegalArgumentException("Service filter cannot be null.");
        }
        setService(null, null, serviceFilter);
        return this;
    }

    /**
     * Sets the name of the service that should be tracked. By specifying the service
     * reference of the service you want to track, you can directly track a single
     * service. The name you use must match the type of service referred to by the
     * service reference and it is up to you to make sure that is the case.
     * 
     * @param serviceName the name of the service
     * @param serviceReference the service reference to track
     * @return this service dependency
     */
    //@Override
    public synchronized ServiceDependency setService(Class serviceName, ServiceReference serviceReference) {
        setService(serviceName, serviceReference, null);
        return this;
    }

    /**
     * Sets the default implementation for this service dependency. You can use this to supply
     * your own implementation that will be used instead of a Null Object when the dependency is
     * not available. This is also convenient if the service dependency is not an interface
     * (which would cause the Null Object creation to fail) but a class.
     * 
     * @param implementation the instance to use or the class to instantiate if you want to lazily
     *     instantiate this implementation
     * @return this service dependency
     */
    //@Override
    public synchronized ServiceDependency setDefaultImplementation(Object implementation) {
        ensureNotActive();
        m_defaultImplementation = implementation;
        return this;
    }

    /**
     * Sets the required flag which determines if this service is required or not.
     * 
     * @param required the required flag
     * @return this service dependency
     */
    //@Override
    public synchronized ServiceDependency setRequired(boolean required) {
        ensureNotActive();
        setIsRequired(required);
        return this;
    }

    //@Override
    public ServiceDependency setInstanceBound(boolean isInstanceBound) {
        setIsInstanceBound(isInstanceBound);
        return this;
    }

    /**
     * Sets auto configuration for this service. Auto configuration allows the
     * dependency to fill in any attributes in the service implementation that
     * are of the same type as this dependency. Default is on.
     * 
     * @param autoConfig the value of auto config
     * @return this service dependency
     */
    //@Override
    public synchronized ServiceDependency setAutoConfig(boolean autoConfig) {
        ensureNotActive();
        m_autoConfig = autoConfig;
        m_autoConfigInvoked = true;
        return this;
    }

    /**
     * Sets auto configuration for this service. Auto configuration allows the
     * dependency to fill in the attribute in the service implementation that
     * has the same type and instance name.
     * 
     * @param instanceName the name of attribute to auto config
     * @return this service dependency
     */
    //@Override
    public synchronized ServiceDependency setAutoConfig(String instanceName) {
        ensureNotActive();
        m_autoConfig = (instanceName != null);
        m_autoConfigInstance = instanceName;
        m_autoConfigInvoked = true;
        return this;
    }

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added or removed. When you specify callbacks, the auto configuration 
     * feature is automatically turned off, because we're assuming you don't need it in this 
     * case.
     * 
     * @param added the method to call when a service was added
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
    //@Override
    public synchronized ServiceDependency setCallbacks(String added, String removed) {
        return setCallbacks((Object) null, added, null, removed);
    }

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added, changed or removed. When you specify callbacks, the auto 
     * configuration feature is automatically turned off, because we're assuming you don't 
     * need it in this case.
     * 
     * @param added the method to call when a service was added
     * @param changed the method to call when a service was changed
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
    //@Override
    public synchronized ServiceDependency setCallbacks(String added, String changed, String removed) {
        return setCallbacks((Object) null, added, changed, removed);
    }

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added, changed or removed. When you specify callbacks, the auto 
     * configuration feature is automatically turned off, because we're assuming you don't 
     * need it in this case.
     * @param added the method to call when a service was added
     * @param changed the method to call when a service was changed
     * @param removed the method to call when a service was removed
     * @param swapped the method to call when the service was swapped due to addition or 
     * removal of an aspect
     * @return this service dependency
     */
    //@Override
    public synchronized ServiceDependency setCallbacks(String added, String changed, String removed, String swapped) {
        return setCallbacks((Object) null, added, changed, removed, swapped);
    }

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added or removed. They are called on the instance you provide. When you
     * specify callbacks, the auto configuration feature is automatically turned off, because
     * we're assuming you don't need it in this case.
     * 
     * @param instance the instance to call the callbacks on
     * @param added the method to call when a service was added
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
    //@Override
    public synchronized ServiceDependency setCallbacks(Object instance, String added, String removed) {
        return setCallbacks(instance, added, (String) null, removed);
    }

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added, changed or removed. They are called on the instance you provide. When you
     * specify callbacks, the auto configuration feature is automatically turned off, because
     * we're assuming you don't need it in this case.
     * 
     * @param instance the instance to call the callbacks on
     * @param added the method to call when a service was added
     * @param changed the method to call when a service was changed
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
    //@Override
    public synchronized ServiceDependency setCallbacks(Object instance, String added, String changed, String removed) {
        return setCallbacks(instance, added, changed, removed, null);
    }

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added, changed or removed. When you specify callbacks, the auto 
     * configuration feature is automatically turned off, because we're assuming you don't 
     * need it in this case.
     * @param instance the instance to call the callbacks on
     * @param added the method to call when a service was added
     * @param changed the method to call when a service was changed
     * @param removed the method to call when a service was removed
     * @param swapped the method to call when the service was swapped due to addition or 
     * removal of an aspect
     * @return this service dependency
     */
    //@Override
    public synchronized ServiceDependency setCallbacks(Object instance, String added, String changed, String removed, String swapped) {
        ensureNotActive();
        // if at least one valid callback is specified, we turn off auto configuration, unless
        // someone already explicitly invoked autoConfig
        if ((added != null || removed != null || changed != null || swapped != null) && !m_autoConfigInvoked) {
            setAutoConfig(false);
        }
        m_callbackInstance = instance;
        m_callbackAdded = added;
        m_callbackChanged = changed;
        m_callbackRemoved = removed;
        m_callbackSwapped = swapped;
        return this;
    }

    //@Override
    public ServiceDependency setPropagate(boolean propagate) {
        ensureNotActive();
        m_propagate = propagate;
        return this;
    }

    //@Override
    public ServiceDependency setPropagate(Object instance, String method) {
        setPropagate(instance != null && method != null);
        m_propagateCallbackInstance = instance;
        m_propagateCallbackMethod = method;
        return this;
    }

    // --------------------------------------- Protected methods -------------------------------------------------------------------

    protected synchronized boolean makeAvailable() {
        if (!isAvailable()) {
            m_isAvailable = true;
            return true;
        }
        return false;
    }

    protected synchronized Object getService() {
        Object service = null;
        if (m_isStarted) {
            service = m_tracker.getService();
        }
        if (service == null && isAutoConfig()) {
            service = getDefaultImplementation();
            if (service == null) {
                service = getNullObject();
            }
        }
        return service;
    }

    // --------------------------------------- Private methods --------------------------------------------

    private void _start(DependencyService service) {
        boolean needsStarting = false;
        synchronized (this) {
            m_services.add(service);
            if (!m_isStarted) {
                if (m_trackedServiceName != null) {
                    if (m_trackedServiceFilter != null) {
                        try {
                            m_tracker = new ServiceTracker(m_context, m_context.createFilter(m_trackedServiceFilter), this);
                        } catch (InvalidSyntaxException e) {
                            throw new IllegalStateException("Invalid filter definition for dependency: " + m_trackedServiceFilter);
                        }
                    } else if (m_trackedServiceReference != null) {
                        m_tracker = new ServiceTracker(m_context, m_trackedServiceReference, this);
                    } else {
                        m_tracker = new ServiceTracker(m_context, m_trackedServiceName.getName(), this);
                    }
                } else {
                    throw new IllegalStateException("Could not create tracker for dependency, no service name specified.");
                }
                m_isStarted = true;
                needsStarting = true;
            }
        }
        if (needsStarting) {
            // when the swapped callback is set, also track the aspects
            boolean trackAllServices = false;
            boolean trackAllAspects = false;
            if (m_callbackSwapped != null) {
                trackAllAspects = true;
            }
            m_tracker.open(trackAllServices, trackAllAspects);
        }
    }

    private void _stop(DependencyService service) {
        boolean needsStopping = false;
        synchronized (this) {
            if (m_services.size() == 1 && m_services.contains(service)) {
                m_isStarted = false;
                needsStopping = true;
            }
        }
        if (needsStopping) {
            m_tracker.close();
            m_tracker = null;
        }
        //moved this down
        synchronized (this) {
            m_services.remove(service);
        }
    }

    private void _addedService(ServiceReference ref, Object service) {
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] addedservice: " + ref);
        }
        boolean makeAvailable = makeAvailable();

        Object[] services;
        synchronized (this) {
            services = m_services.toArray();
        }
        for (int i = 0; i < services.length; i++) {
            DependencyService ds = (DependencyService) services[i];
            if (makeAvailable) {
                if (ds.isInstantiated() && isInstanceBound() && isRequired()) {
                    if (m_debug) {
                        m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke added: " + ref);       
                    }
                    invokeAdded(ds, ref, service); //**
                }
                // The dependency callback will be deferred until all required dependency are available.
                if (m_debug) {
                    m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] dependency available: " + ref);
                }
                ds.dependencyAvailable(this); //**
                if (!isRequired()) {
                    // For optional dependency, we always invoke callback, because at this point, we know
                    // that the service has been started, and the service start method has been called.
                    // (See the ServiceImpl.bindService method, which will activate optional dependencies using 
                    // startTrackingOptional() method).
                    if (m_debug) {
                        m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke added: " + ref);
                    }
                    invokeAdded(ds, ref, service); //**
                }
            } else {
                if (m_debug) {
                    m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] dependency changed: " + ref);
                }
                ds.dependencyChanged(this); //**
                // At this point, either the dependency is optional (meaning that the service has been started,
                // because if not, then our dependency would not be active); or the dependency is required,
                // meaning that either the service is not yet started, or already started.
                // In all cases, we have to inject the required dependency.

                // we only try to invoke the method here if we are really already instantiated
                if (ds.isInstantiated() && ds.getCompositionInstances().length > 0) {
                    if (m_debug) {
                        m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke added: " + ref);
                    }
                    invokeAdded(ds, ref, service); //**
                }
            }
        }
    }

    private void _modifiedService(ServiceReference ref, Object service) {
        Object[] services;
        synchronized (this) {
            services = m_services.toArray();
        }
        for (int i = 0; i < services.length; i++) {
            DependencyService ds = (DependencyService) services[i];
            ds.dependencyChanged(this);
            if (ds.isRegistered()) {
                invokeChanged(ds, ref, service);
            }
        }
    }

    private void _removedService(ServiceReference ref, Object service) {
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] removedservice: " + ref + ", rank: " + ref.getProperty("service.ranking"));
        }
        boolean makeUnavailable = makeUnavailable();
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] make unavailable: " + makeUnavailable);
        }
        Object[] services;
        synchronized (this) {
            services = m_services.toArray();
        }

        for (int i = 0; i < services.length; i++) {
            DependencyService ds = (DependencyService) services[i];
            if (makeUnavailable) {
                ds.dependencyUnavailable(this);
                // when the dependency is optional or the dependency is instance bound and the component is instantiated (and the dependency is required)
                // then remove is invoked. In other cases the removed has been when the component was unconfigured.
                if (!isRequired() || (ds.isInstantiated() && isInstanceBound())) {
                    invokeRemoved(ds, ref, service);
                }
            } else {
                ds.dependencyChanged(this);
                invokeRemoved(ds, ref, service);
            }
        }
        // unget what we got in addingService (see ServiceTracker 701.4.1)
        m_context.ungetService(ref);

    }

    private void _invokeAdded(DependencyService service) {
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke added due to configure. (component is activated)");
        }
        ServiceReference[] refs = m_tracker.getServiceReferences();
        if (refs != null) {
            for (int i = 0; i < refs.length; i++) {
                ServiceReference sr = refs[i];
                Object svc = m_context.getService(sr);
                invokeAdded(service, sr, svc);
            }
        }
    }

    private void _invokeRemoved(DependencyService service) {
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke removed due to unconfigure. (component is destroyed)");
        }
        Set references = null;
        Object[] tupleArray = null;
        synchronized (m_sr) {
            references = (Set) m_sr.get(service);
            // is this null check necessary ??
            if (references != null) {
                tupleArray = references.toArray(new Tuple[references.size()]);
            }
        }

        Tuple[] refs = (Tuple[]) (tupleArray != null ? tupleArray : new Tuple[0]);

        for (int i = 0; i < refs.length; i++) {
            ServiceReference sr = refs[i].getServiceReference();
            Object svc = refs[i].getService();
            invokeRemoved(service, sr, svc);
        }
    }

    private Object lookupService() {
        Object service = null;
        if (m_isStarted) {
            service = getService();
        } else {
            ServiceReference[] refs = null;
            ServiceReference ref = null;
            if (m_trackedServiceName != null) {
                if (m_trackedServiceFilter != null) {
                    try {
                        refs = m_context.getServiceReferences(m_trackedServiceName.getName(), m_trackedServiceFilter);
                        if (refs != null) {
                            Arrays.sort(refs, COMPARATOR);
                            ref = refs[0];
                        }
                    } catch (InvalidSyntaxException e) {
                        throw new IllegalStateException("Invalid filter definition for dependency.");
                    }
                } else if (m_trackedServiceReference != null) {
                    ref = m_trackedServiceReference;
                } else {
                    ref = m_context.getServiceReference(m_trackedServiceName.getName());
                }
                if (ref != null) {
                    service = m_context.getService(ref);
                }
            } else {
                throw new IllegalStateException("Could not lookup dependency, no service name specified.");
            }
        }
        if (service == null && isAutoConfig()) {
            service = getDefaultImplementation();
            if (service == null) {
                service = getNullObject();
            }
        }
        return service;
    }

    private ServiceReference lookupServiceReference() {
        // TODO lots of duplication in lookupService()
        ServiceReference service = null;
        if (m_isStarted) {
            service = m_tracker.getServiceReference();
        } else {
            ServiceReference[] refs = null;
            ServiceReference ref = null;
            if (m_trackedServiceName != null) {
                if (m_trackedServiceFilter != null) {
                    try {
                        refs = m_context.getServiceReferences(m_trackedServiceName.getName(), m_trackedServiceFilter);
                        if (refs != null) {
                            Arrays.sort(refs, COMPARATOR);
                            ref = refs[0];
                        }
                    } catch (InvalidSyntaxException e) {
                        throw new IllegalStateException("Invalid filter definition for dependency.");
                    }
                } else if (m_trackedServiceReference != null) {
                    ref = m_trackedServiceReference;
                } else {
                    ref = m_context.getServiceReference(m_trackedServiceName.getName());
                }
                if (ref != null) {
                    service = ref;
                }
            } else {
                throw new IllegalStateException("Could not lookup dependency, no service name specified.");
            }
        }
        return service;
    }

    private synchronized Class getInterface() {
        return m_trackedServiceName;
    }

    private Object getNullObject() {
        if (m_nullObject == null) {
            Class trackedServiceName;
            synchronized (this) {
                trackedServiceName = m_trackedServiceName;
            }
            try {
                m_nullObject = Proxy.newProxyInstance(trackedServiceName.getClassLoader(), new Class[]{trackedServiceName},
                        new DefaultNullObject());
            } catch (Exception e) {
                m_logger.log(Logger.LOG_ERROR, "Could not create null object for " + trackedServiceName + ".", e);
            }
        }
        return m_nullObject;
    }

    private Object getDefaultImplementation() {
        if (m_defaultImplementation != null) {
            if (m_defaultImplementation instanceof Class) {
                try {
                    m_defaultImplementationInstance = ((Class) m_defaultImplementation).newInstance();
                } catch (Exception e) {
                    m_logger.log(Logger.LOG_ERROR, "Could not create default implementation instance of class " + m_defaultImplementation
                            + ".", e);
                }
            } else {
                m_defaultImplementationInstance = m_defaultImplementation;
            }
        }
        return m_defaultImplementationInstance;
    }

    private void invokeAdded(DependencyService dependencyService, ServiceReference reference, Object service) {
        // We are already serialized.
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke added");
        }
        boolean added = false;
        synchronized (m_sr) {
            Set set = (Set) m_sr.get(dependencyService);
            if (set == null) {
                set = new HashSet();
                m_sr.put(dependencyService, set);
            }
            added = set.add(new Tuple(reference, service));
        }
        if (added) {
            // when a changed callback is specified we might not call the added callback just yet
            if (m_callbackSwapped != null) {
                handleAspectAwareAdded(dependencyService, reference, service);
            } else {
                invoke(dependencyService, reference, service, m_callbackAdded);
            }
        }
    }

    private void invokeChanged(DependencyService dependencyService, ServiceReference reference, Object service) {
        invoke(dependencyService, reference, service, m_callbackChanged);
    }

    private void invokeRemoved(DependencyService dependencyService, ServiceReference reference, Object service) {
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke removed");
        }
        boolean removed = false;
        synchronized (m_sr) {
            Set set = (Set) m_sr.get(dependencyService);
            removed = (set != null && set.remove(new Tuple(reference, service)));
        }
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] removed: " + removed);
        }
        if (removed) {
            if (m_callbackSwapped != null) {
                handleAspectAwareRemoved(dependencyService, reference, service);
            } else {
                invoke(dependencyService, reference, service, m_callbackRemoved);
            }
        }
    }

    private void invoke(DependencyService dependencyService, ServiceReference reference, Object service, String name) {
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke: " + name);
        }
        if (name != null) {
            dependencyService.invokeCallbackMethod(getCallbackInstances(dependencyService), name, new Class[][]{
                    {Component.class, ServiceReference.class, m_trackedServiceName},
                    {Component.class, ServiceReference.class, Object.class}, {Component.class, ServiceReference.class},
                    {Component.class, m_trackedServiceName}, {Component.class, Object.class}, {Component.class},
                    {Component.class, Map.class, m_trackedServiceName}, {ServiceReference.class, m_trackedServiceName},
                    {ServiceReference.class, Object.class}, {ServiceReference.class}, {m_trackedServiceName}, {Object.class}, {},
                    {Map.class, m_trackedServiceName}}, new Object[][]{{dependencyService, reference, service},
                    {dependencyService, reference, service}, {dependencyService, reference}, {dependencyService, service},
                    {dependencyService, service}, {dependencyService}, {dependencyService, new ServicePropertiesMap(reference), service},
                    {reference, service}, {reference, service}, {reference}, {service}, {service}, {},
                    {new ServicePropertiesMap(reference), service}});
        }
    }

    private void handleAspectAwareAdded(final DependencyService dependencyService, final ServiceReference reference, final Object service) {
        // At this point, we are already serialized: no need to synchronized.
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] aspectawareadded: " + reference.getProperty("service.ranking"));
        }
        if (componentIsDependencyManagerFactory(dependencyService)) {
            // component is either aspect or adapter factory instance, these must be ignored.
            return;
        }
        boolean invokeAdded = false;
        boolean invokeSwapped = false;
        Integer ranking = ServiceUtil.getRankingAsInteger(reference);
        Tuple newHighestRankedService = null;
        Tuple prevHighestRankedService = null;
        Map rankings = null;
        //synchronized (m_componentByRank) { // don't synchronize: we are serialized and we can now invoke callbacks
        Long originalServiceId = ServiceUtil.getServiceIdAsLong(reference);
        Map componentMap = (Map) m_componentByRank.get(dependencyService); /* <Long, Map<Integer, Tuple>> */
        if (componentMap == null) {
            // create new componentMap
            componentMap = new HashMap(); /* <Long, Map<Integer, Tuple>> */
            m_componentByRank.put(dependencyService, componentMap);
        }
        rankings = (Map) componentMap.get(originalServiceId); /* <Integer, Tuple> */
        if (rankings == null) {
            // new component added
            rankings = new HashMap(); /* <Integer, Tuple> */
            componentMap.put(originalServiceId, rankings);
            rankings.put(ranking, new Tuple(reference, service));
            invokeAdded = true;
        }

        if (!invokeAdded) {
            // current highest ranked
            prevHighestRankedService = (Tuple) getHighestRankedService(dependencyService, originalServiceId).getValue();
            newHighestRankedService = swapHighestRankedService(dependencyService, originalServiceId, reference, service, ranking);
            if (m_debug) {
                m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] prevhigh: " + prevHighestRankedService.getServiceReference().getProperty("service.ranking") + ", new high: " + 
                     newHighestRankedService.getServiceReference().getProperty("service.ranking"));
            }
            if (!prevHighestRankedService.getServiceReference().equals(newHighestRankedService.getServiceReference())) {
                // new highest ranked service
                if (m_debug) {
                    m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] New highest ranked to swap to");
                }
                invokeSwapped = true;
            } else {
                if (m_debug) {
                    m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] Ignoring lower ranked or irrelevant swap");
                }
            }
        }
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] " + m_componentByRank.toString());
        }
        
        // up until this point should be synchronized on m_componentsByRank to keep integrity of the administration and consequences
        // then the do phase comes, here we want to guarantee the effects of this operation are done like they were synchronized, however
        // synchronization on m_componentsByRank to too course grained here, so we'd like to switch to synchronization on the
        // original service id, therefore we're using our own guarded block to ensure the correct order.

        if (invokeAdded) {
            if (m_debug) {
                m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke added: " + reference.getProperty("service.ranking"));
            }
            // We can safely invoke callback since we are already serialized.
            invoke(dependencyService, reference, service, m_callbackAdded);
        } else if (invokeSwapped) {
            if (m_debug) {
                m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke swapped: " + newHighestRankedService.getServiceReference().getProperty("service.ranking") + " replacing " + 
                     prevHighestRankedService.getServiceReference().getProperty("service.ranking"));
            }
            // We can safely invoke callback since we are already serialized.
            invokeSwappedCallback(dependencyService, prevHighestRankedService.getServiceReference(), prevHighestRankedService.getService(),
                    newHighestRankedService.getServiceReference(), newHighestRankedService.getService());
        }
        //} 
    }
    
    private boolean componentIsDependencyManagerFactory(DependencyService dependencyService) {
        Object component = dependencyService.getService();
        if (component != null) {
            String className = component.getClass().getName();
            return className.startsWith("org.apache.felix.dm")
                    && !className.startsWith("org.apache.felix.dm.impl.AdapterServiceImpl$AdapterImpl")
                    && !className.startsWith("org.apache.felix.dm.test");
        }
        return false;
    }

    private Tuple swapHighestRankedService(DependencyService dependencyService, Long serviceId, ServiceReference newReference,
            Object newService, Integer newRanking) {
        // does a component with a higher ranking exist
        synchronized (m_componentByRank) {
            Map componentMap = (Map) m_componentByRank.get(dependencyService); /* <Long, Map<Integer, Tuple>> */
            Map rankings = (Map) componentMap.get(serviceId); /* <Integer, Tuple> */
            rankings.put(newRanking, new Tuple(newReference, newService));
            Entry highestEntry = getHighestRankedService(dependencyService, serviceId); /* <Integer, Tuple> */
            return (Tuple) highestEntry.getValue();
        }
    }

    private Entry getHighestRankedService(DependencyService dependencyService, Long serviceId) { /* <Integer, Tuple> */
        Entry highestEntry = null; /* <Integer, Tuple> */
        Map componentMap = (Map) m_componentByRank.get(dependencyService); /* <Long, Map<Integer, Tuple>> */
        Map rankings = (Map) componentMap.get(serviceId); /* <Integer, Tuple> */
        if (rankings != null) {
            for (Iterator entryIterator = rankings.entrySet().iterator(); entryIterator.hasNext();) { /* <Integer, Tuple> */
                Entry mapEntry = (Entry) entryIterator.next();
                if (highestEntry == null) {
                    highestEntry = mapEntry;
                } else {
                    if (((Integer) mapEntry.getKey()).intValue() > ((Integer) highestEntry.getKey()).intValue()) {
                        highestEntry = mapEntry;
                    }
                }
            }
        }
        return highestEntry;
    }

    private boolean isLastService(DependencyService dependencyService, ServiceReference reference, Object object, Long serviceId) {
        // get the collection of rankings
        Map componentMap = (Map) m_componentByRank.get(dependencyService); /* <Long, Map<Integer, Tuple>> */

        Map rankings = null; /* <Integer, Tuple> */
        if (componentMap != null) {
            rankings = (Map) componentMap.get(serviceId);
        }
        // if there is only one element left in the collection of rankings
        // and this last element has the same ranking as the supplied service (in other words, it is the same)
        // then this is the last service
        // NOTE: it is possible that there is only one element, but that it's not equal to the supplied service,
        // because an aspect on top of the original service is being removed (but the original service is still
        // there). That in turn triggers:
        // 1) a call to added(original-service)
        // 2) that causes a swap
        // 3) a call to removed(aspect-service) <-- that's what we're talking about
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] last service: " + m_componentByRank.toString());
        }
        return (componentMap != null && rankings != null && rankings.size() == 1 && ((Entry) rankings.entrySet().iterator().next())
                .getKey().equals(ServiceUtil.getRankingAsInteger(reference)));
    }

    private void handleAspectAwareRemoved(DependencyService dependencyService, ServiceReference reference, Object service) {
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] aspectawareremoved: " + reference.getProperty("service.ranking"));
        }
        if (componentIsDependencyManagerFactory(dependencyService)) {
            // component is either aspect or adapter factory instance, these must be ignored.
            return;
        }
        // we might need to swap here too!
        boolean invokeRemoved = false;
        Long serviceId = ServiceUtil.getServiceIdAsLong(reference);
        Tuple prevHighestRankedService = null;
        Tuple newHighestRankedService = null;
        boolean invokeSwapped = false;
        Map rankings = null;
        // synchronized (m_componentByRank) { // don't synchronize: we are serialized and we invoke callbacks
        Long originalServiceId = ServiceUtil.getServiceIdAsLong(reference);
        if (isLastService(dependencyService, reference, service, serviceId)) {
            invokeRemoved = true;
        } else {
            // not the last service, but should we swap?
            prevHighestRankedService = (Tuple) getHighestRankedService(dependencyService, originalServiceId).getValue();
            if (prevHighestRankedService.getServiceReference().equals(reference)) {
                // swapping out
                if (m_debug) {
                    m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] Swap out on remove!");
                }
                invokeSwapped = true;
            }
        }
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] is last service: " + invokeRemoved);
        }
        // cleanup
        Map componentMap = (Map) m_componentByRank.get(dependencyService); /* <Long, Map<Integer, Tuple>> */
        if (componentMap != null) {
            rankings = (Map) componentMap.get(originalServiceId); /* <Integer, Tuple> */
            List rankingsToRemove = new ArrayList();
            for (Iterator entryIterator = rankings.entrySet().iterator(); entryIterator.hasNext();) {
                Entry mapEntry = (Entry) entryIterator.next();
                if (((Tuple) mapEntry.getValue()).getServiceReference().equals(reference)) {
                    // remove the reference
                    // rankings.remove(mapEntry.getKey());
                    rankingsToRemove.add(mapEntry.getKey());
                }
            }
            for (Iterator rankingIterator = rankingsToRemove.iterator(); rankingIterator.hasNext();) {
                rankings.remove(rankingIterator.next());
            }
            if (rankings.size() == 0) {
                componentMap.remove(originalServiceId);
            }
            if (componentMap.size() == 0) {
                m_componentByRank.remove(dependencyService);
            }
        }
        // determine current highest ranked service
        if (invokeSwapped) {
            newHighestRankedService = (Tuple) getHighestRankedService(dependencyService, originalServiceId).getValue();
        }
        if (invokeRemoved) {
            // handle invoke outside the sync block since we won't know what will happen there
            if (m_debug) {
                m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke removed: " + reference.getProperty("service.ranking"));
            }
            // We can safely invoke callback, since we are already serialized
            invoke(dependencyService, reference, service, m_callbackRemoved);
        } else if (invokeSwapped) {
            if (m_debug) {
                m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke swapped: " + newHighestRankedService.getServiceReference().getProperty("service.ranking") + " replacing " + 
                     prevHighestRankedService.getServiceReference().getProperty("service.ranking"));
            }
            // We can safely invoke callback, since we are already serialized
            invokeSwappedCallback(dependencyService, prevHighestRankedService.getServiceReference(), prevHighestRankedService.getService(),
                    newHighestRankedService.getServiceReference(), newHighestRankedService.getService());
        }
        //}
    }

    private void invokeSwappedCallback(DependencyService component, ServiceReference previousReference, Object previous,
            ServiceReference currentServiceReference, Object current) {
        // sanity check on the service references
        Integer oldRank = (Integer) previousReference.getProperty(Constants.SERVICE_RANKING);
        Integer newRank = (Integer) currentServiceReference.getProperty(Constants.SERVICE_RANKING);

        if (oldRank != null && newRank != null && oldRank.equals(newRank)) {
            throw new IllegalStateException("Attempt to swap a service for a service with the same rank! previousReference: "
                    + previousReference + ", currentReference: " + currentServiceReference);
        }

        component.invokeCallbackMethod(getCallbackInstances(component), m_callbackSwapped, new Class[][]{
                {m_trackedServiceName, m_trackedServiceName}, {Object.class, Object.class},
                {ServiceReference.class, m_trackedServiceName, ServiceReference.class, m_trackedServiceName},
                {ServiceReference.class, Object.class, ServiceReference.class, Object.class},
                {Component.class, m_trackedServiceName, m_trackedServiceName}, {Component.class, Object.class, Object.class},
                {Component.class, ServiceReference.class, m_trackedServiceName, ServiceReference.class, m_trackedServiceName},
                {Component.class, ServiceReference.class, Object.class, ServiceReference.class, Object.class}}, new Object[][]{
                {previous, current}, {previous, current}, {previousReference, previous, currentServiceReference, current},
                {previousReference, previous, currentServiceReference, current}, {component, previous, current},
                {component, previous, current}, {component, previousReference, previous, currentServiceReference, current},
                {component, previousReference, previous, currentServiceReference, current}});
    }

    private synchronized boolean makeUnavailable() {
        // TODO should we check also m_isStarted ?
        if ((isAvailable()) && (m_isStarted == false || !m_tracker.hasReference())) {
            m_isAvailable = false;
            return true;
        }
        return false;
    }

    private synchronized Object[] getCallbackInstances(DependencyService dependencyService) {
        if (m_callbackInstance == null) {
            return dependencyService.getCompositionInstances();
        } else {
            return new Object[]{m_callbackInstance};
        }
    }

    // ----- CREATION

    /** Internal method to set the name, service reference and/or filter. */
    private void setService(Class serviceName, ServiceReference serviceReference, String serviceFilter) {
        ensureNotActive();
        if (serviceName == null) {
            m_trackedServiceName = Object.class;
        } else {
            m_trackedServiceName = serviceName;
        }
        if (serviceFilter != null) {
            m_trackedServiceFilterUnmodified = serviceFilter;
            if (serviceName == null) {
                m_trackedServiceFilter = serviceFilter;
            } else {
                m_trackedServiceFilter = "(&(" + Constants.OBJECTCLASS + "=" + serviceName.getName() + ")" + serviceFilter + ")";
            }
        } else {
            m_trackedServiceFilterUnmodified = null;
            m_trackedServiceFilter = null;
        }
        if (serviceReference != null) {
            m_trackedServiceReference = serviceReference;
            if (serviceFilter != null) {
                throw new IllegalArgumentException("Cannot specify both a filter and a service reference.");
            }
        } else {
            m_trackedServiceReference = null;
        }
    }

    private void ensureNotActive() {
        if (m_tracker != null) {
            throw new IllegalStateException("Cannot modify state while active.");
        }
    }
}
