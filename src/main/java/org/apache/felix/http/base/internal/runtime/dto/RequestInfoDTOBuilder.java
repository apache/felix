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
package org.apache.felix.http.base.internal.runtime.dto;

import static java.util.Arrays.asList;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;

public final class RequestInfoDTOBuilder
{
    private static final FilterDTO[] FILTER_DTO_ARRAY = new FilterDTO[0];

    private final HandlerRegistry registry;
    private final String path;

    public RequestInfoDTOBuilder(HandlerRegistry registry, String path)
    {
        this.registry = registry;
        this.path = path;
    }

    public RequestInfoDTO build()
    {
        ServletHandler servletHandler = registry.getServletHandler(path);
        FilterHandler[] filterHandlers = registry.getFilterHandlers(servletHandler, null, path);
        Long contextServiceId = servletHandler.getContextServiceId();

        RequestInfoDTO requestInfoDTO = new RequestInfoDTO();
        requestInfoDTO.path = path;
        requestInfoDTO.servletContextId = contextServiceId;

        requestInfoDTO.filterDTOs = FilterDTOBuilder.create()
                .build(asList(filterHandlers), contextServiceId)
                .toArray(FILTER_DTO_ARRAY);

        if (servletHandler.getServletInfo().isResource())
        {
            requestInfoDTO.resourceDTO = ResourceDTOBuilder.create()
                    .buildDTO(servletHandler, contextServiceId);
        }
        else
        {
            requestInfoDTO.servletDTO = ServletDTOBuilder.create()
                    .buildDTO(servletHandler, contextServiceId);
        }
        return requestInfoDTO;
    }
}
