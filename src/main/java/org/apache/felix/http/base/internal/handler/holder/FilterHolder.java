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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.FilterConfigImpl;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.dto.FilterRuntime;
import org.apache.felix.http.base.internal.util.PatternUtil;
import org.osgi.service.http.runtime.dto.DTOConstants;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FilterHolder implements Comparable<FilterHolder>, FilterRuntime
{
    private final long contextServiceId;

    private final FilterInfo filterInfo;

    private final ExtServletContext context;

    private volatile Filter filter;

    protected volatile int useCount;

    private final Pattern[] patterns;

    public FilterHolder(final long contextServiceId,
            final ExtServletContext context,
            final FilterInfo filterInfo)
    {
        this.contextServiceId = contextServiceId;
        this.context = context;
        this.filterInfo = filterInfo;
        // Compose a single array of all patterns & regexs the filter must represent...
        String[] patterns = getFilterPatterns(filterInfo);

        this.patterns = new Pattern[patterns.length];
        for (int i = 0; i < patterns.length; i++)
        {
            this.patterns[i] = Pattern.compile(patterns[i]);
        }
    }

    private static String[] getFilterPatterns(FilterInfo filterInfo)
    {
        List<String> result = new ArrayList<String>();
        if (filterInfo.getPatterns() != null)
        {
            for (int i = 0; i < filterInfo.getPatterns().length; i++)
            {
                result.add(PatternUtil.convertToRegEx(filterInfo.getPatterns()[i]));
            }
        }
        if (filterInfo.getRegexs() != null)
        {
            for (int i = 0; i < filterInfo.getRegexs().length; i++)
            {
                result.add(filterInfo.getRegexs()[i]);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    public Pattern[] getPatterns() {
        return this.patterns;
    }

    @Override
    public int compareTo(final FilterHolder other)
    {
        return this.filterInfo.compareTo(other.filterInfo);
    }

    @Override
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

    @Override
    public FilterInfo getFilterInfo()
    {
        return this.filterInfo;
    }

    public String getName()
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
}
