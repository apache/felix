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

import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.dto.state.FilterState;
import org.osgi.service.http.runtime.dto.FilterDTO;

final class FilterDTOBuilder<T extends FilterDTO> extends BaseDTOBuilder<FilterState, T>
{
    static FilterDTOBuilder<FilterDTO> create()
    {
        return new FilterDTOBuilder<FilterDTO>(DTOFactories.FILTER);
    }

    FilterDTOBuilder(DTOFactory<T> dtoFactory)
    {
        super(dtoFactory);
    }

    @Override
    T buildDTO(FilterState filterRuntime, long servletContextId)
    {
        FilterInfo info = filterRuntime.getFilterInfo();

        T filterDTO = getDTOFactory().get();
        filterDTO.asyncSupported = info.isAsyncSupported();
        filterDTO.dispatcher = getNames(info.getDispatcher());
        filterDTO.initParams = info.getInitParameters();
        filterDTO.name = info.getName();
        filterDTO.patterns = copyWithDefault(info.getPatterns(), BuilderConstants.STRING_ARRAY);
        filterDTO.regexs = copyWithDefault(info.getRegexs(), BuilderConstants.STRING_ARRAY);
        filterDTO.serviceId = info.getServiceId();
        filterDTO.servletContextId = servletContextId;
        filterDTO.servletNames = copyWithDefault(info.getServletNames(), BuilderConstants.STRING_ARRAY);

        return filterDTO;
    }

    private String[] getNames(DispatcherType[] dispatcher)
    {
        if (dispatcher == null)
        {
            return BuilderConstants.STRING_ARRAY;
        }

        String[] names = new String[dispatcher.length];
        for (int i = 0; i < dispatcher.length; i++)
        {
            names[i] = dispatcher[i].name();
        }
        return names;
    }
}
