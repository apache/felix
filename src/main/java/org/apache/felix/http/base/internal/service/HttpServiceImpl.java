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
import java.util.concurrent.atomic.AtomicLong;

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

    private final AtomicLong serviceIdCounter = new AtomicLong(-1);

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

    /**
     * TODO As the filter can be registered with multiple patterns
     *      we shouldn't pass the filter object around in order to
     *      be able to get different instances (prototype scope).
     *      Or we do the splitting into single pattern registrations
     *      already before calling registerFilter()
     * @param servlet
     * @param servletInfo
     * @throws ServletException
     * @throws NamespaceException
     */
    public void registerFilter(final Filter filter, final FilterInfo filterInfo)
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
        try {
            this.handlerRegistry.addFilter(handler);
        } catch (ServletException e) {
            // TODO create failure DTO
        }
        this.localFilters.add(filter);
    }

    /**
     * @see org.apache.felix.http.api.ExtHttpService#registerFilter(javax.servlet.Filter, java.lang.String, java.util.Dictionary, int, org.osgi.service.http.HttpContext)
     */
    @Override
    public void registerFilter(Filter filter, String pattern, Dictionary initParams, int ranking, HttpContext context) throws ServletException
    {
        if (filter == null)
        {
            throw new IllegalArgumentException("Filter must not be null");
        }
        final FilterInfo info = new FilterInfo();
        if ( initParams != null && initParams.size() > 0 )
        {
            info.initParams = new HashMap<String, String>();
            Enumeration e = initParams.keys();
            while (e.hasMoreElements())
            {
                Object key = e.nextElement();
                Object value = initParams.get(key);

                if ((key instanceof String) && (value instanceof String))
                {
                    info.initParams.put((String) key, (String) value);
                }
            }
        }
        info.patterns = new String[] {pattern};
        info.context = context;
        info.ranking = ranking;
        info.serviceId = serviceIdCounter.getAndDecrement();

        this.registerFilter(filter, info);
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

    /**
     * TODO As the servlet can be registered with multiple patterns
     *      we shouldn't pass the servlet object around in order to
     *      be able to get different instances (prototype scope).
     *      Or we do the splitting into single pattern registrations
     *      already before calling registerServlet()
     * @param servlet
     * @param servletInfo
     * @throws ServletException
     * @throws NamespaceException
     */
    public void registerServlet(Servlet servlet, ServletInfo servletInfo)
    {
        if (servlet == null)
        {
            throw new IllegalArgumentException("Servlet must not be null");
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

        for(final String pattern : servletInfo.patterns) {
            final ServletHandler handler = new ServletHandler(getServletContext(servletInfo.context), servlet, servletInfo, pattern);
            handler.setInitParams(servletInfo.initParams);
            try {
                this.handlerRegistry.addServlet(handler);
            } catch (ServletException e) {
                // TODO create failure DTO
            } catch (NamespaceException e) {
                // TODO create failure DTO
            }
            this.localServlets.add(servlet);
        }
    }

    public void unregisterServlet(final Servlet servlet, final ServletInfo servletInfo)
    {
        if (servletInfo == null)
        {
            throw new IllegalArgumentException("ServletInfo cannot be null!");
        }
        if ( servletInfo.patterns != null )
        {
            this.handlerRegistry.removeServlet(servlet, true);
            this.localServlets.remove(servlet);
        }
    }

    public void unregisterFilter(final Filter filter, final FilterInfo filterInfo)
    {
        if (filterInfo == null)
        {
            throw new IllegalArgumentException("FilterInfo cannot be null!");
        }
        this.handlerRegistry.removeFilter(filter, true);
        this.localFilters.remove(filter);
    }

    /**
     * @see org.osgi.service.http.HttpService#registerServlet(java.lang.String, javax.servlet.Servlet, java.util.Dictionary, org.osgi.service.http.HttpContext)
     */
    @Override
    public void registerServlet(String alias, Servlet servlet, Dictionary initParams, HttpContext context) throws ServletException, NamespaceException
    {
        if (!isAliasValid(alias))
        {
            throw new IllegalArgumentException("Malformed servlet alias [" + alias + "]");
        }

        final ServletInfo info = new ServletInfo();
        if ( initParams != null && initParams.size() > 0 )
        {
            info.initParams = new HashMap<String, String>();
            Enumeration e = initParams.keys();
            while (e.hasMoreElements())
            {
                Object key = e.nextElement();
                Object value = initParams.get(key);

                if ((key instanceof String) && (value instanceof String))
                {
                    info.initParams.put((String) key, (String) value);
                }
            }
        }
        info.ranking = 0;
        info.serviceId = serviceIdCounter.getAndDecrement();
        info.patterns = new String[] {alias};
        info.context = context;

        this.registerServlet(servlet, info);
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

    /**
     * Old whiteboard support
     * @see org.apache.felix.http.api.ExtHttpService#unregisterFilter(javax.servlet.Filter)
     */
    @Override
    public void unregisterFilter(Filter filter)
    {
        unregisterFilter(filter, true);
    }

    /**
     * Old whiteboard support
     * @see org.apache.felix.http.api.ExtHttpService#unregisterServlet(javax.servlet.Servlet)
     */
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
