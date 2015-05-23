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
import org.apache.felix.http.base.internal.runtime.dto.FailedDTOHolder;
import org.apache.felix.http.base.internal.runtime.dto.RegistryRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ServletContextDTOBuilder;
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
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
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

    private final FailureStateHandler failureStateHandler = new FailureStateHandler();

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
     *
     * @param handler The context handler
     * @return {@code true} if activation succeeded.
     */
    private boolean activate(final ContextHandler handler)
    {
        if ( !handler.activate() )
        {
            return false;
        }

        this.httpService.registerContext(handler);

        // use a map to sort the listeners
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
                if ( entry.getValue().size() == 1 )
                {
                    this.failureStateHandler.remove(entry.getKey());
                }
            }
        }
        // context listeners first
        for(final ServletContextListenerInfo info : listeners.values())
        {
            final int reason = handler.getListenerRegistry().initialized(info, handler);
            if ( reason != -1 )
            {
                final String type = info.getClass().getSimpleName().substring(0,info.getClass().getSimpleName().length() - 4);
                SystemLogger.debug("Ignoring " + type + " service " + info.getServiceReference());
                this.failureStateHandler.add(info, handler.getContextInfo().getServiceId(), reason);
            }
        }
        // now register services
        for(final WhiteboardServiceInfo<?> info : services)
        {
            this.registerWhiteboardService(handler, info);
        }

        return true;
    }

    /**
     * Deactivate a servlet context.
     *
     * @param handler A context handler
     */
    private void deactivate(final ContextHandler handler)
    {
        // context listeners last but sorted
        final Map<ServiceReference<ServletContextListener>, ServletContextListenerInfo> listeners = new TreeMap<ServiceReference<ServletContextListener>, ServletContextListenerInfo>();
        final Iterator<Map.Entry<WhiteboardServiceInfo<?>, List<ContextHandler>>> i = this.servicesMap.entrySet().iterator();
        while ( i.hasNext() )
        {
            final Map.Entry<WhiteboardServiceInfo<?>, List<ContextHandler>> entry = i.next();
            if ( entry.getValue().remove(handler) )
            {
                if ( !this.failureStateHandler.remove(entry.getKey(), handler.getContextInfo().getServiceId()) )
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
                if ( entry.getValue().isEmpty() )
                {
                    final String type = entry.getKey().getClass().getSimpleName().substring(0, entry.getKey().getClass().getSimpleName().length() - 4);
                    SystemLogger.debug("Ignoring unmatching " + type + " service " + entry.getKey().getServiceReference());
                    this.failureStateHandler.add(entry.getKey(), FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING);
                }
            }
        }
        for(final ServletContextListenerInfo info : listeners.values())
        {
            handler.getListenerRegistry().destroyed(info, handler);
        }

        this.httpService.unregisterContext(handler);

        handler.deactivate();
    }

    /**
     * Add a servlet context helper.
     *
     * @param info The servlet context helper info
     * @return {@code true} if the service matches this http whiteboard service
     */
    public boolean addContextHelper(final ServletContextHelperInfo info)
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

                    // check for activate/deactivate
                    List<ContextHandler> handlerList = this.contextMap.get(info.getName());
                    if ( handlerList == null )
                    {
                        handlerList = new ArrayList<ContextHandler>();
                    }
                    final boolean activate = handlerList.isEmpty() || handlerList.get(0).compareTo(handler) > 0;
                    if ( activate )
                    {
                        // try to activate
                        if ( this.activate(handler) )
                        {
                            handlerList.add(handler);
                            Collections.sort(handlerList);
                            this.contextMap.put(info.getName(), handlerList);

                            // check for deactivate
                            if ( handlerList.size() > 1 )
                            {
                                ContextHandler oldHead = handlerList.get(1);
                                this.deactivate(oldHead);

                                final String type = info.getClass().getSimpleName().substring(0, info.getClass().getSimpleName().length() - 4);
                                SystemLogger.debug("Ignoring shadowed " + type + " service " + info.getServiceReference());
                                this.failureStateHandler.add(oldHead.getContextInfo(), FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
                            }
                        }
                        else
                        {
                            final String type = info.getClass().getSimpleName().substring(0, info.getClass().getSimpleName().length() - 4);
                            SystemLogger.error("Ignoring ungettable " + type + " service " + info.getServiceReference(), null);
                            this.failureStateHandler.add(handler.getContextInfo(), DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE);
                        }
                    }
                    else
                    {
                        handlerList.add(handler);
                        Collections.sort(handlerList);
                        this.contextMap.put(info.getName(), handlerList);

                        final String type = info.getClass().getSimpleName().substring(0, info.getClass().getSimpleName().length() - 4);
                        SystemLogger.debug("Ignoring shadowed " + type + " service " + info.getServiceReference());
                        this.failureStateHandler.add(handler.getContextInfo(), FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
                    }
                }
            }
            else
            {
                final String type = info.getClass().getSimpleName().substring(0, info.getClass().getSimpleName().length() - 4);
                SystemLogger.debug("Ignoring invalid " + type + " service " + info.getServiceReference());
                this.failureStateHandler.add(info, FAILURE_REASON_VALIDATION_FAILED);
            }
            return true;
        }
        return false;
    }

    /**
     * Remove a servlet context helper
     *
     * @param The servlet context helper info
     */
    public void removeContextHelper(final ServletContextHelperInfo info)
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
                        if ( handler.getContextInfo().equals(info) )
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
                        // Try to activate next
                        boolean done = false;
                        while ( !handlerList.isEmpty() && !done)
                        {
                            final ContextHandler newHead = handlerList.get(0);
                            this.failureStateHandler.removeAll(newHead.getContextInfo());

                            if ( this.activate(newHead) )
                            {
                                done = true;
                            }
                            else
                            {
                                handlerList.remove(0);

                                final String type = info.getClass().getSimpleName().substring(0, info.getClass().getSimpleName().length() - 4);
                                SystemLogger.error("Ignoring ungettable " + type + " service " + info.getServiceReference(), null);
                                this.failureStateHandler.add(newHead.getContextInfo(), DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE);
                            }
                        }
                    }
                }
            }
        }
        this.failureStateHandler.removeAll(info);
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
     *
     * @param info Whiteboard service info
     * @return {@code true} if it matches this http service runtime
     */
    public boolean addWhiteboardService(@Nonnull final WhiteboardServiceInfo<?> info)
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
                        final String type = info.getClass().getSimpleName().substring(0, info.getClass().getSimpleName().length() - 4);
                        SystemLogger.debug("Ignoring unmatched " + type + " service " + info.getServiceReference());
                        this.failureStateHandler.add(info, FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING);
                    }
                    else
                    {
                        for(final ContextHandler h : handlerList)
                        {
                            if ( info instanceof ServletContextListenerInfo )
                            {
                                final int reason = h.getListenerRegistry().initialized((ServletContextListenerInfo)info, h);
                                if ( reason != -1 )
                                {
                                    final String type = info.getClass().getSimpleName().substring(0,info.getClass().getSimpleName().length() - 4);
                                    SystemLogger.debug("Ignoring " + type + " service " + info.getServiceReference());
                                    this.failureStateHandler.add(info, h.getContextInfo().getServiceId(), reason);
                                }
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
                this.failureStateHandler.add(info, FAILURE_REASON_VALIDATION_FAILED);
            }
            return true;
        }
        return false;
    }

    /**
     * Remove whiteboard service from the registry.
     *
     * @param info The service id of the whiteboard service
     */
    public void removeWhiteboardService(final WhiteboardServiceInfo<?> info )
    {
        synchronized ( this.contextMap )
        {
            if ( !failureStateHandler.remove(info) )
            {
                final List<ContextHandler> handlerList = this.servicesMap.remove(info);
                if ( handlerList != null )
                {
                    for(final ContextHandler h : handlerList)
                    {
                        if ( !failureStateHandler.remove(info, h.getContextInfo().getServiceId()) )
                        {
                            if ( !(info instanceof ServletContextListenerInfo ) )
                            {
                                this.unregisterWhiteboardService(h, info);
                            }
                            else
                            {
                                h.getListenerRegistry().destroyed((ServletContextListenerInfo)info, h);
                            }
                        }
                    }
                }
            }
            this.failureStateHandler.removeAll(info);
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
            int failureCode = -1;
            if ( info instanceof ServletInfo )
            {
                failureCode = this.httpService.registerServlet(handler, (ServletInfo)info);
            }
            else if ( info instanceof FilterInfo )
            {
                failureCode = this.httpService.registerFilter(handler, (FilterInfo)info);
            }
            else if ( info instanceof ResourceInfo )
            {
                failureCode = this.httpService.registerResource(handler, (ResourceInfo)info);
            }

            else if ( info instanceof ServletContextAttributeListenerInfo )
            {
                failureCode = handler.getListenerRegistry().addListener((ServletContextAttributeListenerInfo) info);
            }
            else if ( info instanceof HttpSessionListenerInfo )
            {
                failureCode = handler.getListenerRegistry().addListener((HttpSessionListenerInfo) info);
            }
            else if ( info instanceof HttpSessionAttributeListenerInfo )
            {
                failureCode = handler.getListenerRegistry().addListener((HttpSessionAttributeListenerInfo) info);
            }
            else if ( info instanceof HttpSessionIdListenerInfo )
            {
                failureCode = handler.getListenerRegistry().addListener((HttpSessionIdListenerInfo) info);
            }
            else if ( info instanceof ServletRequestListenerInfo )
            {
                failureCode = handler.getListenerRegistry().addListener((ServletRequestListenerInfo) info);
            }
            else if ( info instanceof ServletRequestAttributeListenerInfo )
            {
                failureCode = handler.getListenerRegistry().addListener((ServletRequestAttributeListenerInfo) info);
            }
            else
            {
                // This should never happen, but we log anyway
                SystemLogger.error("Unknown whiteboard service " + info.getServiceReference(), null);
            }
            if ( failureCode != -1 )
            {
                final String type = info.getClass().getSimpleName().substring(0,info.getClass().getSimpleName().length() - 4);
                SystemLogger.debug("Ignoring " + type + " service " + info.getServiceReference());
                this.failureStateHandler.add(info, handler.getContextInfo().getServiceId(), failureCode);
            }
        }
        catch (final Exception e)
        {
            this.failureStateHandler.add(info, handler.getContextInfo().getServiceId(), FAILURE_REASON_UNKNOWN);
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
        catch (final Exception e)
        {
            SystemLogger.error("Exception while unregistering whiteboard service " + info.getServiceReference(), e);
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

    private ContextHandler getContextHandler(final Long contextId)
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

    public RegistryRuntime getRuntime(final HandlerRegistry registry)
    {
        final FailedDTOHolder failedDTOHolder = new FailedDTOHolder();

        final Collection<ServletContextDTO> contextDTOs = new ArrayList<ServletContextDTO>();
        // add the context for the http service
        final ServletContextHelperInfo info = new ServletContextHelperInfo(Integer.MAX_VALUE,
                HttpServiceFactory.HTTP_SERVICE_CONTEXT_SERVICE_ID,
                HttpServiceFactory.HTTP_SERVICE_CONTEXT_NAME, "/", null);
        final ServletContextDTO dto = ServletContextDTOBuilder.build(info, webContext, -1);
        if ( registry.getRuntime(dto, failedDTOHolder) )
        {
            contextDTOs.add(dto);
        }

        // get sort list of context handlers
        final List<ContextHandler> contextHandlerList = new ArrayList<ContextHandler>();
        synchronized ( this.contextMap )
        {
            for (final List<ContextHandler> list : this.contextMap.values())
            {
                if ( !list.isEmpty() )
                {
                    contextHandlerList.add(list.get(0));
                }
            }
            this.failureStateHandler.getRuntime(failedDTOHolder);
        }
        Collections.sort(contextHandlerList);

        for (final ContextHandler handler : contextHandlerList)
        {
            final ServletContextDTO scDTO = ServletContextDTOBuilder.build(handler.getContextInfo(), handler.getSharedContext(), -1);

            if ( registry.getRuntime(scDTO, failedDTOHolder) )
            {
                handler.getListenerRegistry().getRuntime(scDTO);
                contextDTOs.add(scDTO);
            }
        }

        return new RegistryRuntime(failedDTOHolder, contextDTOs);
    }
}
