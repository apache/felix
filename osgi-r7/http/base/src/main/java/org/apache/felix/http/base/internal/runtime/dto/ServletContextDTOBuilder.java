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

import static java.util.Collections.list;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.osgi.dto.DTO;
import org.osgi.service.http.runtime.dto.FailedServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;

public final class ServletContextDTOBuilder
{

    public static ServletContextDTO build(final ServletContextHelperInfo info, final ServletContext context, final int reason)
    {
        final ServletContextDTO dto = (reason == -1 ? new ServletContextDTO() : new FailedServletContextDTO());

        dto.attributes = getAttributes(context);
        dto.contextPath = context != null ? context.getContextPath() : info.getPath();
        dto.initParams = info.getInitParameters();
        dto.name = info.getName();
        dto.serviceId = info.getServiceId();

        dto.errorPageDTOs = BuilderConstants.ERROR_PAGE_DTO_ARRAY;
        dto.filterDTOs = BuilderConstants.FILTER_FAILURE_DTO_ARRAY;
        dto.listenerDTOs = BuilderConstants.LISTENER_DTO_ARRAY;
        dto.resourceDTOs = BuilderConstants.RESOURCE_DTO_ARRAY;
        dto.servletDTOs = BuilderConstants.SERVLET_DTO_ARRAY;

        if ( reason != -1 )
        {
            ((FailedServletContextDTO)dto).failureReason = reason;
        }
        return dto;
    }

    private static Map<String, Object> getAttributes(final ServletContext context)
    {
        if (context == null)
        {
            return Collections.emptyMap();
        }

        Map<String, Object> attributes = new HashMap<String, Object>();
        for (String name : list(context.getAttributeNames()))
        {
            Object attribute = context.getAttribute(name);
            if (isSupportedType(attribute))
            {
                attributes.put(name, attribute);
            }
        }
        return attributes;
    }

    private static boolean isSupportedType(Object attribute)
    {
        Class<?> attributeClass = attribute.getClass();
        Class<?> type = !attributeClass.isArray() ?
                attributeClass : attributeClass.getComponentType();

        return Number.class.isAssignableFrom(type) ||
                Boolean.class.isAssignableFrom(type) ||
                String.class.isAssignableFrom(type) ||
                DTO.class.isAssignableFrom(type) ||
                boolean.class.isAssignableFrom(type) ||
                int.class.isAssignableFrom(type) ||
                double.class.isAssignableFrom(type) ||
                float.class.isAssignableFrom(type) ||
                long.class.isAssignableFrom(type) ||
                short.class.isAssignableFrom(type) ||
                byte.class.isAssignableFrom(type) ||
                char.class.isAssignableFrom(type);
    }
}
