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
package org.apache.felix.ipojo.util;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.IPOJOServiceFactory;
import org.apache.felix.ipojo.dependency.impl.ServiceReferenceManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Abstract dependency model.
 * This class is the parent class of every service dependency. It manages the most
 * part of dependency management. This class creates an interface between the service
 * tracker and the concrete dependency.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class DependencyModel {

    /**
     * Dependency state : BROKEN.
     * A broken dependency cannot be fulfilled anymore. The dependency becomes
     * broken when a used service disappears in the static binding policy.
     */
    public static final int BROKEN = -1;
    /**
     * Dependency state : UNRESOLVED.
     * A dependency is unresolved if the dependency is not valid and no service
     * providers are available.
     */
    public static final int UNRESOLVED = 0;
    /**
     * Dependency state : RESOLVED.
     * A dependency is resolved if the dependency is optional or at least one
     * provider is available.
     */
    public static final int RESOLVED = 1;
    /**
     * Binding policy : Dynamic.
     * In this policy, services can appears and departs without special treatment.
     */
    public static final int DYNAMIC_BINDING_POLICY = 0;
    /**
     * Binding policy : Static.
     * Once a service is used, if this service disappears the dependency becomes
     * {@link DependencyModel#BROKEN}. The instance needs to be recreated.
     */
    public static final int STATIC_BINDING_POLICY = 1;
    /**
     * Binding policy : Dynamic-Priority.
     * In this policy, services can appears and departs. However, once a service
     * with a highest ranking (according to the used comparator) appears, this
     * new service is re-injected.
     */
    public static final int DYNAMIC_PRIORITY_BINDING_POLICY = 2;
    /**
     * The service reference manager.
     */
    protected final ServiceReferenceManager m_serviceReferenceManager;
    /**
     * The manager handling context sources.
     */
    private final ContextSourceManager m_contextSourceManager;
    /**
     * Listener object on which invoking the {@link DependencyStateListener#validate(DependencyModel)}
     * and {@link DependencyStateListener#invalidate(DependencyModel)} methods.
     */
    private final DependencyStateListener m_listener;
    /**
     * The instance requiring the service.
     */
    private final ComponentInstance m_instance;
    /**
     * Does the dependency bind several providers ?
     */
    private boolean m_aggregate;
    /**
     * Is the dependency optional ?
     */
    private boolean m_optional;
    /**
     * The required specification.
     * Cannot change once set.
     */
    private Class m_specification;
    /**
     * Bundle context used by the dependency.
     * (may be a {@link org.apache.felix.ipojo.ServiceContext}).
     */
    private BundleContext m_context;
    /**
     * The actual state of the dependency.
     * {@link DependencyModel#UNRESOLVED} at the beginning.
     */
    private int m_state;
    /**
     * The Binding policy of the dependency.
     */
    private int m_policy = DYNAMIC_BINDING_POLICY;
    /**
     * The tracker used by this dependency to track providers.
     */
    private Tracker m_tracker;
    /**
     * Map {@link ServiceReference} -> Service Object.
     * This map stores service object, and so is able to handle
     * iPOJO custom policies.
     */
    private Map<ServiceReference, Object> m_serviceObjects = new HashMap<ServiceReference, Object>();
    /**
     * The current list of bound services.
     */
    private List<ServiceReference> m_boundServices = new ArrayList<ServiceReference>();
    /**
     * The lock ensuring state consistency of the dependency.
     * This lock can be acquired from all collaborators.
     */
    private ReentrantReadWriteLock m_lock = new ReentrantReadWriteLock();

    /**
     * Creates a DependencyModel.
     * If the dependency has no comparator and follows the
     * {@link DependencyModel#DYNAMIC_PRIORITY_BINDING_POLICY} policy
     * the OSGi Service Reference Comparator is used.
     *
     * @param specification the required specification
     * @param aggregate     is the dependency aggregate ?
     * @param optional      is the dependency optional ?
     * @param filter        the LDAP filter
     * @param comparator    the comparator object to sort references
     * @param policy        the binding policy
     * @param context       the bundle context (or service context)
     * @param listener      the dependency lifecycle listener to notify from dependency
     * @param ci            instance managing the dependency
     *                      state changes.
     */
    public DependencyModel(Class specification, boolean aggregate, boolean optional, Filter filter,
                           Comparator<ServiceReference> comparator, int policy,
                           BundleContext context, DependencyStateListener listener, ComponentInstance ci) {
        m_specification = specification;
        m_aggregate = aggregate;
        m_optional = optional;

        m_instance = ci;

        m_policy = policy;
        // If the dynamic priority policy is chosen, and we have no comparator, fix it to OSGi standard service reference comparator.
        if (m_policy == DYNAMIC_PRIORITY_BINDING_POLICY && comparator == null) {
            comparator = new ServiceReferenceRankingComparator();
        }

        if (context != null) {
            m_context = context;
            // If the context is null, it gonna be set later using the setBundleContext method.
        }

        m_serviceReferenceManager = new ServiceReferenceManager(this, filter, comparator);

        if (filter != null) {
            try {
                m_contextSourceManager = new ContextSourceManager(this);
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            m_contextSourceManager = null;
        }
        m_state = UNRESOLVED;
        m_listener = listener;
    }

    /**
     * Opens the tracking.
     * This method computes the dependency state.
     * <p/>
     * As the dependency is starting, locking is not required here.
     *
     * @see DependencyModel#computeAndSetDependencyState()
     */
    public void start() {
        m_state = UNRESOLVED;
        m_tracker = new Tracker(m_context, m_specification.getName(), m_serviceReferenceManager);
        m_serviceReferenceManager.open();
        m_tracker.open();

        if (m_contextSourceManager != null) {
            m_contextSourceManager.start();
        }

        computeAndSetDependencyState();
    }

    /**
     * Gets the bundle context used by the dependency.
     * @return the bundle context
     */
    public BundleContext getBundleContext() {
        // Immutable member, no lock required.
        return m_context;
    }

    /**
     * This callback is called by ranking interceptor to notify the dependency that the selected service set has
     * changed and must be recomputed.
     */
    public void invalidateSelectedServices() {
        m_serviceReferenceManager.invalidateSelectedServices();
    }

    public void invalidateMatchingServices() {
        m_serviceReferenceManager.invalidateMatchingServices();
    }

    /**
     * Closes the tracking.
     * The dependency becomes {@link DependencyModel#UNRESOLVED}
     * at the end of this method.
     */
    public void stop() {
        // We're stopping, we must take the exclusive lock
        try {
            acquireWriteLockIfNotHeld();
            if (m_tracker != null) {
                m_tracker.close();
                m_tracker = null;
            }
            m_boundServices.clear();
            m_serviceReferenceManager.close();
            ungetAllServices();
            m_state = UNRESOLVED;
            if (m_contextSourceManager != null) {
                m_contextSourceManager.stop();
            }
        } finally {
            releaseWriteLockIfHeld();
        }
    }

    /**
     * Ungets all 'get' service references.
     * This also clears the service object map.
     * The method is called while holding the exclusive lock.
     */
    private void ungetAllServices() {
        for (Map.Entry<ServiceReference, Object> entry : m_serviceObjects.entrySet()) {
            ServiceReference ref = entry.getKey();
            Object svc = entry.getValue();
            if (m_tracker != null) {
                m_tracker.ungetService(ref);
            }
            if (svc instanceof IPOJOServiceFactory) {
                ((IPOJOServiceFactory) svc).ungetService(m_instance, svc);
            }
        }
        m_serviceObjects.clear();
    }

    /**
     * Is the reference set frozen (cannot change anymore)?
     * This method must be override by concrete dependency to support
     * the static binding policy. In fact, this method allows optimizing
     * the static dependencies to become frozen only when needed.
     * This method returns <code>false</code> by default.
     * The method must always return <code>false</code> for non-static dependencies.
     *
     * @return <code>true</code> if the reference set is frozen.
     */
    public boolean isFrozen() {
        return false;
    }

    /**
     * Unfreezes the dependency.
     * This method must be override by concrete dependency to support
     * the static binding policy. This method is called after tracking restarting.
     */
    public void unfreeze() {
        // nothing to do
    }

    /**
     * Does the service reference match ? This method must be overridden by
     * concrete dependencies if they need advanced testing on service reference
     * (that cannot be expressed in the LDAP filter). By default this method
     * returns <code>true</code>.
     *
     * @param ref the tested reference.
     * @return <code>true</code> if the service reference matches.
     */
    public boolean match(ServiceReference ref) {
        return true;
    }

    /**
     * Computes the actual dependency state.
     * This methods invokes the {@link DependencyStateListener}.
     * If this method is called without the write lock, it takes it. Anyway, the lock will be released before called
     * the
     * callbacks.
     */
    private void computeAndSetDependencyState() {
        try {
            boolean mustCallValidate = false;
            boolean mustCallInvalidate = false;

            acquireWriteLockIfNotHeld();

            // The dependency is broken, nothing else can be done
            if (m_state == BROKEN) {
                return;
            }

            if (m_optional || !m_serviceReferenceManager.isEmpty()) {
                // The dependency is valid
                if (m_state == UNRESOLVED) {
                    m_state = RESOLVED;
                    mustCallValidate = true;
                }
            } else {
                // The dependency is invalid
                if (m_state == RESOLVED) {
                    m_state = UNRESOLVED;
                    mustCallInvalidate = true;
                }
            }

            // Invoke callback in a non-synchronized region
            // First unlock the lock
            releaseWriteLockIfHeld();
            // Now we can call the callbacks
            if (mustCallInvalidate) {
                invalidate();
            } else if (mustCallValidate) {
                validate();
            }
        } finally {
            // If we are still holding the exclusive lock, unlock it.
            releaseWriteLockIfHeld();
        }

    }

    /**
     * Gets the first bound service reference.
     *
     * @return <code>null</code> if no more provider is available,
     *         else returns the first reference from the matching set.
     */
    public ServiceReference getServiceReference() {
        // Read lock required
        try {
            acquireReadLockIfNotHeld();
            if (m_boundServices.isEmpty()) {
                return null;
            } else {
                return m_boundServices.get(0);
            }
        } finally {
            releaseReadLockIfHeld();
        }
    }

    /**
     * Gets bound service references.
     *
     * @return the sorted (if a comparator is used) array of matching service
     *         references, <code>null</code> if no references are available.
     */
    public ServiceReference[] getServiceReferences() {
        // Read lock required
        try {
            acquireReadLockIfNotHeld();
            if (m_boundServices.isEmpty()) {
                return null;
            }
            return m_boundServices.toArray(new ServiceReference[m_boundServices.size()]);
        } finally {
            releaseReadLockIfHeld();
        }
    }

    /**
     * Gets the list of currently used service references.
     * If no service references, returns <code>null</code>
     *
     * @return the list of used reference (according to the service tracker).
     */
    public List<ServiceReference> getUsedServiceReferences() {
        // Read lock required
        try {
            acquireReadLockIfNotHeld();
            // The list must confront actual matching services with already get services from the tracker.

            int size = m_boundServices.size();
            List<ServiceReference> usedByTracker = null;
            if (m_tracker != null) {
                usedByTracker = m_tracker.getUsedServiceReferences();
            }
            if (size == 0 || usedByTracker == null) {
                return null;
            }

            List<ServiceReference> list = new ArrayList<ServiceReference>(1);
            for (ServiceReference ref : m_boundServices) {
                if (usedByTracker.contains(ref)) {
                    list.add(ref); // Add the service in the list.
                    if (!isAggregate()) { // IF we are not multiple, return the list when the first element is found.
                        return list;
                    }
                }
            }

            return list;
        } finally {
            releaseReadLockIfHeld();
        }
    }

    /**
     * @return the component instance on which this dependency is plugged.
     */
    public ComponentInstance getComponentInstance() {
        // No lock required as m_instance is final
        return m_instance;
    }

    /**
     * Gets the number of actual matching references.
     *
     * @return the number of matching references
     */
    public int getSize() {
        try {
            acquireReadLockIfNotHeld();
            return m_boundServices.size();
        } finally {
            releaseReadLockIfHeld();
        }
    }

    /**
     * Concrete dependency callback.
     * This method is called when a new service needs to be
     * re-injected in the underlying concrete dependency.
     *
     * @param ref the service reference to inject.
     */
    public abstract void onServiceArrival(ServiceReference ref);

    /**
     * Concrete dependency callback.
     * This method is called when a used service (already injected) is leaving.
     *
     * @param ref the leaving service reference.
     */
    public abstract void onServiceDeparture(ServiceReference ref);

    /**
     * Concrete dependency callback.
     * This method is called when a used service (already injected) is modified.
     *
     * @param ref the modified service reference.
     */
    public abstract void onServiceModification(ServiceReference ref);

    /**
     * Concrete dependency callback.
     * This method is called when the dependency is reconfigured and when this
     * reconfiguration implies changes on the matching service set ( and by the
     * way on the injected service).
     *
     * @param departs  the service leaving the matching set.
     * @param arrivals the service arriving in the matching set.
     */
    public abstract void onDependencyReconfiguration(ServiceReference[] departs, ServiceReference[] arrivals);

    /**
     * Calls the listener callback to notify the new state of the current
     * dependency.
     * No lock hold when calling this callback.
     */
    private void invalidate() {
        m_listener.invalidate(this);
    }

    /**
     * Calls the listener callback to notify the new state of the current
     * dependency.
     * No lock hold when calling this callback.
     */
    private void validate() {
        m_listener.validate(this);
    }

    /**
     * Gets the actual state of the dependency.
     * @return the state of the dependency.
     */
    public int getState() {
        try {
            acquireReadLockIfNotHeld();
            return m_state;
        } finally {
            releaseReadLockIfHeld();
        }
    }

    /**
     * Gets the tracked specification.
     *
     * @return the Class object tracked by the dependency.
     */
    public Class getSpecification() {
        return m_specification;
    }

    /**
     * Sets the required specification of this service dependency.
     * This operation is not supported if the dependency tracking has already begun.
     * So, we don't have to hold a lock.
     *
     * @param specification the required specification.
     */
    public void setSpecification(Class specification) {
        if (m_tracker == null) {
            m_specification = specification;
        } else {
            throw new UnsupportedOperationException("Dynamic specification change is not yet supported");
        }
    }

    /**
     * Acquires the write lock only and only if the write lock is not already held by the current thread.
     * @return {@literal true} if the lock was acquired within the method, {@literal false} otherwise.
     */
    public boolean acquireWriteLockIfNotHeld() {
        if (! m_lock.isWriteLockedByCurrentThread()) {
            m_lock.writeLock().lock();
            return true;
        }
        return false;
    }

    /**
     * Releases the write lock only and only if the write lock is held by the current thread.
     * @return {@literal true} if the lock has no more holders, {@literal false} otherwise.
     */
    public boolean releaseWriteLockIfHeld() {
        if (m_lock.isWriteLockedByCurrentThread()) {
            m_lock.writeLock().unlock();
        }
        return m_lock.getWriteHoldCount() == 0;
    }

    /**
     * Acquires the read lock only and only if no read lock is already held by the current thread.
     *
     * As the introspection methods provided by this method are java 6+, we just take a read lock.
     * @return {@literal true} if the lock was acquired within the method, {@literal false} otherwise.
     */
    public boolean acquireReadLockIfNotHeld() {
        m_lock.readLock().lock();
        return true;
    }

    /**
     * Releases the read lock only and only if the read lock is held by the current thread.
     * * As the introspection methods provided by this method are java 6+, we just unlock the read lock.
     * @return {@literal true} if the lock has no more holders, {@literal false} otherwise.
     */
    public boolean releaseReadLockIfHeld() {
        m_lock.readLock().unlock();
        return true;
    }

    /**
     * Returns the dependency filter (String form).
     *
     * @return the String form of the LDAP filter used by this dependency,
     *         <code>null</code> if not set.
     */
    public String getFilter() {
        Filter filter;
        try {
            acquireReadLockIfNotHeld();
            filter = m_serviceReferenceManager.getFilter();
        } finally {
            releaseReadLockIfHeld();
        }

        if (filter == null) {
            return null;
        } else {
            return filter.toString();
        }
    }

    /**
     * Sets the filter of the dependency. This method recomputes the
     * matching set and call the onDependencyReconfiguration callback.
     *
     * @param filter the new LDAP filter.
     */
    public void setFilter(Filter filter) {
        try {
            acquireWriteLockIfNotHeld();
            ServiceReferenceManager.ChangeSet changeSet = m_serviceReferenceManager.setFilter(filter, m_tracker);
            // We call this method when holding the lock, but the method may decide to release the lock to invoke
            // callbacks, so we must defensively unlock the lock in the finally block.
            applyReconfiguration(changeSet);
        } finally {
            releaseWriteLockIfHeld();
        }
    }

    /**
     * Applies the given reconfiguration.
     * This method check if the current thread is holding the write lock, if not, acquire it.
     * The lock will be released before calling callbacks. As a consequence, the caller has to check if the lock is
     * still hold when this method returns.
     * @param changeSet the reconfiguration changes
     */
    public void applyReconfiguration(ServiceReferenceManager.ChangeSet changeSet) {
        List<ServiceReference> arr = new ArrayList<ServiceReference>();
        List<ServiceReference> dep = new ArrayList<ServiceReference>();

        try  {
            acquireWriteLockIfNotHeld();
            if (m_tracker == null) {
                // Nothing else to do.
                return;
            } else {
                // Update bindings
                m_boundServices.clear();
                if (m_aggregate) {
                    m_boundServices = new ArrayList<ServiceReference>(changeSet.selected);
                    arr = changeSet.arrivals;
                    dep = changeSet.departures;
                } else {
                    ServiceReference used = null;
                    if (!m_boundServices.isEmpty()) {
                        used = m_boundServices.get(0);
                    }

                    if (!changeSet.selected.isEmpty()) {
                        final ServiceReference best = changeSet.newFirstReference;
                        // We didn't a provider
                        if (used == null) {
                            // We are not bound with anyone yet, so take the first of the selected set
                            m_boundServices.add(best);
                            arr.add(best);
                        } else {
                            // A provider was already bound, did we changed ?
                            if (changeSet.selected.contains(used)) {
                                // We are still valid - but in dynamic priority, we may have to change
                                if (getBindingPolicy() == DYNAMIC_PRIORITY_BINDING_POLICY && used != best) {
                                    m_boundServices.add(best);
                                    dep.add(used);
                                    arr.add(best);
                                } else {
                                    // We restore the old binding.
                                    m_boundServices.add(used);
                                }
                            } else {
                                // The used service has left.
                                m_boundServices.add(best);
                                dep.add(used);
                                arr.add(best);
                            }
                        }
                    } else {
                        // We don't have any service anymore
                        if (used != null) {
                            arr.add(used);
                        }
                    }
                }
            }
        } finally {
            releaseWriteLockIfHeld();
        }

        // This method releases the exclusive lock.
        computeAndSetDependencyState();

        // As the previous method has released the lock, we can call the callback safely.
        onDependencyReconfiguration(
                dep.toArray(new ServiceReference[dep.size()]),
                arr.toArray(new ServiceReference[arr.size()]));
    }

    public boolean isAggregate() {
        try {
            acquireReadLockIfNotHeld();
            return m_aggregate;
        } finally {
            releaseReadLockIfHeld();
        }
    }

    /**
     * Sets the aggregate attribute of the current dependency.
     * If the tracking is opened, it will call arrival and departure callbacks.
     *
     * @param isAggregate the new aggregate attribute value.
     */
    public void setAggregate(boolean isAggregate) {
        // Acquire the write lock here.
        acquireWriteLockIfNotHeld();
        List<ServiceReference> arrivals = new ArrayList<ServiceReference>();
        List<ServiceReference> departures = new ArrayList<ServiceReference>();
        try {
            if (m_tracker == null) { // Not started ...
                m_aggregate = isAggregate;
            } else {
                // We become aggregate.
                if (!m_aggregate && isAggregate) {
                    m_aggregate = true;
                    // Call the callback on all non already injected service.
                    if (m_state == RESOLVED) {

                        for (ServiceReference ref : m_serviceReferenceManager.getSelectedServices()) {
                            if (!m_boundServices.contains(ref)) {
                                m_boundServices.add(ref);
                                arrivals.add(ref);
                            }
                        }
                    }
                } else if (m_aggregate && !isAggregate) {
                    m_aggregate = false;
                    // We become non-aggregate.
                    if (m_state == RESOLVED) {
                        List<ServiceReference> list = new ArrayList<ServiceReference>(m_boundServices);
                        for (int i = 1; i < list.size(); i++) { // The loop begin at 1, as the 0 stays injected.
                            m_boundServices.remove(list.get(i));
                            departures.add(list.get(i));
                        }
                    }
                }
                // Else, do nothing.
            }
        } finally {
            releaseWriteLockIfHeld();
        }

        // Now call callbacks, the lock is not held anymore
        // Only one of the list is not empty..
        for (ServiceReference ref : arrivals) {
            onServiceArrival(ref);
        }
        for (ServiceReference ref : departures) {
            onServiceDeparture(ref);
        }


    }

    /**
     * Sets the optionality attribute of the current dependency.
     *
     * @param isOptional the new optional attribute value.
     */
    public void setOptionality(boolean isOptional) {
        try {
            acquireWriteLockIfNotHeld();
            if (m_tracker == null) { // Not started ...
                m_optional = isOptional;
            } else {
                // This method releases the exclusive lock
                computeAndSetDependencyState();
            }
        } finally {
            releaseWriteLockIfHeld();
        }
    }

    public boolean isOptional() {
        try {
            acquireReadLockIfNotHeld();
            return m_optional;
        } finally {
            releaseReadLockIfHeld();
        }
    }

    /**
     * Gets the used binding policy.
     *
     * @return the current binding policy.
     */
    public int getBindingPolicy() {
        try {
            acquireReadLockIfNotHeld();
            return m_policy;
        } finally {
            releaseReadLockIfHeld();
        }

    }

    /**
     * Gets the used comparator name.
     * <code>null</code> if no comparator (i.e. the OSGi one is used).
     *
     * @return the comparator class name or <code>null</code> if the dependency doesn't use a comparator.
     */
    public String getComparator() {
        final Comparator<ServiceReference> comparator;
        try {
            acquireReadLockIfNotHeld();
            comparator = m_serviceReferenceManager.getComparator();
        } finally {
            releaseReadLockIfHeld();
        }

        if (comparator != null) {
            return comparator.getClass().getName();
        } else {
            return null;
        }
    }

    public void setComparator(Comparator<ServiceReference> cmp) {
        try {
            acquireWriteLockIfNotHeld();
            m_serviceReferenceManager.setComparator(cmp);
        } finally {
            releaseWriteLockIfHeld();
        }
    }

    /**
     * Sets the bundle context used by this dependency.
     * This operation is not supported if the tracker is already opened, and as a consequence does not require locking.
     *
     * @param context the bundle context or service context to use
     */
    public void setBundleContext(BundleContext context) {
        if (m_tracker == null) { // Not started ...
            m_context = context;
        } else {
            throw new UnsupportedOperationException("Dynamic bundle (i.e. service) context change is not supported");
        }
    }

    /**
     * Gets a service object for the given reference.
     * The service object is stored to handle custom policies.
     *
     * @param ref the wanted service reference
     * @return the service object attached to the given reference
     */
    public Object getService(ServiceReference ref) {
        return getService(ref, true);
    }

    /**
     * Gets a service object for the given reference.
     *
     * @param ref   the wanted service reference
     * @param store enables / disables the storing of the reference.
     * @return the service object attached to the given reference
     */
    public Object getService(ServiceReference ref, boolean store) {
        Object svc = m_tracker.getService(ref);
        IPOJOServiceFactory factory = null;

        if (svc instanceof IPOJOServiceFactory) {
            factory = (IPOJOServiceFactory) svc;
            svc = factory.getService(m_instance);
        }

        if (store) {
            try {
                acquireWriteLockIfNotHeld();
                if (factory != null) {
                    m_serviceObjects.put(ref, factory);
                } else {
                    m_serviceObjects.put(ref, svc);
                }
            } finally {
                releaseWriteLockIfHeld();
            }
        }

        return svc;
    }

    /**
     * Ungets a used service reference.
     *
     * @param ref the reference to unget.
     */
    public void ungetService(ServiceReference ref) {
        m_tracker.ungetService(ref);
        Object obj;
        try {
            acquireWriteLockIfNotHeld();
            obj = m_serviceObjects.remove(ref);
        } finally {
            releaseWriteLockIfHeld();
        }

        // Call the callback outside the lock.
        if (obj != null && obj instanceof IPOJOServiceFactory) {
            ((IPOJOServiceFactory) obj).ungetService(m_instance, obj);
        }
    }

    public ContextSourceManager getContextSourceManager() {
        // Final member, no lock required.
        return m_contextSourceManager;
    }

    /**
     * Gets the dependency id.
     *
     * @return the dependency id. Specification name by default.
     */
    public String getId() {
        // Immutable, no lock required.
        return getSpecification().getName();
    }

    private void breakDependency() {
        // Static dependency broken.
        m_state = BROKEN;

        // We are going to call callbacks, releasing the lock.
        releaseWriteLockIfHeld();
        invalidate();  // This will invalidate the instance.
        m_instance.stop(); // Stop the instance
        unfreeze();
        m_instance.start();
    }

    /**
     * Callbacks call by the ServiceReferenceManager when the selected service set has changed.
     * @param set the change set.
     */
    public void onChange(ServiceReferenceManager.ChangeSet set) {
        try {
            acquireWriteLockIfNotHeld();
            // First handle the static case with a frozen state
            if (isFrozen() && getState() != BROKEN) {
                for (ServiceReference ref : set.departures) {
                    // Check if any of the service that have left was in used.
                    if (m_boundServices.contains(ref)) {
                        breakDependency();
                        return;
                    }
                }
            }

            List<ServiceReference> arrivals = new ArrayList<ServiceReference>();
            List<ServiceReference> departures = new ArrayList<ServiceReference>();

            // Manage departures
            // We unbind all bound services that are leaving.
            for (ServiceReference ref : set.departures) {
                if (m_boundServices.contains(ref)) {
                    // We were using the reference
                    m_boundServices.remove(ref);
                    departures.add(ref);
                }
            }

            // Manage arrivals
            // For aggregate dependencies, call onServiceArrival for all services not-yet-bound and in the order of the
            // selection.
            if (m_aggregate) {
                // If the dependency is not already in used,
                // the bindings must be sorted as in set.selected
                if (m_serviceObjects.isEmpty() || DYNAMIC_PRIORITY_BINDING_POLICY == getBindingPolicy()) {
                    m_boundServices.clear();
                    m_boundServices.addAll(set.selected);
                }

                // Now we notify from the arrival.
                // If we didn't add the reference yet, we add it.
                for (ServiceReference ref : set.arrivals) {
                    // We bind all not-already bound services, so it's an arrival
                    if (!m_boundServices.contains(ref)) {
                        m_boundServices.add(ref);
                    }
                    arrivals.add(ref);
                }
            } else {
                if (!set.selected.isEmpty()) {
                    final ServiceReference best = set.selected.get(0);
                    // We have a provider
                    if (m_boundServices.isEmpty()) {
                        // We are not bound with anyone yet, so take the first of the selected set
                        m_boundServices.add(best);
                        arrivals.add(best);
                    } else {
                        final ServiceReference current = m_boundServices.get(0);
                        // We are already bound, to the rebinding decision depends on the binding strategy
                        if (getBindingPolicy() == DYNAMIC_PRIORITY_BINDING_POLICY) {
                            // Rebinding in the DP binding policy if the bound one if not the new best one.
                            if (current != best) {
                                m_boundServices.remove(current);
                                m_boundServices.add(best);
                                departures.add(current);
                                arrivals.add(best);
                            }
                        } else {
                            // In static and dynamic binding policy, if the service is not yet used and the new best is not
                            // the currently selected one, we should switch.
                            boolean isUsed = m_serviceObjects.containsKey(current);
                            if (!isUsed && current != best) {
                                m_boundServices.remove(current);
                                m_boundServices.add(best);
                                departures.add(current);
                                arrivals.add(best);
                            }
                        }
                    }
                }
            }

            // Leaving the locked region to invoke callbacks
            releaseWriteLockIfHeld();
            for (ServiceReference ref : departures) {
                onServiceDeparture(ref);
            }
            for (ServiceReference ref : arrivals) {
                onServiceArrival(ref);
            }
            // Do we have a modified service ?
            if (set.modified != null && m_boundServices.contains(set.modified)) {
                onServiceModification(set.modified);
            }


            // Did our state changed ?
            // this method will manage its own synchronization.
            computeAndSetDependencyState();
        } finally {
            releaseWriteLockIfHeld();
        }
    }

    public ServiceReferenceManager getServiceReferenceManager() {
        return m_serviceReferenceManager;
    }

    public Tracker getTracker() {
        return m_tracker;
    }
}
