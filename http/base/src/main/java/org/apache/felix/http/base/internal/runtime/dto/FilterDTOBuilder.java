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

import javax.servlet.DispatcherType;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;

public final class FilterDTOBuilder
{
    /**
     * Build an array of filter DTO from a filter handler array
     * @param handlers The filter handler array
     * @return The filter DTO array
     */
    public static FilterDTO[] build(final FilterHandler[] handlers)
    {
        if ( handlers.length == 0 )
        {
            return BuilderConstants.EMPTY_FILTER_DTO_ARRAY;
        }
        final FilterDTO[] array = new FilterDTO[handlers.length];
        for(int i=0; i<handlers.length; i++)
        {
            array[i] = build(handlers[i]);
        }

        return array;
    }

    /**
     * Build a filter DTO from a filter handler
     * @param handler The filter handler
     * @return A filter DTO
     */
    public static FilterDTO build(final FilterHandler handler)
    {
        final FilterDTO filterDTO = build(handler.getFilterInfo(), false);

        filterDTO.name = handler.getName();
        filterDTO.servletContextId = handler.getContextServiceId();

        return filterDTO;
    }

    /**
     * Build a filter DTO from a filter info
     * @param info The filter info
     * @return A filter DTO
     */
    public static FilterDTO build(final FilterInfo info, final boolean failed)
    {
        final FilterDTO filterDTO = (failed ? new FailedFilterDTO() : new FilterDTO());

        filterDTO.asyncSupported = info.isAsyncSupported();
        filterDTO.dispatcher = getNames(info.getDispatcher());
        filterDTO.initParams = info.getInitParameters();
        filterDTO.name = info.getName();
        filterDTO.patterns = BuilderConstants.copyWithDefault(info.getPatterns(), BuilderConstants.EMPTY_STRING_ARRAY);
        filterDTO.regexs = BuilderConstants.copyWithDefault(info.getRegexs(), BuilderConstants.EMPTY_STRING_ARRAY);
        filterDTO.serviceId = info.getServiceId();
        filterDTO.servletNames = BuilderConstants.copyWithDefault(info.getServletNames(), BuilderConstants.EMPTY_STRING_ARRAY);

        return filterDTO;
    }

    /**
     * Build a filter failed DTO from a filter handler
     * @param handler The filter handler
     * @return A filter DTO
     */
    public static FailedFilterDTO buildFailed(final FilterHandler handler, final int reason)
    {
        final FailedFilterDTO filterDTO = (FailedFilterDTO)build(handler.getFilterInfo(), true);

        filterDTO.name = handler.getName();
        filterDTO.servletContextId = handler.getContextServiceId();
        filterDTO.failureReason = reason;

        return filterDTO;
    }

    private static String[] getNames(final DispatcherType[] dispatcher)
    {
        if (dispatcher == null)
        {
            return BuilderConstants.EMPTY_STRING_ARRAY;
        }

        String[] names = new String[dispatcher.length];
        for (int i = 0; i < dispatcher.length; i++)
        {
            names[i] = dispatcher[i].name();
        }
        return names;
    }
}
