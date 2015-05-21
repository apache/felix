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
import org.osgi.service.http.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.runtime.dto.FailedServletContextDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;

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
        final RuntimeDTO runtimeDTO = new RuntimeDTO();
        runtimeDTO.serviceDTO = createServiceDTO();
        runtimeDTO.servletContextDTOs = createContextDTOs();

        runtimeDTO.failedErrorPageDTOs = registry.getFailedDTOHolder().failedErrorPageDTOs.toArray(new FailedErrorPageDTO[registry.getFailedDTOHolder().failedErrorPageDTOs.size()]);
        runtimeDTO.failedFilterDTOs = registry.getFailedDTOHolder().failedFilterDTOs.toArray(new FailedFilterDTO[registry.getFailedDTOHolder().failedFilterDTOs.size()]);
        runtimeDTO.failedListenerDTOs = registry.getFailedDTOHolder().failedListenerDTOs.toArray(new FailedListenerDTO[registry.getFailedDTOHolder().failedListenerDTOs.size()]);
        runtimeDTO.failedResourceDTOs = registry.getFailedDTOHolder().failedResourceDTOs.toArray(new FailedResourceDTO[registry.getFailedDTOHolder().failedResourceDTOs.size()]);
        runtimeDTO.failedServletContextDTOs = registry.getFailedDTOHolder().failedServletContextDTO.toArray(new FailedServletContextDTO[registry.getFailedDTOHolder().failedServletContextDTO.size()]);
        runtimeDTO.failedServletDTOs = registry.getFailedDTOHolder().failedServletDTOs.toArray(new FailedServletDTO[registry.getFailedDTOHolder().failedServletDTOs.size()]);

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
        final Collection<ServletContextDTO> contexts = registry.getServletContextDTOs();
        return contexts.toArray(new ServletContextDTO[contexts.size()]);
    }
}
