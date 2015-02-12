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

import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ResourceInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.whiteboard.tracker.FilterTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.HttpSessionAttributeListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.HttpSessionListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ResourceTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletContextListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletRequestAttributeListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletRequestListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.util.tracker.ServiceTracker;

public final class WhiteboardHttpService
{

    private final HandlerRegistry handlerRegistry;

    private final BundleContext bundleContext;

    private final ServletContextHelperManager contextManager;

    private final ArrayList<ServiceTracker<?, ?>> trackers = new ArrayList<ServiceTracker<?, ?>>();

    /**
     * Create a new whiteboard http service
     * @param bundleContext
     * @param context
     * @param handlerRegistry
     */
    public WhiteboardHttpService(final BundleContext bundleContext,
            final ServletContext context,
            final HandlerRegistry handlerRegistry)
    {
        this.handlerRegistry = handlerRegistry;
        this.bundleContext = bundleContext;
        this.contextManager = new ServletContextHelperManager(bundleContext, context, this);
        addTracker(new FilterTracker(bundleContext, contextManager));
        addTracker(new ServletTracker(bundleContext, this.contextManager));
        addTracker(new ResourceTracker(bundleContext, this.contextManager));
        addTracker(new HttpSessionAttributeListenerTracker(bundleContext, this.contextManager));
        addTracker(new HttpSessionListenerTracker(bundleContext, this.contextManager));
        addTracker(new ServletContextListenerTracker(bundleContext, this.contextManager));
        addTracker(new ServletContextListenerTracker(bundleContext, this.contextManager));
        addTracker(new ServletRequestListenerTracker(bundleContext, this.contextManager));
        addTracker(new ServletRequestAttributeListenerTracker(bundleContext, this.contextManager));
    }

    public void close()
    {
        for(final ServiceTracker<?, ?> t : this.trackers)
        {
            t.close();
        }
        this.trackers.clear();
        this.contextManager.close();
    }

    private void addTracker(ServiceTracker<?, ?> tracker)
    {
        this.trackers.add(tracker);
        tracker.open();
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
                    this.handlerRegistry.getRegistry(contextHandler.getContextInfo()).addServlet(handler);
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
        final Servlet instance = this.handlerRegistry.getRegistry(contextHandler.getContextInfo()).removeServlet(servletInfo, true);
        if ( instance != null )
        {
            this.bundleContext.getServiceObjects(servletInfo.getServiceReference()).ungetService(instance);
            contextHandler.ungetServletContext(servletInfo.getServiceReference().getBundle());
        }
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
                this.handlerRegistry.getRegistry(contextHandler.getContextInfo()).addFilter(handler);
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
        final Filter instance = this.handlerRegistry.getRegistry(contextHandler.getContextInfo()).removeFilter(filterInfo, true);
        if ( instance != null )
        {
            this.bundleContext.getServiceObjects(filterInfo.getServiceReference()).ungetService(instance);
            contextHandler.ungetServletContext(filterInfo.getServiceReference().getBundle());
        }
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
            this.handlerRegistry.getRegistry(contextHandler.getContextInfo()).addServlet(handler);
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
        this.unregisterServlet(contextHandler, servletInfo);
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
