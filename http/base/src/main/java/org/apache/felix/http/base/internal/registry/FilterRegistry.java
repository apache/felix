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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
 *
 * TODO - we should sort the statusMapping by result and ranking, keeping the active filters first,
 *        highest ranking first. This would allow to stop iterating and avoid sorting the result.
 */
public final class FilterRegistry
{
    /** Map of all filter registrations. */
    private final Map<FilterInfo, FilterRegistrationStatus> statusMapping = new ConcurrentHashMap<FilterInfo, FilterRegistrationStatus>();

    private static final class FilterRegistrationStatus
    {
        public int result;
        public FilterHandler handler;
        public PathResolver[] resolvers;
    }

    public synchronized void addFilter(@Nonnull final FilterHandler handler)
    {
        final int result = handler.init();
        final FilterRegistrationStatus status = new FilterRegistrationStatus();
        status.result = result;
        status.handler = handler;

        if ( result == -1 )
        {
            final List<PathResolver> resolvers = new ArrayList<PathResolver>();
            if ( handler.getFilterInfo().getPatterns() != null )
            {
                for(final String pattern : handler.getFilterInfo().getPatterns() ) {
                    resolvers.add(PathResolverFactory.createPatternMatcher(null, pattern));
                }
            }
            if ( handler.getFilterInfo().getRegexs() != null )
            {
                for(final String regex : handler.getFilterInfo().getRegexs() ) {
                    resolvers.add(PathResolverFactory.createRegexMatcher(regex));
                }
            }
            Collections.sort(resolvers);

            status.resolvers = resolvers.toArray(new PathResolver[resolvers.size()]);
        }

        statusMapping.put(handler.getFilterInfo(), status);
    }

    public synchronized void removeFilter(@Nonnull final FilterInfo filterInfo, final boolean destroy)
    {
        final FilterRegistrationStatus status = statusMapping.remove(filterInfo);
        if ( status != null )
        {
            if ( status.result == -1 )
            {
                if (destroy)
                {
                    status.handler.dispose();
                }
            }
        }
    }

    /**
     * Get all filters handling the request.
     * Filters are applied to the url and/or the servlet
     * @param handler Optional servlet handler
     * @param dispatcherType The dispatcher type
     * @param requestURI The request uri
     * @return The array of filter handlers, might be empty.
     */
    public @Nonnull FilterHandler[] getFilterHandlers(@CheckForNull final ServletHandler handler,
            @Nonnull final DispatcherType dispatcherType,
            @Nonnull final String requestURI)
    {
        final Set<FilterHandler> result = new TreeSet<FilterHandler>();

        for(final FilterRegistrationStatus status : this.statusMapping.values())
        {
            if (referencesDispatcherType(status.handler, dispatcherType) )
            {
                boolean added = false;
                for(final PathResolver resolver : status.resolvers)
                {
                    if ( resolver.resolve(requestURI) != null )
                    {
                        result.add(status.handler);
                        added = true;
                        break;
                    }
                }
                // check for servlet name
                final String servletName = (handler != null) ? handler.getName() : null;
                if ( !added && servletName != null && status.handler.getFilterInfo().getServletNames() != null )
                {
                    for(final String name : status.handler.getFilterInfo().getServletNames())
                    {
                        if ( servletName.equals(name) )
                        {
                            result.add(status.handler);
                            added = true;
                            break;
                        }
                    }
                }

            }
        }

        return result.toArray(new FilterHandler[result.size()]);
    }

    /**
     * Check if the filter is registered for the required dispatcher type
     * @param handler The filter handler
     * @param dispatcherType The requested dispatcher type
     * @return {@code true} if the filter can be applied.
     */
    private boolean referencesDispatcherType(final FilterHandler handler, final DispatcherType dispatcherType)
    {
        for(final DispatcherType dt : handler.getFilterInfo().getDispatcher())
        {
            if ( dt == dispatcherType )
            {
                return true;
            }
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
