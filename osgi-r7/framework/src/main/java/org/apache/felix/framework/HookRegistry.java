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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;

/**
 * This registry holds all services implementing one of the hook services
 */
public class HookRegistry
{
    /** no need to use a sync'ed structure as this is read only. */
    private final static Map<String, Class<?>> HOOK_CLASSES = new HashMap<String, Class<?>>();

    static
    {
        addHookClass(org.osgi.framework.hooks.bundle.CollisionHook.class);
        addHookClass(org.osgi.framework.hooks.bundle.FindHook.class);
        addHookClass(org.osgi.framework.hooks.bundle.EventHook.class);
        addHookClass(org.osgi.framework.hooks.service.EventHook.class);
        addHookClass(org.osgi.framework.hooks.service.EventListenerHook.class);
        addHookClass(org.osgi.framework.hooks.service.FindHook.class);
        addHookClass(org.osgi.framework.hooks.service.ListenerHook.class);
        addHookClass(org.osgi.framework.hooks.weaving.WeavingHook.class);
        addHookClass(org.osgi.framework.hooks.weaving.WovenClassListener.class);
        addHookClass(org.osgi.framework.hooks.resolver.ResolverHookFactory.class);
        addHookClass(org.osgi.service.url.URLStreamHandlerService.class);
        addHookClass(java.net.ContentHandler.class);
    };

    private static void addHookClass(final Class<?> c) {
        HOOK_CLASSES.put(c.getName(), c);
    }

    private final Map<String, SortedSet<ServiceReference<?>>> m_allHooks =
        new ConcurrentHashMap<String, SortedSet<ServiceReference<?>>>();

    private final WeakHashMap<ServiceReference<?>, ServiceReference<?>> m_blackList =
            new WeakHashMap<ServiceReference<?>, ServiceReference<?>>();


    static boolean isHook(final String[] classNames, final Class<?> hookClass, final Object svcObj)
    {
        for (final String serviceName : classNames)
        {
            if (serviceName.equals(hookClass.getName()))
            {
                // For a service factory, we can only match names.
                if (svcObj instanceof ServiceFactory)
                {
                    return true;
                }
                // For a service object, check if its class matches.
                if (hookClass.isAssignableFrom(svcObj.getClass()))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isHook(final String serviceName, final Object svcObj)
    {
        final Class<?> hookClass = HOOK_CLASSES.get(serviceName);
        if ( hookClass != null )
        {
            // For a service factory, we can only match names.
            if (svcObj instanceof ServiceFactory)
            {
                return true;
            }
            // For a service object, check if its class matches.
            if (hookClass.isAssignableFrom(svcObj.getClass()))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Check and add the service to the set of hooks
     * @param classNames The service names
     * @param svcObj The service object
     * @param ref The service reference
     */
    public void addHooks(final String[] classNames, final Object svcObj, final ServiceReference<?> ref)
    {
        for(final String serviceName : classNames)
        {
            if (isHook(serviceName, svcObj))
            {
                synchronized (m_allHooks) // we need to sync as we replace the value
                {
                    SortedSet<ServiceReference<?>> hooks = m_allHooks.get(serviceName);
                    if (hooks == null)
                    {
                        hooks = new TreeSet<ServiceReference<?>>(Collections.reverseOrder());
                    }
                    else
                    {
                        hooks = new TreeSet<ServiceReference<?>>(hooks);
                    }
                    hooks.add(ref);
                    m_allHooks.put(serviceName, hooks);
                }
            }
        }
    }

    /**
     * Update the service ranking for a hook
     * @param ref The service reference
     */
    public void updateHooks(final ServiceReference<?> ref)
    {
        // We maintain the hooks sorted, so if ranking has changed for example,
        // we need to ensure the order remains correct by resorting the hooks.
        final Object svcObj = ((ServiceRegistrationImpl.ServiceReferenceImpl) ref)
                .getRegistration().getService();
        final String [] classNames = (String[]) ref.getProperty(Constants.OBJECTCLASS);

        for(final String serviceName : classNames)
        {
            if (isHook(serviceName, svcObj))
            {
                synchronized (m_allHooks) // we need to sync as we replace the value
                {
                    SortedSet<ServiceReference<?>> hooks = m_allHooks.get(serviceName);
                    if (hooks != null)
                    {
                        TreeSet<ServiceReference<?>> newHooks = new TreeSet<ServiceReference<?>>(Collections.reverseOrder());
                        for (ServiceReference<?> hook : hooks) {
                            newHooks.add(hook); // copy constructor / addAll() does not re-sort
                        }

                        m_allHooks.put(serviceName, newHooks);
                    }
                }
            }
        }
    }

    /**
     * Remove the service hooks
     * @param ref The service reference
     */
    public void removeHooks(final ServiceReference<?> ref)
    {
        final Object svcObj = ((ServiceRegistrationImpl.ServiceReferenceImpl) ref)
            .getRegistration().getService();
        final String [] classNames = (String[]) ref.getProperty(Constants.OBJECTCLASS);

        for(final String serviceName : classNames)
        {
            if (isHook(serviceName, svcObj))
            {
                synchronized (m_allHooks) // we need to sync as we replace the value
                {
                    SortedSet<ServiceReference<?>> hooks = m_allHooks.get(serviceName);
                    if (hooks != null)
                    {
                        hooks = new TreeSet<ServiceReference<?>>(hooks);
                        hooks.remove(ref);
                        m_allHooks.put(serviceName, hooks);
                    }
                }
            }
        }
        synchronized ( m_blackList )
        {
            m_blackList.remove(ref);
        }
    }

    /**
     * Return the sorted set of hooks
     * @param hookClass The hook class
     * @return The sorted set - the set might be empty
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <S> Set<ServiceReference<S>> getHooks(final Class<S> hookClass)
    {
        final Set<ServiceReference<?>> hooks = m_allHooks.get(hookClass.getName());
        if (hooks != null)
        {
            return (Set)hooks;
        }
        return Collections.emptySet();
    }

    public boolean isHookBlackListed(final ServiceReference<?> sr)
    {
        synchronized ( m_blackList )
        {
            return m_blackList.containsKey(sr);
        }
    }

    public void blackListHook(final ServiceReference<?> sr)
    {
        synchronized ( m_blackList )
        {
            m_blackList.put(sr, sr);
        }
    }
}
