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
package org.apache.felix.http.base.internal.runtime.dto;

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.osgi.framework.ServiceReference;

public final class RegistryRuntime
{
    private final Collection<ServletContextHelperRuntime> contexts;
    private final Map<Long, Collection<ServiceReference<?>>> listenerRuntimes;
    private final Map<Long, ContextRuntime> handlerRuntimes;
    private final Map<Long, Collection<ServletRuntime>> servletRuntimes;
    private final Map<Long, Collection<ServletRuntime>> resourceRuntimes;
    private final FailureRuntime failureRuntime;

    public RegistryRuntime(Collection<ServletContextHelperRuntime> contexts,
            Collection<ContextRuntime> contextRuntimes,
            ServletRegistryRuntime servletRegistryRuntime,
            Map<Long, Collection<ServiceReference<?>>> listenerRuntimes,
            FailureRuntime failureRuntime)
    {
        this.contexts = contexts;
        this.failureRuntime = failureRuntime;
        this.handlerRuntimes = createServiceIdMap(contextRuntimes);
        this.servletRuntimes = createServletServiceIdMap(servletRegistryRuntime.getServletRuntimes());
        this.resourceRuntimes = createServletServiceIdMap(servletRegistryRuntime.getResourceRuntimes());
        this.listenerRuntimes = listenerRuntimes;
    }

    private static Map<Long, ContextRuntime> createServiceIdMap(Collection<ContextRuntime> contextRuntimes)
    {
        Map<Long, ContextRuntime> runtimesMap = new HashMap<Long, ContextRuntime>();
        for (ContextRuntime contextRuntime : contextRuntimes)
        {
            runtimesMap.put(contextRuntime.getServiceId(), contextRuntime);
        }
        return runtimesMap;
    }

    private static Map<Long, Collection<ServletRuntime>> createServletServiceIdMap(Collection<ServletRuntime> runtimes)
    {
        Map<Long, Collection<ServletRuntime>> runtimesMap = new HashMap<Long, Collection<ServletRuntime>>();
        for (ServletRuntime runtime : runtimes)
        {
            long contextServiceId = runtime.getContextServiceId();
            if (!runtimesMap.containsKey(contextServiceId))
            {
                runtimesMap.put(contextServiceId, new TreeSet<ServletRuntime>(ServletRuntime.COMPARATOR));
            }
            runtimesMap.get(contextServiceId).add(runtime);
        }
        return runtimesMap;
    }

    public ContextRuntime getHandlerRuntime(ServletContextHelperRuntime contextRuntime)
    {
        long serviceId = contextRuntime.getContextInfo().getServiceId();

        if (handlerRuntimes.containsKey(serviceId) && isDefaultContext(contextRuntime))
        {
            // TODO Merge with the default context of the HttpService ( handlerRuntimes.get(0) )
            return handlerRuntimes.get(serviceId);
        }
        else if (handlerRuntimes.containsKey(serviceId))
        {
            return handlerRuntimes.get(serviceId);
        }
        return ContextRuntime.empty(serviceId);
    }

    public Collection<ServletRuntime> getServletRuntimes(ServletContextHelperRuntime contextRuntime)
    {
        if (servletRuntimes.containsKey(contextRuntime.getContextInfo().getServiceId()))
        {
            return servletRuntimes.get(contextRuntime.getContextInfo().getServiceId());
        }
        return Collections.emptyList();
    }

    public Collection<ServletRuntime> getResourceRuntimes(ServletContextHelperRuntime contextRuntime)
    {
        if (resourceRuntimes.containsKey(contextRuntime.getContextInfo().getServiceId()))
        {
            return resourceRuntimes.get(contextRuntime.getContextInfo().getServiceId());
        }
        return Collections.emptyList();
    }

    private boolean isDefaultContext(ServletContextHelperRuntime contextRuntime)
    {
        return contextRuntime.getContextInfo().getName().equals(HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME);
    }

    public Collection<ServiceReference<?>> getListenerRuntimes(ServletContextHelperRuntime contextRuntime)
    {
        if (listenerRuntimes.containsKey(contextRuntime.getContextInfo().getServiceId()))
        {
            return listenerRuntimes.get(contextRuntime.getContextInfo().getServiceId());
        }
        return Collections.emptyList();
    }

    public Collection<ServletContextHelperRuntime> getContexts()
    {
        return contexts;
    }

    public FailureRuntime getFailureRuntime()
    {
        return failureRuntime;
    }
}
