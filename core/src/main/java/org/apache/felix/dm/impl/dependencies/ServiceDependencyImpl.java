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
import org.apache.felix.dm.impl.DefaultNullObject;
import org.apache.felix.dm.impl.Logger;
import org.apache.felix.dm.impl.SerialExecutor;
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
public class ServiceDependencyImpl extends DependencyBase implements ServiceDependency, ServiceTrackerCustomizer, ComponentDependencyDeclaration
{
    protected final List m_services = new ArrayList();
    protected volatile ServiceTracker m_tracker;
    protected final BundleContext m_context;
    protected volatile Class m_trackedServiceName;
    protected final SerialExecutor m_serial;
    private volatile Object m_nullObject;
    private volatile String m_trackedServiceFilter;
    private volatile String m_trackedServiceFilterUnmodified;
    private volatile ServiceReference m_trackedServiceReference;
    private volatile Object m_callbackInstance;
    private volatile String m_callbackAdded;
    private volatile String m_callbackChanged;
    private volatile String m_callbackRemoved;
    private volatile String m_callbackSwapped;
    private volatile boolean m_autoConfig;
    private volatile String m_autoConfigInstance;
    private volatile boolean m_autoConfigInvoked;
    private volatile Object m_defaultImplementation;
    private volatile Object m_defaultImplementationInstance;
    private volatile boolean m_isAvailable;
    private volatile boolean m_propagate;
    private volatile Object m_propagateCallbackInstance;
    private volatile String m_propagateCallbackMethod;
    private final Map m_sr = new HashMap(); /* <DependencyService, Set<Tuple<ServiceReference, Object>> */
    private final Map m_componentByRank = new HashMap(); /* <Component, Map<Long, Map<Integer, Tuple>>> */
    private volatile boolean m_debug = false;
    private volatile String m_debugKey = null;
    
    /* Some additional logging feature for debugging the race conditions */
    private final List logList = new ArrayList(); /* <String> */
    
    private synchronized void addToLog(String message) {
    	logList.add(message);
    }
    
    private String printableLog() {
    	String log = "";
    	synchronized (logList) {
    		for (Iterator logListIterator = logList.iterator(); logListIterator.hasNext(); ) {
    			String message = (String) logListIterator.next();
				log += message + "\n";
			}
		}
    	return log;
    }
    /* End of debug logging feature */
    
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
            }
            else if (ranka > rankb) {
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

        public String toString() {
            return "{" + m_serviceReference.getProperty(Constants.SERVICE_ID) + "=" + m_service + "}";
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
        m_serial = new SerialExecutor(logger);
    }

    /** Copying constructor that clones an existing instance. */
    public ServiceDependencyImpl(ServiceDependencyImpl prototype) {
        super(prototype);
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
        m_serial = new SerialExecutor(prototype.m_logger);
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
    public boolean isAutoConfig() {
        return m_autoConfig;
    }

    //@Override
    public boolean isAvailable() {
        return m_isAvailable;
    }

    //@Override
    public void start(final DependencyService service) {
    	addToLog("start " + service);
        m_serial.executeNow(new Runnable() {
            public void run() {
                boolean needsStarting = false;
                m_services.add(service);
                if (!m_isStarted) {
                    if (m_trackedServiceName != null) {
                        if (m_trackedServiceFilter != null) {
                            try {
                                m_tracker = new ServiceTracker(m_context,
                                    m_context.createFilter(m_trackedServiceFilter), ServiceDependencyImpl.this);
                            }
                            catch (InvalidSyntaxException e) {
                                throw new IllegalStateException("Invalid filter definition for dependency: "
                                    + m_trackedServiceFilter);
                            }
                        }
                        else if (m_trackedServiceReference != null) {
                            m_tracker = new ServiceTracker(m_context, m_trackedServiceReference,
                                ServiceDependencyImpl.this);
                        }
                        else {
                            m_tracker = new ServiceTracker(m_context, m_trackedServiceName.getName(),
                                ServiceDependencyImpl.this);
                        }
                    }
                    else {
                        throw new IllegalStateException(
                            "Could not create tracker for dependency, no service name specified.");
                    }
                    m_isStarted = true;
                    needsStarting = true;
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
            public String toString() { return "stop"; }
        });
    }

    //@Override
    public void stop(final DependencyService service) {
    	addToLog("stop " + service);
        m_serial.executeNow(new Runnable() {
            public void run() {
                boolean needsStopping = false;
                if (m_services.size() == 1 && m_services.contains(service)) {
                    m_isStarted = false;
                    needsStopping = true;
                    // Don't remove the service from our m_services list now, because when we'll close the tracker,
                    // our removedService callback will be called, and at this point we'll need to access to our m_services list.
                }

                if (needsStopping) {
                    m_tracker.close(); // will invoke our removedService method synchronously. 
                    m_tracker = null;
                }
                // moved this down
                m_services.remove(service);
            }
            public String toString() { return "stop"; }
        });
    }

    //@Override
    public Object addingService(ServiceReference ref) {
    	addToLog("adding service " + ref);
        // Check to make sure the service is actually an instance of our service
        Object service = m_context.getService(ref);
        if (!m_trackedServiceName.isInstance(service)) {
            return null;
        }
        return service;
    }

    //@Override
    public void addedService(final ServiceReference ref, final Object service) {
    	addToLog("added service " + ref);
        m_serial.executeNow(new Runnable() {
            public void run() {
                if (m_debug) {
                    m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] addedservice: " + ref);
                }
                final boolean makeAvailable = makeAvailable();
                final Object[] services = m_services.toArray();

                for (int i = 0; i < services.length; i++) {
                    final DependencyService ds = (DependencyService) services[i];
                    if (makeAvailable) {
                        if (ds.isInstantiated() && isInstanceBound() && isRequired()) {
                            if (m_debug) {
                                m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke added: " + ref);
                            }
                            invokeAdded(ds, ref, service);
                        }
                        // The dependency callback will be deferred until all required dependency are available.
                        if (m_debug) {
                            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] dependency available: " + ref);
                        }
                        ds.dependencyAvailable(ServiceDependencyImpl.this); // may callback invokeAdded, but  *synchronously* !
                        if (!isRequired()) {
                            // For optional dependency, we always invoke callback, because at this point, we know
                            // that the service has been started, and the service start method has been called.
                            // (See the ServiceImpl.bindService method, which will activate optional dependencies using 
                            // startTrackingOptional() method).
                            if (m_debug) {
                                m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke added: " + ref);
                            }
                            invokeAdded(ds, ref, service);
                        }
                    }
                    else {
                        if (m_debug) {
                            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] dependency added (was available): "
                                + ref);
                        }
                        // First, inject the added service in autoconfig field, if any. 
                        ds.autoConfig(ServiceDependencyImpl.this);

                        // At this point, either the dependency is optional (meaning that the service has been started,
                        // because if not, then our dependency would not be active); or the dependency is required,
                        // meaning that either the service is not yet started, or already started.
                        // In all cases, we have to inject the required dependency.
                        // we only try to invoke the method here if we are really already instantiated
                        if (ds.isInstantiated() && ds.getCompositionInstances().length > 0) {
                            if (m_debug) {
                                m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke added: " + ref);
                            }
                            if (invokeAdded(ds, ref, service)) {
                                // Propagate (if needed) all "setPropagate" dependencies to the dependency service.
                                ds.propagate(ServiceDependencyImpl.this);
                            }
                        }
                    }
                }
            }
            public String toString() { return "addedService"; }
        });
    }

    //@Override
    public void modifiedService(final ServiceReference ref, final Object service) {
    	addToLog("modified service " + ref);
        m_serial.executeNow(new Runnable() {
            public void run() {
                final Object[] services = m_services.toArray();
                for (int i = 0; i < services.length; i++) {
                    final DependencyService ds = (DependencyService) services[i];
                    ds.autoConfig(ServiceDependencyImpl.this);
                    if (ds.isInstantiated()) {
                        if (invokeChanged(ds, ref, service)) {
                            // The "change" or "swap" callback has been invoked (if not it means that the modified service 
                            // is for a lower ranked aspect to which we are not interested in).
                            // Now, propagate (if needed) changed properties to dependency service properties.
                            ds.propagate(ServiceDependencyImpl.this);
                        }
                    }
                }
            }
            public String toString() { return "modifiedService"; }
        });
    }

    //@Override
    public void removedService(final ServiceReference ref, final Object service) {
    	addToLog("removed service " + ref);
        m_serial.executeNow(new Runnable() {
            public void run() {
                if (m_debug) {
                    m_logger.log(Logger.LOG_DEBUG,
                        "[" + m_debugKey + "] removedService: " + ref + ", rank: " + ref.getProperty("service.ranking"));
                }
                final boolean makeUnavailable = makeUnavailable();
                if (m_debug) {
                    m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] make unavailable: " + makeUnavailable);
                }
                final Object[] services = m_services.toArray();

                for (int i = 0; i < services.length; i++) {
                    final DependencyService ds = (DependencyService) services[i];
                    if (makeUnavailable) {
                        ds.dependencyUnavailable(ServiceDependencyImpl.this);
                        // when the dependency is optional or the dependency is instance bound and the component is instantiated (and the dependency is required)
                        // then remove is invoked. In other cases the removed has been when the component was unconfigured.
                        if (!isRequired() || (ds.isInstantiated() && isInstanceBound())) {
                            invokeRemoved(ds, ref, service);
                        }
                    }
                    else {
                        // Some dependencies are still available: first inject the remaining highest ranked dependency
                        // in component class field, if the dependency is configured in autoconfig mode.
                        ds.autoConfig(ServiceDependencyImpl.this);
                        // Next, invoke "removed" callback. If the dependency is aspect aware, we only invoke removed cb
                        // if the removed service is the highest ranked service. Note that if the cb is not called, we don't
                        // propagate the remaining dependency properties.
                        if (invokeRemoved(ds, ref, service)) {
                            // Finally, since we have lost one dependency, we have to possibly propagate the highest ranked 
                            // dependency available.
                            ds.propagate(ServiceDependencyImpl.this);
                        }
                    }
                }
                m_context.ungetService(ref);
            }
            public String toString() { return "removedService"; }
        });
    }

    // @Override
    public void invokeAdded(final DependencyService service) {
        m_serial.executeNow(new Runnable() {
            public void run() {
		        if (m_debug) {
		            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey
		                + "] invoke added due to configure. (component is activated)");
		        }
		        if (m_tracker == null) {
		        	throw new IllegalStateException("The service tracker is gone. Log: \n" + printableLog());
		        }
		        ServiceReference[] refs = m_tracker.getServiceReferences();
		        if (refs != null) {
		            for (int i = 0; i < refs.length; i++) {
		                final ServiceReference sr = refs[i];
		                final Object svc = m_context.getService(sr); // May be null if service has just been unregistered !
		
		                if (svc == null) {
		                    m_logger.log(Logger.LOG_INFO, "[" + m_debugKey
		                        + "] invoke added found a null service from osgi registry for service ref:" + sr);
		                    continue; // The service has just been unregistered !
		                }
		                // We may be currently executing from our executor, that's why we "execute now".
		                m_serial.executeNow(new Runnable() {
		                    public void run() {
		                        invokeAdded(service, sr, svc);
		                    }
		                    public String toString() { return "invokeAdded"; }
		                });
		            }
		        }
            }
            public String toString() { return "invokeAdded"; }
        });
    }

    // @Override
    public void invokeRemoved(final DependencyService service) {
    	Runnable task = new Runnable() {
            public void run() {
                if (m_debug) {
                    m_logger.log(Logger.LOG_INFO, "[" + m_debugKey
                        + "] invoke removed due to unconfigure. (component is destroyed)");
                }
                Set references = null;
                Object[] tupleArray = null;
                references = (Set) m_sr.get(service);
                // is this null check necessary ??
                if (references != null) {
                    tupleArray = references.toArray(new Tuple[references.size()]);
                }

                Tuple[] refs = (Tuple[]) (tupleArray != null ? tupleArray : new Tuple[0]);

                for (int i = 0; i < refs.length; i++) {
                    ServiceReference sr = refs[i].getServiceReference();
                    Object svc = refs[i].getService();
                    if (svc == null) { // it seems that the service has just gone !
                        m_logger.log(Logger.LOG_INFO, "[" + m_debugKey
                            + "] invoke removed found a null service from osgi registry for service ref:" + sr);
                        continue;
                    }

                    invokeRemoved(service, sr, svc);
                }
            }
            public String toString() { return "invokeRemoved"; }
        };
        m_serial.executeNow(task);
    }

    //@Override
    public String toString() {
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
                    return (Dictionary) InvocationUtil.invokeCallbackMethod(m_propagateCallbackInstance,
                        m_propagateCallbackMethod, new Class[][] { { ServiceReference.class, Object.class },
                                { ServiceReference.class } }, new Object[][] { { reference, service }, { reference } });
                }
                catch (InvocationTargetException e) {
                    m_logger.log(LogService.LOG_WARNING, "Exception while invoking callback method", e.getCause());
                }
                catch (Exception e) {
                    m_logger.log(LogService.LOG_WARNING, "Exception while trying to invoke callback method", e);
                }
                throw new IllegalStateException("Could not invoke callback");
            }
            else {
                Properties props = new Properties();
                String[] keys = reference.getPropertyKeys();
                for (int i = 0; i < keys.length; i++) {
                    if (!(keys[i].equals(Constants.SERVICE_ID) || keys[i].equals(Constants.SERVICE_PID))) {
                        props.put(keys[i], reference.getProperty(keys[i]));
                    }
                }
                return props;
            }
        }
        else {
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
    public ServiceDependency setService(Class serviceName) {
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
    public ServiceDependency setService(Class serviceName, String serviceFilter) {
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
    public ServiceDependency setService(String serviceFilter) {
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
    public ServiceDependency setService(Class serviceName, ServiceReference serviceReference) {
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
    public ServiceDependency setDefaultImplementation(Object implementation) {
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
    public ServiceDependency setRequired(boolean required) {
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
    public ServiceDependency setAutoConfig(boolean autoConfig) {
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
    public ServiceDependency setAutoConfig(String instanceName) {
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
    public ServiceDependency setCallbacks(String added, String removed) {
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
    public ServiceDependency setCallbacks(String added, String changed, String removed) {
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
    public ServiceDependency setCallbacks(String added, String changed, String removed, String swapped) {
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
    public ServiceDependency setCallbacks(Object instance, String added, String removed) {
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
    public ServiceDependency setCallbacks(Object instance, String added, String changed, String removed) {
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
    public ServiceDependency setCallbacks(Object instance, String added, String changed, String removed, String swapped)
    {
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

    public void invoke(final Object[] callbackInstances, final DependencyService component,
        final ServiceReference reference, final Object service, final String name)
    {
    	addToLog("invoke " + name + " for " + reference);
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke: " + name);
        }
        if (name != null) {
            component.invokeCallbackMethod(callbackInstances, name, new Class[][] {
                    { Component.class, ServiceReference.class, m_trackedServiceName },
                    { Component.class, ServiceReference.class, Object.class },
                    { Component.class, ServiceReference.class }, { Component.class, m_trackedServiceName },
                    { Component.class, Object.class }, { Component.class },
                    { Component.class, Map.class, m_trackedServiceName },
                    { ServiceReference.class, m_trackedServiceName }, { ServiceReference.class, Object.class },
                    { ServiceReference.class }, { m_trackedServiceName }, { Object.class }, {},
                    { Map.class, m_trackedServiceName } }, new Object[][] { { component, reference, service },
                    { component, reference, service }, { component, reference }, { component, service },
                    { component, service }, { component }, { component, new ServicePropertiesMap(reference), service },
                    { reference, service }, { reference, service }, { reference }, { service }, { service }, {},
                    { new ServicePropertiesMap(reference), service } });

        }
    }

    public void invokeSwappedCallback(final Object[] callbackInstances, final DependencyService component,
        final ServiceReference previousReference, final Object previous,
        final ServiceReference currentServiceReference, final Object current, final String swapCallback)
    {
    	addToLog("invoke swapped " + previousReference + " with " + currentServiceReference);

        // sanity check on the service references
        Integer oldRank = (Integer) previousReference.getProperty(Constants.SERVICE_RANKING);
        Integer newRank = (Integer) currentServiceReference.getProperty(Constants.SERVICE_RANKING);

        if (oldRank != null && newRank != null && oldRank.equals(newRank)) {
            throw new IllegalStateException(
                "Attempt to swap a service for a service with the same rank! previousReference: " + previousReference
                    + ", currentReference: " + currentServiceReference);
        }

        component.invokeCallbackMethod(callbackInstances, swapCallback, new Class[][] {
                { m_trackedServiceName, m_trackedServiceName },
                { Object.class, Object.class },
                { ServiceReference.class, m_trackedServiceName, ServiceReference.class, m_trackedServiceName },
                { ServiceReference.class, Object.class, ServiceReference.class, Object.class },
                { Component.class, m_trackedServiceName, m_trackedServiceName },
                { Component.class, Object.class, Object.class },
                { Component.class, ServiceReference.class, m_trackedServiceName, ServiceReference.class,
                        m_trackedServiceName },
                { Component.class, ServiceReference.class, Object.class, ServiceReference.class, Object.class } },
            new Object[][] { { previous, current }, { previous, current },
                    { previousReference, previous, currentServiceReference, current },
                    { previousReference, previous, currentServiceReference, current },
                    { component, previous, current }, { component, previous, current },
                    { component, previousReference, previous, currentServiceReference, current },
                    { component, previousReference, previous, currentServiceReference, current } });
    }

    // --------------------------------------- Protected methods -------------------------------------------------------------------

    protected synchronized boolean makeAvailable() {
        if (!isAvailable()) {
            m_isAvailable = true;
            return true;
        }
        return false;
    }

    protected Object getService() {
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

    private Object lookupService() {
        Object service = null;
        if (m_isStarted) {
            service = getService();
        }
        else {
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
                    }
                    catch (InvalidSyntaxException e) {
                        throw new IllegalStateException("Invalid filter definition for dependency.");
                    }
                }
                else if (m_trackedServiceReference != null) {
                    ref = m_trackedServiceReference;
                }
                else {
                    ref = m_context.getServiceReference(m_trackedServiceName.getName());
                }
                if (ref != null) {
                    service = m_context.getService(ref);
                }
            }
            else {
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
        }
        else {
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
                    }
                    catch (InvalidSyntaxException e) {
                        throw new IllegalStateException("Invalid filter definition for dependency.");
                    }
                }
                else if (m_trackedServiceReference != null) {
                    ref = m_trackedServiceReference;
                }
                else {
                    ref = m_context.getServiceReference(m_trackedServiceName.getName());
                }
                if (ref != null) {
                    service = ref;
                }
            }
            else {
                throw new IllegalStateException("Could not lookup dependency, no service name specified.");
            }
        }
        return service;
    }

    private Class getInterface() {
        return m_trackedServiceName;
    }

    private Object getNullObject() {
        if (m_nullObject == null) {
            Class trackedServiceName;
            synchronized (this) {
                trackedServiceName = m_trackedServiceName;
            }
            try {
                m_nullObject = Proxy.newProxyInstance(trackedServiceName.getClassLoader(),
                    new Class[] { trackedServiceName }, new DefaultNullObject());
            }
            catch (Exception e) {
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
                }
                catch (Exception e) {
                    m_logger.log(Logger.LOG_ERROR, "Could not create default implementation instance of class "
                        + m_defaultImplementation + ".", e);
                }
            }
            else {
                m_defaultImplementationInstance = m_defaultImplementation;
            }
        }
        return m_defaultImplementationInstance;
    }

    /**
     * Invoke added or swap callback.
     * @return true if one of the swap/added callbacks has been invoked, false if not, meaning that the added dependency is not the highest ranked one.
     */
    protected boolean invokeAdded(DependencyService dependencyService, ServiceReference reference, Object service) {
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke added");
        }
        boolean added = false;
        Set set = (Set) m_sr.get(dependencyService);
        if (set == null) {
            set = new HashSet();
            m_sr.put(dependencyService, set);
        }
        added = set.add(new Tuple(reference, service));
        if (added) {
            // when a changed callback is specified we might not call the added callback just yet
            if (m_callbackSwapped != null) {
                return handleAspectAwareAdded(dependencyService, reference, service);
            }
            else {
                invoke(getCallbackInstances(dependencyService), dependencyService, reference, service, m_callbackAdded);
                return true;
            }
        }
        return false;
    }

    private boolean invokeChanged(DependencyService dependencyService, ServiceReference reference, Object service) {
        if (m_callbackSwapped != null) {
            if (m_debug) {
                m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] handleAspectAwareChanged on " + service);
            }
            return handleAspectAwareChanged(dependencyService, reference, service);
        }
        invoke(getCallbackInstances(dependencyService), dependencyService, reference, service, m_callbackChanged);
        return true;
    }

    /*
     * Invoke the removed or the swap callback. Called from component's executor.
     * @return true if the swap or the removed callback has  been called, false if not.
     */
    private boolean invokeRemoved(DependencyService dependencyService, ServiceReference reference, Object service) {
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke removed");
        }
        boolean removed = false;
        Set set = (Set) m_sr.get(dependencyService);
        removed = (set != null && set.remove(new Tuple(reference, service)));
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] removed: " + removed);
        }
        if (removed) {
            if (m_callbackSwapped != null) {
                return handleAspectAwareRemoved(dependencyService, reference, service);
            }
            else {
                invoke(getCallbackInstances(dependencyService), dependencyService, reference, service,
                    m_callbackRemoved);
                return true;
            }
        }
        return false;
    }

    /**
     * Invoke added or swap callback for aspect aware service dependency.
     * @return true if one of the swap/added callbacks has been invoked, false if not, meaning that the added dependency is not the highest ranked one.
     */
    private boolean handleAspectAwareAdded(final DependencyService dependencyService, final ServiceReference reference,
        final Object service)
    {
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG,
                "[" + m_debugKey + "] aspectawareadded: " + reference.getProperty("service.ranking"));
        }
        if (componentIsDependencyManagerFactory(dependencyService)) {
            // component is either aspect or adapter factory instance, these must be ignored.
            return false;
        }
        boolean invokeAdded = false;
        boolean invokeSwapped = false;
        Integer ranking = ServiceUtil.getRankingAsInteger(reference);
        Tuple newHighestRankedService = null;
        Tuple prevHighestRankedService = null;
        Map rankings = null;

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
            newHighestRankedService = swapHighestRankedService(dependencyService, originalServiceId, reference,
                service, ranking);
            if (m_debug) {
                m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] prevhigh: "
                    + prevHighestRankedService.getServiceReference().getProperty("service.ranking") + ", new high: "
                    + newHighestRankedService.getServiceReference().getProperty("service.ranking"));
            }
            if (!prevHighestRankedService.getServiceReference().equals(newHighestRankedService.getServiceReference())) {
                // new highest ranked service
                if (m_debug) {
                    m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] New highest ranked to swap to");
                }
                invokeSwapped = true;
            }
            else {
                if (m_debug) {
                    m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] Ignoring lower ranked or irrelevant swap");
                }
            }
        }
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] " + m_componentByRank.toString());
        }

        if (invokeAdded) {
            if (m_debug) {
                m_logger.log(Logger.LOG_DEBUG,
                    "[" + m_debugKey + "] invoke added: " + reference.getProperty("service.ranking"));
            }
            invoke(getCallbackInstances(dependencyService), dependencyService, reference, service, m_callbackAdded);

            return true;
        }
        else if (invokeSwapped) {
            if (m_debug) {
                m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke swapped: "
                    + newHighestRankedService.getServiceReference().getProperty("service.ranking") + " replacing "
                    + prevHighestRankedService.getServiceReference().getProperty("service.ranking"));
            }
            invokeSwappedCallback(dependencyService, prevHighestRankedService.getServiceReference(),
                prevHighestRankedService.getService(), newHighestRankedService.getServiceReference(),
                newHighestRankedService.getService());
            return true;
        }
        return false;
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

    private Tuple swapHighestRankedService(DependencyService dependencyService, Long serviceId,
        ServiceReference newReference, Object newService, Integer newRanking)
    {
        // does a component with a higher ranking exist
        Map componentMap = (Map) m_componentByRank.get(dependencyService); /* <Long, Map<Integer, Tuple>> */
        Map rankings = (Map) componentMap.get(serviceId); /* <Integer, Tuple> */
        rankings.put(newRanking, new Tuple(newReference, newService));
        Entry highestEntry = getHighestRankedService(dependencyService, serviceId); /* <Integer, Tuple> */
        return (Tuple) highestEntry.getValue();
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
                }
                else {
                    if (((Integer) mapEntry.getKey()).intValue() > ((Integer) highestEntry.getKey()).intValue()) {
                        highestEntry = mapEntry;
                    }
                }
            }
        }
        return highestEntry;
    }

    private boolean isLastService(DependencyService dependencyService, ServiceReference reference, Object object,
        Long serviceId)
    {
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
        return (componentMap != null && rankings != null && rankings.size() == 1 && ((Entry) rankings.entrySet().iterator().next()).getKey().equals(
            ServiceUtil.getRankingAsInteger(reference)));
    }

    private boolean handleAspectAwareChanged(DependencyService dependencyService, ServiceReference reference,
        Object service)
    {
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG,
                "[" + m_debugKey + "] aspectawareChanged: service.ranking=" + reference.getProperty("service.ranking"));
        }
        if (componentIsDependencyManagerFactory(dependencyService)) {
            // component is either aspect or adapter factory instance, these must be ignored.
            return false;
        }
        boolean invokeChanged = false;

        // Determine current highest ranked service: we'll take into account the change event only if it
        // comes from the highest ranked service.
        Long serviceId = ServiceUtil.getServiceIdAsLong(reference);
        Map componentMap = (Map) m_componentByRank.get(dependencyService); /* <Long, Map<Integer, Tuple>> */
        if (componentMap != null) {
            Map rankings = (Map) componentMap.get(serviceId); /* <Integer, Tuple> */
            if (rankings != null) {
                Entry highestEntry = getHighestRankedService(dependencyService, serviceId); /* <Integer, Tuple> */
                if (m_debug) {
                    m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] highest service ref:" + highestEntry);
                }
                if (highestEntry == null) {
                    invokeChanged = true;
                }
                else {
                    Tuple tuple = (Tuple) highestEntry.getValue();
                    if (reference.equals(tuple.getServiceReference())) {
                        // The changed service is the highest ranked service: we can handle the modification event.
                        invokeChanged = true;
                    }
                }
            }
        }

        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invokeChanged=" + invokeChanged);
        }

        if (invokeChanged) {
            if (m_debug) {
                m_logger.log(Logger.LOG_DEBUG,
                    "[" + m_debugKey + "] invoke changed: ref ranking=" + reference.getProperty("service.ranking"));
            }
            invoke(getCallbackInstances(dependencyService), dependencyService, reference, service, m_callbackChanged);
            return true;
        }
        return false;
    }

    /*
     * handles aspect aware removed service.
     * @return true if the swap or the removed callback has  been called, false if not.
     */
    private boolean handleAspectAwareRemoved(DependencyService dependencyService, ServiceReference reference,
        Object service)
    {
        if (m_debug) {
            m_logger.log(Logger.LOG_DEBUG,
                "[" + m_debugKey + "] aspectawareremoved: " + reference.getProperty("service.ranking"));
        }
        if (componentIsDependencyManagerFactory(dependencyService)) {
            // component is either aspect or adapter factory instance, these must be ignored.
            return false;
        }
        // we might need to swap here too!
        boolean invokeRemoved = false;
        Long serviceId = ServiceUtil.getServiceIdAsLong(reference);
        Tuple prevHighestRankedService = null;
        Tuple newHighestRankedService = null;
        boolean invokeSwapped = false;
        Map rankings = null;
        Long originalServiceId = ServiceUtil.getServiceIdAsLong(reference);

        if (isLastService(dependencyService, reference, service, serviceId)) {
            invokeRemoved = true;
        }
        else {
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
                m_logger.log(Logger.LOG_DEBUG,
                    "[" + m_debugKey + "] invoke removed: " + reference.getProperty("service.ranking"));
            }
            invoke(getCallbackInstances(dependencyService), dependencyService, reference, service, m_callbackRemoved);
            return true;
        }
        else if (invokeSwapped) {
            if (m_debug) {
                m_logger.log(Logger.LOG_DEBUG, "[" + m_debugKey + "] invoke swapped: "
                    + newHighestRankedService.getServiceReference().getProperty("service.ranking") + " replacing "
                    + prevHighestRankedService.getServiceReference().getProperty("service.ranking"));
            }
            invokeSwappedCallback(dependencyService, prevHighestRankedService.getServiceReference(),
                prevHighestRankedService.getService(), newHighestRankedService.getServiceReference(),
                newHighestRankedService.getService());
            return true;
        }

        return false;
    }

    private void invokeSwappedCallback(DependencyService component, ServiceReference previousReference,
        Object previous, ServiceReference currentServiceReference, Object current)
    {
        invokeSwappedCallback(getCallbackInstances(component), component, previousReference, previous,
            currentServiceReference, current, m_callbackSwapped);
    }

    private synchronized boolean makeUnavailable() {
        if ((isAvailable()) && (m_isStarted == false || !m_tracker.hasReference())) {
            m_isAvailable = false;
            return true;
        }
        return false;
    }

    private synchronized Object[] getCallbackInstances(DependencyService dependencyService) {
        if (m_callbackInstance == null) {
            return dependencyService.getCompositionInstances();
        }
        else {
            return new Object[] { m_callbackInstance };
        }
    }

    // ----- CREATION

    /** Internal method to set the name, service reference and/or filter. */
    private void setService(Class serviceName, ServiceReference serviceReference, String serviceFilter) {
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
        }
        else {
            m_trackedServiceReference = null;
        }
    }

    private void ensureNotActive() {
        if (m_tracker != null) {
            throw new IllegalStateException("Cannot modify state while active.");
        }
    }
}
