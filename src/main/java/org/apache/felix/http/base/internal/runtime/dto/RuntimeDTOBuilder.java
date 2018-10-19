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
import org.osgi.service.http.runtime.dto.FailedPreprocessorDTO;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.runtime.dto.FailedServletContextDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.runtime.dto.PreprocessorDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;

public final class RuntimeDTOBuilder
{

    private final RegistryRuntime registry;
    private final ServiceReferenceDTO serviceRefDTO;

    public RuntimeDTOBuilder(final RegistryRuntime registry, final ServiceReferenceDTO srDTO)
    {
        this.registry = registry;
        this.serviceRefDTO = srDTO;
    }

    public RuntimeDTO build()
    {
        final RuntimeDTO runtimeDTO = new RuntimeDTO();
        runtimeDTO.serviceDTO = this.serviceRefDTO;
        runtimeDTO.servletContextDTOs = createContextDTOs();
        runtimeDTO.preprocessorDTOs = createPreprocessorDTOs();

        runtimeDTO.failedErrorPageDTOs = registry.getFailedDTOHolder().failedErrorPageDTOs.toArray(new FailedErrorPageDTO[registry.getFailedDTOHolder().failedErrorPageDTOs.size()]);
        runtimeDTO.failedFilterDTOs = registry.getFailedDTOHolder().failedFilterDTOs.toArray(new FailedFilterDTO[registry.getFailedDTOHolder().failedFilterDTOs.size()]);
        runtimeDTO.failedListenerDTOs = registry.getFailedDTOHolder().failedListenerDTOs.toArray(new FailedListenerDTO[registry.getFailedDTOHolder().failedListenerDTOs.size()]);
        runtimeDTO.failedResourceDTOs = registry.getFailedDTOHolder().failedResourceDTOs.toArray(new FailedResourceDTO[registry.getFailedDTOHolder().failedResourceDTOs.size()]);
        runtimeDTO.failedServletContextDTOs = registry.getFailedDTOHolder().failedServletContextDTOs.toArray(new FailedServletContextDTO[registry.getFailedDTOHolder().failedServletContextDTOs.size()]);
        runtimeDTO.failedServletDTOs = registry.getFailedDTOHolder().failedServletDTOs.toArray(new FailedServletDTO[registry.getFailedDTOHolder().failedServletDTOs.size()]);
        runtimeDTO.failedPreprocessorDTOs = registry.getFailedDTOHolder().failedPreprocessorDTOs.toArray(new FailedPreprocessorDTO[registry.getFailedDTOHolder().failedPreprocessorDTOs.size()]);

        return runtimeDTO;
    }

    private ServletContextDTO[] createContextDTOs()
    {
        final Collection<ServletContextDTO> contexts = registry.getServletContextDTOs();
        return contexts.toArray(new ServletContextDTO[contexts.size()]);
    }

    private PreprocessorDTO[] createPreprocessorDTOs()
    {
        final Collection<PreprocessorDTO> dtos = registry.getPreprocessorDTOs();
        return dtos.toArray(new PreprocessorDTO[dtos.size()]);
    }
}
