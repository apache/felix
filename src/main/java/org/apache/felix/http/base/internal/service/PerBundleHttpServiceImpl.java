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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;

import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.HttpServiceFilterHandler;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.util.PatternUtil;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

/**
 * This implementation of the {@link ExtHttpService} implements the front end
 * used by client bundles. It performs the validity checks and passes the
 * real operation to the shared http service.
 */
public final class PerBundleHttpServiceImpl implements ExtHttpService
{
    private final Bundle bundle;
    private final Set<Servlet> localServlets = new HashSet<Servlet>();
    private final Set<FilterHandler> localFilters = new HashSet<FilterHandler>();
    private final ServletContextManager contextManager;
    private final SharedHttpServiceImpl sharedHttpService;

    public PerBundleHttpServiceImpl(final Bundle bundle,
            final SharedHttpServiceImpl sharedHttpService,
            final ServletContext context,
            final ServletContextAttributeListener servletAttributeListener,
            final boolean sharedContextAttributes,
            final ServletRequestListener reqListener,
            final ServletRequestAttributeListener reqAttrListener)
    {
        if (bundle == null)
        {
            throw new IllegalArgumentException("Bundle cannot be null!");
        }
        if (context == null)
        {
            throw new IllegalArgumentException("Context cannot be null!");
        }

        this.bundle = bundle;
        this.contextManager = new ServletContextManager(this.bundle, 
        		context, 
        		servletAttributeListener, 
        		sharedContextAttributes, 
        		reqListener, 
        		reqAttrListener,
        		sharedHttpService.getHandlerRegistry().getRegistry(HttpServiceFactory.HTTP_SERVICE_CONTEXT_SERVICE_ID));
        this.sharedHttpService = sharedHttpService;
    }

    @Override
    public HttpContext createDefaultHttpContext()
    {
        return new DefaultHttpContext(this.bundle);
    }

    /**
     * @see org.apache.felix.http.api.ExtHttpService#registerFilter(javax.servlet.Filter, java.lang.String, java.util.Dictionary, int, org.osgi.service.http.HttpContext)
     */
    @Override
    public void registerFilter(final Filter filter,
            final String pattern,
            final Dictionary initParams,
            final int ranking,
            final HttpContext context)
    throws ServletException
    {
        if (filter == null)
        {
            throw new IllegalArgumentException("Filter must not be null");
        }

        final Map<String, String> paramMap = new HashMap<String, String>();
        if (initParams != null && initParams.size() > 0)
        {
            Enumeration e = initParams.keys();
            while (e.hasMoreElements())
            {
                Object key = e.nextElement();
                Object value = initParams.get(key);

                if ((key instanceof String) && (value instanceof String))
                {
                    paramMap.put((String) key, (String) value);
                }
            }
        }

        final FilterInfo filterInfo = new FilterInfo(String.format("%s_%d", filter.getClass(), this.hashCode()), pattern, ranking, paramMap);
        if (!filterInfo.isValid())
        {
            throw new ServletException("Invalid registration information for filter.");
        }

        final ExtServletContext httpContext = getServletContext(context);
        final FilterHandler holder = new HttpServiceFilterHandler(httpContext, filterInfo, filter);

        if ( this.sharedHttpService.registerFilter(holder) )
        {
            synchronized ( this.localFilters )
            {
                this.localFilters.add(holder);
            }
        }
    }

    /**
     * No need to sync this method, syncing is done via {@link #registerServlet(String, Servlet, Dictionary, HttpContext)}
     * @see org.osgi.service.http.HttpService#registerResources(java.lang.String, java.lang.String, org.osgi.service.http.HttpContext)
     */
    @Override
    public void registerResources(final String alias, final String name, final HttpContext context) throws NamespaceException
    {
        if (!isNameValid(name))
        {
            throw new IllegalArgumentException("Malformed resource name [" + name + "]");
        }

        if (!PatternUtil.isValidPattern(alias) || !alias.startsWith("/") )
        {
            throw new IllegalArgumentException("Malformed resource alias [" + alias + "]");
        }
        try
        {
            final Servlet servlet = new ResourceServlet(name);
            registerServlet(alias, servlet, null, context);
        }
        catch (ServletException e)
        {
            SystemLogger.error("Failed to register resources", e);
        }
    }

    /**
     * @see org.osgi.service.http.HttpService#registerServlet(java.lang.String, javax.servlet.Servlet, java.util.Dictionary, org.osgi.service.http.HttpContext)
     */
    @Override
    public void registerServlet(String alias, Servlet servlet, Dictionary initParams, HttpContext context) throws ServletException, NamespaceException
    {
        if (servlet == null)
        {
            throw new IllegalArgumentException("Servlet must not be null");
        }
        if (!PatternUtil.isValidPattern(alias) || !alias.startsWith("/") )
        {
            throw new IllegalArgumentException("Malformed servlet alias [" + alias + "]");
        }

        final Map<String, String> paramMap = new HashMap<String, String>();
        if (initParams != null && initParams.size() > 0)
        {
            Enumeration e = initParams.keys();
            while (e.hasMoreElements())
            {
                Object key = e.nextElement();
                Object value = initParams.get(key);

                if ((key instanceof String) && (value instanceof String))
                {
                    paramMap.put((String) key, (String) value);
                }
            }
        }

        synchronized (this.localServlets)
        {
            if (this.localServlets.contains(servlet))
            {
                throw new ServletException("Servlet instance " + servlet + " already registered");
            }
            this.localServlets.add(servlet);
        }

        final ServletInfo servletInfo = new ServletInfo(String.format("%s_%d", servlet.getClass(), this.hashCode()), alias, paramMap);
        final ExtServletContext httpContext = getServletContext(context);

        boolean success = false;
        try
        {
            this.sharedHttpService.registerServlet(alias, httpContext,  servlet,  servletInfo);
            success = true;
        }
        finally
        {
            if ( !success )
            {
                synchronized ( this.localServlets )
                {
                    this.localServlets.remove(servlet);
                }
            }
        }
    }

    /**
     * @see org.osgi.service.http.HttpService#unregister(java.lang.String)
     */
    @Override
    public void unregister(final String alias)
    {
        final Servlet servlet = this.sharedHttpService.unregister(alias);
        if ( servlet != null )
        {
            synchronized ( this.localServlets )
            {
                this.localServlets.remove(servlet);
            }
        }
    }

    public void unregisterAll()
    {
        final Set<Servlet> servlets = new HashSet<Servlet>(this.localServlets);
        for (final Servlet servlet : servlets)
        {
            unregisterServlet(servlet, false);
        }

        final Set<FilterHandler> filters = new HashSet<FilterHandler>(this.localFilters);
        for (final FilterHandler holder : filters)
        {
            this.sharedHttpService.unregisterFilter(holder, false);
        }
    }

    /**
     * Old whiteboard support
     * @see org.apache.felix.http.api.ExtHttpService#unregisterFilter(javax.servlet.Filter)
     */
    @Override
    public void unregisterFilter(final Filter filter)
    {
        this.unregisterFilter(filter, true);
    }

    /**
     * Old whiteboard support
     * @see org.apache.felix.http.api.ExtHttpService#unregisterServlet(javax.servlet.Servlet)
     */
    @Override
    public void unregisterServlet(final Servlet servlet)
    {
        this.unregisterServlet(servlet, true);
    }

    private void unregisterServlet(final Servlet servlet, final boolean destroy)
    {
        if (servlet != null)
        {
            synchronized ( this.localServlets )
            {
                this.localServlets.remove(servlet);
            }
            this.sharedHttpService.unregisterServlet(servlet, destroy);
        }
    }

    public ExtServletContext getServletContext(HttpContext context)
    {
        if (context == null)
        {
            context = createDefaultHttpContext();
        }

        return this.contextManager.getServletContext(context);
    }

    private void unregisterFilter(final Filter filter, final boolean destroy)
    {
        if (filter != null)
        {
            synchronized ( this.localFilters )
            {
                final Iterator<FilterHandler> i = this.localFilters.iterator();
                while ( i.hasNext() )
                {
                    final FilterHandler h = i.next();
                    if ( h.getFilter() == filter )
                    {
                        this.sharedHttpService.unregisterFilter(h, destroy);
                        i.remove();
                        break;
                    }
                }
            }
        }
    }

    private boolean isNameValid(final String name)
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
}
