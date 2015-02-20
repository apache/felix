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
package org.apache.felix.http.base.internal.whiteboard;

import javax.annotation.Nonnull;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.handler.PerContextHandlerRegistry;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ResourceInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;

public final class WhiteboardHttpService
{
    private final HandlerRegistry handlerRegistry;

    private final BundleContext bundleContext;

    /**
     * Create a new whiteboard http service
     * @param bundleContext
     * @param handlerRegistry
     */
    public WhiteboardHttpService(final BundleContext bundleContext,
            final HandlerRegistry handlerRegistry)
    {
        this.handlerRegistry = handlerRegistry;
        this.bundleContext = bundleContext;
    }

    /**
     * Register a servlet.
     * @param contextInfo The servlet context helper info
     * @param servletInfo The servlet info
     */
    public void registerServlet(@Nonnull final ContextHandler contextHandler,
            @Nonnull final ServletInfo servletInfo)
    {
        final ServiceObjects<Servlet> so = this.bundleContext.getServiceObjects(servletInfo.getServiceReference());
        if ( so != null )
        {
            final Servlet servlet = so.getService();
            // TODO create failure DTO if null
            if ( servlet != null )
            {
                final ServletHandler handler = new ServletHandler(contextHandler.getContextInfo(),
                        contextHandler.getServletContext(servletInfo.getServiceReference().getBundle()),
                        servletInfo,
                        servlet);
                try {
                    final PerContextHandlerRegistry registry = this.handlerRegistry.getRegistry(contextHandler.getContextInfo());
                    if (registry != null )
                    {
                        registry.addServlet(handler);
                    }
                } catch (final ServletException e) {
                    so.ungetService(servlet);
                    // TODO create failure DTO
                }
            }
        }
    }

    /**
     * Unregister a servlet
     * @param contextInfo The servlet context helper info
     * @param servletInfo The servlet info
     */
    public void unregisterServlet(@Nonnull final ContextHandler contextHandler, @Nonnull final ServletInfo servletInfo)
    {
        final PerContextHandlerRegistry registry = this.handlerRegistry.getRegistry(contextHandler.getContextInfo());
        if (registry != null )
        {
            final Servlet instance = registry.removeServlet(servletInfo, true);
            if ( instance != null )
            {
                this.bundleContext.getServiceObjects(servletInfo.getServiceReference()).ungetService(instance);
            }
        }
        contextHandler.ungetServletContext(servletInfo.getServiceReference().getBundle());
    }

    /**
     * Register a filter
     * @param contextInfo The servlet context helper info
     * @param filterInfo The filter info
     */
    public void registerFilter(@Nonnull  final ContextHandler contextHandler,
            @Nonnull final FilterInfo filterInfo)
    {
        final Filter filter = this.bundleContext.getServiceObjects(filterInfo.getServiceReference()).getService();
        // TODO create failure DTO if null
        if ( filter != null )
        {
            final FilterHandler handler = new FilterHandler(contextHandler.getContextInfo(),
                    contextHandler.getServletContext(filterInfo.getServiceReference().getBundle()),
                    filter,
                    filterInfo);
            try {
                final PerContextHandlerRegistry registry = this.handlerRegistry.getRegistry(contextHandler.getContextInfo());
                if (registry != null )
                {
                    registry.addFilter(handler);
                }
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
    public void unregisterFilter(@Nonnull final ContextHandler contextHandler, @Nonnull final FilterInfo filterInfo)
    {
        final PerContextHandlerRegistry registry = this.handlerRegistry.getRegistry(contextHandler.getContextInfo());
        if (registry != null )
        {
            final Filter instance = registry.removeFilter(filterInfo, true);
            if ( instance != null )
            {
                this.bundleContext.getServiceObjects(filterInfo.getServiceReference()).ungetService(instance);
            }
        }
        contextHandler.ungetServletContext(filterInfo.getServiceReference().getBundle());
    }

    /**
     * Register a resource.
     * @param contextInfo The servlet context helper info
     * @param resourceInfo The resource info
     */
    public void registerResource(@Nonnull final ContextHandler contextHandler,
            @Nonnull final ResourceInfo resourceInfo)
    {
        final ServletInfo servletInfo = new ServletInfo(resourceInfo);

        final Servlet servlet = new ResourceServlet(resourceInfo.getPrefix());
        final ServletHandler handler = new ServletHandler(contextHandler.getContextInfo(),
                contextHandler.getServletContext(servletInfo.getServiceReference().getBundle()),
                servletInfo,
                servlet);
        try {
            final PerContextHandlerRegistry registry = this.handlerRegistry.getRegistry(contextHandler.getContextInfo());
            if (registry != null )
            {
                registry.addServlet(handler);
            }
        } catch (ServletException e) {
            // TODO create failure DTO
        }
    }

    /**
     * Unregister a resource.
     * @param contextInfo The servlet context helper info
     * @param resourceInfo The resource info
     */
    public void unregisterResource(@Nonnull final ContextHandler contextHandler, @Nonnull final ResourceInfo resourceInfo)
    {
        final ServletInfo servletInfo = new ServletInfo(resourceInfo);
        final PerContextHandlerRegistry registry = this.handlerRegistry.getRegistry(contextHandler.getContextInfo());
        if (registry != null )
        {
            registry.removeServlet(servletInfo, true);
        }
        contextHandler.ungetServletContext(servletInfo.getServiceReference().getBundle());
    }

    public void registerContext(@Nonnull final ContextHandler contextHandler)
    {
        this.handlerRegistry.add(contextHandler.getContextInfo());
    }

    public void unregisterContext(@Nonnull final ContextHandler contextHandler)
    {
        this.handlerRegistry.remove(contextHandler.getContextInfo());
    }
}
