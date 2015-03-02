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
package org.apache.felix.http.base.internal.runtime;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.http.base.internal.whiteboard.ContextHandler;
import org.osgi.framework.ServiceReference;

public final class RegistryRuntime
{
    private final Collection<ContextHandler> contexts;
    private final Map<Long, Collection<ServiceReference<?>>> listenerRuntimes;
    private final Map<Long, HandlerRuntime> handlerRuntimes;
    private final Collection<AbstractInfo<?>> invalidServices;

    public RegistryRuntime(final Collection<ContextHandler> contexts,
            final Collection<HandlerRuntime> contextRuntimes,
            final Map<Long, Collection<ServiceReference<?>>> listenerRuntimes,
            final Set<AbstractInfo<?>> invalidServices)
    {
        this.contexts = contexts;
        this.handlerRuntimes = createServiceIdMap(contextRuntimes);
        this.listenerRuntimes = listenerRuntimes;
        this.invalidServices = new HashSet<AbstractInfo<?>>(invalidServices);
    }

    private static Map<Long, HandlerRuntime> createServiceIdMap(Collection<HandlerRuntime> contextRuntimes)
    {
        Map<Long, HandlerRuntime> runtimesMap = new HashMap<Long, HandlerRuntime>();
        for (HandlerRuntime contextRuntime : contextRuntimes)
        {
            runtimesMap.put(contextRuntime.getServiceId(), contextRuntime);
        }
        return runtimesMap;
    }

    public HandlerRuntime getHandlerRuntime(ContextHandler contextHandler)
    {
        long serviceId = contextHandler.getContextInfo().getServiceId();

        if (handlerRuntimes.containsKey(serviceId))
        {
            return handlerRuntimes.get(serviceId);
        }
        return HandlerRuntime.empty(serviceId);
    }

    public Collection<ServiceReference<?>> getListenerRuntime(ContextHandler contextHandler)
    {
        if (listenerRuntimes.containsKey(contextHandler.getContextInfo().getServiceId()))
        {
            return listenerRuntimes.get(contextHandler.getContextInfo().getServiceId());
        }
        return Collections.emptyList();
    }

    public Collection<ContextHandler> getContexts()
    {
        return contexts;
    }

    public Collection<AbstractInfo<?>> getInvalidServices()
    {
        return this.invalidServices;
    }
}
