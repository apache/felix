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
import org.apache.felix.http.base.internal.runtime.ResourceInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;

public final class ResourceDTOBuilder
{
    /**
     * Build a servlet DTO from a servlet handler
     * @param handler The servlet handler
     * @param reason If reason is -1, a servlet DTO is created, otherwise a failed servlet DTO is returned
     * @return A servlet DTO
     */
    public static ResourceDTO build(final ServletHandler handler, final int reason)
    {
        final ResourceDTO dto = build(handler.getServletInfo(), reason != -1);

        dto.servletContextId = handler.getContextServiceId();

        if ( reason != -1 )
        {
            ((FailedResourceDTO)dto).failureReason = reason;
        }

        return dto;
    }

    /**
     * Build a servlet DTO from a servlet info
     * @param info The servlet info
     * @return A servlet DTO
     */
    public static ResourceDTO build(final ServletInfo info, final boolean failed)
    {
        final ResourceDTO dto = (failed ? new FailedResourceDTO() : new ResourceDTO());

        dto.patterns = BuilderConstants.EMPTY_STRING_ARRAY;
        dto.prefix = info.getPrefix();
        dto.serviceId = info.getServiceId();

        return dto;
    }

    /**
     * Build a servlet DTO from a servlet info
     * @param info The servlet info
     * @return A servlet DTO
     */
    public static ResourceDTO build(final ResourceInfo info, final boolean failed)
    {
        final ResourceDTO dto = (failed ? new FailedResourceDTO() : new ResourceDTO());

        dto.patterns = BuilderConstants.copyWithDefault(info.getPatterns(), BuilderConstants.EMPTY_STRING_ARRAY);
        dto.prefix = info.getPrefix();
        dto.serviceId = info.getServiceId();

        return dto;
    }
}
