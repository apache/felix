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

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.HttpServiceServletHandler;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.handler.WhiteboardFilterHandler;
import org.apache.felix.http.base.internal.handler.WhiteboardServletHandler;
import org.apache.felix.http.base.internal.registry.HandlerRegistry;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ResourceInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.service.ResourceServlet;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.runtime.dto.DTOConstants;

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
    public int registerServlet(@Nonnull final ContextHandler contextHandler,
            @Nonnull final ServletInfo servletInfo)
    {
        final ExtServletContext context = contextHandler.getServletContext(servletInfo.getServiceReference().getBundle());
        if ( context == null )
        {
            return DTOConstants.FAILURE_REASON_SERVLET_CONTEXT_FAILURE;
        }
        final ServletHandler holder = new WhiteboardServletHandler(
                contextHandler.getContextInfo().getServiceId(),
                context,
                servletInfo, bundleContext);
        handlerRegistry.addServlet(holder);
        return -1;
    }

    /**
     * Unregister a servlet
     * @param contextInfo The servlet context helper info
     * @param servletInfo The servlet info
     */
    public void unregisterServlet(@Nonnull final ContextHandler contextHandler, @Nonnull final ServletInfo servletInfo)
    {
        handlerRegistry.removeServlet(contextHandler.getContextInfo().getServiceId(), servletInfo, true);
        contextHandler.ungetServletContext(servletInfo.getServiceReference().getBundle());
    }

    /**
     * Register a filter
     * @param contextInfo The servlet context helper info
     * @param filterInfo The filter info
     */
    public int registerFilter(@Nonnull  final ContextHandler contextHandler,
            @Nonnull final FilterInfo filterInfo)
    {
        final ExtServletContext context = contextHandler.getServletContext(filterInfo.getServiceReference().getBundle());
        if ( context == null )
        {
            return DTOConstants.FAILURE_REASON_SERVLET_CONTEXT_FAILURE;
        }
        final FilterHandler holder = new WhiteboardFilterHandler(
                contextHandler.getContextInfo().getServiceId(),
                context,
                filterInfo, bundleContext);
        handlerRegistry.addFilter(holder);
        return -1;
    }

    /**
     * Unregister a filter
     * @param contextInfo The servlet context helper info
     * @param filterInfo The filter info
     */
    public void unregisterFilter(@Nonnull final ContextHandler contextHandler, @Nonnull final FilterInfo filterInfo)
    {
        handlerRegistry.removeFilter(contextHandler.getContextInfo().getServiceId(), filterInfo, true);
        contextHandler.ungetServletContext(filterInfo.getServiceReference().getBundle());
    }

    /**
     * Register a resource.
     * @param contextInfo The servlet context helper info
     * @param resourceInfo The resource info
     */
    public int registerResource(@Nonnull final ContextHandler contextHandler,
            @Nonnull final ResourceInfo resourceInfo)
    {
        final ServletInfo servletInfo = new ServletInfo(resourceInfo);

        final ExtServletContext context = contextHandler.getServletContext(servletInfo.getServiceReference().getBundle());
        if ( context == null )
        {
            return DTOConstants.FAILURE_REASON_SERVLET_CONTEXT_FAILURE;
        }

        final ServletHandler holder = new HttpServiceServletHandler(
                contextHandler.getContextInfo().getServiceId(),
                context,
                servletInfo, new ResourceServlet(resourceInfo.getPrefix()));

        handlerRegistry.addServlet(holder);
        return -1;
    }

    /**
     * Unregister a resource.
     * @param contextInfo The servlet context helper info
     * @param resourceInfo The resource info
     */
    public void unregisterResource(@Nonnull final ContextHandler contextHandler, @Nonnull final ResourceInfo resourceInfo)
    {
        final ServletInfo servletInfo = new ServletInfo(resourceInfo);
        handlerRegistry.removeServlet(contextHandler.getContextInfo().getServiceId(), servletInfo, true);
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
