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

import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.service.http.runtime.dto.ResourceDTO;

final class ResourceDTOBuilder extends BaseDTOBuilder<ServletHandler, ResourceDTO>
{
    private static final String[] STRING_ARRAY = new String[0];

    @Override
    ResourceDTO buildDTO(ServletHandler handler, long servletContextId)
    {
        ServletInfo servletInfo = handler.getServletInfo();

        ResourceDTO resourceDTO = new ResourceDTO();
        resourceDTO.patterns = copyWithDefault(servletInfo.getPatterns(), STRING_ARRAY);
        resourceDTO.prefix = servletInfo.getPrefix();
        resourceDTO.serviceId = servletInfo.getServiceId();
        resourceDTO.servletContextId = servletContextId;
        return resourceDTO;
    }
}
