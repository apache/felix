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

package org.apache.felix.ipojo.handlers.dependency;

import org.apache.felix.ipojo.*;
import org.apache.felix.ipojo.handlers.dependency.ServiceUsage.Usage;
import org.apache.felix.ipojo.util.DependencyModel;
import org.apache.felix.ipojo.util.Log;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

import java.lang.reflect.*;
import java.util.*;

/**
 * Represent a service dependency of the component instance.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Dependency extends DependencyModel implements FieldInterceptor, MethodInterceptor,
        ConstructorInjector {

    /**
     * Reference on the Dependency Handler.
     */
    private final DependencyHandler m_handler;
    /**
     * Field of the dependency.
     */
    private final String m_field;
    /**
     * Default-Implementation.
     */
    private final String m_di;
    /**
     * Exception to throw when no providers are available.
     */
    private final String m_exception;
    /**
     * Is the Nullable pattern enabled?
     */
    private final boolean m_supportNullable;
    /**
     * List of dependency callback.
     * Immutable once set.
     */
    private DependencyCallback[] m_callbacks;
    /**
     * Is the dependency a service level dependency.
     * Immutable once set.
     */
    private boolean m_isServiceLevelRequirement;
    /**
     * Is the provider set frozen ?
     */
    private boolean m_isFrozen;
    /**
     * Is the dependency started ?
     */
    private boolean m_isStarted;
    /**
     * Thread Local.
     */
    private ServiceUsage m_usage;
    /**
     * Type of the object to inject in aggregate dependency. This value is used to determine what kind of object need
     * to be injected for fields and constructor parameter for aggregate dependencies.
     * Cannot change once set.
     */
    private AggregateDependencyInjectionType m_type;
    /**
     * Nullable object.
     * Immutable once set.
     */
    private Object m_nullable;
    /**
     * Id of the dependency.
     * Immutable once set.
     */
    private String m_id;
    /**
     * Do we have to inject proxy?
     */
    private boolean m_isProxy;
    /**
     * Proxy Object.
     */
    private Object m_proxyObject;
    /**
     * Constructor parameter index.
     * -1 if not used.
     */
    private int m_index = -1;

    /**
     * The dependency timeout.
     */
    private int m_timeout;

    /**
     * Dependency constructor. After the creation the dependency is not started.
     *
     * @param handler               : the dependency handler managing this dependency
     * @param field                 : field of the dependency
     * @param spec                  : required specification
     * @param filter                : LDAP filter of the dependency
     * @param isOptional            : is the dependency an optional dependency ?
     * @param isAggregate           : is the dependency an aggregate dependency
     * @param nullable              : describe if the nullable ability is enable or disable
     * @param isProxy               : is the proxied dependency
     * @param identity              : id of the dependency, may be null
     * @param context               : bundle context (or service context) to use.
     * @param policy                : resolution policy
     * @param cmp                   : comparator to sort references
     * @param defaultImplementation : default-implementation class
     */
    public Dependency(DependencyHandler handler, String field, Class spec, Filter filter, boolean isOptional,
                      boolean isAggregate, boolean nullable, boolean isProxy, String identity, BundleContext context,
                      int policy, Comparator cmp, String defaultImplementation, String exception) {
        super(spec, isAggregate, isOptional, filter, cmp, policy, context, handler, handler.getInstanceManager());
        m_handler = handler;
        m_field = field;
        m_isProxy = isProxy;

        if (field != null) {
            m_usage = new ServiceUsage();
        } else {
            m_usage = null;
        }

        m_supportNullable = nullable;
        m_di = defaultImplementation;
        m_exception = exception;

        if (identity == null) {
            if (spec != null) {
                m_id = spec.getName();
            }
        } else {
            m_id = identity;
        }

        // Else wait the setSpecification call.
    }

    /**
     * Set the specification of the current dependency.
     * In order to store the id of the dependency, this
     * method is override. This method is called during the
     * configuration.
     *
     * @param spec : request service Class
     * @see org.apache.felix.ipojo.util.DependencyModel#setSpecification(java.lang.Class)
     */
    public void setSpecification(Class spec) {
        super.setSpecification(spec);
        if (m_id == null) {
            m_id = spec.getName();
        }
    }

    public String getField() {
        return m_field;
    }

    /**
     * Add a callback to the dependency.
     * This method is called during the configuration.
     *
     * @param callback : callback to add
     */
    protected void addDependencyCallback(DependencyCallback callback) {
        if (m_callbacks == null) {
            m_callbacks = new DependencyCallback[]{callback};
        } else {
            DependencyCallback[] newCallbacks = new DependencyCallback[m_callbacks.length + 1];
            System.arraycopy(m_callbacks, 0, newCallbacks, 0, m_callbacks.length);
            newCallbacks[m_callbacks.length] = callback;
            m_callbacks = newCallbacks;
        }
    }

    protected void addConstructorInjection(int index) throws ConfigurationException {
        m_index = index;
        m_usage = new ServiceUsage();
        m_handler.getInstanceManager().register(index, this);
    }

    /**
     * Stop the current dependency.
     *
     * @see org.apache.felix.ipojo.util.DependencyModel#stop()
     */
    public void stop() {
        acquireWriteLockIfNotHeld();
        m_isStarted = false;
        super.stop();
        releaseWriteLockIfHeld();

    }

    public DependencyHandler getHandler() {
        return m_handler;
    }

    public boolean isFrozen() {
        try {
            acquireReadLockIfNotHeld();
            return m_isFrozen;
        } finally {
            releaseReadLockIfHeld();
        }
    }

    /**
     * Unfreeze the dependency.
     *
     * @see org.apache.felix.ipojo.util.DependencyModel#unfreeze()
     */
    public void unfreeze() {
        try {
            acquireWriteLockIfNotHeld();
            m_isFrozen = false;
        } finally {
            releaseWriteLockIfHeld();
        }
    }

    /**
     * Call the bind method.
     *
     * @param pojo : pojo instance on which calling the bind method.
     */
    protected void onObjectCreation(Object pojo) {

        ServiceReference[] refs;
        try {
            acquireWriteLockIfNotHeld();
            if (!m_isStarted) {
                return;
            }

            // We are notified of an instance creation, we have to freeze when the static policy is used
            if (getBindingPolicy() == STATIC_BINDING_POLICY) {
                m_isFrozen = true;
            }

            // Check optional case : nullable object case : do not call bind on nullable object
            if (isOptional() && getSize() == 0) {
                return;
            }

            refs = getServiceReferences(); // Stack confinement.
        } finally {
            releaseWriteLockIfHeld();
        }

        // This is a pretty strange case, but we don't have any service.
        // This may happen during refresh.
        // So we just return.
        if (refs == null) {
            return;
        }

        // Call bind callback.
        for (int j = 0; m_callbacks != null && j < m_callbacks.length; j++) { // The array is constant.
            if (m_callbacks[j].getMethodType() == DependencyCallback.BIND) {
                if (isAggregate()) {
                    for (ServiceReference ref : refs) {
                        Object svc = getService(ref);
                        if (svc != null) {
                            invokeCallback(m_callbacks[j], ref, svc, pojo);
                        } else {
                            // The service left already, or the service object cannot be created.
                            // We consider it as a departure.
                            m_serviceReferenceManager.removedService(ref, null);
                        }
                    }
                } else {
                    // Take the first reference.
                    Object svc = getService(refs[0]);
                    if (svc != null) {
                        invokeCallback(m_callbacks[j], refs[0], svc, pojo);
                    } else {
                        // The service left already, or the service object cannot be created.
                        // We consider it as a departure.
                        m_serviceReferenceManager.removedService(refs[0], null);
                    }
                }
            }
        }
    }

    /**
     * Call unbind callback method.
     *
     * @param ref : reference to send (if accepted) to the method
     */
    private void callUnbindMethod(ServiceReference ref) {
        if (m_handler.getInstanceManager().getState() > InstanceManager.STOPPED && m_handler.getInstanceManager().getPojoObjects() != null) {
            for (int i = 0; m_callbacks != null && i < m_callbacks.length; i++) {
                if (m_callbacks[i].getMethodType() == DependencyCallback.UNBIND) {
                    invokeCallback(m_callbacks[i], ref, getService(ref, false), null); // Call on each created pojo objects.
                }
            }
        }
    }

    /**
     * Helper method calling the given callback.
     *
     * @param callback  : callback to call.
     * @param ref       : service reference.
     * @param svcObject : the service object
     * @param pojo      : pojo on which calling the callback, if null call on each created pojo objects.
     */
    private void invokeCallback(DependencyCallback callback, ServiceReference ref, Object svcObject, Object pojo) {
        try {
            if (pojo == null) {
                callback.call(ref, svcObject);
            } else {
                callback.callOnInstance(pojo, ref, svcObject);
            }
        } catch (NoSuchMethodException e) {
            m_handler.error("The method " + callback.getMethodName() + " does not exist in the implementation class " + m_handler.getInstanceManager().getClassName(), e);
            m_handler.getInstanceManager().stop();
        } catch (IllegalAccessException e) {
            m_handler.error("The method " + callback.getMethodName() + " is not accessible in the implementation class " + m_handler.getInstanceManager().getClassName(), e);
            m_handler.getInstanceManager().stop();
        } catch (InvocationTargetException e) {
            m_handler.error("The method " + callback.getMethodName() + " in the implementation class " + m_handler.getInstanceManager().getClassName() + " throws an exception : " + e.getTargetException().getMessage(), e.getTargetException());
            m_handler.getInstanceManager().stop();
        }

    }

    /**
     * Call 'modify' method with the service reference in parameter (if accepted).
     *
     * @param ref : the service reference of the modified service
     */
    private void callModifyMethod(ServiceReference ref) {
        if (m_handler.getInstanceManager().getState() > InstanceManager.STOPPED && m_handler.getInstanceManager().getPojoObjects() != null) {
            for (int i = 0; m_callbacks != null && i < m_callbacks.length; i++) {
                if (m_callbacks[i].getMethodType() == DependencyCallback.MODIFIED) {
                    invokeCallback(m_callbacks[i], ref, getService(ref), null); // Call on each created pojo objects.
                }
            }
        }
    }

    /**
     * Call  method with the service reference in parameter (if accepted).
     *
     * @param ref : the service reference of the new service
     */
    private void callBindMethod(ServiceReference ref) {
        // call bind method :
        // if (m_handler.getInstanceManager().getState() == InstanceManager.VALID) {
        if (m_handler.getInstanceManager().getState() > InstanceManager.STOPPED && m_handler.getInstanceManager().getPojoObjects() != null) {
            for (int i = 0; m_callbacks != null && i < m_callbacks.length; i++) {
                if (m_callbacks[i].getMethodType() == DependencyCallback.BIND) {
                    Object svc = getService(ref);
                    if (svc != null) {
                        invokeCallback(m_callbacks[i], ref, svc, null);
                    } else {
                        // We can't get the service object (https://issues.apache.org/jira/browse/FELIX-3896).
                        // This is probably because the service is leaving.
                        // We consider it as a departure.
                        m_serviceReferenceManager.removedService(ref, null);
                    }
                }
            }
        }
    }

    private RuntimeException createExceptionToThrow() {
        final String message = "No service available for " + DependencyHandler.getDependencyIdentifier(this);
        if (m_exception == null) {
            // Should never happen, but let's see.
            return new RuntimeException(message);
        }
        try {
            Class<RuntimeException> exceptionClass = (Class<RuntimeException>) getBundleContext()
                    .getBundle().loadClass(m_exception);
            // Check constructor
            final Constructor<RuntimeException> constructor = exceptionClass.getConstructor(new Class[]{String.class});
            if (constructor != null) {
                return constructor.newInstance(message);
            } else {
                return exceptionClass.newInstance();
            }
        } catch (Exception e) {
            m_handler.getLogger().log(Log.ERROR, "Cannot create the exception object for dependency " +
                    DependencyHandler.getDependencyIdentifier(this) + " : " + e.getMessage(), e);
        }

        return new RuntimeException(message);
    }

    private Object createNullableObject() {
        // To load the proxy we use the POJO class loader. Indeed, this classloader imports iPOJO (so can access to Nullable) and has
        // access to the service specification.
        if ( ! getSpecification().isInterface()) {
            getHandler().getLogger().log(Log.INFO, "Cannot create the nullable object for " + getSpecification()
                    .getName() + " - the specification is not an interface");
            return null;
        }

        try {
            ClassLoader cl = new NullableClassLoader(
                    getHandler().getInstanceManager().getClazz().getClassLoader(),
                    getSpecification().getClassLoader());

            m_nullable =
                    Proxy.newProxyInstance(cl, new Class[]{
                            getSpecification(), Nullable.class}, new NullableObject()); // NOPMD

        } catch (NoClassDefFoundError e) {
            // A NoClassDefFoundError is thrown if the specification uses a class not accessible by the actual instance.
            // It generally comes from a missing import.
            throw new IllegalStateException("Cannot create the Nullable object, a referenced class cannot be loaded", e);
        } catch (Throwable e) { // Catch any other exception that can occurs
            throw new IllegalStateException("Cannot create the Nullable object, an unexpected error occurs", e);
        }

        return m_nullable;
    }

    /**
     * Start the dependency.
     */
    public void start() {

        if (isOptional() && !isAggregate()) {
            if (m_di == null && m_exception == null) {
                // If nullable are supported, create the nullable object.
                if (m_supportNullable) {
                    createNullableObject();
                }
            } else if (m_di != null) {
                // Create the default-implementation object.
                try {
                    Class clazz = getHandler().getInstanceManager().getContext().getBundle().loadClass(m_di);
                    m_nullable = clazz.newInstance();
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Cannot load the default-implementation " + m_di, e);
                } catch (InstantiationException e) {
                    throw new IllegalStateException("Cannot load the default-implementation " + m_di, e);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Cannot load the default-implementation " + m_di, e);
                } catch (Throwable e) { // Catch any other exception
                    throw new IllegalStateException("Cannot load the default-implementation (unexpected exception) " + m_di, e);
                }
            }
        }

        if (m_isProxy) {
            if (isAggregate()) {
                if (m_type == AggregateDependencyInjectionType.SET) {
                    m_proxyObject = new ServiceSet(this);
                } else {
                    m_proxyObject = new ServiceList(this);
                }
            } else {
                // Can we really proxy ? We can proxy only interfaces.
                if (getSpecification().isInterface()) {
                    String type = getHandler().getInstanceManager().getContext().getProperty(DependencyHandler.PROXY_TYPE_PROPERTY);

                    // If it's null we should check on the System directly, Felix delegates to it,
                    // but not other frameworks
                    if (type == null) {
                        type = System.getProperty(DependencyHandler.PROXY_TYPE_PROPERTY);
                    }

                    if (type == null || type.equals(DependencyHandler.SMART_PROXY)) {
                        SmartProxyFactory proxyFactory = new SmartProxyFactory(this.getClass().getClassLoader());
                        m_proxyObject = proxyFactory.getProxy(this);
                    } else {
                        DynamicProxyFactory proxyFactory = new DynamicProxyFactory();
                        m_proxyObject = proxyFactory.getProxy(getSpecification());
                    }
                } else {
                    m_handler.warn("Cannot create a proxy for a service dependency which is not an interface " +
                            "- disabling proxy for " + getId());
                }
            }
        }

        super.start();

        // Once the dependency is started, access to fields must be protected.
        acquireWriteLockIfNotHeld();
        if (getBindingPolicy() == STATIC_BINDING_POLICY && m_handler.getInstanceManager().getPojoObjects() != null) {
            m_isFrozen = true;
        }
        m_isStarted = true;
        releaseWriteLockIfHeld();
    }

    protected DependencyCallback[] getCallbacks() {
        return m_callbacks;
    }

    /**
     * Set that this dependency is a service level dependency.
     * This forces the scoping policy to be STRICT.
     */
    public void setServiceLevelDependency() {
        m_isServiceLevelRequirement = true;
        setBundleContext(new PolicyServiceContext(m_handler.getInstanceManager().getGlobalContext(), m_handler.getInstanceManager().getLocalServiceContext(), PolicyServiceContext.LOCAL));
    }

    public String getId() {
        // No synchronization required, the id is constant.
        return m_id;
    }

    public boolean isServiceLevelRequirement() {
        return m_isServiceLevelRequirement;
    }

    /**
     * A new service has to be injected.
     *
     * @param reference : the new matching service reference.
     * @see org.apache.felix.ipojo.util.DependencyModel#onServiceArrival(org.osgi.framework.ServiceReference)
     */
    public void onServiceArrival(ServiceReference reference) {
        callBindMethod(reference);
        //The method is only called when a new service arrives, or when the used one is replaced.
    }

    /**
     * An already injected service is modified.
     *
     * @param reference : the modified service reference.
     * @see org.apache.felix.ipojo.util.DependencyModel#onServiceModification(org.osgi.framework.ServiceReference)
     */
    public void onServiceModification(ServiceReference reference) {
        callModifyMethod(reference);
    }

    /**
     * A used (already injected) service disappears.
     *
     * @param ref : leaving service reference.
     * @see org.apache.felix.ipojo.util.DependencyModel#onServiceDeparture(org.osgi.framework.ServiceReference)
     */
    public void onServiceDeparture(ServiceReference ref) {
        callUnbindMethod(ref);
    }

    /**
     * The dependency has been reconfigured.
     * Call unbind method and then bind methods. If the dependency cache is not reset,
     * the thread continues to get older services.
     *
     * @param departs  : no more matching services.
     * @param arrivals : new services
     * @see org.apache.felix.ipojo.util.DependencyModel#onDependencyReconfiguration(org.osgi.framework.ServiceReference[], org.osgi.framework.ServiceReference[])
     */
    public void onDependencyReconfiguration(ServiceReference[] departs, ServiceReference[] arrivals) {
        for (int i = 0; departs != null && i < departs.length; i++) {
            callUnbindMethod(departs[i]);
        }

        for (int i = 0; arrivals != null && i < arrivals.length; i++) {
            callBindMethod(arrivals[i]);
        }
    }

    /**
     * Reset the thread local cache if used.
     * For testing purpose only.
     */
    public void resetLocalCache() {
        if (m_usage != null) {
            Usage usage = (Usage) m_usage.get();
            if (usage.m_stack > 0) {
                createServiceObject(usage);
            }
        }
    }

    /**
     * Get the used service references list.
     *
     * @return the used service reference or null if no service reference are available.
     */
    public List<ServiceReference> getServiceReferencesAsList() {
        ServiceReference[] refs = super.getServiceReferences();
        if (refs == null) {
            return null;
        } else {
            return Arrays.asList(refs);
        }
    }


    /**
     * Gets the list of callbacks attached to the current dependency.
     * @return the array of dependency callback, {@code null} if no callbacks are attached to the current dependency.
     */
    public DependencyCallback[] getDependencyCallbacks() {
        return m_callbacks;
    }


    /**
     * Called by the proxy to get  service objects to delegate a method.
     * On aggregate dependencies, it returns a list.
     *
     * @return a service object or a nullable/default-implementation object.
     *         For aggregate dependencies it returns a list or an empty list.
     */
    public Object getService() {
        // Check that we're in proxy mode.
        if (!m_isProxy) {
            throw new IllegalStateException("The dependency has not enabled the `proxy` mode.");
        }

        Usage usage = (Usage) m_usage.get();
        if (usage.m_stack == 0) { // uninitialized usage.
            if (usage.m_componentStack > 0) {
                // We comes from the component who didn't touch the service.
                // So we initialize the usage.
                createServiceObject(usage);
                usage.inc(); // Start the caching, so set the stack level to 1
                m_usage.set(usage); // Required by Dalvik.
                if (isAggregate()) {
                    Object obj = usage.m_object;
                    if (obj instanceof Set) {
                        List<Object> list = new ArrayList<Object>();
                        list.addAll((Set) obj);
                        return list;
                    } else {
                        // We already have a list
                        return obj;
                    }
                } else {
                    return usage.m_object;
                }
            } else {
                // External access => Immediate get.
                if (isAggregate()) {
                    ServiceReference[] refs = getServiceReferences();
                    if (refs == null) {
                        return new ArrayList(0); // Create an empty list.
                    } else {
                        List<Object> objs = new ArrayList<Object>(refs.length);
                        for (ServiceReference ref : refs) {
                            objs.add(getService(ref));
                        }
                        return objs;
                    }
                } else { // Scalar dependency.
                    ServiceReference ref = getServiceReference();
                    if (ref != null) {
                        return getService(ref);
                    } else {
                        // No service available.
                        // TODO Decide what we have to do.
                        throw new RuntimeException("Service " + getSpecification() + " unavailable");
                    }
                }
            }
        } else {
            // Use the copy.
            // if the copy is a set, transform to a list
            if (isAggregate()) {
                Object obj = usage.m_object;
                if (obj instanceof Set) {
                    List<Object> list = new ArrayList<Object>();
                    list.addAll((Set) obj);
                    return list;
                } else {
                    // We already have a list
                    return obj;
                }
            } else {
                return usage.m_object;
            }

        }
    }

    /**
     * This method is called by the replaced code in the component
     * implementation class. Construct the service object list is necessary.
     *
     * @param pojo      : POJO object.
     * @param fieldName : field
     * @param value     : last value.
     * @return the service object or a nullable / default implementation if defined.
     * @see org.apache.felix.ipojo.FieldInterceptor#onGet(java.lang.Object, java.lang.String, java.lang.Object)
     */
    public Object onGet(Object pojo, String fieldName, Object value) {

        // Initialize the thread local object is not already touched.
        Usage usage = m_usage.get();
        if (usage.m_stack == 0) { // uninitialized usage.
            createServiceObject(usage);
            usage.inc(); // Start the caching, so set the stack level to 1
            m_usage.set(usage); // Required by Dalvik
        }
        if (!m_isProxy) {
            return usage.m_object;
        } else {
            return m_proxyObject;
        }

    }

    /**
     * Creates the object to store in the given Thread Local.
     * This object will be injected inside the POJO field.
     *
     * @param usage : Thread Local to populate.
     */
    private void createServiceObject(Usage usage) {
        ServiceReference[] refs = getServiceReferences();

        // manage timeout
        if (refs == null) {
            waitForServiceUntilTimeout();
        }

        refs = getServiceReferences();

        if (!isAggregate()) {
            if (refs == null) {
                if (m_exception != null) {
                    // Throw the exception.
                    throw createExceptionToThrow();
                }

                if (m_nullable == null && m_supportNullable) {
                    m_handler.warn("[" + m_handler.getInstanceManager().getInstanceName() + "] The dependency is not optional, however no service object can be injected in " + m_field + " -> " + getSpecification().getName());
                    createNullableObject();
                }
                usage.m_object = m_nullable; // Add null if the Nullable pattern is disabled.
            } else {
                ServiceReference ref = getServiceReference();
                usage.m_object = getService(ref);
            }
        } else {
            switch(m_type) {
                case ARRAY:
                    try {
                        if (refs == null) {
                            usage.m_object = (Object[]) Array.newInstance(getSpecification(), 0); // Create an empty array.
                        } else {
                            //  Use a reflective construction to avoid class cast exception. This method allows setting the component type.
                            Object[] objs = (Object[]) Array.newInstance(getSpecification(), refs.length);
                            for (int i = 0; i < refs.length; i++) {
                                ServiceReference ref = refs[i];
                                objs[i] = getService(ref);
                            }
                            usage.m_object = objs;
                        }
                    } catch (ArrayStoreException e) {
                        throw new RuntimeException("Cannot create the array - Check that the bundle can access the service interface", e);
                    }
                    break;
                case LIST:
                    if (refs == null) {
                        usage.m_object = Collections.emptyList();
                    } else {
                        // Use a list to store service objects
                        List<Object> objs = new ArrayList<Object>(refs.length);
                        for (ServiceReference ref : refs) {
                            objs.add(getService(ref));
                        }
                        usage.m_object = objs;
                    }
                    break;
                case SET:
                    if (refs == null) {
                        usage.m_object = Collections.emptySet();
                    } else {
                        // Use a vector to store service objects
                        Set<Object> objs = new HashSet<Object>(refs.length);
                        for (ServiceReference ref : refs) {
                            objs.add(getService(ref));
                        }
                        usage.m_object = objs;
                    }
                    break;
                case VECTOR:
                    if (refs == null) {
                        usage.m_object = new Vector(0); // Create an empty vector.
                    } else {
                        // Use a vector to store service objects
                        Vector<Object> objs = new Vector<Object>(refs.length);
                        for (ServiceReference ref : refs) {
                            objs.add(getService(ref));
                        }
                        usage.m_object = objs;
                    }
                    break;
            }
        }
    }

    /**
     * Waits a service providers. The wait stops when the timeout is reached.
     */
    private void waitForServiceUntilTimeout() {
        // Begin to wait ...
        long enter = System.currentTimeMillis();
        boolean exhausted = false;

        // We used a synchronized block here because we must hold the monitor lock during the 'wait'
        synchronized (this) {
            while (getServiceReference() == null && !exhausted) {
                try {
                    wait(1);
                } catch (InterruptedException e) {
                    // We was interrupted ....
                } finally {
                    long end = System.currentTimeMillis();
                    exhausted = (end - enter) > m_timeout;
                }
            }
        }
        // When this method exit, the check will be done...
    }

    /**
     * The field was set.
     * This method should not be call if the POJO is written correctly.
     *
     * @param pojo      : POJO object
     * @param fieldName : field name
     * @param value     : set value.
     * @see org.apache.felix.ipojo.FieldInterceptor#onSet(java.lang.Object, java.lang.String, java.lang.Object)
     */
    public void onSet(Object pojo, String fieldName, Object value) {
        // Nothing to do.
    }

    /**
     * A POJO method will be invoked.
     *
     * @param pojo   : Pojo object
     * @param method : called method
     * @param args   : arguments
     * @see org.apache.felix.ipojo.MethodInterceptor#onEntry(java.lang.Object, java.lang.reflect.Member, java.lang.Object[])
     */
    public void onEntry(Object pojo, Member method, Object[] args) {
        if (m_usage != null) {
            Usage usage = m_usage.get();
            usage.incComponentStack(); // Increment the number of component access.
            if (usage.m_stack > 0) {
                usage.inc();
                m_usage.set(usage); // Set the Thread local as value has been modified
            }
        }
    }

    /**
     * A POJO method has thrown an error.
     * This method does nothing and wait for the finally.
     *
     * @param pojo      : POJO object.
     * @param method    : Method object.
     * @param throwable : thrown error
     * @see org.apache.felix.ipojo.MethodInterceptor#onError(java.lang.Object, java.lang.reflect.Member, java.lang.Throwable)
     */
    public void onError(Object pojo, Member method, Throwable throwable) {
        // Nothing to do  : wait onFinally
    }

    /**
     * A POJO method has returned.
     *
     * @param pojo        : POJO object.
     * @param method      : Method object.
     * @param returnedObj : returned object (null for void method)
     * @see org.apache.felix.ipojo.MethodInterceptor#onExit(java.lang.Object, java.lang.reflect.Member, java.lang.Object)
     */
    public void onExit(Object pojo, Member method, Object returnedObj) {
        // Nothing to do  : wait onFinally
    }

    /**
     * A POJO method is finished.
     *
     * @param pojo   : POJO object.
     * @param method : Method object.
     * @see org.apache.felix.ipojo.MethodInterceptor#onFinally(java.lang.Object, java.lang.reflect.Member)
     */
    public void onFinally(Object pojo, Member method) {
        if (m_usage != null) {
            Usage usage = m_usage.get();
            usage.decComponentStack();
            if (usage.m_stack > 0) {
                if (usage.dec()) {
                    // Exit the method flow => Release all objects
                    usage.clear();
                    // Also remove the thread local object.
                    m_usage.remove();
                }
            }
        }
    }

    /**
     * Gets true if the dependency use Nullable objects.
     *
     * @return true if the dependency is optional and supports nullable objects.
     */
    public boolean supportsNullable() {
        return isOptional()
                && ! isAggregate()
                && m_supportNullable;
    }

    public String getDefaultImplementation() {
        return m_di;
    }

    public boolean isProxy() {
        return m_isProxy;
    }

    public void setProxy(boolean proxy) {
        m_isProxy = proxy;
    }

    /**
     * Set the type to inject.
     * This method set the dependency as aggregate.
     *
     * @param type the type to inject.
     */
    protected void setAggregateType(AggregateDependencyInjectionType type) {
        setAggregate(true);
        m_type = type;
    }

    /**
     * Sets the dependency timeout.
     *
     * @param timeout the timeout in ms.
     */
    public void setTimeout(int timeout) {
        m_timeout = timeout;
    }

    /**
     * Gets the constructor parameter.
     *
     * @return the index of the constructor parameter,
     *         or <code>-1</code> if not set.
     */
    public int getConstructorParameterIndex() {
        return m_index;
    }

    /**
     * Gets the object to inject in the constructor parameter.
     *
     * @param index the index of the parameter
     * @return the created proxy object
     * @see org.apache.felix.ipojo.ConstructorInjector#getConstructorParameter(int)
     */
    public Object getConstructorParameter(int index) {
        if (m_index == index && m_proxyObject != null) {
            return m_proxyObject;
        }
        return null;
    }

    /**
     * Gets the type of the constructor parameter.
     *
     * @param index the parameter index
     * @return the class of the object. For scalar dependency, it's the
     *         specification, for aggregate it depends of the container object:
     *         {@link List} or {@link Set}.
     * @see org.apache.felix.ipojo.ConstructorInjector#getConstructorParameterType(int)
     */
    public Class getConstructorParameterType(int index) {
        if (m_index == index && m_proxyObject != null) {
            if (isAggregate()) {
                switch (m_type) {
                    case LIST:
                        return List.class;
                    case SET:
                        return Set.class;
                    default:
                        return null; // Should never happen, it was checked before.
                }
            } else {
                return getSpecification();
            }
        } else {
            return null;
        }
    }

    public String getException() {
        return m_exception;
    }

    public int getTimeout() {
        return m_timeout;
    }

    public AggregateDependencyInjectionType getAggregateType() {
        return m_type;
    }

    /**
     * Classloader for nullable objects.
     */
    private static class NullableClassLoader extends ClassLoader {
        /**
         * Component classloader.
         */
        private ClassLoader m_component;
        /**
         * Specification classloader.
         */
        private ClassLoader m_specification;

        /**
         * Creates a NullableClassLoader.
         *
         * @param cmp  the component class loader.
         * @param spec the specification class loader.
         */
        public NullableClassLoader(ClassLoader cmp, ClassLoader spec) {
            m_component = cmp;
            m_specification = spec;
        }

        /**
         * Loads the given class.
         * This method uses the classloader of the component class
         * and (if not found) the specification classloader.
         *
         * @param name the class name
         * @return the class object
         * @throws ClassNotFoundException if the class is not found by the two classloaders.
         * @see java.lang.ClassLoader#loadClass(java.lang.String)
         */
        public Class loadClass(String name) throws ClassNotFoundException {
            try {
                return m_component.loadClass(name);
            } catch (ClassNotFoundException e) {
                return m_specification.loadClass(name);
            }
        }


    }

    /**
     * Creates smart proxy object for proxied scalar dependencies.
     */
    private class SmartProxyFactory extends ClassLoader {

        /**
         * Handler classloader, used to load the temporal dependency class.
         */
        private ClassLoader m_handlerCL;

        /**
         * Creates the proxy classloader.
         *
         * @param parent the handler classloader.
         */
        public SmartProxyFactory(ClassLoader parent) {
            super(getHandler().getInstanceManager().getFactory().getBundleClassLoader());
            m_handlerCL = parent;
        }

        /**
         * Loads a proxy class generated for the given (interface) class.
         *
         * @param clazz the service specification to proxy
         * @return the Class object of the proxy.
         */
        protected Class getProxyClass(Class clazz) {
            byte[] clz = ProxyGenerator.dumpProxy(clazz); // Generate the proxy.
            // Turn around the VM changes (FELIX-2716) about java.* classes.
            String cn = clazz.getName();
            if (cn.startsWith("java.")) {
                cn = "$" + cn;
            }
            return defineClass(cn + "$$Proxy", clz, 0, clz.length);
        }

        /**
         * Create a proxy object for the given specification. The proxy
         * uses the given dependency to get the service object.
         *
         * @param dep  the dependency used to get the service
         * @return the proxy object.
         */
        public Object getProxy(Dependency dep) {
            try {
                Class clazz = getProxyClass(getSpecification());
                Constructor constructor = clazz.getConstructor(
                        new Class[]{clazz.getClassLoader().loadClass(Dependency.class.getName())});
                return constructor.newInstance(new Object[]{dep});
            } catch (Throwable e) {
                m_handler.error("Cannot create the proxy object", e);
                m_handler.getInstanceManager().stop();
                return null;
            }
        }

        /**
         * Loads the given class.
         * This method uses the classloader of the specification class
         * or the handler class loader.
         *
         * @param name the class name
         * @return the class object
         * @throws ClassNotFoundException if the class is not found by the two classloaders.
         * @see java.lang.ClassLoader#loadClass(java.lang.String)
         */
        public Class loadClass(String name) throws ClassNotFoundException {
            try {
                return getHandler().getInstanceManager().getContext().getBundle().loadClass(name);
            } catch (ClassNotFoundException e) {
                return m_handlerCL.loadClass(name);
            }
        }
    }

    /**
     * Creates java dynamic proxy object for proxied scalar dependencies.
     */
    private class DynamicProxyFactory implements InvocationHandler {

        /**
         * HashCode method.
         */
        private Method m_hashCodeMethod;
        /**
         * Equals method.
         */
        private Method m_equalsMethod;
        /**
         * toStirng method.
         */
        private Method m_toStringMethod;

        /**
         * Creates a DynamicProxyFactory.
         */
        public DynamicProxyFactory() {
            try {
                m_hashCodeMethod = Object.class.getMethod("hashCode", null);
                m_equalsMethod = Object.class
                        .getMethod("equals", new Class[]{Object.class});
                m_toStringMethod = Object.class.getMethod("toString", null);
            } catch (NoSuchMethodException e) {
                throw new NoSuchMethodError(e.getMessage());
            }
        }

        /**
         * Creates a proxy object for the given specification. The proxy
         * uses the given dependency to get the service object.
         *
         * @param spec the service specification (interface)
         * @return the proxy object.
         */
        public Object getProxy(Class spec) {
            return java.lang.reflect.Proxy.newProxyInstance(
                    getHandler().getInstanceManager().getClazz().getClassLoader(),
                    new Class[]{spec},
                    this);
        }

        /**
         * Invocation Handler delegating invocation on the
         * service object.
         *
         * @param proxy  the proxy object
         * @param method the method
         * @param args   the arguments
         * @return a proxy object.
         * @throws Exception if the invocation throws an exception
         * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
         */
        public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
            Object svc = getService();
            Class declaringClass = method.getDeclaringClass();
            if (declaringClass == Object.class) {
                if (method.equals(m_hashCodeMethod)) {
                    return this.hashCode();
                } else if (method.equals(m_equalsMethod)) {
                    return proxy == args[0] ? Boolean.TRUE : Boolean.FALSE;
                } else if (method.equals(m_toStringMethod)) {
                    return this.toString();
                } else {
                    throw new InternalError(
                            "Unexpected Object method dispatched: " + method);
                }
            }

            return method.invoke(svc, args);
        }

    }

}
