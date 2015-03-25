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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;

public final class RuntimeDTOBuilder
{

    private final RegistryRuntime registry;
    private final Map<String, Object> serviceProperties;

    public RuntimeDTOBuilder(RegistryRuntime registry, Map<String, Object> serviceProperties)
    {
        this.registry = registry;
        this.serviceProperties = serviceProperties;
    }

    public RuntimeDTO build()
    {
        FailureRuntime failureRuntime = registry.getFailureRuntime();

        RuntimeDTO runtimeDTO = new RuntimeDTO();
        runtimeDTO.attributes = createAttributes();
        runtimeDTO.failedErrorPageDTOs = failureRuntime.getErrorPageDTOs();
        runtimeDTO.failedFilterDTOs = failureRuntime.getFilterDTOs();
        runtimeDTO.failedListenerDTOs = failureRuntime.getListenerDTOs();
        runtimeDTO.failedResourceDTOs = failureRuntime.getResourceDTOs();
        runtimeDTO.failedServletContextDTOs = failureRuntime.getServletContextDTOs();
        runtimeDTO.failedServletDTOs = failureRuntime.getServletDTOs();
        runtimeDTO.servletContextDTOs = createContextDTOs();
        return runtimeDTO;
    }

    private Map<String, String> createAttributes()
    {
        Map<String, String> attributes = new HashMap<String, String>();
        for (Map.Entry<String, Object> entry : this.serviceProperties.entrySet())
        {
            attributes.put(entry.getKey(), entry.getValue().toString());
        }
        return attributes;
    }

    private ServletContextDTO[] createContextDTOs()
    {
        List<ServletContextDTO> contextDTOs = new ArrayList<ServletContextDTO>();
        for (ServletContextHelperRuntime context : registry.getContexts())
        {
            contextDTOs.add(createContextDTO(context,
                    registry.getHandlerRuntime(context),
                    registry.getListenerRuntimes(context)));
        }
        return contextDTOs.toArray(BuilderConstants.CONTEXT_DTO_ARRAY);
    }

    private ServletContextDTO createContextDTO(ServletContextHelperRuntime context,
            ContextRuntime contextRuntime,
            Collection<ServiceReference<?>> listenerRuntimes)
    {
        Collection<ServletRuntime> servletRuntimes = contextRuntime.getServletRuntimes();
        Collection<ServletRuntime> resourceRuntimes = contextRuntime.getResourceRuntimes();
        Collection<FilterRuntime> filterRuntimes = contextRuntime.getFilterRuntimes();
        Collection<ErrorPageRuntime> errorPageRuntimes = contextRuntime.getErrorPageRuntimes();
        long servletContextId = contextRuntime.getServiceId();

        Collection<ServletDTO> servletDTOs = ServletDTOBuilder.create().build(servletRuntimes, servletContextId);
        Collection<ResourceDTO> resourceDTOs = ResourceDTOBuilder.create().build(resourceRuntimes, servletContextId);
        Collection<FilterDTO> filterDTOs = FilterDTOBuilder.create().build(filterRuntimes, servletContextId);
        Collection<ErrorPageDTO> errorDTOs = ErrorPageDTOBuilder.create().build(errorPageRuntimes, servletContextId);
        Collection<ListenerDTO> listenerDTOs = ListenerDTOBuilder.create().build(listenerRuntimes, servletContextId);

        return new ServletContextDTOBuilder(context,
                    servletDTOs,
                    resourceDTOs,
                    filterDTOs,
                    errorDTOs,
                    listenerDTOs)
                .build();
    }
}
