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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.service.http.NamespaceException;

public final class HandlerRegistry
{
    private final ConcurrentMap<Filter, FilterHandler> filterMap = new ConcurrentHashMap<Filter, FilterHandler>();
    private final ConcurrentMap<String, ServletHandler> aliasMap = new ConcurrentHashMap<String, ServletHandler>();
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

    public void addServlet(final ServletHandler handler)
    throws ServletException, NamespaceException
    {
        handler.init();

        if (aliasMap.putIfAbsent(handler.getAlias(), handler) != null)
        {
            handler.destroy();
            throw new NamespaceException("Servlet with alias '" + handler.getAlias() + "' already registered");
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

    public Set<Servlet> removeServlet(final ServletInfo servletInfo, final boolean destroy)
    {
        final Set<Servlet> servletInstances = new HashSet<Servlet>();
        boolean update = false;
        for (Iterator<ServletHandler> it = aliasMap.values().iterator(); it.hasNext(); )
        {
            final ServletHandler handler = it.next();
            if (handler.getServletInfo().compareTo(servletInfo) == 0 ) {
                it.remove();
                servletInstances.add(handler.getServlet());
                if (destroy)
                {
                    handler.destroy();
                }
                update = true;
            }
        }
        if ( update )
        {
            updateServletArray();
        }

        return servletInstances;
    }

    /**
     * Support for old Http Service registrations
     * @param servlet
     * @param destroy
     */
    public void removeServlet(final Servlet servlet, final boolean destroy)
    {
        boolean update = false;
        for (Iterator<ServletHandler> it = aliasMap.values().iterator(); it.hasNext(); )
        {
            ServletHandler handler = it.next();
            if (handler.getServlet() == servlet ) {
                it.remove();
                if (destroy)
                {
                    handler.destroy();
                }
                update = true;
            }
        }
        if ( update )
        {
            updateServletArray();
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
        final ServletHandler handler = aliasMap.get(alias);
        if ( handler != null ) {
            return handler.getServlet();
        }
        return null;
    }

    public void addErrorServlet(String errorPage, ServletHandler handler) throws ServletException
    {
        // TODO
    }

    public void removeAll()
    {
        for (Iterator<ServletHandler> it = aliasMap.values().iterator(); it.hasNext(); )
        {
            ServletHandler handler = it.next();
            it.remove();

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
        final ServletHandler[] tmp = aliasMap.values().toArray(new ServletHandler[aliasMap.size()]);
        Arrays.sort(tmp);
        servlets = tmp;
    }

    private void updateFilterArray()
    {
        final FilterHandler[] tmp = filterMap.values().toArray(new FilterHandler[filterMap.size()]);
        Arrays.sort(tmp);
        filters = tmp;
    }
}
