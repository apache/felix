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
package org.apache.felix.http.base.internal.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.DispatcherType;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.dto.FilterDTOBuilder;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;

/**
 * The filter registry keeps track of all filter mappings for a single servlet context.
 */
public final class FilterRegistry
{
    private volatile FilterHandlerMapping filterMapping = new FilterHandlerMapping();

    private final Map<FilterInfo, FilterRegistrationStatus> statusMapping = new ConcurrentHashMap<FilterInfo, FilterRegistrationStatus>();

    private static final class FilterRegistrationStatus
    {
        public int result;
        public FilterHandler handler;
    }

    public synchronized void addFilter(@Nonnull final FilterHandler handler)
    {
        final int result = handler.init();
        if ( result == -1 )
        {
            this.filterMapping = this.filterMapping.add(handler);
        }
        final FilterRegistrationStatus status = new FilterRegistrationStatus();
        status.result = result;
        status.handler = handler;

        statusMapping.put(handler.getFilterInfo(), status);
    }

    public synchronized void removeFilter(@Nonnull final FilterInfo filterInfo, final boolean destroy)
    {
        final FilterRegistrationStatus status = statusMapping.remove(filterInfo);
        if ( status != null )
        {
            if ( status.result == -1 )
            {
                this.filterMapping = this.filterMapping.remove(status.handler);
                if (destroy)
                {
                    status.handler.dispose();
                }
            }
        }
    }

    public FilterHandler[] getFilterHandlers(@CheckForNull final ServletHandler handler,
            @CheckForNull DispatcherType dispatcherType,
            @Nonnull String requestURI)
    {
        // See Servlet 3.0 specification, section 6.2.4...
        final List<FilterHandler> result = new ArrayList<FilterHandler>();
        result.addAll(this.filterMapping.getAllMatches(requestURI));

        // TODO this is not the most efficient/fastest way of doing this...
        Iterator<FilterHandler> iter = result.iterator();
        while (iter.hasNext())
        {
            if (!referencesDispatcherType(iter.next(), dispatcherType))
            {
                iter.remove();
            }
        }

        final String servletName = (handler != null) ? handler.getName() : null;
        // TODO this is not the most efficient/fastest way of doing this...
        for (FilterHandler filterHandler : this.filterMapping.values())
        {
            if (referencesServletByName(filterHandler, servletName))
            {
                result.add(filterHandler);
            }
        }

        return result.toArray(new FilterHandler[result.size()]);
    }

    private boolean referencesDispatcherType(FilterHandler handler, DispatcherType dispatcherType)
    {
        if (dispatcherType == null)
        {
            return true;
        }
        return Arrays.asList(handler.getFilterInfo().getDispatcher()).contains(dispatcherType);
    }

    private boolean referencesServletByName(FilterHandler handler, String servletName)
    {
        if (servletName == null)
        {
            return false;
        }
        String[] names = handler.getFilterInfo().getServletNames();
        if (names != null && names.length > 0)
        {
            return Arrays.asList(names).contains(servletName);
        }
        return false;
    }

    public void getRuntimeInfo(final ServletContextDTO servletContextDTO,
                               final Collection<FailedFilterDTO> failedFilterDTOs)
    {
        // we create a map to sort filter DTOs by ranking/service id
        final Map<FilterInfo, FilterDTO> filterDTOs = new TreeMap<FilterInfo, FilterDTO>();
        final Map<FilterInfo, FailedFilterDTO> failureFilterDTOs = new TreeMap<FilterInfo, FailedFilterDTO>();

        for(final Map.Entry<FilterInfo, FilterRegistrationStatus> entry : this.statusMapping.entrySet())
        {
            if ( entry.getValue().result != -1 )
            {
                failureFilterDTOs.put(entry.getKey(), FilterDTOBuilder.buildFailed(entry.getValue().handler, entry.getValue().result));
            }
            else
            {
                filterDTOs.put(entry.getKey(), FilterDTOBuilder.build(entry.getValue().handler));
            }
        }

        final Collection<FilterDTO> filterDTOArray = filterDTOs.values();
        if ( !filterDTOArray.isEmpty() )
        {
            servletContextDTO.filterDTOs = filterDTOArray.toArray(new FilterDTO[filterDTOArray.size()]);
        }
        failedFilterDTOs.addAll(failureFilterDTOs.values());
    }
}
