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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.handler.HttpSessionWrapper;
import org.apache.felix.http.base.internal.handler.PerContextHandlerRegistry;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ResourceInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.WhiteboardServiceInfo;
import org.apache.felix.http.base.internal.service.HttpServiceFactory;
import org.apache.felix.http.base.internal.whiteboard.tracker.FilterTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.HttpSessionAttributeListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.HttpSessionListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ResourceTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletContextAttributeListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletContextHelperTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletContextListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletRequestAttributeListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletRequestListenerTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;
import org.osgi.util.tracker.ServiceTracker;

public final class WhiteboardHttpService implements HttpServiceRuntime
{

    private final HandlerRegistry handlerRegistry;

    private final BundleContext bundleContext;

    private volatile ServletContextHelperManager contextManager;

    private final List<ServiceTracker<?, ?>> trackers = new ArrayList<ServiceTracker<?, ?>>();

    private final Hashtable<String, Object> runtimeServiceProps = new Hashtable<String, Object>();;

    private final HttpServiceFactory httpServiceFactory;

    private volatile ServiceRegistration<HttpServiceRuntime> runtimeServiceReg;

    /**
     * Create a new whiteboard http service
     * @param bundleContext
     * @param context
     * @param handlerRegistry
     */
    public WhiteboardHttpService(final BundleContext bundleContext,
            final HandlerRegistry handlerRegistry,
            final HttpServiceFactory httpServiceFactory)
    {
        this.handlerRegistry = handlerRegistry;
        this.bundleContext = bundleContext;
        this.httpServiceFactory = httpServiceFactory;
    }

    public void start(final ServletContext context)
    {
        this.contextManager = new ServletContextHelperManager(bundleContext, context, this);

        addTracker(new FilterTracker(bundleContext, contextManager));
        addTracker(new ServletTracker(bundleContext, this.contextManager));
        addTracker(new ResourceTracker(bundleContext, this.contextManager));

        addTracker(new HttpSessionListenerTracker(bundleContext, this.contextManager));
        addTracker(new HttpSessionAttributeListenerTracker(bundleContext, this.contextManager));

        addTracker(new ServletContextHelperTracker(bundleContext, this.contextManager));
        addTracker(new ServletContextListenerTracker(bundleContext, this.contextManager));
        addTracker(new ServletContextAttributeListenerTracker(bundleContext, this.contextManager));

        addTracker(new ServletRequestListenerTracker(bundleContext, this.contextManager));
        addTracker(new ServletRequestAttributeListenerTracker(bundleContext, this.contextManager));

        this.runtimeServiceProps.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ID_ATTRIBUTE,
                this.httpServiceFactory.getHttpServiceServiceId());
        this.runtimeServiceReg = this.bundleContext.registerService(HttpServiceRuntime.class,
                this,
                this.runtimeServiceProps);
    }

    public void stop()
    {
        if ( this.runtimeServiceReg != null )
        {
            this.runtimeServiceReg.unregister();
            this.runtimeServiceReg = null;
        }

        for(final ServiceTracker<?, ?> t : this.trackers)
        {
            t.close();
        }
        this.trackers.clear();
        if ( this.contextManager != null )
        {
            this.contextManager.close();
            this.contextManager = null;
        }
    }

    private void addTracker(ServiceTracker<?, ?> tracker)
    {
        this.trackers.add(tracker);
        tracker.open();
    }

    public void setProperties(final Hashtable<String, Object> props)
    {
        // runtime service gets the same props for now
        this.runtimeServiceProps.clear();
        this.runtimeServiceProps.putAll(props);

        if (this.runtimeServiceReg != null)
        {
            this.runtimeServiceProps.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ID_ATTRIBUTE,
                    this.httpServiceFactory.getHttpServiceServiceId());
            this.runtimeServiceReg.setProperties(this.runtimeServiceProps);
        }
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
                        contextHandler.addWhiteboardService(servletInfo);
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
        contextHandler.removeWhiteboardService(servletInfo);
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
                    contextHandler.addWhiteboardService(filterInfo);
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
        contextHandler.removeWhiteboardService(filterInfo);
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
                contextHandler.addWhiteboardService(resourceInfo);
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
        contextHandler.removeWhiteboardService(servletInfo);
    }

    public void registerContext(@Nonnull final ContextHandler contextHandler)
    {
        this.handlerRegistry.add(contextHandler.getContextInfo());
    }

    public void unregisterContext(@Nonnull final ContextHandler contextHandler)
    {
        this.handlerRegistry.remove(contextHandler.getContextInfo());
    }

    public void sessionDestroyed(@Nonnull final HttpSession session, final Set<Long> contextIds)
    {
        for(final Long contextId : contextIds)
        {
            // TODO - on shutdown context manager is already NULL which shouldn't be the case
            if ( this.contextManager != null )
            {
                final ContextHandler handler = this.contextManager.getContextHandler(contextId);
                if ( handler != null )
                {
                    final ExtServletContext context = handler.getServletContext(this.bundleContext.getBundle());
                    new HttpSessionWrapper(contextId, session, context, true).invalidate();
                    handler.ungetServletContext(this.bundleContext.getBundle());
                }
            }
        }
    }

    public ServiceReference<HttpServiceRuntime> getServiceReference()
    {
        return this.runtimeServiceReg.getReference();
    }

    @Override
    public RuntimeDTO getRuntimeDTO()
    {
        // create new DTO on every call
        final RuntimeDTO runtime = new RuntimeDTO();

        // attributes
        runtime.attributes = new HashMap<String, String>();
        for(final Map.Entry<String, Object> entry : this.runtimeServiceProps.entrySet())
        {
            runtime.attributes.put(entry.getKey(), entry.getValue().toString());
        }

        // servlet context DTOs
        final List<ServletContextDTO> contextDTOs = new ArrayList<ServletContextDTO>();
        for(final ContextHandler handler : this.contextManager.getContextHandlers())
        {
            final ServletContextDTO dto = new ServletContextDTO();

            final ServletContext ctx = handler.getServletContext(this.bundleContext.getBundle());
            try
            {
                dto.name = handler.getContextInfo().getName();
                dto.contextPath = handler.getContextInfo().getPath();
                dto.initParams = new HashMap<String, String>(handler.getContextInfo().getInitParameters());
                dto.serviceId = handler.getContextInfo().getServiceId();

                dto.contextName = ctx.getServletContextName();
                dto.attributes = new HashMap<String, Object>();
                final Enumeration<String> e = ctx.getAttributeNames();
                while ( e.hasMoreElements() )
                {
                    final String name = e.nextElement();
                    final Object value = ctx.getAttribute(name);
                    if ( value != null )
                    {
                        // TODO - check for appropriate value types
                    }
                }

                final List<ErrorPageDTO> errorPages = new ArrayList<ErrorPageDTO>();
                final List<FilterDTO> filters = new ArrayList<FilterDTO>();
                final List<ServletDTO> servlets = new ArrayList<ServletDTO>();
                final List<ResourceDTO> resources = new ArrayList<ResourceDTO>();
                for(final WhiteboardServiceInfo<?> info : handler.getWhiteboardServices())
                {
                    if ( info instanceof ServletInfo )
                    {
                        final ServletInfo si = (ServletInfo)info;
                        if ( si.getErrorPage() != null )
                        {
                            final ErrorPageDTO page = new ErrorPageDTO();
                            errorPages.add(page);
                            page.asyncSupported = si.isAsyncSupported();
                            page.errorCodes = new long[0]; // TODO
                            page.exceptions = toStringArray(si.getErrorPage()); // TODO
                            page.initParams = new HashMap<String, String>(si.getInitParameters());
                            page.name = si.getName();
                            page.serviceId = si.getServiceId();
                            page.servletContextId = handler.getContextInfo().getServiceId();
                            page.servletInfo = null; // TODO
                        }
                        if ( si.getPatterns() != null )
                        {
                            final ServletDTO servlet = new ServletDTO();
                            servlets.add(servlet);
                            servlet.asyncSupported = si.isAsyncSupported();
                            servlet.initParams = new HashMap<String, String>(si.getInitParameters());
                            servlet.name = si.getName();
                            servlet.patterns = toStringArray(si.getPatterns());
                            servlet.serviceId = si.getServiceId();
                            servlet.servletContextId = handler.getContextInfo().getServiceId();
                            servlet.servletInfo = null; // TODO
                        }
                    }
                    else if ( info instanceof ResourceInfo )
                    {
                        final ResourceDTO rsrc = new ResourceDTO();
                        resources.add(rsrc);
                        rsrc.patterns = ((ResourceInfo)info).getPatterns();
                        rsrc.prefix = ((ResourceInfo)info).getPrefix();
                        rsrc.serviceId = info.getServiceId();
                        rsrc.servletContextId = handler.getContextInfo().getServiceId();
                    }
                    else if ( info instanceof FilterInfo )
                    {
                        final FilterDTO filter = new FilterDTO();
                        filters.add(filter);
                        filter.asyncSupported = ((FilterInfo)info).isAsyncSupported();
                        final DispatcherType[] dTypes = ((FilterInfo)info).getDispatcher();
                        filter.dispatcher = new String[dTypes.length];
                        int index = 0;
                        for(final DispatcherType dt : dTypes)
                        {
                            filter.dispatcher[index++] = dt.name();
                        }
                        filter.initParams = new HashMap<String, String>(((FilterInfo)info).getInitParameters());
                        filter.name = ((FilterInfo)info).getName();
                        filter.patterns = toStringArray(((FilterInfo)info).getPatterns());
                        filter.regexs = toStringArray(((FilterInfo)info).getRegexs());
                        filter.serviceId = info.getServiceId();
                        filter.servletContextId = handler.getContextInfo().getServiceId();
                        filter.servletNames = toStringArray(((FilterInfo)info).getServletNames());
                    }
                }
                dto.errorPageDTOs = errorPages.toArray(new ErrorPageDTO[errorPages.size()]);
                dto.filterDTOs = filters.toArray(new FilterDTO[filters.size()]);
                dto.resourceDTOs = resources.toArray(new ResourceDTO[resources.size()]);
                dto.servletDTOs = servlets.toArray(new ServletDTO[servlets.size()]);

                dto.listenerDTOs = new ListenerDTO[0]; // TODO
            }
            finally
            {
                handler.ungetServletContext(this.bundleContext.getBundle());
            }
            contextDTOs.add(dto);
        }
        runtime.servletContextDTOs = contextDTOs.toArray(new ServletContextDTO[contextDTOs.size()]);

        runtime.failedErrorPageDTOs = null; // TODO
        runtime.failedFilterDTOs = null; // TODO
        runtime.failedListenerDTOs = null; // TODO
        runtime.failedResourceDTOs = null; // TODO
        runtime.failedServletContextDTOs = null; // TODO
        runtime.failedServletDTOs = null; // TODO

        return runtime;
    }

    @Override
    public RequestInfoDTO calculateRequestInfoDTO(final String path) {
        // TODO
        return null;
    }

    private static final String[] EMPTY_ARRAY = new String[0];
    private String[] toStringArray(final String[] array)
    {
        if ( array == null )
        {
            return EMPTY_ARRAY;
        }
        return array;
    }
}
