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
package org.apache.felix.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.framework.capabilityset.CapabilitySet;
import org.apache.felix.framework.capabilityset.SimpleFilter;
import org.apache.felix.framework.wiring.BundleCapabilityImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.resource.Capability;

public class ServiceRegistry
{
    private final Logger m_logger;

    /** Counter for the service id */
    private final AtomicLong m_currentServiceId = new AtomicLong(1);

    // Maps bundle to an array of service registrations.
    private final ConcurrentMap<Bundle, List<ServiceRegistration<?>>> m_regsMap = new ConcurrentHashMap<Bundle, List<ServiceRegistration<?>>>();

    // Capability set for all service registrations.
    private final CapabilitySet m_regCapSet = new CapabilitySet(Collections.singletonList(Constants.OBJECTCLASS), false);

    // Maps bundle to an array of usage counts.
    private final ConcurrentMap<Bundle, UsageCount[]> m_inUseMap = new ConcurrentHashMap<Bundle, UsageCount[]>();

    private final ServiceRegistryCallbacks m_callbacks;

    private final HookRegistry hookRegistry = new HookRegistry();

    public ServiceRegistry(final Logger logger, final ServiceRegistryCallbacks callbacks)
    {
        m_logger = logger;
        m_callbacks = callbacks;
    }

    /**
     * Get all service references for a bundle
     * @param bundle
     * @return List with all valid service references or {@code null}.
     */
    public ServiceReference<?>[] getRegisteredServices(final Bundle bundle)
    {
        final List<ServiceRegistration<?>> regs = m_regsMap.get(bundle);
        if (regs != null)
        {
            final List<ServiceReference<?>> refs = new ArrayList<ServiceReference<?>>(regs.size());
            // this is a per bundle list, therefore synchronizing this should be fine
            synchronized ( regs )
            {
                for (final ServiceRegistration<?> reg : regs)
                {
                    try
                    {
                        refs.add(reg.getReference());
                    }
                    catch (final IllegalStateException ex)
                    {
                        // Don't include the reference as it is not valid anymore
                    }
                }
            }
            return refs.toArray(new ServiceReference[refs.size()]);
        }
        return null;
    }

    /**
     * Register a new service
     *
     * Caller must fire service event as this method is not doing it!
     *
     * @param bundle The bundle registering the service
     * @param classNames The service class names
     * @param svcObj The service object
     * @param dict Optional service properties
     * @return Service registration
     */
    public ServiceRegistration<?> registerService(
        final Bundle bundle,
        final String[] classNames,
        final Object svcObj,
        final Dictionary<?,?> dict)
    {
        // Create the service registration.
        final ServiceRegistrationImpl reg = new ServiceRegistrationImpl(
            this, bundle, classNames, m_currentServiceId.getAndIncrement(), svcObj, dict);

        // Keep track of registered hooks.
        this.hookRegistry.addHooks(classNames, svcObj, reg.getReference());

        // Get the bundles current registered services.
        final List<ServiceRegistration<?>> newRegs = new ArrayList<ServiceRegistration<?>>();
        List<ServiceRegistration<?>> regs = m_regsMap.putIfAbsent(bundle, newRegs);
        if (regs == null)
        {
            regs = newRegs;
        }
        // this is a per bundle list, therefore synchronizing this should be fine
        synchronized ( regs )
        {
            regs.add(reg);
        }
        m_regCapSet.addCapability((BundleCapabilityImpl) reg.getReference());

        return reg;
    }

    /**
     * Unregister a service
     * @param bundle The bundle unregistering the service
     * @param reg The service registration
     */
    public void unregisterService(
            final Bundle bundle,
            final ServiceRegistration<?> reg)
    {
        // If this is a hook, it should be removed.
        this.hookRegistry.removeHooks(reg.getReference());

        // Now remove the registered service.
        final List<ServiceRegistration<?>> regs = m_regsMap.get(bundle);
        if (regs != null)
        {
            // this is a per bundle list, therefore synchronizing this should be fine
            synchronized ( regs )
            {
                regs.remove(reg);
            }
        }
        m_regCapSet.removeCapability((BundleCapabilityImpl) reg.getReference());

        // Notify callback objects about unregistering service.
        if (m_callbacks != null)
        {
            m_callbacks.serviceChanged(
                new ServiceEvent(ServiceEvent.UNREGISTERING, reg.getReference()), null);
        }

        // Now forcibly unget the service object for all stubborn clients.
        final ServiceReference<?> ref = reg.getReference();
        ungetServices(ref);

        // Invalidate registration
        ((ServiceRegistrationImpl) reg).invalidate();

        // Bundles are allowed to get a reference while unregistering
        // get fresh set of bundles (should be empty, but this is a sanity check)
        ungetServices(ref);
    }

    private void ungetServices(final ServiceReference<?> ref)
    {
        final Bundle[] clients = getUsingBundles(ref);
        for (int i = 0; (clients != null) && (i < clients.length); i++)
        {
            final UsageCount[] usages = m_inUseMap.get(clients[i]);
            for (int x = 0; (usages != null) && (x < usages.length); x++)
            {
                if (usages[x].m_ref.equals(ref))
                {
                    ungetService(clients[i], ref, (usages[x].m_prototype ? usages[x].getService() : null));
                }
            }
        }
    }

    /**
     * This method retrieves all services registrations for the specified
     * bundle and invokes <tt>ServiceRegistration.unregister()</tt> on each
     * one. This method is only called be the framework to clean up after
     * a stopped bundle.
     * @param bundle the bundle whose services should be unregistered.
     **/
    public void unregisterServices(final Bundle bundle)
    {
        // Simply remove all service registrations for the bundle.
        final List<ServiceRegistration<?>> regs = m_regsMap.remove(bundle);

        // Note, there is no race condition here with respect to the
        // bundle registering more services, because its bundle context
        // has already been invalidated by this point, so it would not
        // be able to register more services.

        // Unregister each service.
        if (regs != null)
        {
            final List<ServiceRegistration<?>> copyRefs;
            // there shouldn't be a need to sync, but just to be safe
            // we create a copy array and use that for iterating
            synchronized ( regs )
            {
                copyRefs = new ArrayList<ServiceRegistration<?>>(regs);
            }
            for (final ServiceRegistration<?> reg : copyRefs)
            {
                if (((ServiceRegistrationImpl) reg).isValid())
                {
                    try
                    {
                        reg.unregister();
                    }
                    catch (final IllegalStateException e)
                    {
                        // Ignore exception if the service has already been unregistered
                    }
                }
            }
        }
    }

    public Collection<Capability> getServiceReferences(final String className, SimpleFilter filter)
    {
        if ((className == null) && (filter == null))
        {
            // Return all services.
            filter = new SimpleFilter(null, null, SimpleFilter.MATCH_ALL);
        }
        else if ((className != null) && (filter == null))
        {
            // Return services matching the class name.
            filter = new SimpleFilter(Constants.OBJECTCLASS, className, SimpleFilter.EQ);
        }
        else if ((className != null) && (filter != null))
        {
            // Return services matching the class name and filter.
            final List<SimpleFilter> filters = new ArrayList<SimpleFilter>(2);
            filters.add(new SimpleFilter(Constants.OBJECTCLASS, className, SimpleFilter.EQ));
            filters.add(filter);
            filter = new SimpleFilter(null, filters, SimpleFilter.AND);
        }
        // else just use the specified filter.

        return m_regCapSet.match(filter, false);
    }

    public ServiceReference<?>[] getServicesInUse(final Bundle bundle)
    {
        final UsageCount[] usages = m_inUseMap.get(bundle);
        if (usages != null)
        {
            final ServiceReference<?>[] refs = new ServiceReference[usages.length];
            for (int i = 0; i < refs.length; i++)
            {
                refs[i] = usages[i].m_ref;
            }
            return refs;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <S> S getService(final Bundle bundle, final ServiceReference<S> ref, final boolean isServiceObjects)
    {
    	// prototype scope is only possible if called from ServiceObjects
    	final boolean isPrototype = isServiceObjects && ref.getProperty(Constants.SERVICE_SCOPE) == Constants.SCOPE_PROTOTYPE;
        UsageCount usage = null;
        Object svcObj = null;

        // Get the service registration.
        final ServiceRegistrationImpl reg =
            ((ServiceRegistrationImpl.ServiceReferenceImpl) ref).getRegistration();

        // We don't allow cycles when we call out to the service factory.
        if ( reg.currentThreadMarked() )
        {
            throw new ServiceException(
                    "ServiceFactory.getService() resulted in a cycle.",
                    ServiceException.FACTORY_ERROR,
                    null);
        }

        try
        {
            reg.markCurrentThread();

            // Make sure the service registration is still valid.
            if (reg.isValid())
            {
                // Get the usage count, or create a new one. If this is a
                // prototype, the we'll alway create a new one.
                usage = obtainUsageCount(bundle, ref, null, isPrototype);

                // Increment the usage count and grab the already retrieved
                // service object, if one exists.
                incrementToPositiveValue(usage.m_count);
                svcObj = usage.getService();

                if ( isServiceObjects )
                {
                    incrementToPositiveValue(usage.m_serviceObjectsCount);
                }

                // If we have a usage count, but no service object, then we haven't
                // cached the service object yet, so we need to create one.
                if (usage != null)
                {
                    ServiceHolder holder = null;

                    // There is a possibility that the holder is unset between the compareAndSet() and the get()
                    // below. If that happens get() returns null and we may have to set a new holder. This is
                    // why the below section is in a loop.
                    while (holder == null)
                    {
                        ServiceHolder h = new ServiceHolder();
                        if (usage.m_svcHolderRef.compareAndSet(null, h))
                        {
                            holder = h;
                            try {
                                svcObj = reg.getService(bundle);
                                holder.m_service = svcObj;
                            } finally {
                                holder.m_latch.countDown();
                            }
                        }
                        else
                        {
                            holder = usage.m_svcHolderRef.get();
                            if (holder != null)
                            {
                                try
                                {
                                    // Need to ensure that the other thread has obtained
                                    // the service.
                                    holder.m_latch.await();
                                }
                                catch (InterruptedException e)
                                {
                                    throw new RuntimeException(e);
                                }
                                svcObj = holder.m_service;
                            }
                        }

                        // if someone concurrently changed the holder, loop again
                        if (holder != usage.m_svcHolderRef.get())
                            holder = null;
                    }
                }
            }
        }
        finally
        {
            reg.unmarkCurrentThread();

            if (!reg.isValid() || (svcObj == null))
            {
                flushUsageCount(bundle, ref, usage);
            }
        }

        return (S) svcObj;
    }

    // Increment the Atomic Long by 1, and ensure the result is at least 1.
    // This method uses a loop, optimistic algorithm to do this in a threadsafe
    // way without locks.
    private void incrementToPositiveValue(AtomicLong al)
    {
        boolean success = false;

        while (!success)
        {
            long oldVal = al.get();
            long newVal = Math.max(oldVal + 1L, 1L);
            checkCountOverflow(newVal);

            success = al.compareAndSet(oldVal, newVal);
        }
    }

    private void checkCountOverflow(long c)
    {
        if (c == Long.MAX_VALUE)
        {
            throw new ServiceException(
                    "The use count for the service overflowed.",
                    ServiceException.UNSPECIFIED,
                    null);
        }
    }

    public boolean ungetService(final Bundle bundle, final ServiceReference<?> ref, final Object svcObj)
    {
        final ServiceRegistrationImpl reg =
            ((ServiceRegistrationImpl.ServiceReferenceImpl) ref).getRegistration();

        if ( reg.currentThreadMarked() )
        {
            throw new IllegalStateException(
                    "ServiceFactory.ungetService() resulted in a cycle.");
        }

        try
        {
            // Mark the current thread to avoid cycles
            reg.markCurrentThread();

            // Get the usage count.
            UsageCount usage = obtainUsageCount(bundle, ref, svcObj, null);
            // If there are no cached services, then just return immediately.
            if (usage == null)
            {
                return false;
            }
            // if this is a call from service objects and the service was not fetched from
            // there, return false
            if ( svcObj != null )
            {
                if (usage.m_serviceObjectsCount.decrementAndGet() < 0)
                {
                    return false;
                }
            }

            // If usage count will go to zero, then unget the service
            // from the registration.
            long count = usage.m_count.decrementAndGet();
            try
            {
                if (count <= 0)
                {
                    ServiceHolder holder = usage.m_svcHolderRef.get();
                    Object svc = holder != null ? holder.m_service : null;

                    if (svc != null)
                    {
                        // Check the count again to ensure that nobody else has just
                        // obtained the service again
                        if (usage.m_count.get() <= 0)
                        {
                            if (usage.m_svcHolderRef.compareAndSet(holder, null))
                            {
                                // Remove reference from usages array.
                                ((ServiceRegistrationImpl.ServiceReferenceImpl) ref)
                                    .getRegistration().ungetService(bundle, svc);

                            }
                        }
                    }
                }

                return count >= 0;
            }
            finally
            {
                if (!reg.isValid())
                {
                    usage.m_svcHolderRef.set(null);
                }

                // If the registration is invalid or the usage count for a prototype
                // reached zero, then flush it. Non-prototype services are not flushed
                // on ungetService() when they reach 0 as this introduces a race
                // condition of concurrently the same service is obtained via getService()
                if (!reg.isValid() || (count <= 0 && svcObj != null))
                {
                    flushUsageCount(bundle, ref, usage);
                }
            }
        }
        finally
        {
            reg.unmarkCurrentThread();
        }
    }


    /**
     * This is a utility method to release all services being
     * used by the specified bundle.
     * @param bundle the bundle whose services are to be released.
    **/
    public void ungetServices(final Bundle bundle)
    {
        UsageCount[] usages = m_inUseMap.get(bundle);
        if (usages == null)
        {
            return;
        }

        // Note, there is no race condition here with respect to the
        // bundle using more services, because its bundle context
        // has already been invalidated by this point, so it would not
        // be able to look up more services.

        // Remove each service object from the
        // service cache.
        for (int i = 0; i < usages.length; i++)
        {
            if (usages[i].m_svcHolderRef.get() == null)
                continue;

            // Keep ungetting until all usage count is zero.
            while (ungetService(bundle, usages[i].m_ref, usages[i].m_prototype ? usages[i].getService() : null))
            {
                // Empty loop body.
            }
        }
    }

    public Bundle[] getUsingBundles(ServiceReference<?> ref)
    {
        Bundle[] bundles = null;
        for (Iterator<Map.Entry<Bundle, UsageCount[]>> iter = m_inUseMap.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry<Bundle, UsageCount[]> entry = iter.next();
            Bundle bundle = entry.getKey();
            UsageCount[] usages = entry.getValue();
            for (int useIdx = 0; useIdx < usages.length; useIdx++)
            {
                if (usages[useIdx].m_ref.equals(ref))
                {
                    // Add the bundle to the array to be returned.
                    if (bundles == null)
                    {
                        bundles = new Bundle[] { bundle };
                    }
                    else
                    {
                        Bundle[] nbs = new Bundle[bundles.length + 1];
                        System.arraycopy(bundles, 0, nbs, 0, bundles.length);
                        nbs[bundles.length] = bundle;
                        bundles = nbs;
                    }
                }
            }
        }
        return bundles;
    }

    void servicePropertiesModified(ServiceRegistration<?> reg, Dictionary<?,?> oldProps)
    {
        this.hookRegistry.updateHooks(reg.getReference());
        if (m_callbacks != null)
        {
            m_callbacks.serviceChanged(
                new ServiceEvent(ServiceEvent.MODIFIED, reg.getReference()), oldProps);
        }
    }

    public Logger getLogger()
    {
        return m_logger;
    }

    /**
     * Obtain a UsageCount object, by looking for an existing one or creating a new one (if possible).
     * This method tries to find a UsageCount object in the {@code m_inUseMap}. If one is found then
     * this is returned, otherwise a UsageCount object will be created, but this can only be done if
     * the {@code isPrototype} parameter is not {@code null}. If {@code isPrototype} is {@code TRUE}
     * then a new UsageCount object will always be created.
     * @param bundle The bundle using the service.
     * @param ref The Service Reference.
     * @param svcObj A Service Object, if applicable.
     * @param isPrototype {@code TRUE} if we know that this is a prototype, {@ FALSE} if we know that
     * it isn't. There are cases where we don't know (the pure lookup case), in that case use {@code null}.
     * @return The UsageCount object if it could be obtained, or {@code null} otherwise.
     */
    UsageCount obtainUsageCount(Bundle bundle, ServiceReference<?> ref, Object svcObj, Boolean isPrototype)
    {
        UsageCount usage = null;

        // This method uses an optimistic concurrency mechanism with a conditional put/replace
        // on the m_inUseMap. If this fails (because another thread made changes) this thread
        // retries the operation. This is the purpose of the while loop.
        boolean success = false;
        while (!success)
        {
            UsageCount[] usages = m_inUseMap.get(bundle);

            // If we know it's a prototype, then we always need to create a new usage count
            if (!Boolean.TRUE.equals(isPrototype))
            {
                for (int i = 0; (usages != null) && (i < usages.length); i++)
                {
                    if (usages[i].m_ref.equals(ref)
                       && ((svcObj == null && !usages[i].m_prototype) || usages[i].getService() == svcObj))
                    {
                        return usages[i];
                    }
                }
            }

            // We haven't found an existing usage count object so we need to create on. For this we need to
            // know whether this is a prototype or not.
            if (isPrototype == null)
            {
                // If this parameter isn't passed in we can't create a usage count.
                return null;
            }

            // Add a new Usage Count.
            usage = new UsageCount(ref, isPrototype);
            if (usages == null)
            {
                UsageCount[] newUsages = new UsageCount[] { usage };
                success = m_inUseMap.putIfAbsent(bundle, newUsages) == null;
            }
            else
            {
                UsageCount[] newUsages = new UsageCount[usages.length + 1];
                System.arraycopy(usages, 0, newUsages, 0, usages.length);
                newUsages[usages.length] = usage;
                success = m_inUseMap.replace(bundle, usages, newUsages);
            }
        }
        return usage;
    }

    /**
     * Utility method to flush the specified bundle's usage count for the
     * specified service reference. This should be called to completely
     * remove the associated usage count object for the specified service
     * reference. If the goal is to simply decrement the usage, then get
     * the usage count and decrement its counter. This method will also
     * remove the specified bundle from the "in use" map if it has no more
     * usage counts after removing the usage count for the specified service
     * reference.
     * @param bundle The bundle whose usage count should be removed.
     * @param ref The service reference whose usage count should be removed.
    **/
    void flushUsageCount(Bundle bundle, ServiceReference<?> ref, UsageCount uc)
    {
        // This method uses an optimistic concurrency mechanism with conditional modifications
        // on the m_inUseMap. If this fails (because another thread made changes) this thread
        // retries the operation. This is the purpose of the while loop.
        boolean success = false;
        while (!success)
        {
            UsageCount[] usages = m_inUseMap.get(bundle);
            final UsageCount[] orgUsages = usages;
            for (int i = 0; (usages != null) && (i < usages.length); i++)
            {
                if ((uc == null && usages[i].m_ref.equals(ref)) || (uc == usages[i]))
                {
                    // If this is the only usage, then point to empty list.
                    if ((usages.length - 1) == 0)
                    {
                        usages = null;
                    }
                    // Otherwise, we need to do some array copying.
                    else
                    {
                        UsageCount[] newUsages = new UsageCount[usages.length - 1];
                        System.arraycopy(usages, 0, newUsages, 0, i);
                        if (i < newUsages.length)
                        {
                            System.arraycopy(
                                usages, i + 1, newUsages, i, newUsages.length - i);
                        }
                        usages = newUsages;
                        i--;
                    }
                }
            }

            if (usages == orgUsages)
                return; // no change in map

            if (orgUsages != null)
            {
                if (usages != null)
                    success = m_inUseMap.replace(bundle, orgUsages, usages);
                else
                    success = m_inUseMap.remove(bundle, orgUsages);
            }
        }
    }

    public HookRegistry getHookRegistry()
    {
        return this.hookRegistry;
    }

    static class UsageCount
    {
        final ServiceReference<?> m_ref;
        final boolean m_prototype;

        final AtomicLong m_count = new AtomicLong();
        final AtomicLong m_serviceObjectsCount = new AtomicLong();
        final AtomicReference<ServiceHolder> m_svcHolderRef = new AtomicReference<ServiceHolder>();

        UsageCount(final ServiceReference<?> ref, final boolean isPrototype)
        {
            m_ref = ref;
            m_prototype = isPrototype;
        }

        Object getService()
        {
            ServiceHolder sh = m_svcHolderRef.get();
            return sh == null ? null : sh.m_service;
        }
    }

    static class ServiceHolder
    {
        final CountDownLatch m_latch = new CountDownLatch(1);
        volatile Object m_service;
    }

    public interface ServiceRegistryCallbacks
    {
        void serviceChanged(ServiceEvent event, Dictionary<?,?> oldProps);
    }
}
