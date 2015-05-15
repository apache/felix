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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.dto.FilterRuntime;
import org.apache.felix.http.base.internal.util.PatternUtil;

public final class FilterHandler extends AbstractHandler<FilterHandler> implements FilterRuntime
{
    private final Filter filter;
    private final FilterInfo filterInfo;
    private final Pattern[] patterns;

    private final long contextServiceId;

    public FilterHandler(final ServletContextHelperInfo contextInfo, ExtServletContext context, Filter filter, FilterInfo filterInfo)
    {
        super(context, filterInfo.getInitParameters(), filterInfo.getName());
        this.filter = filter;
        this.filterInfo = filterInfo;
        // Compose a single array of all patterns & regexs the filter must represent...
        String[] patterns = getFilterPatterns(filterInfo);

        this.patterns = new Pattern[patterns.length];
        for (int i = 0; i < patterns.length; i++)
        {
            this.patterns[i] = Pattern.compile(patterns[i]);
        }
        if ( contextInfo != null )
        {
            this.contextServiceId = contextInfo.getServiceId();
        }
        else
        {
            this.contextServiceId = 0;
        }
    }

    @Override
    public int compareTo(FilterHandler other)
    {
        return this.filterInfo.compareTo(other.filterInfo);
    }

    @Override
    public void destroy()
    {
        this.filter.destroy();
    }

    public Filter getFilter()
    {
        return this.filter;
    }

    @Override
    public FilterInfo getFilterInfo()
    {
        return this.filterInfo;
    }

    public int getRanking()
    {
        return filterInfo.getRanking();
    }

    public void handle(ServletRequest req, ServletResponse res, FilterChain chain) throws ServletException, IOException
    {
        this.filter.doFilter(req, res, chain);
    }

    @Override
    public void init() throws ServletException
    {
        this.filter.init(new FilterConfigImpl(getName(), getContext(), getInitParams()));
    }

    @Override
    protected Object getSubject()
    {
        return this.filter;
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

    @Override
    public Pattern[] getPatterns() {
        return this.patterns;
    }

    @Override
    public long getContextServiceId()
    {
        return this.contextServiceId;
    }

    @Override
    protected long getServiceId()
    {
        return this.filterInfo.getServiceId();
    }
}
