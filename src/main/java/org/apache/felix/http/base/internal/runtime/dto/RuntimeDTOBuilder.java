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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.http.runtime.HttpServiceRuntime;
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
    private final ServiceReference<HttpServiceRuntime> serviceReference;

    public RuntimeDTOBuilder(final RegistryRuntime registry, final ServiceReference<HttpServiceRuntime> ref)
    {
        this.registry = registry;
        this.serviceReference = ref;
    }

    public RuntimeDTO build()
    {
        final FailureRuntime failureRuntime = registry.getFailureRuntime();

        final RuntimeDTO runtimeDTO = new RuntimeDTO();
        runtimeDTO.serviceDTO = createServiceDTO();
        runtimeDTO.failedErrorPageDTOs = failureRuntime.getErrorPageDTOs();
        runtimeDTO.failedFilterDTOs = failureRuntime.getFilterDTOs();
        runtimeDTO.failedListenerDTOs = failureRuntime.getListenerDTOs();
        runtimeDTO.failedResourceDTOs = failureRuntime.getResourceDTOs();
        runtimeDTO.failedServletContextDTOs = failureRuntime.getServletContextDTOs();
        runtimeDTO.failedServletDTOs = failureRuntime.getServletDTOs();
        runtimeDTO.servletContextDTOs = createContextDTOs();
        return runtimeDTO;
    }

    private ServiceReferenceDTO createServiceDTO()
    {
        final ServiceReferenceDTO dto = new ServiceReferenceDTO();
        dto.bundle = this.serviceReference.getBundle().getBundleId();
        dto.id = (Long) this.serviceReference.getProperty(Constants.SERVICE_ID);
        final Map<String, Object> props = new HashMap<String, Object>();
        for (String key : this.serviceReference.getPropertyKeys())
        {
            props.put(key, this.serviceReference.getProperty(key));
        }
        dto.properties = props;

        final Bundle[] ubs = this.serviceReference.getUsingBundles();
        if (ubs == null)
        {
            dto.usingBundles = new long[0];
        }
        else
        {
            dto.usingBundles = new long[ubs.length];
            for (int j=0; j < ubs.length; j++)
            {
                dto.usingBundles[j] = ubs[j].getBundleId();
            }
        }

        return dto;
    }

    private ServletContextDTO[] createContextDTOs()
    {
        final Collection<ServletContextHelperRuntime> contexts = registry.getContexts();
        final ServletContextDTO[] result = new ServletContextDTO[contexts.size()];
        int index = 0;
        for (final ServletContextHelperRuntime context : contexts)
        {
            result[index++] = createContextDTO(context,
                    context.getContextRuntime(),
                    context.getListeners());
        }
        return result;
    }

    private ServletContextDTO createContextDTO(final ServletContextHelperRuntime context,
            final ContextRuntime contextRuntime,
            final Collection<ServiceReference<?>> listenerRuntimes)
    {
        final long servletContextId = context.getContextInfo().getServiceId();

        Collection<ServletDTO> servletDTOs = ServletDTOBuilder.create().build(contextRuntime.getServletRuntimes(), servletContextId);
        Collection<ResourceDTO> resourceDTOs = ResourceDTOBuilder.create().build(contextRuntime.getServletRuntimes(), servletContextId);
        Collection<FilterDTO> filterDTOs = FilterDTOBuilder.create().build(contextRuntime.getFilterRuntimes(), servletContextId);
        Collection<ErrorPageDTO> errorDTOs = ErrorPageDTOBuilder.create().build(contextRuntime.getServletRuntimes(), servletContextId);
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
