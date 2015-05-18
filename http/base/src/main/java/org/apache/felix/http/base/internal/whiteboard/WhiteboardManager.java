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

import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_UNKNOWN;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_VALIDATION_FAILED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.apache.felix.http.base.internal.console.HttpServicePlugin;
import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.HttpSessionWrapper;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.registry.HandlerRegistry;
import org.apache.felix.http.base.internal.runtime.AbstractInfo;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.HttpSessionAttributeListenerInfo;
import org.apache.felix.http.base.internal.runtime.HttpSessionIdListenerInfo;
import org.apache.felix.http.base.internal.runtime.HttpSessionListenerInfo;
import org.apache.felix.http.base.internal.runtime.ResourceInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextAttributeListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.ServletRequestAttributeListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletRequestListenerInfo;
import org.apache.felix.http.base.internal.runtime.WhiteboardServiceInfo;
import org.apache.felix.http.base.internal.runtime.dto.ContextRuntime;
import org.apache.felix.http.base.internal.runtime.dto.FailureRuntime;
import org.apache.felix.http.base.internal.runtime.dto.RegistryRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ServletContextHelperRuntime;
import org.apache.felix.http.base.internal.service.HttpServiceFactory;
import org.apache.felix.http.base.internal.service.HttpServiceRuntimeImpl;
import org.apache.felix.http.base.internal.util.MimeTypes;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

public final class WhiteboardManager
{
    private final BundleContext bundleContext;

    private final HttpServiceFactory httpServiceFactory;

    private final HttpServiceRuntimeImpl serviceRuntime;

    /** A map containing all servlet context registrations. Mapped by context name */
    private final Map<String, List<ContextHandler>> contextMap = new HashMap<String, List<ContextHandler>>();

    /** A map with all servlet/filter registrations, mapped by abstract info. */
    private final Map<WhiteboardServiceInfo<?>, List<ContextHandler>> servicesMap = new HashMap<WhiteboardServiceInfo<?>, List<ContextHandler>>();

    private final WhiteboardHttpService httpService;

    private final Map<AbstractInfo<?>, Integer> serviceFailures = new ConcurrentSkipListMap<AbstractInfo<?>, Integer>();

    private volatile ServletContext webContext;

    private volatile ServiceRegistration<ServletContextHelper> defaultContextRegistration;

    private final List<ServiceTracker<?, ?>> trackers = new ArrayList<ServiceTracker<?, ?>>();

    private volatile ServiceRegistration<HttpServiceRuntime> runtimeServiceReg;

    private final HttpServicePlugin plugin;

    /**
     * Create a new whiteboard http manager
     * @param bundleContext
     * @param httpServiceFactory
     * @param registry
     */
    public WhiteboardManager(final BundleContext bundleContext,
            final HttpServiceFactory httpServiceFactory,
            final HandlerRegistry registry)
    {
        this.bundleContext = bundleContext;
        this.httpServiceFactory = httpServiceFactory;
        this.httpService = new WhiteboardHttpService(this.bundleContext, registry);
        this.serviceRuntime = new HttpServiceRuntimeImpl(registry, this);
        this.plugin = new HttpServicePlugin(bundleContext, this.serviceRuntime);
    }

    public void start(final ServletContext context)
    {
        // TODO set Endpoint
        this.serviceRuntime.setAttribute(HttpServiceRuntimeConstants.HTTP_SERVICE_ID,
                Collections.singletonList(this.httpServiceFactory.getHttpServiceServiceId()));
        this.runtimeServiceReg = this.bundleContext.registerService(HttpServiceRuntime.class,
                serviceRuntime,
                this.serviceRuntime.getAttributes());
        this.serviceRuntime.setServiceReference(this.runtimeServiceReg.getReference());

        this.webContext = context;

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME);
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
        props.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);

        this.defaultContextRegistration = bundleContext.registerService(
                ServletContextHelper.class,
                new ServiceFactory<ServletContextHelper>()
                {

                    @Override
                    public ServletContextHelper getService(
                            final Bundle bundle,
                            final ServiceRegistration<ServletContextHelper> registration)
                    {
                        return new ServletContextHelper(bundle)
                        {

                            @Override
                            public String getMimeType(final String file)
                            {
                                return MimeTypes.get().getByFile(file);
                            }
                        };
                    }

                    @Override
                    public void ungetService(
                            final Bundle bundle,
                            final ServiceRegistration<ServletContextHelper> registration,
                            final ServletContextHelper service)
                    {
                        // nothing to do
                    }
                }, props);
        addTracker(new FilterTracker(this.bundleContext, this));
        addTracker(new ServletTracker(this.bundleContext, this));
        addTracker(new ResourceTracker(this.bundleContext, this));

        addTracker(new HttpSessionListenerTracker(this.bundleContext, this));
        addTracker(new HttpSessionAttributeListenerTracker(this.bundleContext, this));

        addTracker(new ServletContextHelperTracker(this.bundleContext, this));
        addTracker(new ServletContextListenerTracker(this.bundleContext, this));
        addTracker(new ServletContextAttributeListenerTracker(this.bundleContext, this));

        addTracker(new ServletRequestListenerTracker(this.bundleContext, this));
        addTracker(new ServletRequestAttributeListenerTracker(this.bundleContext, this));
        this.plugin.register();
    }

    private void addTracker(ServiceTracker<?, ?> tracker)
    {
        this.trackers.add(tracker);
        tracker.open();
    }

    /**
     * Stop the instance
     */
    public void stop()
    {
        this.plugin.unregister();
        for(final ServiceTracker<?, ?> t : this.trackers)
        {
            t.close();
        }
        this.trackers.clear();

        this.serviceRuntime.setServiceReference(null);

        // TODO cleanup
        if (this.defaultContextRegistration != null)
        {
            this.defaultContextRegistration.unregister();
            this.defaultContextRegistration = null;
        }

        if ( this.runtimeServiceReg != null )
        {
            this.runtimeServiceReg.unregister();
            this.runtimeServiceReg = null;
        }
    }

    public void setProperties(final Hashtable<String, Object> props)
    {
        // runtime service gets the same props for now
        this.serviceRuntime.setAllAttributes(props);

        if (this.runtimeServiceReg != null)
        {
            this.serviceRuntime.setAttribute(HttpServiceRuntimeConstants.HTTP_SERVICE_ID,
                    Collections.singletonList(this.httpServiceFactory.getHttpServiceServiceId()));
            this.runtimeServiceReg.setProperties(this.serviceRuntime.getAttributes());
        }
    }

    public void sessionDestroyed(@Nonnull final HttpSession session, final Set<Long> contextIds)
    {
        for(final Long contextId : contextIds)
        {
            final ContextHandler handler = this.getContextHandler(contextId);
            if ( handler != null )
            {
                final ExtServletContext context = handler.getServletContext(this.bundleContext.getBundle());
                new HttpSessionWrapper(contextId, session, context, true).invalidate();
                handler.ungetServletContext(this.bundleContext.getBundle());
            }
        }
    }

    /**
     * Handle session id changes
     * @param session The session where the id changed
     * @param oldSessionId The old session id
     * @param contextIds The context ids using that session
     */
    public void sessionIdChanged(@Nonnull final HttpSessionEvent event, String oldSessionId, final Set<Long> contextIds)
    {
        for(final Long contextId : contextIds)
        {
            final ContextHandler handler = this.getContextHandler(contextId);
            if ( handler != null )
            {
                handler.getListenerRegistry().sessionIdChanged(event, oldSessionId);
            }
        }
    }

    /**
     * Activate a servlet context helper.
     * @param contextInfo A context info
     */
    private void activate(final ContextHandler handler)
    {
        handler.activate();

        this.httpService.registerContext(handler);

        final Map<ServiceReference<ServletContextListener>, ServletContextListenerInfo> listeners = new TreeMap<ServiceReference<ServletContextListener>, ServletContextListenerInfo>();
        final List<WhiteboardServiceInfo<?>> services = new ArrayList<WhiteboardServiceInfo<?>>();

        for(final Map.Entry<WhiteboardServiceInfo<?>, List<ContextHandler>> entry : this.servicesMap.entrySet())
        {
            if ( entry.getKey().getContextSelectionFilter().match(handler.getContextInfo().getServiceReference()) )
            {
                entry.getValue().add(handler);
                if ( entry.getKey() instanceof ServletContextListenerInfo )
                {
                    final ServletContextListenerInfo info = (ServletContextListenerInfo)entry.getKey();
                    listeners.put(info.getServiceReference(), info);
                }
                else
                {
                    services.add(entry.getKey());
                }
                removeFailure(entry.getKey(), FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING);
            }
        }
        // context listeners first
        for(final ServletContextListenerInfo info : listeners.values())
        {
            handler.getListenerRegistry().initialized(info);
        }
        // now register services
        for(final WhiteboardServiceInfo<?> info : services)
        {
            this.registerWhiteboardService(handler, info);
        }
    }

    /**
     * Deactivate a servlet context helper.
     * @param contextInfo A context info
     */
    private void deactivate(final ContextHandler handler)
    {
        // context listeners last
        final Map<ServiceReference<ServletContextListener>, ServletContextListenerInfo> listeners = new TreeMap<ServiceReference<ServletContextListener>, ServletContextListenerInfo>();
        final Iterator<Map.Entry<WhiteboardServiceInfo<?>, List<ContextHandler>>> i = this.servicesMap.entrySet().iterator();
        while ( i.hasNext() )
        {
            final Map.Entry<WhiteboardServiceInfo<?>, List<ContextHandler>> entry = i.next();
            if ( entry.getValue().remove(handler) )
            {
                if ( entry.getKey() instanceof ServletContextListenerInfo )
                {
                    final ServletContextListenerInfo info = (ServletContextListenerInfo)entry.getKey();
                    listeners.put(info.getServiceReference(), info);
                }
                else
                {
                    this.unregisterWhiteboardService(handler, entry.getKey());
                }
            }
        }
        for(final ServletContextListenerInfo info : listeners.values())
        {
            handler.getListenerRegistry().destroyed(info);
        }
        handler.deactivate();

        this.httpService.unregisterContext(handler);
    }

    /**
     * Add a servlet context helper.
     */
    public void addContextHelper(final ServletContextHelperInfo info)
    {
        // no failure DTO and no logging if not matching
        if ( isMatchingService(info) )
        {
            if ( info.isValid() )
            {
                synchronized ( this.contextMap )
                {
                    final ContextHandler handler = new ContextHandler(info,
                            this.webContext,
                            this.bundleContext.getBundle());

                    List<ContextHandler> handlerList = this.contextMap.get(info.getName());
                    if ( handlerList == null )
                    {
                        handlerList = new ArrayList<ContextHandler>();
                        this.contextMap.put(info.getName(), handlerList);
                    }
                    handlerList.add(handler);
                    Collections.sort(handlerList);
                    // check for activate/deactivate
                    if ( handlerList.get(0) == handler )
                    {
                        // check for deactivate
                        if ( handlerList.size() > 1 )
                        {
                            ContextHandler oldHead = handlerList.get(1);
                            this.deactivate(oldHead);
                            this.serviceFailures.put(oldHead.getContextInfo(), FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
                        }
                        removeFailure(handler.getContextInfo(), FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
                        this.activate(handler);
                    }
                    else
                    {
                        this.serviceFailures.put(handler.getContextInfo(), FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
                    }
                }
            }
            else
            {
                final String type = info.getClass().getSimpleName().substring(0, info.getClass().getSimpleName().length() - 4);
                SystemLogger.debug("Ignoring " + type + " service " + info.getServiceReference());
                this.serviceFailures.put(info, FAILURE_REASON_VALIDATION_FAILED);
            }
        }
    }

    /**
     * Remove a servlet context helper
     */
    public void removeContextHelper(final ServletContextHelperInfo info)
    {
        // no failure DTO and no logging if not matching
        if ( isMatchingService(info) )
        {
            if ( info.isValid() )
            {
                synchronized ( this.contextMap )
                {
                    final List<ContextHandler> handlerList = this.contextMap.get(info.getName());
                    if ( handlerList != null )
                    {
                        final Iterator<ContextHandler> i = handlerList.iterator();
                        boolean first = true;
                        boolean activateNext = false;
                        while ( i.hasNext() )
                        {
                            final ContextHandler handler = i.next();
                            if ( handler.getContextInfo().compareTo(info) == 0 )
                            {
                                i.remove();
                                // check for deactivate
                                if ( first )
                                {
                                    this.deactivate(handler);
                                    activateNext = true;
                                }
                                break;
                            }
                            first = false;
                        }
                        if ( handlerList.isEmpty() )
                        {
                            this.contextMap.remove(info.getName());
                        }
                        else if ( activateNext )
                        {
                            ContextHandler newHead = handlerList.get(0);
                            this.activate(newHead);
                            removeFailure(newHead.getContextInfo(), FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
                        }
                    }
                }
            }
            this.serviceFailures.remove(info);
        }
    }

    /**
     * Find the list of matching contexts for the whiteboard service
     */
    private List<ContextHandler> getMatchingContexts(final WhiteboardServiceInfo<?> info)
    {
        final List<ContextHandler> result = new ArrayList<ContextHandler>();
        for(final List<ContextHandler> handlerList : this.contextMap.values())
        {
            final ContextHandler h = handlerList.get(0);
            // check whether the servlet context helper is visible to the whiteboard bundle
            // see chapter 140.2
            boolean visible = h.getContextInfo().getServiceId() < 0; // internal ones are always visible
            if ( !visible )
            {
                final String filterString = "(" + Constants.SERVICE_ID + "=" + String.valueOf(h.getContextInfo().getServiceId()) + ")";
                try
                {
                    final Collection<ServiceReference<ServletContextHelper>> col = info.getServiceReference().getBundle().getBundleContext().getServiceReferences(ServletContextHelper.class, filterString);
                    if ( !col.isEmpty() )
                    {
                        visible = true;
                    }
                }
                catch ( final InvalidSyntaxException ise )
                {
                    // we ignore this and treat it as an invisible service
                }
            }
            if ( visible && info.getContextSelectionFilter().match(h.getContextInfo().getServiceReference()) )
            {
                result.add(h);
            }
        }
        return result;
    }

    /**
     * Add new whiteboard service to the registry
     * @param info Whiteboard service info
     */
    public void addWhiteboardService(@Nonnull final WhiteboardServiceInfo<?> info)
    {
        // no logging and no DTO if other target service
        if ( isMatchingService(info) )
        {
            if ( info.isValid() )
            {
                synchronized ( this.contextMap )
                {
                    final List<ContextHandler> handlerList = this.getMatchingContexts(info);
                    this.servicesMap.put(info, handlerList);
                    if (handlerList.isEmpty())
                    {
                        this.serviceFailures.put(info, FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING);
                    }
                    else
                    {
                        for(final ContextHandler h : handlerList)
                        {
                            if ( info instanceof ServletContextListenerInfo )
                            {
                                h.getListenerRegistry().initialized((ServletContextListenerInfo)info);
                            }
                            else
                            {
                                this.registerWhiteboardService(h, info);
                            }
                        }
                    }
                }
            }
            else
            {
                final String type = info.getClass().getSimpleName().substring(0, info.getClass().getSimpleName().length() - 4);
                SystemLogger.debug("Ignoring invalid " + type + " service " + info.getServiceReference());
                this.serviceFailures.put(info, FAILURE_REASON_VALIDATION_FAILED);
            }
        }
    }

    /**
     * Remove whiteboard service from the registry
     * @param info Whiteboard service info
     */
    public void removeWhiteboardService(@Nonnull final WhiteboardServiceInfo<?> info)
    {
        // no logging and no DTO if other target service
        if ( isMatchingService(info) ) {
            if ( info.isValid() )
            {
                synchronized ( this.contextMap )
                {
                    final List<ContextHandler> handlerList = this.servicesMap.remove(info);
                    if ( handlerList != null )
                    {
                        for(final ContextHandler h : handlerList)
                        {
                            if ( !(info instanceof ServletContextListenerInfo ) )
                            {
                                this.unregisterWhiteboardService(h, info);
                            }
                            else
                            {
                                h.getListenerRegistry().initialized((ServletContextListenerInfo)info);
                            }
                        }
                    }
                }
            }
            this.serviceFailures.remove(info);
        }
    }

    /**
     * Register whiteboard service in the http service
     * @param handler Context handler
     * @param info Whiteboard service info
     */
    private void registerWhiteboardService(final ContextHandler handler, final WhiteboardServiceInfo<?> info)
    {
        try
        {
            if ( info instanceof ServletInfo )
            {
                this.httpService.registerServlet(handler, (ServletInfo)info);
            }
            else if ( info instanceof FilterInfo )
            {
                this.httpService.registerFilter(handler, (FilterInfo)info);
            }
            else if ( info instanceof ResourceInfo )
            {
                this.httpService.registerResource(handler, (ResourceInfo)info);
            }

            else if ( info instanceof ServletContextAttributeListenerInfo )
            {
                handler.getListenerRegistry().addListener((ServletContextAttributeListenerInfo) info);
            }
            else if ( info instanceof HttpSessionListenerInfo )
            {
                handler.getListenerRegistry().addListener((HttpSessionListenerInfo) info);
            }
            else if ( info instanceof HttpSessionAttributeListenerInfo )
            {
                handler.getListenerRegistry().addListener((HttpSessionAttributeListenerInfo) info);
            }
            else if ( info instanceof HttpSessionIdListenerInfo )
            {
                handler.getListenerRegistry().addListener((HttpSessionIdListenerInfo) info);
            }
            else if ( info instanceof ServletRequestListenerInfo )
            {
                handler.getListenerRegistry().addListener((ServletRequestListenerInfo) info);
            }
            else if ( info instanceof ServletRequestAttributeListenerInfo )
            {
                handler.getListenerRegistry().addListener((ServletRequestAttributeListenerInfo) info);
            }
        }
        catch (final RuntimeException e)
        {
            serviceFailures.put(info, FAILURE_REASON_UNKNOWN);
            SystemLogger.error("Exception while registering whiteboard service " + info.getServiceReference(), e);
        }
    }

    /**
     * Unregister whiteboard service from the http service
     * @param handler Context handler
     * @param info Whiteboard service info
     */
    private void unregisterWhiteboardService(final ContextHandler handler, final WhiteboardServiceInfo<?> info)
    {
        try
        {
            if ( info instanceof ServletInfo )
            {
                this.httpService.unregisterServlet(handler, (ServletInfo)info);
            }
            else if ( info instanceof FilterInfo )
            {
                this.httpService.unregisterFilter(handler, (FilterInfo)info);
            }
            else if ( info instanceof ResourceInfo )
            {
                this.httpService.unregisterResource(handler, (ResourceInfo)info);
            }

            else if ( info instanceof ServletContextAttributeListenerInfo )
            {
                handler.getListenerRegistry().removeListener((ServletContextAttributeListenerInfo) info);
            }
            else if ( info instanceof HttpSessionListenerInfo )
            {
                handler.getListenerRegistry().removeListener((HttpSessionListenerInfo) info);
            }
            else if ( info instanceof HttpSessionAttributeListenerInfo )
            {
                handler.getListenerRegistry().removeListener((HttpSessionAttributeListenerInfo) info);
            }
            else if ( info instanceof HttpSessionIdListenerInfo )
            {
                handler.getListenerRegistry().removeListener((HttpSessionIdListenerInfo) info);
            }
            else if ( info instanceof ServletRequestListenerInfo )
            {
                handler.getListenerRegistry().removeListener((ServletRequestListenerInfo) info);
            }
            else if ( info instanceof ServletRequestAttributeListenerInfo )
            {
                handler.getListenerRegistry().removeListener((ServletRequestAttributeListenerInfo) info);
            }
        }
        catch (final RegistrationFailureException e)
        {
            serviceFailures.put(e.getInfo(), e.getErrorCode());
            SystemLogger.error("Exception while removing servlet", e);
        }
        serviceFailures.remove(info);
    }

    private void removeFailure(AbstractInfo<?> info, int failureCode)
    {
        Integer registeredFailureCode = this.serviceFailures.get(info);
        if (registeredFailureCode != null && registeredFailureCode == failureCode)
        {
            this.serviceFailures.remove(info);
        }
    }

    /**
     * Check whether the service is specifying a target http service runtime
     * and if so if that is matching this runtime
     */
    private boolean isMatchingService(final AbstractInfo<?> info)
    {
        final String target = info.getTarget();
        if ( target != null )
        {
            try
            {
                final Filter f = this.bundleContext.createFilter(target);
                return f.match(this.runtimeServiceReg.getReference());
            }
            catch ( final InvalidSyntaxException ise)
            {
                // log and ignore service
                SystemLogger.error("Invalid target filter expression for " + info.getServiceReference() + " : " + target, ise);
                return false;
            }
        }
        return true;
    }

    public ContextHandler getContextHandler(final Long contextId)
    {
        synchronized ( this.contextMap )
        {
            for(final List<ContextHandler> handlerList : this.contextMap.values())
            {
                final ContextHandler h = handlerList.get(0);
                if ( h.getContextInfo().getServiceId() == contextId )
                {
                    return h;
                }
            }
        }
        return null;
    }

    public Collection<ContextHandler> getContextHandlers()
    {
         final List<ContextHandler> handlers = new ArrayList<ContextHandler>();
         synchronized ( this.contextMap )
         {
             for(final List<ContextHandler> handlerList : this.contextMap.values())
             {
                 final ContextHandler h = handlerList.get(0);
                 handlers.add(h);
             }
         }
         return handlers;
    }

    private static final String HTTP_SERVICE_CONTEXT_NAME = "Http Service context";

    public RegistryRuntime getRuntime(final HandlerRegistry registry)
    {
        // we create a ServletContextHelperRuntime for each servlet context
        final Collection<ServletContextHelperRuntime> contextRuntimes = new TreeSet<ServletContextHelperRuntime>(ServletContextHelperRuntime.COMPARATOR);

        final FailureRuntime.Builder failureRuntime = FailureRuntime.builder();
        synchronized ( this.contextMap )
        {
            for (final List<ContextHandler> contextHandlerList : this.contextMap.values())
            {
                if ( !contextHandlerList.isEmpty() )
                {
                    final ContextHandler handler = contextHandlerList.get(0);
                    final ContextRuntime cr = registry.getRuntime(handler.getContextInfo().getServiceId());
                    if ( cr != null )
                    {
                        contextRuntimes.add(new ServletContextHelperRuntime() {

                            @Override
                            public ServletContext getSharedContext() {
                                return handler.getSharedContext();
                            }

                            @Override
                            public Collection<ServiceReference<?>> getListeners() {
                                return handler.getListenerRegistry().getRuntime();
                            }

                            @Override
                            public ContextRuntime getContextRuntime() {
                                return cr;
                            }

                            @Override
                            public ServletContextHelperInfo getContextInfo() {
                                return handler.getContextInfo();
                            }
                        });
                    }
                }
            }
            failureRuntime.add(serviceFailures);
        }

        // add the context for the http service
        final ServletContextHelperInfo info = new ServletContextHelperInfo(Integer.MAX_VALUE, 0, HTTP_SERVICE_CONTEXT_NAME, "/", null);
        final ContextRuntime cr = registry.getRuntime(0);
        if ( cr != null )
        {
            contextRuntimes.add(new ServletContextHelperRuntime() {

                @Override
                public ServletContext getSharedContext() {
                    return webContext;
                }

                @Override
                public Collection<ServiceReference<?>> getListeners() {
                    return Collections.emptyList();
                }

                @Override
                public ContextRuntime getContextRuntime() {
                    return cr;
                }

                @Override
                public ServletContextHelperInfo getContextInfo() {
                    return info;
                }
            });
        }
        return new RegistryRuntime(contextRuntimes, failureRuntime.build());
    }
}
