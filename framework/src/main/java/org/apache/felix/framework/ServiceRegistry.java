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

<<<<<<< HEAD
import java.util.*;
=======
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

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.apache.felix.framework.capabilityset.CapabilitySet;
import org.apache.felix.framework.capabilityset.SimpleFilter;
import org.apache.felix.framework.wiring.BundleCapabilityImpl;
import org.osgi.framework.Bundle;
<<<<<<< HEAD
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleCapability;
=======
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.resource.Capability;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

public class ServiceRegistry
{
    private final Logger m_logger;
<<<<<<< HEAD
    private long m_currentServiceId = 1L;
    // Maps bundle to an array of service registrations.
    private final Map m_regsMap = Collections.synchronizedMap(new HashMap());
    // Capability set for all service registrations.
    private final CapabilitySet m_regCapSet;

    // Maps registration to thread to keep track when a
    // registration is in use, which will cause other
    // threads to wait.
    private final Map m_lockedRegsMap = new HashMap();
    // Maps bundle to an array of usage counts.
    private final Map m_inUseMap = new HashMap();

    private final ServiceRegistryCallbacks m_callbacks;

    private final WeakHashMap<ServiceReference, ServiceReference> m_blackList =
        new WeakHashMap<ServiceReference, ServiceReference>();

    private final static Class<?>[] m_hookClasses = {
        org.osgi.framework.hooks.bundle.FindHook.class,
        org.osgi.framework.hooks.bundle.EventHook.class,
        org.osgi.framework.hooks.service.EventHook.class,
        org.osgi.framework.hooks.service.EventListenerHook.class,
        org.osgi.framework.hooks.service.FindHook.class,
        org.osgi.framework.hooks.service.ListenerHook.class,
        org.osgi.framework.hooks.weaving.WeavingHook.class,
        org.osgi.framework.hooks.resolver.ResolverHookFactory.class,
        org.osgi.service.url.URLStreamHandlerService.class,
        java.net.ContentHandler.class
    };
    private final Map<Class<?>, Set<ServiceReference<?>>> m_allHooks =
        new HashMap<Class<?>, Set<ServiceReference<?>>>();

    public ServiceRegistry(Logger logger, ServiceRegistryCallbacks callbacks)
    {
        m_logger = logger;
        m_callbacks = callbacks;

        List indices = new ArrayList();
        indices.add(Constants.OBJECTCLASS);
        m_regCapSet = new CapabilitySet(indices, false);
    }

    public ServiceReference[] getRegisteredServices(Bundle bundle)
    {
        ServiceRegistration[] regs = (ServiceRegistration[]) m_regsMap.get(bundle);
        if (regs != null)
        {
            List refs = new ArrayList(regs.length);
            for (int i = 0; i < regs.length; i++)
            {
                try
                {
                    refs.add(regs[i].getReference());
                }
                catch (IllegalStateException ex)
                {
                    // Don't include the reference as it is not valid anymore
                }
            }
            return (ServiceReference[]) refs.toArray(new ServiceReference[refs.size()]);
=======

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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        return null;
    }

<<<<<<< HEAD
    // Caller is expected to fire REGISTERED event.
    public ServiceRegistration registerService(
        BundleContext context, String[] classNames, Object svcObj, Dictionary dict)
    {
        ServiceRegistrationImpl reg = null;
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

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
<<<<<<< HEAD
            Bundle bundle = context.getBundle();

            // Create the service registration.
            reg = new ServiceRegistrationImpl(
                this, bundle, classNames, new Long(m_currentServiceId++), svcObj, dict);

            // Keep track of registered hooks.
            addHooks(classNames, svcObj, reg.getReference());

            // Get the bundles current registered services.
            ServiceRegistration[] regs = (ServiceRegistration[]) m_regsMap.get(bundle);
            m_regsMap.put(bundle, addServiceRegistration(regs, reg));
            m_regCapSet.addCapability((BundleCapabilityImpl) reg.getReference());
        }
=======
            regs.add(reg);
        }
        m_regCapSet.addCapability((BundleCapabilityImpl) reg.getReference());
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

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
<<<<<<< HEAD
        removeHook(reg.getReference());

        synchronized (this)
        {
            // Note that we don't lock the service registration here using
            // the m_lockedRegsMap because we want to allow bundles to get
            // the service during the unregistration process. However, since
            // we do remove the registration from the service registry, no
            // new bundles will be able to look up the service.

            // Now remove the registered service.
            ServiceRegistration[] regs = (ServiceRegistration[]) m_regsMap.get(bundle);
            m_regsMap.put(bundle, removeServiceRegistration(regs, reg));
            m_regCapSet.removeCapability((BundleCapabilityImpl) reg.getReference());
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        m_regCapSet.removeCapability((BundleCapabilityImpl) reg.getReference());

        // Notify callback objects about unregistering service.
        if (m_callbacks != null)
        {
            m_callbacks.serviceChanged(
                new ServiceEvent(ServiceEvent.UNREGISTERING, reg.getReference()), null);
        }

        // Now forcibly unget the service object for all stubborn clients.
<<<<<<< HEAD
        Bundle[] clients = getUsingBundles(reg.getReference());
        for (int i = 0; (clients != null) && (i < clients.length); i++)
        {
            while (ungetService(clients[i], reg.getReference()))
                ; // Keep removing until it is no longer possible
=======
        final ServiceReference<?> ref = reg.getReference();
        ungetServices(ref);

        // Invalidate registration
        ((ServiceRegistrationImpl) reg).invalidate();

        // Bundles are allowed to get a reference while unregistering
        // get fresh set of bundles (should be empty, but this is a sanity check)
        ungetServices(ref);

        // Now flush all usage counts as the registration is invalid
        for (Bundle usingBundle : m_inUseMap.keySet())
        {
            flushUsageCount(usingBundle, ref, null);
        }
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        ((ServiceRegistrationImpl) reg).invalidate();
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
<<<<<<< HEAD
        ServiceRegistration[] regs = null;
        synchronized (this)
        {
            regs = (ServiceRegistration[]) m_regsMap.get(bundle);
        }
=======
        final List<ServiceRegistration<?>> regs = m_regsMap.remove(bundle);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        // Note, there is no race condition here with respect to the
        // bundle registering more services, because its bundle context
        // has already been invalidated by this point, so it would not
        // be able to register more services.

        // Unregister each service.
<<<<<<< HEAD
        for (int i = 0; (regs != null) && (i < regs.length); i++)
        {
            if (((ServiceRegistrationImpl) regs[i]).isValid())
            {
                try
                {
                    regs[i].unregister();
                }
                catch (IllegalStateException e)
                {
                    // Ignore exception if the service has already been unregistered
                }
            }
        }

        // Now remove the bundle itself.
        synchronized (this)
        {
            m_regsMap.remove(bundle);
        }
    }

    public synchronized List getServiceReferences(String className, SimpleFilter filter)
    {
        if ((className == null) && (filter == null))
        {
            // Return all services.
            filter = new SimpleFilter(Constants.OBJECTCLASS, "*", SimpleFilter.PRESENT);
        }
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        else if ((className != null) && (filter == null))
        {
            // Return services matching the class name.
            filter = new SimpleFilter(Constants.OBJECTCLASS, className, SimpleFilter.EQ);
        }
        else if ((className != null) && (filter != null))
        {
            // Return services matching the class name and filter.
<<<<<<< HEAD
            List filters = new ArrayList(2);
=======
            final List<SimpleFilter> filters = new ArrayList<SimpleFilter>(2);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            filters.add(new SimpleFilter(Constants.OBJECTCLASS, className, SimpleFilter.EQ));
            filters.add(filter);
            filter = new SimpleFilter(null, filters, SimpleFilter.AND);
        }
        // else just use the specified filter.
<<<<<<< HEAD

        Set<BundleCapability> matches = m_regCapSet.match(filter, false);

        return new ArrayList(matches);
=======

        return m_regCapSet.match(filter, false);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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

<<<<<<< HEAD
    public <S> S getService(Bundle bundle, ServiceReference<S> ref)
=======
    @SuppressWarnings("unchecked")
    public <S> S getService(final Bundle bundle, final ServiceReference<S> ref, final boolean isServiceObjects)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        // prototype scope is only possible if called from ServiceObjects
        final boolean isPrototype = isServiceObjects && ref.getProperty(Constants.SERVICE_SCOPE) == Constants.SCOPE_PROTOTYPE;
        UsageCount usage = null;
        Object svcObj = null;

        // Get the service registration.
<<<<<<< HEAD
        ServiceRegistrationImpl reg =
=======
        final ServiceRegistrationImpl reg =
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            ((ServiceRegistrationImpl.ServiceReferenceImpl) ref).getRegistration();

        // We don't allow cycles when we call out to the service factory.
        if ( reg.currentThreadMarked() )
        {
<<<<<<< HEAD
            // First make sure that no existing operation is currently
            // being performed by another thread on the service registration.
            for (Object o = m_lockedRegsMap.get(reg); (o != null); o = m_lockedRegsMap.get(reg))
            {
                // We don't allow cycles when we call out to the service factory.
                if (o.equals(Thread.currentThread()))
                {
                    throw new ServiceException(
                        "ServiceFactory.getService() resulted in a cycle.",
                        ServiceException.FACTORY_ERROR,
                        null);
                }

                // Otherwise, wait for it to be freed.
                try
                {
                    wait();
                }
                catch (InterruptedException ex)
                {
                }
            }
=======
            throw new ServiceException(
                    "ServiceFactory.getService() resulted in a cycle.",
                    ServiceException.FACTORY_ERROR,
                    null);
        }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        try
        {
            reg.markCurrentThread();

            // Make sure the service registration is still valid.
            if (reg.isValid())
            {
                // Get the usage count, or create a new one. If this is a
                // prototype, the we'll alway create a new one.
                usage = obtainUsageCount(bundle, ref, null, isPrototype);

<<<<<<< HEAD
                // If we don't have a usage count, then create one and
                // since the spec says we increment usage count before
                // actually getting the service object.
                if (usage == null)
                {
                    usage = addUsageCount(bundle, ref);
                }

                // Increment the usage count and grab the already retrieved
                // service object, if one exists.
                usage.m_count++;
                svcObj = usage.m_svcObj;
            }
        }

        // If we have a usage count, but no service object, then we haven't
        // cached the service object yet, so we need to create one now without
        // holding the lock, since we will potentially call out to a service
        // factory.
        try
        {
            if ((usage != null) && (svcObj == null))
            {
                svcObj = reg.getService(bundle);
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }
        finally
        {
<<<<<<< HEAD
            // If we successfully retrieved a service object, then we should
            // cache it in the usage count. If not, we should flush the usage
            // count. Either way, we need to unlock the service registration
            // so that any threads waiting for it can continue.
            synchronized (this)
            {
                // Before caching the service object, double check to see if
                // the registration is still valid, since it may have been
                // unregistered while we didn't hold the lock.
                if (!reg.isValid() || (svcObj == null))
                {
                    flushUsageCount(bundle, ref);
                }
                else
                {
                    usage.m_svcObj = svcObj;
                }
                m_lockedRegsMap.remove(reg);
                notifyAll();
=======
            reg.unmarkCurrentThread();

            if (!reg.isValid() || (svcObj == null))
            {
                flushUsageCount(bundle, ref, usage);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }

        return (S) svcObj;
    }

    // Increment the Atomic Long by 1, and ensure the result is at least 1.
    // This method uses a loop, optimistic algorithm to do this in a threadsafe
    // way without locks.
    private void incrementToPositiveValue(AtomicLong al)
    {
<<<<<<< HEAD
        UsageCount usage = null;
        ServiceRegistrationImpl reg =
            ((ServiceRegistrationImpl.ServiceReferenceImpl) ref).getRegistration();
=======
        boolean success = false;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

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
<<<<<<< HEAD

            // Lock the service registration.
            m_lockedRegsMap.put(reg, Thread.currentThread());
        }

        // If usage count will go to zero, then unget the service
        // from the registration; we do this outside the lock
        // since this might call out to the service factory.
        try
        {
            if (usage.m_count == 1)
            {
                // Remove reference from usages array.
                ((ServiceRegistrationImpl.ServiceReferenceImpl) ref)
                    .getRegistration().ungetService(bundle, usage.m_svcObj);
            }
        }
        finally
        {
            // Finally, decrement usage count and flush if it goes to zero or
            // the registration became invalid while we were not holding the
            // lock. Either way, unlock the service registration so that any
            // threads waiting for it can continue.
            synchronized (this)
            {
                // Decrement usage count, which spec says should happen after
                // ungetting the service object.
                usage.m_count--;

                // If the registration is invalid or the usage count has reached
                // zero, then flush it.
                if (!reg.isValid() || (usage.m_count <= 0))
                {
                    usage.m_svcObj = null;
                    flushUsageCount(bundle, ref);
                }

                // Release the registration lock so any waiting threads can
                // continue.
                m_lockedRegsMap.remove(reg);
                notifyAll();
            }
        }

        return true;
=======
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
                                // Temporarily increase the usage again so that the 
                                // service factory still sees the usage in the unget
                                usage.m_count.incrementAndGet();
                                try
                                {
                                    // Remove reference from usages array.
                                    ((ServiceRegistrationImpl.ServiceReferenceImpl) ref)
                                        .getRegistration().ungetService(bundle, svc);
                                }
                                finally 
                                {
                                    // now we can decrease the usage again
                                    usage.m_count.decrementAndGet();
                                }

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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    /**
     * This is a utility method to release all services being
     * used by the specified bundle.
     * @param bundle the bundle whose services are to be released.
    **/
    public void ungetServices(final Bundle bundle)
    {
<<<<<<< HEAD
        UsageCount[] usages;
        synchronized (this)
        {
            usages = (UsageCount[]) m_inUseMap.get(bundle);
        }

=======
        UsageCount[] usages = m_inUseMap.get(bundle);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
                if (usages[useIdx].m_ref.equals(ref) && usages[useIdx].m_count.get() > 0)
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

<<<<<<< HEAD
    void servicePropertiesModified(ServiceRegistration reg, Dictionary oldProps)
    {
        updateHook(reg.getReference());
=======
    void servicePropertiesModified(ServiceRegistration<?> reg, Dictionary<?,?> oldProps)
    {
        this.hookRegistry.updateHooks(reg.getReference());
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
        }
        return regs;
    }

    /**
     * Utility method to retrieve the specified bundle's usage count for the
     * specified service reference.
     * @param bundle The bundle whose usage counts are being searched.
     * @param ref The service reference to find in the bundle's usage counts.
     * @return The associated usage count or null if not found.
    **/
    private UsageCount getUsageCount(Bundle bundle, ServiceReference ref)
    {
        UsageCount[] usages = (UsageCount[]) m_inUseMap.get(bundle);
        for (int i = 0; (usages != null) && (i < usages.length); i++)
        {
            if (usages[i].m_ref.equals(ref))
            {
                return usages[i];
            }
        }
        return null;
    }

    /**
     * Utility method to update the specified bundle's usage count array to
     * include the specified service. This method should only be called
     * to add a usage count for a previously unreferenced service. If the
     * service already has a usage count, then the existing usage count
     * counter simply needs to be incremented.
     * @param bundle The bundle acquiring the service.
     * @param ref The service reference of the acquired service.
     * @param svcObj The service object of the acquired service.
    **/
    private UsageCount addUsageCount(Bundle bundle, ServiceReference ref)
    {
        UsageCount[] usages = (UsageCount[]) m_inUseMap.get(bundle);

        UsageCount usage = new UsageCount();
        usage.m_ref = ref;

        if (usages == null)
        {
            usages = new UsageCount[] { usage };
        }
        else
        {
            UsageCount[] newUsages = new UsageCount[usages.length + 1];
            System.arraycopy(usages, 0, newUsages, 0, usages.length);
            newUsages[usages.length] = usage;
            usages = newUsages;
        }

        m_inUseMap.put(bundle, usages);

=======

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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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

<<<<<<< HEAD
    //
    // Hook-related methods.
    //

    boolean isHookBlackListed(ServiceReference sr)
    {
        return m_blackList.containsKey(sr);
    }

    void blackListHook(ServiceReference sr)
    {
        m_blackList.put(sr, sr);
    }

    static boolean isHook(String[] classNames, Class<?> hookClass, Object svcObj)
    {
        // For a service factory, we can only match names.
        if (svcObj instanceof ServiceFactory)
        {
            for (String className : classNames)
            {
                if (className.equals(hookClass.getName()))
                {
                    return true;
                }
            }
        }

        // For a service object, check if its class matches.
        if (hookClass.isAssignableFrom(svcObj.getClass()))
        {
            // But still only if it is registered under that interface.
            String hookName = hookClass.getName();
            for (String className : classNames)
            {
                if (className.equals(hookName))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private void addHooks(String[] classNames, Object svcObj, ServiceReference<?> ref)
    {
        for (Class<?> hookClass : m_hookClasses)
        {
            if (isHook(classNames, hookClass, svcObj))
            {
                synchronized (m_allHooks)
                {
                    Set<ServiceReference<?>> hooks = m_allHooks.get(hookClass);
                    if (hooks == null)
                    {
                        hooks = new TreeSet<ServiceReference<?>>(Collections.reverseOrder());
                        m_allHooks.put(hookClass, hooks);
                    }
                    hooks.add(ref);
                }
            }
        }
    }

    private void updateHook(ServiceReference ref)
    {
        // We maintain the hooks sorted, so if ranking has changed for example,
        // we need to ensure the order remains correct by resorting the hooks.
        Object svcObj = ((ServiceRegistrationImpl.ServiceReferenceImpl) ref)
                .getRegistration().getService();
        String [] classNames = (String[]) ref.getProperty(Constants.OBJECTCLASS);

        for (Class<?> hookClass : m_hookClasses)
        {
            if (isHook(classNames, hookClass, svcObj))
            {
                synchronized (m_allHooks)
                {
                    Set<ServiceReference<?>> hooks = m_allHooks.get(hookClass);
                    if (hooks != null)
                    {
                        List<ServiceReference<?>> refs = new ArrayList<ServiceReference<?>>(hooks);
                        hooks.clear();
                        hooks.addAll(refs);
                    }
                }
            }
        }
    }

    private void removeHook(ServiceReference ref)
    {
        Object svcObj = ((ServiceRegistrationImpl.ServiceReferenceImpl) ref)
            .getRegistration().getService();
        String [] classNames = (String[]) ref.getProperty(Constants.OBJECTCLASS);

        for (Class<?> hookClass : m_hookClasses)
        {
            if (isHook(classNames, hookClass, svcObj))
            {
                synchronized (m_allHooks)
                {
                    Set<ServiceReference<?>> hooks = m_allHooks.get(hookClass);
                    if (hooks != null)
                    {
                        hooks.remove(ref);
                        if (hooks.isEmpty())
                        {
                            m_allHooks.remove(hookClass);
                        }
                    }
                }
            }
        }
    }

    public <S> Set<ServiceReference<S>> getHooks(Class<S> hookClass)
    {
        synchronized (m_allHooks)
        {
            Set<ServiceReference<?>> hooks = m_allHooks.get(hookClass);
            if (hooks != null)
            {
                SortedSet sorted = new TreeSet<ServiceReference<?>>(Collections.reverseOrder());
                sorted.addAll(hooks);
                return asTypedSortedSet(sorted);
            }
            return Collections.EMPTY_SET;
        }
    }

    private static <S> SortedSet<ServiceReference<S>> asTypedSortedSet(
        SortedSet<ServiceReference<?>> ss)
    {
        return (SortedSet<ServiceReference<S>>) (SortedSet) ss;
    }

    private static class UsageCount
=======
    static class ServiceHolder
    {
        final CountDownLatch m_latch = new CountDownLatch(1);
        volatile Object m_service;
    }

    public interface ServiceRegistryCallbacks
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        void serviceChanged(ServiceEvent event, Dictionary<?,?> oldProps);
    }
<<<<<<< HEAD

    public interface ServiceRegistryCallbacks
    {
        void serviceChanged(ServiceEvent event, Dictionary oldProps);
    }
}
=======
}
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
