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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.DispatcherType;

import org.apache.felix.http.base.internal.handler.holder.FilterHolder;
import org.apache.felix.http.base.internal.handler.holder.ServletHolder;
import org.apache.felix.http.base.internal.runtime.FilterInfo;

public final class FilterRegistry
{
    private volatile HandlerMapping filterMapping = new HandlerMapping();

    private final Map<FilterInfo, FilterRegistrationStatus> statusMapping = new ConcurrentHashMap<FilterInfo, FilterRegistrationStatus>();

    private static final class FilterRegistrationStatus
    {
        public int result;
        public FilterHolder holder;
    }

    public void addFilter(@Nonnull final FilterHolder holder)
    {
        final int result = holder.init();
        if ( result == -1 )
        {
            this.filterMapping = this.filterMapping.add(holder);
        }
        final FilterRegistrationStatus status = new FilterRegistrationStatus();
        status.result = result;
        status.holder = holder;

        statusMapping.put(holder.getFilterInfo(), status);
    }

    public void removeFilter(@Nonnull final FilterInfo filterInfo, final boolean destroy)
    {
        final FilterRegistrationStatus status = statusMapping.remove(filterInfo);
        if ( status != null )
        {
            if ( status.result == -1 )
            {
                this.filterMapping = this.filterMapping.remove(status.holder);
                if (destroy)
                {
                    status.holder.dispose();
                }
            }
        }
    }

    public FilterHolder[] getFilterHolders(@CheckForNull final ServletHolder holder,
            @CheckForNull DispatcherType dispatcherType,
            @Nonnull String requestURI)
    {
        // See Servlet 3.0 specification, section 6.2.4...
        final List<FilterHolder> result = new ArrayList<FilterHolder>();
        result.addAll(this.filterMapping.getAllMatches(requestURI));

        // TODO this is not the most efficient/fastest way of doing this...
        Iterator<FilterHolder> iter = result.iterator();
        while (iter.hasNext())
        {
            if (!referencesDispatcherType(iter.next(), dispatcherType))
            {
                iter.remove();
            }
        }

        final String servletName = (holder != null) ? holder.getName() : null;
        // TODO this is not the most efficient/fastest way of doing this...
        for (FilterHolder filterHandler : this.filterMapping.values())
        {
            if (referencesServletByName(filterHandler, servletName))
            {
                result.add(filterHandler);
            }
        }

        return result.toArray(new FilterHolder[result.size()]);
    }

    private boolean referencesDispatcherType(FilterHolder holder, DispatcherType dispatcherType)
    {
        if (dispatcherType == null)
        {
            return true;
        }
        return Arrays.asList(holder.getFilterInfo().getDispatcher()).contains(dispatcherType);
    }

    private boolean referencesServletByName(FilterHolder handler, String servletName)
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
}
