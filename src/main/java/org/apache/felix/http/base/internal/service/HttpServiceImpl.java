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
package org.apache.felix.http.base.internal.service;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletException;

import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.context.ServletContextManager;
import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

public final class HttpServiceImpl implements ExtHttpService
{
    private final Bundle bundle;
    private final HandlerRegistry handlerRegistry;
    private final HashSet<Servlet> localServlets;
    private final HashSet<Filter> localFilters;
    private final ServletContextManager contextManager;

    public HttpServiceImpl(Bundle bundle, ServletContext context, HandlerRegistry handlerRegistry, ServletContextAttributeListener servletAttributeListener, boolean sharedContextAttributes)
    {
        this.bundle = bundle;
        this.handlerRegistry = handlerRegistry;
        this.localServlets = new HashSet<Servlet>();
        this.localFilters = new HashSet<Filter>();
        this.contextManager = new ServletContextManager(this.bundle, context, servletAttributeListener, sharedContextAttributes);
    }

    static Map<String, String> convertToMap(Dictionary dict)
    {
        Map<String, String> result = new HashMap<String, String>();
        if (dict != null)
        {
            Enumeration keyEnum = dict.keys();
            while (keyEnum.hasMoreElements())
            {
                String key = String.valueOf(keyEnum.nextElement());
                Object value = dict.get(key);
                result.put(key, value == null ? null : String.valueOf(value));
            }
        }
        return result;
    }

    static <T> boolean isEmpty(T[] array)
    {
        return array == null || array.length < 1;
    }

    static boolean isEmpty(String str)
    {
        return str == null || "".equals(str.trim());
    }

    @Override
    public HttpContext createDefaultHttpContext()
    {
        return new DefaultHttpContext(this.bundle);
    }

    public void registerFilter(Filter filter, FilterInfo filterInfo) throws ServletException
    {
        if (filter == null)
        {
            throw new IllegalArgumentException("Filter cannot be null!");
        }
        if (filterInfo == null)
        {
            throw new IllegalArgumentException("FilterInfo cannot be null!");
        }
        if (isEmpty(filterInfo.patterns) && isEmpty(filterInfo.regexs) && isEmpty(filterInfo.servletNames))
        {
            throw new IllegalArgumentException("FilterInfo must have at least one pattern or regex, or provide at least one servlet name!");
        }
        if (isEmpty(filterInfo.name))
        {
            filterInfo.name = filter.getClass().getName();
        }

        FilterHandler handler = new FilterHandler(getServletContext(filterInfo.context), filter, filterInfo);
        handler.setInitParams(filterInfo.initParams);
        this.handlerRegistry.addFilter(handler);
        this.localFilters.add(filter);
    }

    @Override
    public void registerFilter(Filter filter, String pattern, Dictionary initParams, int ranking, HttpContext context) throws ServletException
    {
        if (filter == null)
        {
            throw new IllegalArgumentException("Filter must not be null");
        }
        String filterName = null; // XXX
        FilterHandler handler = new FilterHandler(getServletContext(context), filter, pattern, ranking, filterName);
        handler.setInitParams(initParams);
        this.handlerRegistry.addFilter(handler);
        this.localFilters.add(filter);
    }

    @Override
    public void registerResources(String alias, String name, HttpContext context) throws NamespaceException
    {
        if (!isNameValid(name))
        {
            throw new IllegalArgumentException("Malformed resource name [" + name + "]");
        }

        try
        {
            Servlet servlet = new ResourceServlet(name);
            registerServlet(alias, servlet, null, context);
        }
        catch (ServletException e)
        {
            SystemLogger.error("Failed to register resources", e);
        }
    }

    public void registerServlet(Servlet servlet, ServletInfo servletInfo) throws ServletException, NamespaceException
    {
        if (servlet == null)
        {
            throw new IllegalArgumentException("Servlet cannot be null!");
        }
        if (servletInfo == null)
        {
            throw new IllegalArgumentException("ServletInfo cannot be null!");
        }
        if (isEmpty(servletInfo.patterns) && isEmpty(servletInfo.errorPage))
        {
            throw new IllegalArgumentException("ServletInfo must at least have one pattern or error page!");
        }
        if (isEmpty(servletInfo.name))
        {
            servletInfo.name = servlet.getClass().getName();
        }

        ServletHandler handler = new ServletHandler(getServletContext(servletInfo.context), servlet, servletInfo);
        handler.setInitParams(servletInfo.initParams);
        this.handlerRegistry.addServlet(handler);
        this.localServlets.add(servlet);
    }

    @Override
    public void registerServlet(String alias, Servlet servlet, Dictionary initParams, HttpContext context) throws ServletException, NamespaceException
    {
        if (servlet == null)
        {
            throw new IllegalArgumentException("Servlet must not be null");
        }
        if (!isAliasValid(alias))
        {
            throw new IllegalArgumentException("Malformed servlet alias [" + alias + "]");
        }
        String servletName = null; // XXX
        ServletHandler handler = new ServletHandler(getServletContext(context), servlet, alias, servletName);
        handler.setInitParams(initParams);
        this.handlerRegistry.addServlet(handler);
        this.localServlets.add(servlet);
    }

    @Override
    public void unregister(String alias)
    {
        final Servlet servlet = this.handlerRegistry.getServletByAlias(alias);
        if (servlet == null)
        {
            // FELIX-4561 - don't bother throwing an exception if we're stopping anyway...
            if ((bundle.getState() & Bundle.STOPPING) != 0)
            {
                throw new IllegalArgumentException("Nothing registered at " + alias);
            }
            else
            {
                SystemLogger.debug("Nothing registered at " + alias + "; ignoring this because the bundle is stopping!", null);
            }
        }
        unregisterServlet(servlet);
    }

    public void unregisterAll()
    {
        HashSet<Servlet> servlets = new HashSet<Servlet>(this.localServlets);
        for (Servlet servlet : servlets)
        {
            unregisterServlet(servlet, false);
        }

        HashSet<Filter> filters = new HashSet<Filter>(this.localFilters);
        for (Filter fiter : filters)
        {
            unregisterFilter(fiter, false);
        }
    }

    @Override
    public void unregisterFilter(Filter filter)
    {
        unregisterFilter(filter, true);
    }

    @Override
    public void unregisterServlet(Servlet servlet)
    {
        unregisterServlet(servlet, true);
    }

    private ExtServletContext getServletContext(HttpContext context)
    {
        if (context == null)
        {
            context = createDefaultHttpContext();
        }

        return this.contextManager.getServletContext(context);
    }

    private boolean isNameValid(String name)
    {
        if (name == null)
        {
            return false;
        }

        if (!name.equals("/") && name.endsWith("/"))
        {
            return false;
        }

        return true;
    }

    private void unregisterFilter(Filter filter, final boolean destroy)
    {
        if (filter != null)
        {
            this.handlerRegistry.removeFilter(filter, destroy);
            this.localFilters.remove(filter);
        }
    }

    private void unregisterServlet(Servlet servlet, final boolean destroy)
    {
        if (servlet != null)
        {
            this.handlerRegistry.removeServlet(servlet, destroy);
            this.localServlets.remove(servlet);
        }
    }

    private boolean isAliasValid(String alias)
    {
        if (alias == null)
        {
            return false;
        }

        if (!alias.equals("/") && (!alias.startsWith("/") || alias.endsWith("/")))
        {
            return false;
        }

        return true;
    }
}
