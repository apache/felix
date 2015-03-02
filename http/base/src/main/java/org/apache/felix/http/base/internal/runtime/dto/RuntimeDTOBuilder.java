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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.AbstractInfo;
import org.apache.felix.http.base.internal.runtime.HandlerRuntime;
import org.apache.felix.http.base.internal.runtime.HandlerRuntime.ErrorPage;
import org.apache.felix.http.base.internal.runtime.RegistryRuntime;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.whiteboard.ContextHandler;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.runtime.dto.FailedServletContextDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;

public final class RuntimeDTOBuilder
{
    private static final ServletContextDTO[] CONTEXT_DTO_ARRAY = new ServletContextDTO[0];

    private static final ServletDTOBuilder SERVLET_DTO_BUILDER = new ServletDTOBuilder();
    private static final ResourceDTOBuilder RESOURCE_DTO_BUILDER = new ResourceDTOBuilder();
    private static final FilterDTOBuilder FILTER_DTO_BUILDER = new FilterDTOBuilder();
    private static final ErrorPageDTOBuilder ERROR_PAGE_DTO_BUILDER = new ErrorPageDTOBuilder();
    private static final ListenerDTOBuilder LISTENER_DTO_BUILDER = new ListenerDTOBuilder();

    private final RegistryRuntime registry;
    private final Map<String, Object> serviceProperties;

    public RuntimeDTOBuilder(RegistryRuntime registry, Map<String, Object> serviceProperties)
    {
        this.registry = registry;
        this.serviceProperties = serviceProperties;
    }

    public RuntimeDTO build()
    {
        RuntimeDTO runtimeDTO = new RuntimeDTO();
        runtimeDTO.attributes = createAttributes();
        final List<FailedErrorPageDTO> failedErrorPageDTOs = new ArrayList<FailedErrorPageDTO>();
        final List<FailedFilterDTO> failedFilterDTOs = new ArrayList<FailedFilterDTO>();
        final List<FailedListenerDTO> failedListenerDTOs = new ArrayList<FailedListenerDTO>();
        final List<FailedResourceDTO> failedResourceDTOs = new ArrayList<FailedResourceDTO>();
        final List<FailedServletContextDTO> failedServletContextDTOs = new ArrayList<FailedServletContextDTO>();
        final List<FailedServletDTO> failedServletDTOs = new ArrayList<FailedServletDTO>();

        for(final AbstractInfo<?> info : this.registry.getInvalidServices())
        {
            if ( info instanceof ServletContextHelperInfo )
            {
                final ServletContextHelperInfo sch = (ServletContextHelperInfo)info;
                final FailedServletContextDTO dto = new FailedServletContextDTO();
                dto.attributes = Collections.emptyMap();
                dto.contextPath = sch.getPath();
                dto.errorPageDTOs = new ErrorPageDTO[0];
                dto.failureReason = DTOConstants.FAILURE_REASON_VALIDATION_FAILED;
                dto.filterDTOs = new FilterDTO[0];
                dto.initParams = sch.getInitParameters();
                dto.listenerDTOs = new ListenerDTO[0];
                dto.name = sch.getName();
                dto.resourceDTOs = new ResourceDTO[0];
                dto.serviceId = sch.getServiceId();
                dto.servletDTOs = new ServletDTO[0];

                failedServletContextDTOs.add(dto);
            }
        }
        //TODO <**
        runtimeDTO.failedErrorPageDTOs = failedErrorPageDTOs.toArray(new FailedErrorPageDTO[failedErrorPageDTOs.size()]);
        runtimeDTO.failedFilterDTOs = failedFilterDTOs.toArray(new FailedFilterDTO[failedFilterDTOs.size()]);
        runtimeDTO.failedListenerDTOs = failedListenerDTOs.toArray(new FailedListenerDTO[failedListenerDTOs.size()]);
        runtimeDTO.failedResourceDTOs = failedResourceDTOs.toArray(new FailedResourceDTO[failedResourceDTOs.size()]);
        runtimeDTO.failedServletContextDTOs = failedServletContextDTOs.toArray(new FailedServletContextDTO[failedServletContextDTOs.size()]);
        runtimeDTO.failedServletDTOs = failedServletDTOs.toArray(new FailedServletDTO[failedServletDTOs.size()]);
        //**>
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
        for (ContextHandler context : registry.getContexts())
        {
            contextDTOs.add(createContextDTO(context,
                    registry.getHandlerRuntime(context),
                    registry.getListenerRuntime(context)));
        }
        return contextDTOs.toArray(CONTEXT_DTO_ARRAY);
    }

    private ServletContextDTO createContextDTO(ContextHandler context,
            HandlerRuntime handlerRuntime,
            Collection<ServiceReference<?>> listenerRefs)
    {
        Collection<ServletHandler> servletHandlers = handlerRuntime.getServletHandlers();
        Collection<ServletHandler> resourceHandlers = handlerRuntime.getResourceHandlers();
        Collection<FilterHandler> filterHandlers = handlerRuntime.getFilterHandlers();
        Collection<ErrorPage> errorPages = handlerRuntime.getErrorPages();
        long servletContextId = handlerRuntime.getServiceId();

        Collection<ServletDTO> servletDTOs = SERVLET_DTO_BUILDER.build(servletHandlers, servletContextId);
        Collection<ResourceDTO> resourcesDTOs = RESOURCE_DTO_BUILDER.build(resourceHandlers, servletContextId);
        Collection<FilterDTO> filtersDTOs = FILTER_DTO_BUILDER.build(filterHandlers, servletContextId);
        Collection<ErrorPageDTO> errorsDTOs = ERROR_PAGE_DTO_BUILDER.build(errorPages, servletContextId);
        Collection<ListenerDTO> listenersDTOs = LISTENER_DTO_BUILDER.build(listenerRefs, servletContextId);

        return new ServletContextDTOBuilder(context,
                    servletDTOs,
                    resourcesDTOs,
                    filtersDTOs,
                    errorsDTOs,
                    listenersDTOs)
                .build();
    }
}
