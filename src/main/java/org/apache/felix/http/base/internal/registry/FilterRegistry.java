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
import java.util.Iterator;
import java.util.List;

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
 */
public final class FilterRegistry
{
    /** List of all filter registrations. These are sorted by the status objects. */
    private volatile List<FilterRegistrationStatus> filters = Collections.emptyList();

    /**
     * The status object keeps track of the registration status of a filter and holds
     * the resolvers to match against a uri.
     * The status objects are sorted by result first, followed by ranking. The active
     * filters ( result == -1) are first, followed by the inactive ones. This sorting
     * allows to only traverse over the active ones and also avoids any sorting
     * as the filters are processed in the correct order already.
     */
    private static final class FilterRegistrationStatus implements Comparable<FilterRegistrationStatus>
    {
        private final int result;
        private final FilterHandler handler;
        private final PathResolver[] resolvers;

        public FilterRegistrationStatus(@Nonnull final FilterHandler handler, @CheckForNull final PathResolver[] resolvers, final int result)
        {
            this.handler = handler;
            this.resolvers = resolvers;
            this.result = result;
        }

        public int getResult()
        {
            return this.result;
        }

        public @Nonnull FilterHandler getHandler()
        {
            return this.handler;
        }

        public @CheckForNull PathResolver[] getResolvers()
        {
            return this.resolvers;
        }

        @Override
        public int compareTo(final FilterRegistrationStatus o) {
            int result = this.result - o.result;
            if ( result == 0 )
            {
                result = this.handler.compareTo(o.handler);
            }
            return result;
        }
    }

    /**
     * Add a filter.
     * @param handler The handler for the filter
     */
    public synchronized void addFilter(@Nonnull final FilterHandler handler)
    {
        final int result = handler.init();
        PathResolver[] prs = null;

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

            prs = resolvers.toArray(new PathResolver[resolvers.size()]);
        }

        final FilterRegistrationStatus status = new FilterRegistrationStatus(handler, prs, result);

        final List<FilterRegistrationStatus> newList = new ArrayList<FilterRegistry.FilterRegistrationStatus>(this.filters);
        newList.add(status);
        Collections.sort(newList);

        this.filters = newList;
    }

    /**
     * Remove a filter
     * @param filterInfo The filter info
     * @param destroy boolean flag indicating whether to call destroy on the filter.
     */
    public synchronized void removeFilter(@Nonnull final FilterInfo filterInfo, final boolean destroy)
    {
        FilterRegistrationStatus found = null;
        final List<FilterRegistrationStatus> newList = new ArrayList<FilterRegistry.FilterRegistrationStatus>(this.filters);
        final Iterator<FilterRegistrationStatus> i = newList.iterator();
        while ( i.hasNext() )
        {
            final FilterRegistrationStatus status = i.next();
            if ( status.getHandler().getFilterInfo().equals(filterInfo) )
            {
                found = status;
                i.remove();
                break;
            }
        }
        if ( found != null )
        {
            this.filters = newList;

            if ( found.getResult() == -1 && destroy )
            {
                found.getHandler().dispose();
            }
        }
    }

    public synchronized void cleanup()
    {
        this.filters = Collections.emptyList();
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
        final List<FilterHandler> result = new ArrayList<FilterHandler>();
        final List<FilterRegistrationStatus> allFilters = this.filters;

        for(final FilterRegistrationStatus status : allFilters)
        {
            // as soon as we encounter a failing filter, we can stop
            if ( status.getResult() != -1 )
            {
                break;
            }
            if (referencesDispatcherType(status.getHandler(), dispatcherType) )
            {
                boolean added = false;
                for(final PathResolver resolver : status.getResolvers())
                {
                    if ( resolver.resolve(requestURI) != null )
                    {
                        result.add(status.getHandler());
                        added = true;
                        break;
                    }
                }
                // check for servlet name if it's not a resource
                final String servletName = (handler != null && !handler.getServletInfo().isResource()) ? handler.getName() : null;
                if ( !added && servletName != null && status.getHandler().getFilterInfo().getServletNames() != null )
                {
                    for(final String name : status.getHandler().getFilterInfo().getServletNames())
                    {
                        if ( servletName.equals(name) )
                        {
                            result.add(status.getHandler());
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

    /**
     * Get the runtime information about filters
     * @param servletContextDTO The servlet context DTO
     * @param failedFilterDTOs The collection holding the failed filters.
     */
    public void getRuntimeInfo(final ServletContextDTO servletContextDTO,
                               final Collection<FailedFilterDTO> failedFilterDTOs)
    {
        final List<FilterDTO> filterDTOs = new ArrayList<FilterDTO>();

        final List<FilterRegistrationStatus> allFilters = this.filters;
        for(final FilterRegistrationStatus status : allFilters)
        {
            if ( status.getResult() != -1 )
            {
                failedFilterDTOs.add((FailedFilterDTO)FilterDTOBuilder.build(status.getHandler(), status.getResult()));
            }
            else
            {
                filterDTOs.add(FilterDTOBuilder.build(status.getHandler(), status.getResult()));
            }
        }

        if ( !filterDTOs.isEmpty() )
        {
            servletContextDTO.filterDTOs = filterDTOs.toArray(new FilterDTO[filterDTOs.size()]);
        }
    }
}
