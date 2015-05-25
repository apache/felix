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
package org.apache.felix.http.base.internal.registry;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.DispatcherType;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.HttpServiceServletHandler;
import org.apache.felix.http.base.internal.handler.ListenerHandler;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.handler.WhiteboardFilterHandler;
import org.apache.felix.http.base.internal.handler.WhiteboardListenerHandler;
import org.apache.felix.http.base.internal.handler.WhiteboardServletHandler;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ListenerInfo;
import org.apache.felix.http.base.internal.runtime.ResourceInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.FailedDTOHolder;
import org.apache.felix.http.base.internal.service.HttpServiceFactory;
import org.apache.felix.http.base.internal.service.ResourceServlet;
import org.apache.felix.http.base.internal.whiteboard.ContextHandler;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.ServletContextDTO;

/**
 * This registry keeps track of all processing components per context:
 * - servlets
 * - filters
 * - error pages
 */
public final class PerContextHandlerRegistry implements Comparable<PerContextHandlerRegistry>
{
    /** Service id of the context. */
    private final long serviceId;

    /** Ranking of the context. */
    private final int ranking;

    /** The context path. */
    private final String path;

    /** The context prefix. */
    private final String prefix;

    private final ServletRegistry servletRegistry = new ServletRegistry();

    private final FilterRegistry filterRegistry = new FilterRegistry();

    private final ErrorPageRegistry errorPageRegistry = new ErrorPageRegistry();

    private final EventListenerRegistry eventListenerRegistry = new EventListenerRegistry();

    /**
     * Default http service registry
     */
    public PerContextHandlerRegistry()
    {
        this.serviceId = HttpServiceFactory.HTTP_SERVICE_CONTEXT_SERVICE_ID;
        this.ranking = Integer.MAX_VALUE;
        this.path = "/";
        this.prefix = null;
    }

    /**
     * Registry for a servlet context helper (whiteboard support)
     * @param info The servlet context helper info
     */
    public PerContextHandlerRegistry(@Nonnull final ServletContextHelperInfo info)
    {
        this.serviceId = info.getServiceId();
        this.ranking = info.getRanking();
        this.path = info.getPath();
        if ( this.path.equals("/") )
        {
            this.prefix = null;
        }
        else
        {
            this.prefix = this.path + "/";
        }
    }

    public long getContextServiceId()
    {
        return this.serviceId;
    }

    public void removeAll()
    {
        this.errorPageRegistry.cleanup();
        this.eventListenerRegistry.cleanup();
        this.filterRegistry.cleanup();
        this.servletRegistry.cleanup();
    }

    @Override
    public int compareTo(@Nonnull final PerContextHandlerRegistry other)
    {
        final int result = Integer.compare(other.path.length(), this.path.length());
        if ( result == 0 ) {
            if (this.ranking == other.ranking)
            {
                // Service id's can be negative. Negative id's follow the reverse natural ordering of integers.
                int reverseOrder = ( this.serviceId <= 0 && other.serviceId <= 0 ) ? -1 : 1;
                return reverseOrder * Long.compare(this.serviceId, other.serviceId);
            }

            return Integer.compare(other.ranking, this.ranking);
        }
        return result;
    }

    public String isMatching(@Nonnull final String requestURI)
    {
        if (requestURI.equals(this.path))
        {
            return "";
        }
        if (this.prefix == null)
        {
            return requestURI;
        }
        if (requestURI.startsWith(this.prefix))
        {
            return requestURI.substring(this.prefix.length() - 1);
        }
        return null;
    }

    public PathResolution resolve(@Nonnull final String relativeRequestURI)
    {
        return this.servletRegistry.resolve(relativeRequestURI);
    }

    public ServletHandler resolveServletByName(final String name)
    {
        return this.servletRegistry.resolveByName(name);
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
        final ServletHandler handler = new WhiteboardServletHandler(
                contextHandler.getContextInfo().getServiceId(),
                context,
                servletInfo, contextHandler.getBundleContext());
        this.servletRegistry.addServlet(handler);
        this.errorPageRegistry.addServlet(handler);
        return -1;
    }

    /**
     * Unregister a servlet
     * @param contextInfo The servlet context helper info
     * @param servletInfo The servlet info
     */
    public void unregisterServlet(@Nonnull final ContextHandler contextHandler, @Nonnull final ServletInfo servletInfo)
    {
        this.servletRegistry.removeServlet(servletInfo, true);
        this.errorPageRegistry.removeServlet(servletInfo, true);
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
        final FilterHandler handler = new WhiteboardFilterHandler(
                contextHandler.getContextInfo().getServiceId(),
                context,
                filterInfo, contextHandler.getBundleContext());
        this.filterRegistry.addFilter(handler);
        return -1;
    }

    /**
     * Unregister a filter
     * @param contextInfo The servlet context helper info
     * @param filterInfo The filter info
     */
    public void unregisterFilter(@Nonnull final ContextHandler contextHandler, @Nonnull final FilterInfo filterInfo)
    {
        this.filterRegistry.removeFilter(filterInfo, true);
        contextHandler.ungetServletContext(filterInfo.getServiceReference().getBundle());
    }

    /**
     * Register listeners
     *
     * @param contextHandler The context handler
     * @param info The listener info
     * @return {@code -1} on successful registration, failure code otherwise
     */
    public int registerListeners(@Nonnull final ContextHandler contextHandler,
            @Nonnull final ListenerInfo info)
    {
        final ExtServletContext context = contextHandler.getServletContext(info.getServiceReference().getBundle());
        if ( context == null )
        {
            return DTOConstants.FAILURE_REASON_SERVLET_CONTEXT_FAILURE;
        }
        final ListenerHandler handler = new WhiteboardListenerHandler(
                contextHandler.getContextInfo().getServiceId(),
                context,
                info,
                contextHandler.getBundleContext());
        this.eventListenerRegistry.addListeners(handler);
        return -1;
    }

    /**
     * Unregister listeners
     *
     * @param contextHandler The context handler
     * @param info The listener info
     */
    public void unregisterListeners(@Nonnull final ContextHandler contextHandler, @Nonnull final ListenerInfo info)
    {
        this.eventListenerRegistry.removeListeners(info);
        contextHandler.ungetServletContext(info.getServiceReference().getBundle());
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

        final ServletHandler handler = new HttpServiceServletHandler(
                contextHandler.getContextInfo().getServiceId(),
                context,
                servletInfo, new ResourceServlet(resourceInfo.getPrefix()));

        this.servletRegistry.addServlet(handler);
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
        this.servletRegistry.removeServlet(servletInfo, true);
        contextHandler.ungetServletContext(servletInfo.getServiceReference().getBundle());
    }

    public FilterHandler[] getFilterHandlers(@CheckForNull final ServletHandler servletHandler,
            @Nonnull final DispatcherType dispatcherType,
            @Nonnull final String requestURI)
    {
        return this.filterRegistry.getFilterHandlers(servletHandler, dispatcherType, requestURI);
    }

    public ServletHandler getErrorHandler(int code, Throwable exception)
    {
        return this.errorPageRegistry.get(exception, code);
    }

    public EventListenerRegistry getEventListenerRegistry()
    {
        return this.eventListenerRegistry;
    }

    /**
     * Create all DTOs for servlets, filters, resources and error pages
     * @param dto The servlet context DTO
     * @param failedDTOHolder The container for all failed DTOs
     */
    public void getRuntime(final ServletContextDTO dto,
            final FailedDTOHolder failedDTOHolder)
    {
        // collect filters
        this.filterRegistry.getRuntimeInfo(dto, failedDTOHolder.failedFilterDTOs);

        // collect error pages
        this.errorPageRegistry.getRuntimeInfo(dto, failedDTOHolder.failedErrorPageDTOs);

        // collect servlets and resources
        this.servletRegistry.getRuntimeInfo(dto, failedDTOHolder.failedServletDTOs, failedDTOHolder.failedResourceDTOs);

        // collect listeners
        this.eventListenerRegistry.getRuntimeInfo(dto, failedDTOHolder.failedListenerDTOs);
    }

    /**
     * Add a filter for the http service.
     * @param handler The filter handler
     */
    public void registerFilter(@Nonnull final FilterHandler handler) {
        this.filterRegistry.addFilter(handler);
    }

    /**
     * Add a servlet for the http service.
     * @param handler The servlet handler
     */
    public void registerServlet(@Nonnull final ServletHandler handler) {
        this.servletRegistry.addServlet(handler);
        this.errorPageRegistry.addServlet(handler);
    }

    /**
     * Remove a servlet for the http service.
     * @param servletInfo The servlet info
     * @param destroy Destroy the servlet
     */
    public void unregisterServlet(@Nonnull final ServletInfo servletInfo, final boolean destroy) {
        this.servletRegistry.removeServlet(servletInfo, destroy);
        this.errorPageRegistry.removeServlet(servletInfo, destroy);
    }

    /**
     * Remove a filter for the http service.
     * @param info The filter info
     * @param destroy Destroy the filter
     */
    public void unregisterFilter(@Nonnull final FilterInfo info, final boolean destroy) {
        this.filterRegistry.removeFilter(info, destroy);
    }
}
