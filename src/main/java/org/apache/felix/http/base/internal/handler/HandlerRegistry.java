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

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.osgi.service.http.NamespaceException;

public final class HandlerRegistry
{
    private final ConcurrentMap<Servlet, ServletHandler> servletMap = new ConcurrentHashMap<Servlet, ServletHandler>();
    private final ConcurrentMap<Filter, FilterHandler> filterMap = new ConcurrentHashMap<Filter, FilterHandler>();
    private final ConcurrentMap<String, Servlet> aliasMap = new ConcurrentHashMap<String, Servlet>();
    private volatile ServletHandler[] servlets;
    private volatile FilterHandler[] filters;

    public HandlerRegistry()
    {
        servlets = new ServletHandler[0];
        filters = new FilterHandler[0];
    }

    public ServletHandler[] getServlets()
    {
        return servlets;
    }

    public FilterHandler[] getFilters()
    {
        return filters;
    }

    public void addServlet(ServletHandler handler) throws ServletException, NamespaceException
    {
        handler.init();

        // there is a window of opportunity that the servlet/alias was registered between the
        // previous check and this one, so we have to atomically add if absent here.
        if (servletMap.putIfAbsent(handler.getServlet(), handler) != null)
        {
            // Do not destroy the servlet as the same instance was already registered
            throw new ServletException("Servlet instance already registered");
        }
        if (aliasMap.putIfAbsent(handler.getAlias(), handler.getServlet()) != null)
        {
            // Remove it from the servletmap too
            servletMap.remove(handler.getServlet(), handler);

            handler.destroy();
            throw new NamespaceException("Servlet with alias already registered");
        }

        updateServletArray();
    }

    public void addFilter(FilterHandler handler) throws ServletException
    {
        handler.init();

        // there is a window of opportunity that the servlet/alias was registered between the
        // previous check and this one, so we have to atomically add if absent here.
        if (filterMap.putIfAbsent(handler.getFilter(), handler) != null)
        {
            // Do not destroy the filter as the same instance was already registered
            throw new ServletException("Filter instance already registered");
        }

        updateFilterArray();
    }

    public void removeServlet(Servlet servlet, final boolean destroy)
    {
        ServletHandler handler = servletMap.remove(servlet);
        if (handler != null)
        {
            updateServletArray();
            aliasMap.remove(handler.getAlias());
            if (destroy)
            {
                handler.destroy();
            }
        }
    }

    public void removeFilter(Filter filter, final boolean destroy)
    {
        FilterHandler handler = filterMap.remove(filter);
        if (handler != null)
        {
            updateFilterArray();
            if (destroy)
            {
                handler.destroy();
            }
        }
    }

    public Servlet getServletByAlias(String alias)
    {
        return aliasMap.get(alias);
    }

    public void removeAll()
    {
        for (Iterator<ServletHandler> it = servletMap.values().iterator(); it.hasNext(); )
        {
            ServletHandler handler = it.next();
            it.remove();

            aliasMap.remove(handler.getAlias());
            handler.destroy();
        }

        for (Iterator<FilterHandler> it = filterMap.values().iterator(); it.hasNext(); )
        {
            FilterHandler handler = it.next();
            it.remove();
            handler.destroy();
        }

        updateServletArray();
        updateFilterArray();
    }

    private void updateServletArray()
    {
        ServletHandler[] tmp = servletMap.values().toArray(new ServletHandler[servletMap.size()]);
        Arrays.sort(tmp);
        servlets = tmp;
    }

    private void updateFilterArray()
    {
        FilterHandler[] tmp = filterMap.values().toArray(new FilterHandler[filterMap.size()]);
        Arrays.sort(tmp);
        filters = tmp;
    }
}
