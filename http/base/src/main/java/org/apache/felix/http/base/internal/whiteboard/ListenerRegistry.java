/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.whiteboard;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.apache.felix.http.base.internal.runtime.ListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextListenerInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public final class ListenerRegistry
{
    private final Map<ServletContextHelperInfo, PerContextEventListener> registriesByContext = new TreeMap<ServletContextHelperInfo, PerContextEventListener>();

    private final Bundle bundle;

    public ListenerRegistry(final Bundle bundle)
    {
        this.bundle = bundle;
    }

    public void initialized(@Nonnull final ServletContextListenerInfo listenerInfo,
            ContextHandler contextHandler)
    {
        registriesByContext.get(contextHandler.getContextInfo()).initialized(listenerInfo, contextHandler);
    }

    public void destroyed(@Nonnull final ServletContextListenerInfo listenerInfo,
            ContextHandler contextHandler)
    {
        registriesByContext.get(contextHandler.getContextInfo()).destroyed(listenerInfo, contextHandler);
    }

    public <T extends ListenerInfo<?>> void addListener(@Nonnull final T info,
            final ContextHandler contextHandler)
    {
        getRegistryForContext(contextHandler).addListener(info);
    }

    public <T extends ListenerInfo<?>> void removeListener(@Nonnull final T info,
           final ContextHandler contextHandler)
    {
        getRegistryForContext(contextHandler).removeListener(info);
    }

    private PerContextEventListener getRegistryForContext(ContextHandler contextHandler)
    {
        PerContextEventListener contextRegistry = registriesByContext.get(contextHandler.getContextInfo());
        if (contextRegistry == null)
        {
            throw new IllegalArgumentException("ContextHandler " + contextHandler.getContextInfo().getName() + " is not registered");
        }
        return contextRegistry;
    }

    public PerContextEventListener addContext(ServletContextHelperInfo info)
    {
        if (registriesByContext.containsKey(info))
        {
            throw new IllegalArgumentException("Context with id " + info.getServiceId() + "is already registered");
        }

        PerContextEventListener contextRegistry = new PerContextEventListener(bundle);
        registriesByContext.put(info, contextRegistry);
        return contextRegistry;
    }

    public void removeContext(ServletContextHelperInfo info)
    {
        registriesByContext.remove(info);
    }

    public Map<Long, Collection<ServiceReference<?>>> getContextRuntimes()
    {
        Map<Long, Collection<ServiceReference<?>>> listenersByContext = new HashMap<Long, Collection<ServiceReference<?>>>();
        for (ServletContextHelperInfo contextInfo : registriesByContext.keySet())
        {
            listenersByContext.put(contextInfo.getServiceId(), registriesByContext.get(contextInfo).getRuntime());
        }
        return listenersByContext;
    }
}
