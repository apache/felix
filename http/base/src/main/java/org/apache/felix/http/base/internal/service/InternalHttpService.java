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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.AbstractInfo;
import org.apache.felix.http.base.internal.runtime.ContextInfo;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ResourceInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.context.ServletContextHelper;

public final class InternalHttpService
{

    private final HandlerRegistry handlerRegistry;

    private final BundleContext bundleContext;

    private final ServletContext webContext;

    private static final class ContextHolder
    {
        public long counter;
        public ExtServletContext servletContext;
        public ServletContextHelper servletContextHelper;
    }

    private final Map<Long, ContextHolder> contextMap = new HashMap<Long, ContextHolder>();

    public InternalHttpService(final BundleContext bundleContext,
            final ServletContext context,
            final HandlerRegistry handlerRegistry)
    {
        this.handlerRegistry = handlerRegistry;
        this.bundleContext = bundleContext;
        this.webContext = context;
    }

    private ExtServletContext getServletContext(@Nonnull final ContextInfo contextInfo,
            @Nonnull final AbstractInfo<?> serviceInfo)
    {
        final Long key = contextInfo.getServiceId();
        synchronized ( this.contextMap )
        {
            ContextHolder holder = this.contextMap.get(key);
            if ( holder == null )
            {
                holder = new ContextHolder();
                // TODO check for null
                holder.servletContextHelper = serviceInfo.getServiceReference().getBundle().getBundleContext()
                        .getServiceObjects(contextInfo.getServiceReference()).getService();
                holder.servletContext = new ServletContextImpl(serviceInfo.getServiceReference().getBundle(),
                        this.webContext,
                        holder.servletContextHelper);
            }
            holder.counter++;

            return holder.servletContext;
        }
    }

    private void ungetServletContext(@Nonnull final ContextInfo contextInfo)
    {
        final Long key = contextInfo.getServiceId();
        synchronized ( this.contextMap )
        {
            ContextHolder holder = this.contextMap.get(key);
            if ( holder != null )
            {
                holder.counter--;
                if ( holder.counter <= 0 )
                {
                    this.contextMap.remove(key);
                }
            }
        }
    }

    /**
     * Register a servlet.
     * @param contextInfo The servlet context helper info
     * @param servletInfo The servlet info
     */
    public void registerServlet(@Nonnull final ContextInfo contextInfo,
            @Nonnull final ServletInfo servletInfo)
    {
        final Servlet servlet = this.bundleContext.getServiceObjects(servletInfo.getServiceReference()).getService();
        // TODO create failure DTO if null
        if ( servlet != null )
        {
            final ServletHandler handler = new ServletHandler(contextInfo,
                    getServletContext(contextInfo, servletInfo),
                    servletInfo,
                    servlet);
            try {
                this.handlerRegistry.addServlet(contextInfo, handler);
            } catch (ServletException e) {
                // TODO create failure DTO
            } catch (NamespaceException e) {
                // TODO create failure DTO
            }
        }
    }

    /**
     * Unregister a servlet
     * @param contextInfo The servlet context helper info
     * @param servletInfo The servlet info
     */
    public void unregisterServlet(@Nonnull final ContextInfo contextInfo, @Nonnull final ServletInfo servletInfo)
    {
        final Servlet instance = this.handlerRegistry.removeServlet(contextInfo, servletInfo, true);
        if ( instance != null )
        {
            this.bundleContext.getServiceObjects(servletInfo.getServiceReference()).ungetService(instance);
            this.ungetServletContext(contextInfo);
        }
    }

    /**
     * Register a filter
     * @param contextInfo The servlet context helper info
     * @param filterInfo The filter info
     */
    public void registerFilter(@Nonnull  final ContextInfo contextInfo,
            @Nonnull final FilterInfo filterInfo)
    {
        final Filter filter = this.bundleContext.getServiceObjects(filterInfo.getServiceReference()).getService();
        // TODO create failure DTO if null
        if ( filter != null )
        {
            final FilterHandler handler = new FilterHandler(contextInfo,
                    getServletContext(contextInfo, filterInfo),
                    filter,
                    filterInfo);
            try {
                this.handlerRegistry.addFilter(handler);
            } catch (final ServletException e) {
                // TODO create failure DTO
            }
        }
    }

    /**
     * Unregister a filter
     * @param contextInfo The servlet context helper info
     * @param filterInfo The filter info
     */
    public void unregisterFilter(@Nonnull final ContextInfo contextInfo, @Nonnull final FilterInfo filterInfo)
    {
        final Filter instance = this.handlerRegistry.removeFilter(filterInfo, true);
        if ( instance != null )
        {
            this.bundleContext.getServiceObjects(filterInfo.getServiceReference()).ungetService(instance);
            this.ungetServletContext(contextInfo);
        }
    }

    /**
     * Register a resource.
     * @param contextInfo The servlet context helper info
     * @param resourceInfo The resource info
     */
    public void registerResource(@Nonnull final ContextInfo contextInfo,
            @Nonnull final ResourceInfo resourceInfo)
    {
        final ServletInfo servletInfo = new ServletInfo(resourceInfo, new ResourceServlet(resourceInfo.getPrefix()));

        this.registerServlet(contextInfo, servletInfo);
    }

    /**
     * Unregister a resource.
     * @param contextInfo The servlet context helper info
     * @param resourceInfo The resource info
     */
    public void unregisterResource(@Nonnull final ContextInfo contextInfo, @Nonnull final ResourceInfo resourceInfo)
    {
        final ServletInfo servletInfo = new ServletInfo(resourceInfo, null);
        this.unregisterServlet(contextInfo, servletInfo);
    }
}
