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
package org.apache.felix.http.base.internal.handler;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.osgi.service.http.runtime.dto.DTOConstants;

/**
 * The filter handler handles the initialization and destruction of filter
 * objects.
 */
public abstract class FilterHandler implements Comparable<FilterHandler>
{
    private final long contextServiceId;

    private final FilterInfo filterInfo;

    private final ExtServletContext context;

    private volatile Filter filter;

    protected volatile int useCount;

    public FilterHandler(final long contextServiceId,
            final ExtServletContext context,
            final FilterInfo filterInfo)
    {
        this.contextServiceId = contextServiceId;
        this.context = context;
        this.filterInfo = filterInfo;
    }

    @Override
    public int compareTo(final FilterHandler other)
    {
        return this.filterInfo.compareTo(other.filterInfo);
    }

    public long getContextServiceId()
    {
        return this.contextServiceId;
    }

    public ExtServletContext getContext()
    {
        return this.context;
    }

    public Filter getFilter()
    {
        return filter;
    }

    protected void setFilter(final Filter f)
    {
        this.filter = f;
    }

    public FilterInfo getFilterInfo()
    {
        return this.filterInfo;
    }

    public String getName()
    {
        String name = this.filterInfo.getName();
        if (name == null && filter != null )
        {
            name = filter.getClass().getName();
        }
        return name;
    }

    /**
     * Initialize the object
     * @return {code -1} on success, a failure reason according to {@link DTOConstants} otherwise.
     */
    public int init()
    {
        if ( this.useCount > 0 )
        {
            this.useCount++;
            return -1;
        }

        if (this.filter == null)
        {
            return DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE;
        }

        try
        {
            filter.init(new FilterConfigImpl(getName(), getContext(), getFilterInfo().getInitParameters()));
        }
        catch (final Exception e)
        {
            SystemLogger.error(this.getFilterInfo().getServiceReference(),
                    "Error during calling init() on filter " + this.filter,
                    e);
            return DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT;
        }
        this.useCount++;
        return -1;
    }

    public void handle(@Nonnull final ServletRequest req,
            @Nonnull final ServletResponse res,
            @Nonnull final FilterChain chain) throws ServletException, IOException
    {
        this.filter.doFilter(req, res, chain);
    }

    public boolean destroy()
    {
        if (this.filter == null)
        {
            return false;
        }

        this.useCount--;
        if ( this.useCount == 0 )
        {
            try
            {
                filter.destroy();
            }
            catch ( final Exception ignore )
            {
                // we ignore this
                SystemLogger.error(this.getFilterInfo().getServiceReference(),
                        "Error during calling destroy() on filter " + this.filter,
                        ignore);
            }

            filter = null;
            return true;
        }
        return false;
    }

    public boolean dispose()
    {
        // fully destroy the filter
        this.useCount = 1;
        return this.destroy();
    }

    @Override
    public int hashCode()
    {
        return 31 + filterInfo.hashCode();
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || getClass() != obj.getClass() )
        {
            return false;
        }
        final FilterHandler other = (FilterHandler) obj;
        return filterInfo.equals(other.filterInfo);
    }
}
