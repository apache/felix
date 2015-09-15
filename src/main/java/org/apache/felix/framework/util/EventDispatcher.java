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
package org.apache.felix.framework.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.ServiceRegistry;
import org.osgi.framework.AllServiceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.UnfilteredServiceListener;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.framework.launch.Framework;

public class EventDispatcher
{
    private final Logger m_logger;
    private final ServiceRegistry m_registry;

    private Map<BundleContext, List<ListenerInfo>>
        m_fwkListeners = Collections.EMPTY_MAP;
    private Map<BundleContext, List<ListenerInfo>>
        m_bndlListeners = Collections.EMPTY_MAP;
    private Map<BundleContext, List<ListenerInfo>>
        m_syncBndlListeners = Collections.EMPTY_MAP;
    private Map<BundleContext, List<ListenerInfo>>
        m_svcListeners = Collections.EMPTY_MAP;

    // A single thread is used to deliver events for all dispatchers.
    private static Thread m_thread = null;
    private final static String m_threadLock = new String("thread lock");
    private static int m_references = 0;
    private static volatile boolean m_stopping = false;

    // List of requests.
    private static final List<Request> m_requestList = new ArrayList<Request>();
    // Pooled requests to avoid memory allocation.
    private static final List<Request> m_requestPool = new ArrayList<Request>();

    private static final SecureAction m_secureAction = new SecureAction();

    public EventDispatcher(Logger logger, ServiceRegistry registry)
    {
        m_logger = logger;
        m_registry = registry;
    }

    public void startDispatching()
    {
        synchronized (m_threadLock)
        {
            // Start event dispatching thread if necessary.
            if (m_thread == null || !m_thread.isAlive())
            {
                m_stopping = false;

                m_thread = new Thread(new Runnable() {
                    @Override
                    public void run()
                    {
                        try
                        {
                            EventDispatcher.run();
                        }
                        finally
                        {
                            // Ensure we update state even if stopped by external cause
                            // e.g. an Applet VM forceably killing threads
                            synchronized (m_threadLock)
                            {
                                m_thread = null;
                                m_stopping = false;
                                m_references = 0;
                                m_threadLock.notifyAll();
                            }
                        }
                    }
                }, "FelixDispatchQueue");
                m_thread.start();
            }

            // reference counting and flags
            m_references++;
        }
    }

    public void stopDispatching()
    {
        synchronized (m_threadLock)
        {
            // Return if already dead or stopping.
            if (m_thread == null || m_stopping)
            {
                return;
            }

            // decrement use counter, don't continue if there are users
            m_references--;
            if (m_references > 0)
            {
                return;
            }

            m_stopping = true;
        }

        // Signal dispatch thread.
        synchronized (m_requestList)
        {
            m_requestList.notify();
        }

        // Use separate lock for shutdown to prevent any chance of nested lock deadlock
        synchronized (m_threadLock)
        {
            while (m_thread != null)
            {
                try
                {
                    m_threadLock.wait();
                }
                catch (InterruptedException ex)
                {
                }
            }
        }
    }

    public Filter addListener(BundleContext bc, Class clazz, EventListener l, Filter filter)
    {
        // Verify the listener.
        if (l == null)
        {
            throw new IllegalArgumentException("Listener is null");
        }
        else if (!clazz.isInstance(l))
        {
            throw new IllegalArgumentException(
                "Listener not of type " + clazz.getName());
        }

        // See if we can simply update the listener, if so then
        // return immediately.
        Filter oldFilter = updateListener(bc, clazz, l, filter);
        if (oldFilter != null)
        {
            return oldFilter;
        }

        // Lock the object to add the listener.
        synchronized (this)
        {
            // Verify that the bundle context is still valid.
            try
            {
                bc.getBundle();
            }
            catch (IllegalStateException ex)
            {
                // Bundle context is no longer valid, so just return.
                return null;
            }

            Map<BundleContext, List<ListenerInfo>> listeners = null;
            Object acc = null;

            if (clazz == FrameworkListener.class)
            {
                listeners = m_fwkListeners;
            }
            else if (clazz == BundleListener.class)
            {
                if (SynchronousBundleListener.class.isInstance(l))
                {
                    listeners = m_syncBndlListeners;
                }
                else
                {
                    listeners = m_bndlListeners;
                }
            }
            else if (clazz == ServiceListener.class)
            {
                // Remember security context for filtering service events.
                Object sm = System.getSecurityManager();
                if (sm != null)
                {
                    acc = ((SecurityManager) sm).getSecurityContext();
                }
                // We need to create a Set for keeping track of matching service
                // registrations so we can fire ServiceEvent.MODIFIED_ENDMATCH
                // events. We need a Set even if filter is null, since the
                // listener can be updated and have a filter added later.
                listeners = m_svcListeners;
            }
            else
            {
                throw new IllegalArgumentException("Unknown listener: " + l.getClass());
            }

            // Add listener.
            ListenerInfo info =
                new ListenerInfo(bc.getBundle(), bc, clazz, l, filter, acc, false);
            listeners = addListenerInfo(listeners, info);

            if (clazz == FrameworkListener.class)
            {
                m_fwkListeners = listeners;
            }
            else if (clazz == BundleListener.class)
            {
                if (SynchronousBundleListener.class.isInstance(l))
                {
                    m_syncBndlListeners = listeners;
                }
                else
                {
                    m_bndlListeners = listeners;
                }
            }
            else if (clazz == ServiceListener.class)
            {
                m_svcListeners = listeners;
            }
        }
        return null;
    }

    public ListenerHook.ListenerInfo removeListener(
        BundleContext bc, Class clazz, EventListener l)
    {
        ListenerHook.ListenerInfo returnInfo = null;

        // Verify listener.
        if (l == null)
        {
            throw new IllegalArgumentException("Listener is null");
        }
        else if (!clazz.isInstance(l))
        {
            throw new IllegalArgumentException(
                "Listener not of type " + clazz.getName());
        }

        // Lock the object to remove the listener.
        synchronized (this)
        {
            Map<BundleContext, List<ListenerInfo>> listeners = null;

            if (clazz == FrameworkListener.class)
            {
                listeners = m_fwkListeners;
            }
            else if (clazz == BundleListener.class)
            {
                if (SynchronousBundleListener.class.isInstance(l))
                {
                    listeners = m_syncBndlListeners;
                }
                else
                {
                    listeners = m_bndlListeners;
                }
            }
            else if (clazz == ServiceListener.class)
            {
                listeners = m_svcListeners;
            }
            else
            {
                throw new IllegalArgumentException("Unknown listener: " + l.getClass());
            }

            // Try to find the instance in our list.
            int idx = -1;
            for (Entry<BundleContext, List<ListenerInfo>> entry : listeners.entrySet())
            {
                List<ListenerInfo> infos = entry.getValue();
                for (int i = 0; i < infos.size(); i++)
                {
                    ListenerInfo info = infos.get(i);
                    if (info.getBundleContext().equals(bc) &&
                        (info.getListenerClass() == clazz) &&
                        (info.getListener() == l))
                    {
                        // For service listeners, we must return some info about
                        // the listener for the ListenerHook callback.
                        if (ServiceListener.class == clazz)
                        {
                            returnInfo = new ListenerInfo(infos.get(i), true);
                        }
                        idx = i;
                        break;
                    }
                }
            }

            // If we have the instance, then remove it.
            if (idx >= 0)
            {
                listeners = removeListenerInfo(listeners, bc, idx);
            }

            if (clazz == FrameworkListener.class)
            {
                m_fwkListeners = listeners;
            }
            else if (clazz == BundleListener.class)
            {
                if (SynchronousBundleListener.class.isInstance(l))
                {
                    m_syncBndlListeners = listeners;
                }
                else
                {
                    m_bndlListeners = listeners;
                }
            }
            else if (clazz == ServiceListener.class)
            {
                m_svcListeners = listeners;
            }
        }

        // Return information about the listener; this is null
        // for everything but service listeners.
        return returnInfo;
    }

    public void removeListeners(BundleContext bc)
    {
        if (bc == null)
        {
            return;
        }

        synchronized (this)
        {
            // Remove all framework listeners associated with the specified bundle.
            m_fwkListeners = removeListenerInfos(m_fwkListeners, bc);

            // Remove all bundle listeners associated with the specified bundle.
            m_bndlListeners = removeListenerInfos(m_bndlListeners, bc);

            // Remove all synchronous bundle listeners associated with
            // the specified bundle.
            m_syncBndlListeners = removeListenerInfos(m_syncBndlListeners, bc);

            // Remove all service listeners associated with the specified bundle.
            m_svcListeners = removeListenerInfos(m_svcListeners, bc);
        }
    }

    public Filter updateListener(BundleContext bc, Class clazz, EventListener l, Filter filter)
    {
        if (clazz == ServiceListener.class)
        {
            synchronized (this)
            {
                // Verify that the bundle context is still valid.
                try
                {
                    bc.getBundle();
                }
                catch (IllegalStateException ex)
                {
                    // Bundle context is no longer valid, so just return.
                }

                // See if the service listener is already registered; if so then
                // update its filter per the spec.
                List<ListenerInfo> infos = m_svcListeners.get(bc);
                for (int i = 0; (infos != null) && (i < infos.size()); i++)
                {
                    ListenerInfo info = infos.get(i);
                    if (info.getBundleContext().equals(bc) &&
                        (info.getListenerClass() == clazz) &&
                        (info.getListener() == l))
                    {
                        // The spec says to update the filter in this case.
                        Filter oldFilter = info.getParsedFilter();
                        ListenerInfo newInfo = new ListenerInfo(
                            info.getBundle(),
                            info.getBundleContext(),
                            info.getListenerClass(),
                            info.getListener(),
                            filter,
                            info.getSecurityContext(),
                            info.isRemoved());
                        m_svcListeners = updateListenerInfo(m_svcListeners, i, newInfo);
                        return oldFilter;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Returns all existing service listener information into a collection of
     * ListenerHook.ListenerInfo objects. This is used the first time a listener
     * hook is registered to synchronize it with the existing set of listeners.
     * @return Returns all existing service listener information into a collection of
     *         ListenerHook.ListenerInfo objects
    **/
    public Collection<ListenerHook.ListenerInfo> getAllServiceListeners()
    {
        List<ListenerHook.ListenerInfo> listeners = new ArrayList<ListenerHook.ListenerInfo>();
        synchronized (this)
        {
            for (Entry<BundleContext, List<ListenerInfo>> entry : m_svcListeners.entrySet())
            {
                listeners.addAll(entry.getValue());
            }
        }
        return listeners;
    }

    public void fireFrameworkEvent(FrameworkEvent event)
    {
        // Take a snapshot of the listener array.
        Map<BundleContext, List<ListenerInfo>> listeners = null;
        synchronized (this)
        {
            listeners = m_fwkListeners;
        }

        // Fire all framework listeners on a separate thread.
        fireEventAsynchronously(this, Request.FRAMEWORK_EVENT, listeners, event);
    }

    public void fireBundleEvent(BundleEvent event, Framework felix)
    {
        // Take a snapshot of the listener array.
        Map<BundleContext, List<ListenerInfo>> listeners = null;
        Map<BundleContext, List<ListenerInfo>> syncListeners = null;
        synchronized (this)
        {
            listeners = m_bndlListeners;
            syncListeners = m_syncBndlListeners;
        }

        // Create a whitelist of bundle context for bundle listeners,
        // if we have hooks.
        Set<BundleContext> whitelist = createWhitelistFromHooks(event, felix,
            listeners, syncListeners, org.osgi.framework.hooks.bundle.EventHook.class);

        // If we have a whitelist, then create copies of only the whitelisted
        // listeners.
        if (whitelist != null)
        {
            Map<BundleContext, List<ListenerInfo>> copy =
                new HashMap<BundleContext, List<ListenerInfo>>();
            for (BundleContext bc : whitelist)
            {
                List<ListenerInfo> infos = listeners.get(bc);
                if (infos != null)
                {
                    copy.put(bc, infos);
                }
            }
            listeners = copy;
            copy = new HashMap<BundleContext, List<ListenerInfo>>();
            for (BundleContext bc : whitelist)
            {
                List<ListenerInfo> infos = syncListeners.get(bc);
                if (infos != null)
                {
                    copy.put(bc, infos);
                }
            }
            syncListeners = copy;
        }

        // Fire synchronous bundle listeners immediately on the calling thread.
        fireEventImmediately(
            this, Request.BUNDLE_EVENT, syncListeners, event, null);

        // The spec says that asynchronous bundle listeners do not get events
        // of types STARTING, STOPPING, or LAZY_ACTIVATION.
        if ((event.getType() != BundleEvent.STARTING) &&
            (event.getType() != BundleEvent.STOPPING) &&
            (event.getType() != BundleEvent.LAZY_ACTIVATION))
        {
            // Fire asynchronous bundle listeners on a separate thread.
            fireEventAsynchronously(
                this, Request.BUNDLE_EVENT, listeners, event);
        }
    }

    public void fireServiceEvent(
        final ServiceEvent event, final Dictionary oldProps, final Framework felix)
    {
        // Take a snapshot of the listener array.
        Map<BundleContext, List<ListenerInfo>> listeners = null;
        synchronized (this)
        {
            listeners = m_svcListeners;
        }

        // Use service registry hooks to filter target listeners.
        listeners = filterListenersUsingHooks(event, felix, listeners);

        // Fire all service events immediately on the calling thread.
        fireEventImmediately(
            this, Request.SERVICE_EVENT, listeners, event, oldProps);
    }

// TODO: OSGi R4.3 - This is ugly and inefficient.
    private Map<BundleContext, List<ListenerInfo>> filterListenersUsingHooks(
        ServiceEvent event, Framework felix, Map<BundleContext, List<ListenerInfo>> listeners)
    {
        Set<ServiceReference<org.osgi.framework.hooks.service.EventHook>> ehs =
            m_registry.getHookRegistry().getHooks(org.osgi.framework.hooks.service.EventHook.class);
        if (!ehs.isEmpty())
        {
            // Create a whitelist of bundle context for bundle listeners,
            // if we have hooks.
            Set<BundleContext> whitelist = createWhitelistFromHooks(event, felix,
                listeners, null, org.osgi.framework.hooks.service.EventHook.class);

            // If we have a whitelist, then create copies of only the whitelisted
            // listeners.
            if (whitelist != null)
            {
                Map<BundleContext, List<ListenerInfo>> copy =
                    new HashMap<BundleContext, List<ListenerInfo>>();
                for (BundleContext bc : whitelist)
                {
                    copy.put(bc, listeners.get(bc));
                }
                listeners = copy;
            }
        }

        Set<ServiceReference<org.osgi.framework.hooks.service.EventListenerHook>> elhs =
            m_registry.getHookRegistry().getHooks(org.osgi.framework.hooks.service.EventListenerHook.class);
        if (!elhs.isEmpty())
        {
            List<ListenerInfo> systemBundleListeners = null;

            // The mutable map is used to keep the lists that underpin the shrinkable collections.
            Map<BundleContext, List<ListenerInfo>> mutableMap =
                    new HashMap<BundleContext, List<ListenerInfo>>();
            // Create map with shrinkable collections.
            Map<BundleContext, Collection<ListenerHook.ListenerInfo>> shrinkableMap =
                new HashMap<BundleContext, Collection<ListenerHook.ListenerInfo>>();
            for (Entry<BundleContext, List<ListenerInfo>> entry : listeners.entrySet())
            {
                BundleContext bc = entry.getKey();
                ArrayList<ListenerInfo> mutableList = new ArrayList<ListenerInfo>(entry.getValue());
                mutableMap.put(bc, mutableList);

                // We want to pass the list as a generic collection to Shrinkable Collection.
                // Need to convert to raw type before we can do this...
                ArrayList ml = mutableList;

                Collection<ListenerHook.ListenerInfo> shrinkableCollection =
                        new ShrinkableCollection<ListenerHook.ListenerInfo>(ml);
                shrinkableMap.put(bc, shrinkableCollection);

                // Keep a copy of the System Bundle Listeners, as they might be removed by the hooks, but we
                // actually need to keep them in the end.
                if (bc == felix.getBundleContext())
                    systemBundleListeners = new ArrayList<ListenerInfo>(entry.getValue());
            }
            shrinkableMap =
                new ShrinkableMap<BundleContext, Collection<ListenerHook.ListenerInfo>>
                    (shrinkableMap);

            for (ServiceReference<org.osgi.framework.hooks.service.EventListenerHook> sr : elhs)
            {
                if (felix != null)
                {
                    org.osgi.framework.hooks.service.EventListenerHook elh = null;
                    try
                    {
                        elh = m_registry.getService(felix, sr, false);
                    }
                    catch (Exception ex)
                    {
                        // If we can't get the hook, then ignore it.
                    }
                    if (elh != null)
                    {
                        try
                        {
                            m_secureAction.invokeServiceEventListenerHook(
                                elh, event, shrinkableMap);
                        }
                        catch (Throwable th)
                        {
                            m_logger.log(sr, Logger.LOG_WARNING,
                                "Problem invoking event hook", th);
                        }
                        finally
                        {
                            m_registry.ungetService(felix, sr, null);
                        }
                    }
                }
            }

// TODO: OSGi R4.3 - Should check and only do this if there was a change.

            // the listeners map should only contain List values, and not Shrinkable
            // Collections. Therefore we create a new map from the lists that are
            // the delegates for the Shrinkable Collections. Any changes made by the
            // hooks will have propagated to these maps.
            Map<BundleContext, List<ListenerInfo>> newMap =
                new HashMap<BundleContext, List<ListenerInfo>>();
            for (Map.Entry<BundleContext, Collection<ListenerHook.ListenerInfo>> entry : shrinkableMap.entrySet())
            {
                if (!entry.getValue().isEmpty())
                {
                    newMap.put(entry.getKey(), mutableMap.get(entry.getKey()));
                }
            }

            // Put the system bundle listeners back, because they really need to be called
            // regardless whether they were removed by the hooks or not.
            if (systemBundleListeners != null)
                newMap.put(felix.getBundleContext(), systemBundleListeners);

            listeners = newMap;
        }

        return listeners;
    }

    private <T> Set<BundleContext> createWhitelistFromHooks(
        EventObject event, Framework felix,
        Map<BundleContext, List<ListenerInfo>> listeners1,
        Map<BundleContext, List<ListenerInfo>> listeners2,
        Class<T> hookClass)
    {
        // Create a whitelist of bundle context, if we have hooks.
        Set<BundleContext> whitelist = null;
        Set<ServiceReference<T>> hooks = m_registry.getHookRegistry().getHooks(hookClass);
        if (!hooks.isEmpty())
        {
            boolean systemBundleListener = false;
            BundleContext systemBundleContext = felix.getBundleContext();

            whitelist = new HashSet<BundleContext>();
            for (Entry<BundleContext, List<ListenerInfo>> entry : listeners1.entrySet())
            {
                whitelist.add(entry.getKey());
                if (entry.getKey() == systemBundleContext)
                    systemBundleListener = true;
            }
            if (listeners2 != null)
            {
                for (Entry<BundleContext, List<ListenerInfo>> entry : listeners2.entrySet())
                {
                    whitelist.add(entry.getKey());
                    if (entry.getKey() == systemBundleContext)
                        systemBundleListener = true;
                }
            }

            int originalSize = whitelist.size();
            ShrinkableCollection<BundleContext> shrinkable =
                new ShrinkableCollection<BundleContext>(whitelist);
            for (ServiceReference<T> sr : hooks)
            {
                if (felix != null)
                {
                    T eh = null;
                    try
                    {
                        eh = m_registry.getService(felix, sr, false);
                    }
                    catch (Exception ex)
                    {
                        // If we can't get the hook, then ignore it.
                    }
                    if (eh != null)
                    {
                        try
                        {
                            if (eh instanceof org.osgi.framework.hooks.service.EventHook)
                            {
                                m_secureAction.invokeServiceEventHook(
                                    (org.osgi.framework.hooks.service.EventHook) eh,
                                    (ServiceEvent) event, shrinkable);
                            }
                            else if (eh instanceof org.osgi.framework.hooks.bundle.EventHook)
                            {
                                m_secureAction.invokeBundleEventHook(
                                    (org.osgi.framework.hooks.bundle.EventHook) eh,
                                    (BundleEvent) event, shrinkable);
                            }
                        }
                        catch (Throwable th)
                        {
                            m_logger.log(sr, Logger.LOG_WARNING,
                                "Problem invoking event hook", th);
                        }
                        finally
                        {
                            m_registry.ungetService(felix, sr, null);
                        }
                    }
                }
            }

            if (systemBundleListener && !whitelist.contains(systemBundleContext))
            {
                // The system bundle cannot be removed from the listeners, so if it was
                // removed, add it back in. Note that this cannot be prevented by the shrinkable
                // since the effect of removing the system bundle from the listeners must be
                // visible between the event hooks.
                whitelist.add(systemBundleContext);
            }

            // If the whitelist hasn't changed, then null it to avoid having
            // to do whitelist lookups during event delivery.
            if (originalSize == whitelist.size())
            {
                whitelist = null;
            }
        }
        return whitelist;
    }

    private static void fireEventAsynchronously(
        EventDispatcher dispatcher, int type,
        Map<BundleContext, List<ListenerInfo>> listeners,
        EventObject event)
    {
        //TODO: should possibly check this within thread lock, seems to be ok though without
        // If dispatch thread is stopped, then ignore dispatch request.
        if (m_stopping || m_thread == null)
        {
            return;
        }

        // First get a request from the pool or create one if necessary.
        Request req = null;
        synchronized (m_requestPool)
        {
            if (m_requestPool.size() > 0)
            {
                req = m_requestPool.remove(0);
            }
            else
            {
                req = new Request();
            }
        }

        // Initialize dispatch request.
        req.m_dispatcher = dispatcher;
        req.m_type = type;
        req.m_listeners = listeners;
        req.m_event = event;

        // Lock the request list.
        synchronized (m_requestList)
        {
            // Add our request to the list.
            m_requestList.add(req);
            // Notify the dispatch thread that there is work to do.
            m_requestList.notify();
        }
    }

    private static void fireEventImmediately(
        EventDispatcher dispatcher, int type,
        Map<BundleContext, List<ListenerInfo>> listeners,
        EventObject event, Dictionary oldProps)
    {
        if (!listeners.isEmpty())
        {
            // Notify appropriate listeners.
            for (Entry<BundleContext, List<ListenerInfo>> entry : listeners.entrySet())
            {
                for (ListenerInfo info : entry.getValue())
                {
                    Bundle bundle = info.getBundle();
                    EventListener l = info.getListener();
                    Filter filter = info.getParsedFilter();
                    Object acc = info.getSecurityContext();

                    try
                    {
                        if (type == Request.FRAMEWORK_EVENT)
                        {
                            invokeFrameworkListenerCallback(bundle, l, event);
                        }
                        else if (type == Request.BUNDLE_EVENT)
                        {
                            invokeBundleListenerCallback(bundle, l, event);
                        }
                        else if (type == Request.SERVICE_EVENT)
                        {
                            invokeServiceListenerCallback(
                                bundle, l, filter, acc, event, oldProps);
                        }
                    }
                    catch (Throwable th)
                    {
                        if ((type != Request.FRAMEWORK_EVENT)
                            || (((FrameworkEvent) event).getType() != FrameworkEvent.ERROR))
                        {
                            dispatcher.m_logger.log(bundle,
                                Logger.LOG_ERROR,
                                "EventDispatcher: Error during dispatch.", th);
                            dispatcher.fireFrameworkEvent(
                                new FrameworkEvent(FrameworkEvent.ERROR, bundle, th));
                        }
                    }
                }
            }
        }
    }

    private static void invokeFrameworkListenerCallback(
        Bundle bundle, final EventListener l, final EventObject event)
    {
        // The spec says only active bundles receive asynchronous events,
        // but we will include starting bundles too otherwise
        // it is impossible to see everything.
        if ((bundle.getState() == Bundle.STARTING) ||
            (bundle.getState() == Bundle.ACTIVE))
        {
            if (System.getSecurityManager() != null)
            {
                AccessController.doPrivileged(new PrivilegedAction() {
                    @Override
                    public Object run()
                    {
                        ((FrameworkListener) l).frameworkEvent((FrameworkEvent) event);
                        return null;
                    }
                });
            }
            else
            {
                ((FrameworkListener) l).frameworkEvent((FrameworkEvent) event);
            }
        }
    }

    private static void invokeBundleListenerCallback(
        Bundle bundle, final EventListener l, final EventObject event)
    {
        // A bundle listener is either synchronous or asynchronous.
        // If the bundle listener is synchronous, then deliver the
        // event to bundles with a state of STARTING, STOPPING, or
        // ACTIVE. If the listener is asynchronous, then deliver the
        // event only to bundles that are STARTING or ACTIVE.
        if (((SynchronousBundleListener.class.isAssignableFrom(l.getClass())) &&
            ((bundle.getState() == Bundle.STARTING) ||
            (bundle.getState() == Bundle.STOPPING) ||
            (bundle.getState() == Bundle.ACTIVE)))
            ||
            ((bundle.getState() == Bundle.STARTING) ||
            (bundle.getState() == Bundle.ACTIVE)))
        {
            if (System.getSecurityManager() != null)
            {
                AccessController.doPrivileged(new PrivilegedAction() {
                    @Override
                    public Object run()
                    {
                        ((BundleListener) l).bundleChanged((BundleEvent) event);
                        return null;
                    }
                });
            }
            else
            {
                ((BundleListener) l).bundleChanged((BundleEvent) event);
            }
        }
    }

    private static void invokeServiceListenerCallback(
        Bundle bundle, final EventListener l, Filter filter, Object acc,
        final EventObject event, final Dictionary oldProps)
    {
        // Service events should be delivered to STARTING,
        // STOPPING, and ACTIVE bundles.
        if ((bundle.getState() != Bundle.STARTING) &&
            (bundle.getState() != Bundle.STOPPING) &&
            (bundle.getState() != Bundle.ACTIVE))
        {
            return;
        }

        // Check that the bundle has permission to get at least
        // one of the service interfaces; the objectClass property
        // of the service stores its service interfaces.
        ServiceReference ref = ((ServiceEvent) event).getServiceReference();

        boolean hasPermission = true;
        Object sm = System.getSecurityManager();
        if ((acc != null) && (sm != null))
        {
            try
            {
                ServicePermission perm =
                    new ServicePermission(
                        ref, ServicePermission.GET);
                ((SecurityManager) sm).checkPermission(perm, acc);
            }
            catch (Exception ex)
            {
                hasPermission = false;
            }
        }

        if (hasPermission)
        {
            // Dispatch according to the filter.
            boolean matched;
            if (l instanceof UnfilteredServiceListener)
            {
                // An UnfilteredServiceListener always matches, regardless of the filter.
                // The filter is still passed on to the Service Registry Hooks.
                matched = true;
            }
            else
            {
                matched = (filter == null)
                        || filter.match(((ServiceEvent) event).getServiceReference());
            }

            if (matched)
            {
                if ((l instanceof AllServiceListener) ||
                    Util.isServiceAssignable(bundle, ((ServiceEvent) event).getServiceReference()))
                {
                    if (System.getSecurityManager() != null)
                    {
                        AccessController.doPrivileged(new PrivilegedAction()
                        {
                            @Override
                            public Object run()
                            {
                                ((ServiceListener) l).serviceChanged((ServiceEvent) event);
                                return null;
                            }
                        });
                    }
                    else
                    {
                        ((ServiceListener) l).serviceChanged((ServiceEvent) event);
                    }
                }
            }
            // We need to send an MODIFIED_ENDMATCH event if the listener
            // matched previously.
            else if (((ServiceEvent) event).getType() == ServiceEvent.MODIFIED)
            {
                if (filter.match(oldProps))
                {
                    final ServiceEvent se = new ServiceEvent(
                        ServiceEvent.MODIFIED_ENDMATCH,
                        ((ServiceEvent) event).getServiceReference());
                    if (System.getSecurityManager() != null)
                    {
                        AccessController.doPrivileged(new PrivilegedAction()
                        {
                            @Override
                            public Object run()
                            {
                                ((ServiceListener) l).serviceChanged(se);
                                return null;
                            }
                        });
                    }
                    else
                    {
                        ((ServiceListener) l).serviceChanged(se);
                    }
                }
            }
        }
    }

    private static Map<BundleContext, List<ListenerInfo>> addListenerInfo(
        Map<BundleContext, List<ListenerInfo>> listeners, ListenerInfo info)
    {
        // Make a copy of the map, since we will be mutating it.
        Map<BundleContext, List<ListenerInfo>> copy =
            new HashMap<BundleContext, List<ListenerInfo>>(listeners);
        // Remove the affected entry and make a copy so we can modify it.
        List<ListenerInfo> infos = copy.remove(info.getBundleContext());
        if (infos == null)
        {
            infos = new ArrayList<ListenerInfo>();
        }
        else
        {
            infos = new ArrayList<ListenerInfo>(infos);
        }
        // Add the new listener info.
        infos.add(info);
        // Put the listeners back into the copy of the map and return it.
        copy.put(info.getBundleContext(), infos);
        return copy;
    }

    private static Map<BundleContext, List<ListenerInfo>> updateListenerInfo(
        Map<BundleContext, List<ListenerInfo>> listeners, int idx,
        ListenerInfo info)
    {
        // Make a copy of the map, since we will be mutating it.
        Map<BundleContext, List<ListenerInfo>> copy =
            new HashMap<BundleContext, List<ListenerInfo>>(listeners);
        // Remove the affected entry and make a copy so we can modify it.
        List<ListenerInfo> infos = copy.remove(info.getBundleContext());
        if (infos != null)
        {
            infos = new ArrayList<ListenerInfo>(infos);
            // Update the new listener info.
            infos.set(idx, info);
            // Put the listeners back into the copy of the map and return it.
            copy.put(info.getBundleContext(), infos);
            return copy;
        }
        return listeners;
    }

    private static Map<BundleContext, List<ListenerInfo>> removeListenerInfo(
        Map<BundleContext, List<ListenerInfo>> listeners, BundleContext bc, int idx)
    {
        // Make a copy of the map, since we will be mutating it.
        Map<BundleContext, List<ListenerInfo>> copy =
            new HashMap<BundleContext, List<ListenerInfo>>(listeners);
        // Remove the affected entry and make a copy so we can modify it.
        List<ListenerInfo> infos = copy.remove(bc);
        if (infos != null)
        {
            infos = new ArrayList<ListenerInfo>(infos);
            // Remove the listener info.
            infos.remove(idx);
            if (!infos.isEmpty())
            {
                // Put the listeners back into the copy of the map and return it.
                copy.put(bc, infos);
            }
            return copy;
        }
        return listeners;
    }

    private static Map<BundleContext, List<ListenerInfo>> removeListenerInfos(
        Map<BundleContext, List<ListenerInfo>> listeners, BundleContext bc)
    {
        // Make a copy of the map, since we will be mutating it.
        Map<BundleContext, List<ListenerInfo>> copy =
            new HashMap<BundleContext, List<ListenerInfo>>(listeners);
        // Remove the affected entry and return the copy.
        copy.remove(bc);
        return copy;
    }

    /**
     * This is the dispatching thread's main loop.
    **/
    private static void run()
    {
        Request req = null;
        while (true)
        {
            // Lock the request list so we can try to get a
            // dispatch request from it.
            synchronized (m_requestList)
            {
                // Wait while there are no requests to dispatch. If the
                // dispatcher thread is supposed to stop, then let the
                // dispatcher thread exit the loop and stop.
                while (m_requestList.isEmpty() && !m_stopping)
                {
                    // Wait until some signals us for work.
                    try
                    {
                        m_requestList.wait();
                    }
                    catch (InterruptedException ex)
                    {
                        // Not much we can do here except for keep waiting.
                    }
                }

                // If there are no events to dispatch and shutdown
                // has been called then exit, otherwise dispatch event.
                if (m_requestList.isEmpty() && m_stopping)
                {
                    return;
                }

                // Get the dispatch request.
                req = m_requestList.remove(0);
            }

            // Deliver event outside of synchronized block
            // so that we don't block other requests from being
            // queued during event processing.
            // NOTE: We don't catch any exceptions here, because
            // the invoked method shields us from exceptions by
            // catching Throwables when it invokes callbacks.
            fireEventImmediately(
                req.m_dispatcher, req.m_type, req.m_listeners,
                req.m_event, null);

            // Put dispatch request in cache.
            synchronized (m_requestPool)
            {
                req.m_dispatcher = null;
                req.m_type = -1;
                req.m_listeners = null;
                req.m_event = null;
                m_requestPool.add(req);
            }
        }
    }

    private static class Request
    {
        public static final int FRAMEWORK_EVENT = 0;
        public static final int BUNDLE_EVENT = 1;
        public static final int SERVICE_EVENT = 2;

        public EventDispatcher m_dispatcher = null;
        public int m_type = -1;
        public Map<BundleContext, List<ListenerInfo>> m_listeners = null;
        public EventObject m_event = null;
    }
}
