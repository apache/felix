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
import java.util.Iterator;
import java.util.Map;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.registry.ErrorPageRegistry;
import org.apache.felix.http.base.internal.runtime.AbstractInfo;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ListenerInfo;
import org.apache.felix.http.base.internal.runtime.ResourceInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.service.http.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.runtime.dto.FailedServletContextDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;

public final class FailedDTOHolder
{

    public Collection<FailedFilterDTO> failedFilterDTOs = new ArrayList<FailedFilterDTO>();

    public Collection<FailedListenerDTO> failedListenerDTOs = new ArrayList<FailedListenerDTO>();

    public Collection<FailedServletDTO> failedServletDTOs = new ArrayList<FailedServletDTO>();

    public Collection<FailedResourceDTO> failedResourceDTOs = new ArrayList<FailedResourceDTO>();

    public Collection<FailedErrorPageDTO> failedErrorPageDTOs = new ArrayList<FailedErrorPageDTO>();

    public Collection<FailedServletContextDTO> failedServletContextDTO = new ArrayList<FailedServletContextDTO>();

    public void add(Map<AbstractInfo<?>, Integer> failureInfos)
    {
        for (Map.Entry<AbstractInfo<?>, Integer> failureEntry : failureInfos.entrySet())
        {
            add(failureEntry.getKey(), failureEntry.getValue());
        }
    }

    private void add(final AbstractInfo<?> info, final int failureCode)
    {
        if (info instanceof ServletContextHelperInfo)
        {
            final FailedServletContextDTO dto = (FailedServletContextDTO)ServletContextDTOBuilder.build((ServletContextHelperInfo)info, null, failureCode);
            this.failedServletContextDTO.add(dto);
        }
        else if (info instanceof ServletInfo )
        {
            boolean isError = false;
            if ( ((ServletInfo) info).getErrorPage() != null)
            {
                isError = true;
                final FailedErrorPageDTO dto = (FailedErrorPageDTO)ErrorPageDTOBuilder.build((ServletInfo)info, true);
                dto.failureReason = failureCode;
                final ErrorPageRegistry.ErrorRegistration  reg = ErrorPageRegistry.getErrorRegistration((ServletInfo)info);
                if ( !reg.errorCodes.isEmpty() )
                {
                    final long[] codes = new long[reg.errorCodes.size()];
                    int index = 0;
                    final Iterator<Long> i = reg.errorCodes.iterator();
                    while ( i.hasNext() )
                    {
                        codes[index++] = i.next();
                    }
                    dto.errorCodes = codes;
                }
                if ( !reg.exceptions.isEmpty() )
                {
                    dto.exceptions = reg.exceptions.toArray(new String[reg.exceptions.size()]);
                }
                this.failedErrorPageDTOs.add(dto);
            }

            if ( ((ServletInfo) info).getPatterns() != null || !isError )
            {
                final FailedServletDTO dto = (FailedServletDTO)ServletDTOBuilder.build((ServletInfo) info, true);
                dto.failureReason = failureCode;
                if ( ((ServletInfo) info).getPatterns() != null )
                {
                    dto.patterns = ((ServletInfo) info).getPatterns();
                }
                this.failedServletDTOs.add(dto);
            }
        }
        else if (info instanceof FilterInfo)
        {
            final FailedFilterDTO dto = (FailedFilterDTO)FilterDTOBuilder.build((FilterInfo) info, true);
            dto.failureReason = failureCode;

            this.failedFilterDTOs.add(dto);
        }
        else if (info instanceof ResourceInfo)
        {
            final FailedResourceDTO dto = (FailedResourceDTO)ResourceDTOBuilder.build((ResourceInfo) info, true);
            dto.failureReason = failureCode;
            this.failedResourceDTOs.add(dto);
        }
        else if (info instanceof ListenerInfo)
        {
            final FailedListenerDTO dto = (FailedListenerDTO)ListenerDTOBuilder.build((ListenerInfo<?>)info, failureCode);
            this.failedListenerDTOs.add(dto);
        }
        else
        {
            SystemLogger.error("Unsupported info type: " + info.getClass(), null);
        }
    }
}

