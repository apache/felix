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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
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
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.framework.launch.Framework;

public class EventDispatcher
{
    static final int LISTENER_BUNDLE_OFFSET = 0;
    static final int LISTENER_CLASS_OFFSET = 1;
    static final int LISTENER_OBJECT_OFFSET = 2;
    static final int LISTENER_FILTER_OFFSET = 3;
    static final int LISTENER_SECURITY_OFFSET = 4;
    static final int LISTENER_ARRAY_SIZE = 5;

    private final Logger m_logger;
    private final ServiceRegistry m_registry;

    private List<Object[]> m_frameworkListeners = Collections.EMPTY_LIST;
    private List<Object[]> m_bundleListeners = Collections.EMPTY_LIST;
    private List<Object[]> m_syncBundleListeners = Collections.EMPTY_LIST;
    private List<Object[]> m_serviceListeners = Collections.EMPTY_LIST;

    // A single thread is used to deliver events for all dispatchers.
    private static Thread m_thread = null;
    private final static String m_threadLock = new String("thread lock");
    private static int m_references = 0;
    private static volatile boolean m_stopping = false;

    // List of requests.
    private static final List<Request> m_requestList = new ArrayList<Request>();
    // Pooled requests to avoid memory allocation.
    private static final List<Request> m_requestPool = new ArrayList<Request>();

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

    public Filter addListener(Bundle bundle, Class clazz, EventListener l, Filter filter)
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
        Filter oldFilter = updateListener(bundle, clazz, l, filter);
        if (oldFilter != null)
        {
            return oldFilter;
        }

        // Lock the object to add the listener.
        synchronized (this)
        {
            List<Object[]> listeners = null;
            Object acc = null;

            if (clazz == FrameworkListener.class)
            {
                listeners = m_frameworkListeners;
            }
            else if (clazz == BundleListener.class)
            {
                if (SynchronousBundleListener.class.isInstance(l))
                {
                    listeners = m_syncBundleListeners;
                }
                else
                {
                    listeners = m_bundleListeners;
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
                listeners = m_serviceListeners;
            }
            else
            {
                throw new IllegalArgumentException("Unknown listener: " + l.getClass());
            }

            // Add listener.
            Object[] listener = new Object[LISTENER_ARRAY_SIZE];
            listener[LISTENER_BUNDLE_OFFSET] = bundle;
            listener[LISTENER_CLASS_OFFSET] = clazz;
            listener[LISTENER_OBJECT_OFFSET] = l;
            listener[LISTENER_FILTER_OFFSET] = filter;
            listener[LISTENER_SECURITY_OFFSET] = acc;

            List<Object[]> newListeners = new ArrayList<Object[]>(listeners.size() + 1);
            newListeners.addAll(listeners);
            newListeners.add(listener);
            listeners = newListeners;

            if (clazz == FrameworkListener.class)
            {
                m_frameworkListeners = listeners;
            }
            else if (clazz == BundleListener.class)
            {
                if (SynchronousBundleListener.class.isInstance(l))
                {
                    m_syncBundleListeners = listeners;
                }
                else
                {
                    m_bundleListeners = listeners;
                }
            }
            else if (clazz == ServiceListener.class)
            {
                m_serviceListeners = listeners;
            }
        }
        return null;
    }

    public ListenerHook.ListenerInfo removeListener(
        Bundle bundle, Class clazz, EventListener l)
    {
        ListenerHook.ListenerInfo listenerInfo = null;

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
            List<Object[]> listeners = null;

            if (clazz == FrameworkListener.class)
            {
                listeners = m_frameworkListeners;
            }
            else if (clazz == BundleListener.class)
            {
                if (SynchronousBundleListener.class.isInstance(l))
                {
                    listeners = m_syncBundleListeners;
                }
                else
                {
                    listeners = m_bundleListeners;
                }
            }
            else if (clazz == ServiceListener.class)
            {
                listeners = m_serviceListeners;
            }
            else
            {
                throw new IllegalArgumentException("Unknown listener: " + l.getClass());
            }

            // Try to find the instance in our list.
            int idx = -1;
            for (int i = 0; i < listeners.size(); i++)
            {
                Object[] listener = listeners.get(i);
                if (listener[LISTENER_BUNDLE_OFFSET].equals(bundle) &&
                    (listener[LISTENER_CLASS_OFFSET] == clazz) &&
                    (listener[LISTENER_OBJECT_OFFSET] == l))
                {
                    // For service listeners, we must return some info about
                    // the listener for the ListenerHook callback.
                    if (ServiceListener.class == clazz)
                    {
                        listenerInfo = wrapListener(listeners.get(i), true);
                    }
                    idx = i;
                    break;
                }
            }

            // If we have the instance, then remove it.
            if (idx >= 0)
            {
                // If this is the last listener, then point to empty list.
                if (listeners.size() == 1)
                {
                    listeners = Collections.EMPTY_LIST;
                }
                // Otherwise, we need to do some array copying.
                // Notice, the old array is always valid, so if
                // the dispatch thread is in the middle of a dispatch,
                // then it has a reference to the old listener array
                // and is not affected by the new value.
                else
                {
                    List<Object[]> newListeners = new ArrayList<Object[]>(listeners);
                    newListeners.remove(idx);
                    listeners = newListeners;
                }
            }

            if (clazz == FrameworkListener.class)
            {
                m_frameworkListeners = listeners;
            }
            else if (clazz == BundleListener.class)
            {
                if (SynchronousBundleListener.class.isInstance(l))
                {
                    m_syncBundleListeners = listeners;
                }
                else
                {
                    m_bundleListeners = listeners;
                }
            }
            else if (clazz == ServiceListener.class)
            {
                m_serviceListeners = listeners;
            }
        }

        // Return information about the listener; this is null
        // for everything but service listeners.
        return listenerInfo;
    }

    public void removeListeners(Bundle bundle)
    {
        if (bundle == null)
        {
            return;
        }

        synchronized (this)
        {
            // Remove all framework listeners associated with the specified bundle.
            List<Object[]> newListeners = new ArrayList<Object[]>(m_frameworkListeners);
            for (Iterator<Object[]> it = newListeners.iterator(); it.hasNext(); )
            {
                // Check if the bundle associated with the current listener
                // is the same as the specified bundle, if so remove the listener.
                Object[] listener = it.next();
                Bundle registeredBundle = (Bundle) listener[LISTENER_BUNDLE_OFFSET];
                if (bundle.equals(registeredBundle))
                {
                    it.remove();
                }
            }
            m_frameworkListeners = newListeners;

            // Remove all bundle listeners associated with the specified bundle.
            newListeners = new ArrayList<Object[]>(m_bundleListeners);
            for (Iterator<Object[]> it = newListeners.iterator(); it.hasNext(); )
            {
                // Check if the bundle associated with the current listener
                // is the same as the specified bundle, if so remove the listener.
                Object[] listener = it.next();
                Bundle registeredBundle = (Bundle) listener[LISTENER_BUNDLE_OFFSET];
                if (bundle.equals(registeredBundle))
                {
                    it.remove();
                }
            }
            m_bundleListeners = newListeners;

            // Remove all synchronous bundle listeners associated with
            // the specified bundle.
            newListeners = new ArrayList<Object[]>(m_syncBundleListeners);
            for (Iterator<Object[]> it = newListeners.iterator(); it.hasNext(); )
            {
                // Check if the bundle associated with the current listener
                // is the same as the specified bundle, if so remove the listener.
                Object[] listener = it.next();
                Bundle registeredBundle = (Bundle) listener[LISTENER_BUNDLE_OFFSET];
                if (bundle.equals(registeredBundle))
                {
                    it.remove();
                }
            }
            m_syncBundleListeners = newListeners;

            // Remove all service listeners associated with the specified bundle.
            newListeners = new ArrayList<Object[]>(m_serviceListeners);
            for (Iterator<Object[]> it = newListeners.iterator(); it.hasNext(); )
            {
                // Check if the bundle associated with the current listener
                // is the same as the specified bundle, if so remove the listener.
                Object[] listener = it.next();
                Bundle registeredBundle = (Bundle) listener[LISTENER_BUNDLE_OFFSET];
                if (bundle.equals(registeredBundle))
                {
                    it.remove();
                }
            }
            m_serviceListeners = newListeners;
        }
    }

    public Filter updateListener(Bundle bundle, Class clazz, EventListener l, Filter filter)
    {
        synchronized (this)
        {
            List<Object[]> listeners = null;

            if (clazz == FrameworkListener.class)
            {
                listeners = m_frameworkListeners;
            }
            else if (clazz == BundleListener.class)
            {
                if (SynchronousBundleListener.class.isInstance(l))
                {
                    listeners = m_syncBundleListeners;
                }
                else
                {
                    listeners = m_bundleListeners;
                }
            }
            else if (clazz == ServiceListener.class)
            {
                listeners = m_serviceListeners;
            }

            // See if the listener is already registered, if so then
            // handle it according to the spec.
            for (int i = 0; i < listeners.size(); i++)
            {
                Object[] listener = listeners.get(i);
                if (listener[LISTENER_BUNDLE_OFFSET].equals(bundle) &&
                    (listener[LISTENER_CLASS_OFFSET] == clazz) &&
                    (listener[LISTENER_OBJECT_OFFSET] == l))
                {
                    Filter oldFilter = null;
                    if (clazz == FrameworkListener.class)
                    {
                        // The spec says to ignore this case.
                    }
                    else if (clazz == BundleListener.class)
                    {
                        // The spec says to ignore this case.
                    }
                    else if (clazz == ServiceListener.class)
                    {
                        // The spec says to update the filter in this case.
                        oldFilter = (Filter) listener[LISTENER_FILTER_OFFSET];
                        listener[LISTENER_FILTER_OFFSET] = filter;
                    }
                    return oldFilter;
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
    public Collection /* <? extends ListenerHook.ListenerInfo> */
        wrapAllServiceListeners(boolean removed)
    {
        List<Object[]> listeners = null;
        synchronized (this)
        {
            listeners = m_serviceListeners;
        }

        List existingListeners = new ArrayList();
        for (int i = 0; i < listeners.size(); i++)
        {
            existingListeners.add(wrapListener(listeners.get(i), removed));
        }
        return existingListeners;
    }

    /**
     * Wraps the information about a given listener in a ListenerHook.ListenerInfo
     * object.
     * @param listeners The array of listeners.
     * @param offset The offset into the array of the listener to wrap.
     * @return A ListenerHook.ListenerInfo object for the specified listener.
     */
    private static ListenerHook.ListenerInfo wrapListener(Object[] listener, boolean removed)
    {
        Filter filter = (Filter) listener[LISTENER_FILTER_OFFSET];

        return new ListenerHookInfoImpl(
            ((Bundle)listener[LISTENER_BUNDLE_OFFSET]).getBundleContext(),
            (ServiceListener) listener[LISTENER_OBJECT_OFFSET],
            filter == null ? null : filter.toString(),
            removed);
    }

    public void fireFrameworkEvent(FrameworkEvent event)
    {
        // Take a snapshot of the listener array.
        List<Object[]> listeners = null;
        synchronized (this)
        {
            listeners = m_frameworkListeners;
        }

        // Fire all framework listeners on a separate thread.
        fireEventAsynchronously(this, Request.FRAMEWORK_EVENT, listeners, null, event);
    }

    public void fireBundleEvent(BundleEvent event, Framework felix)
    {
        // Take a snapshot of the listener array.
        List<Object[]> listeners = null;
        List<Object[]> syncListeners = null;
        synchronized (this)
        {
            listeners = m_bundleListeners;
            syncListeners = m_syncBundleListeners;
        }

        // Create a whitelist of bundle context for bundle listeners,
        // if we have hooks.
        List<Object[]> allListeners = new ArrayList(listeners);
        allListeners.addAll(syncListeners);
        Set<BundleContext> whitelist =
            createWhitelistFromHooks(
                event, felix, allListeners, org.osgi.framework.hooks.bundle.EventHook.class);

        // Fire synchronous bundle listeners immediately on the calling thread.
        fireEventImmediately(
            this, Request.BUNDLE_EVENT, syncListeners, whitelist, event, null);

        // The spec says that asynchronous bundle listeners do not get events
        // of types STARTING, STOPPING, or LAZY_ACTIVATION.
        if ((event.getType() != BundleEvent.STARTING) &&
            (event.getType() != BundleEvent.STOPPING) &&
            (event.getType() != BundleEvent.LAZY_ACTIVATION))
        {
            // Fire asynchronous bundle listeners on a separate thread.
            fireEventAsynchronously(
                this, Request.BUNDLE_EVENT, listeners, whitelist, event);
        }
    }

    public void fireServiceEvent(
        final ServiceEvent event, final Dictionary oldProps, final Framework felix)
    {
        // Take a snapshot of the listener array.
        List<Object[]> listeners = null;
        synchronized (this)
        {
            listeners = m_serviceListeners;
        }

        // Create a whitelist of bundle context, if we have hooks.
        Set<BundleContext> whitelist =
            createWhitelistFromHooks(
                event, felix, listeners, org.osgi.framework.hooks.service.EventHook.class);

        // Fire all service events immediately on the calling thread.
        fireEventImmediately(
            this, Request.SERVICE_EVENT, listeners, whitelist, event, oldProps);
    }


    private Set<BundleContext> createWhitelistFromHooks(
        EventObject event, Framework felix, List<Object[]> listeners, Class hookClass)
    {
        // Create a whitelist of bundle context, if we have hooks.
        Set<BundleContext> whitelist = null;
        Set<ServiceReference> hooks = m_registry.getHooks(hookClass);
        if ((hooks != null) && !hooks.isEmpty())
        {
            whitelist = new HashSet<BundleContext>();
            for (Object[] listener : listeners)
            {
                BundleContext bc =
                    ((Bundle) listener[LISTENER_BUNDLE_OFFSET]).getBundleContext();
                if (bc != null)
                {
                    whitelist.add(bc);
                }
            }
            int originalSize = whitelist.size();
            ShrinkableCollection<BundleContext> shrinkable =
                new ShrinkableCollection<BundleContext>(whitelist);
            for (ServiceReference sr : hooks)
            {
                if (felix != null)
                {
                    Object eh = null;
                    try
                    {
                        eh = m_registry.getService(felix, sr);
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
                                ((org.osgi.framework.hooks.service.EventHook)
                                    eh).event((ServiceEvent) event, shrinkable);
                            }
                            else if (eh instanceof org.osgi.framework.hooks.bundle.EventHook)
                            {
                                ((org.osgi.framework.hooks.bundle.EventHook)
                                    eh).event((BundleEvent) event, shrinkable);
                            }
                        }
                        catch (Throwable th)
                        {
                            m_logger.log(sr, Logger.LOG_WARNING,
                                "Problem invoking event hook", th);
                        }
                        finally
                        {
                            m_registry.ungetService(felix, sr);
                        }
                    }
                }
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
        EventDispatcher dispatcher, int type, List<Object[]> listeners,
        Set<BundleContext> whitelist, EventObject event)
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
        req.m_whitelist = whitelist;
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
        EventDispatcher dispatcher, int type, List<Object[]> listeners,
        Set<BundleContext> whitelist, EventObject event, Dictionary oldProps)
    {
        if (!listeners.isEmpty())
        {
            // Notify appropriate listeners.
            for (Object[] listener : listeners)
            {
                Bundle bundle = (Bundle) listener[LISTENER_BUNDLE_OFFSET];
                EventListener l = (EventListener) listener[LISTENER_OBJECT_OFFSET];
                Filter filter = (Filter) listener[LISTENER_FILTER_OFFSET];
                Object acc = listener[LISTENER_SECURITY_OFFSET];

                // Only deliver events to bundles in the whitelist, if we have one.
                if ((whitelist == null) || whitelist.contains(bundle.getBundleContext()))
                {
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
            boolean matched = (filter == null)
                || filter.match(((ServiceEvent) event).getServiceReference());

            if (matched)
            {
                if ((l instanceof AllServiceListener) ||
                    Util.isServiceAssignable(bundle, ((ServiceEvent) event).getServiceReference()))
                {
                    if (System.getSecurityManager() != null)
                    {
                        AccessController.doPrivileged(new PrivilegedAction()
                        {
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
                req.m_dispatcher, req.m_type, req.m_listeners, req.m_whitelist, req.m_event, null);

            // Put dispatch request in cache.
            synchronized (m_requestPool)
            {
                req.m_dispatcher = null;
                req.m_type = -1;
                req.m_listeners = null;
                req.m_whitelist = null;
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
        public List<Object[]> m_listeners = null;
        public Set<BundleContext> m_whitelist = null;
        public EventObject m_event = null;
    }
}