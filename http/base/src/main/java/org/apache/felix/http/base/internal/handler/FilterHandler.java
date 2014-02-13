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

import static javax.servlet.http.HttpServletResponse.*;
import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.context.ExtServletContext;

public final class FilterHandler extends AbstractHandler implements Comparable<FilterHandler>
{
    private final Filter filter;
    private final Pattern regex;
    private final int ranking;

    public FilterHandler(ExtServletContext context, Filter filter, String pattern, int ranking, String name)
    {
        super(context, name);
        this.filter = filter;
        this.ranking = ranking;
        this.regex = Pattern.compile(pattern);
    }

    public int compareTo(FilterHandler other)
    {
        if (other.ranking == this.ranking)
        {
            return 0;
        }

        return (other.ranking > this.ranking) ? 1 : -1;
    }

    public void destroy()
    {
        this.filter.destroy();
    }

    public Filter getFilter()
    {
        return this.filter;
    }

    public String getPattern()
    {
        return regex.toString();
    }

    public int getRanking()
    {
        return ranking;
    }

    public void handle(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException
    {
        final boolean matches = matches(req.getPathInfo());
        if (matches)
        {
            doHandle(req, res, chain);
        }
        else
        {
            chain.doFilter(req, res);
        }
    }

    public void init() throws ServletException
    {
        this.filter.init(new FilterConfigImpl(getName(), getContext(), getInitParams()));
    }

    public boolean matches(String uri)
    {
        // assume root if uri is null
        if (uri == null)
        {
            uri = "/";
        }

        return this.regex.matcher(uri).matches();
    }

    final void doHandle(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException
    {
        if (getContext().handleSecurity(req, res))
        {
            this.filter.doFilter(req, res, chain);
        }
        else
        {
            // FELIX-3988: If the response is not yet committed and still has the default 
            // status, we're going to override this and send an error instead.
            if (!res.isCommitted() && res.getStatus() == SC_OK)
            {
                res.sendError(SC_FORBIDDEN);
            }
        }
    }

    @Override
    protected Object getSubject()
    {
        return this.filter;
    }
}
