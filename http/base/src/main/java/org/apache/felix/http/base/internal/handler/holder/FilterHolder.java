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
package org.apache.felix.http.base.internal.handler.holder;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.handler.FilterConfigImpl;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.osgi.service.http.runtime.dto.DTOConstants;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class FilterHolder implements Comparable<FilterHolder>
{
    private final FilterInfo filterInfo;

    private final ServletContext context;

    private volatile Filter filter;

    protected volatile int useCount;

    public FilterHolder(final ServletContext context,
            final FilterInfo filterInfo)
    {
        this.context = context;
        this.filterInfo = filterInfo;
    }

    @Override
    public int compareTo(final FilterHolder other)
    {
        return this.filterInfo.compareTo(other.filterInfo);
    }

    protected ServletContext getContext()
    {
        return this.context;
    }

    protected Filter getFilter()
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

    protected String getName()
    {
        String name = this.filterInfo.getName();
        if (name == null)
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
        catch (final ServletException e)
        {
            SystemLogger.error(this.getFilterInfo().getServiceReference(),
                    "Error during calling init() on filter " + this.filter,
                    e);
            return DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT;
        }
        this.useCount++;
        return -1;
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
}
