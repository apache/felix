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

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.apache.felix.http.base.internal.console.HttpServicePlugin;
import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.HttpServiceServletHandler;
import org.apache.felix.http.base.internal.handler.HttpSessionWrapper;
import org.apache.felix.http.base.internal.handler.ListenerHandler;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.handler.WhiteboardFilterHandler;
import org.apache.felix.http.base.internal.handler.WhiteboardListenerHandler;
import org.apache.felix.http.base.internal.handler.WhiteboardServletHandler;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.registry.EventListenerRegistry;
import org.apache.felix.http.base.internal.registry.HandlerRegistry;
import org.apache.felix.http.base.internal.runtime.AbstractInfo;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ListenerInfo;
import org.apache.felix.http.base.internal.runtime.ResourceInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.WhiteboardServiceInfo;
import org.apache.felix.http.base.internal.runtime.dto.FailedDTOHolder;
import org.apache.felix.http.base.internal.runtime.dto.RegistryRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ServletContextDTOBuilder;
import org.apache.felix.http.base.internal.service.HttpServiceFactory;
import org.apache.felix.http.base.internal.service.HttpServiceRuntimeImpl;
import org.apache.felix.http.base.internal.service.ResourceServlet;
import org.apache.felix.http.base.internal.whiteboard.tracker.FilterTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ListenersTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ResourceTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletContextHelperTracker;
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
    /** The bundle context of the http bundle. */
    private final BundleContext httpBundleContext;

    /** The http service factory. */
    private final HttpServiceFactory httpServiceFactory;

    private final HttpServiceRuntimeImpl serviceRuntime;

    private final List<ServiceTracker<?, ?>> trackers = new ArrayList<ServiceTracker<?, ?>>();

    private final HttpServicePlugin plugin;

    /** A map containing all servlet context registrations. Mapped by context name */
    private final Map<String, List<WhiteboardContextHandler>> contextMap = new HashMap<String, List<WhiteboardContextHandler>>();

    /** A map with all servlet/filter registrations, mapped by abstract info. */
    private final Map<WhiteboardServiceInfo<?>, List<WhiteboardContextHandler>> servicesMap = new HashMap<WhiteboardServiceInfo<?>, List<WhiteboardContextHandler>>();

    private final HandlerRegistry registry;

    private final FailureStateHandler failureStateHandler = new FailureStateHandler();

    private volatile ServletContext webContext;

    private volatile ServiceRegistration<ServletContextHelper> defaultContextRegistration;

    private volatile ServiceRegistration<HttpServiceRuntime> runtimeServiceReg;

    /**
     * Create a new whiteboard http manager
     *
     * @param bundleContext The bundle context of the http bundle
     * @param httpServiceFactory The http service factory
     * @param registry The handler registry
     */
    public WhiteboardManager(final BundleContext bundleContext,
            final HttpServiceFactory httpServiceFactory,
            final HandlerRegistry registry)
    {
        this.httpBundleContext = bundleContext;
        this.httpServiceFactory = httpServiceFactory;
        this.registry = registry;
        this.serviceRuntime = new HttpServiceRuntimeImpl(registry, this);
        this.plugin = new HttpServicePlugin(bundleContext, this.serviceRuntime);
    }

    /**
     * Start the whiteboard manager
     * @param containerContext The servlet context
     */
    public void start(final ServletContext containerContext)
    {
        this.serviceRuntime.setAttribute(HttpServiceRuntimeConstants.HTTP_SERVICE_ID,
                Collections.singletonList(this.httpServiceFactory.getHttpServiceServiceId()));
        this.runtimeServiceReg = this.httpBundleContext.registerService(HttpServiceRuntime.class,
                serviceRuntime,
                this.serviceRuntime.getAttributes());
        this.serviceRuntime.setServiceReference(this.runtimeServiceReg.getReference());

        this.webContext = containerContext;

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME);
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
        props.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);

        // add context for http service
        final List<WhiteboardContextHandler> list = new ArrayList<WhiteboardContextHandler>();
        final ServletContextHelperInfo info = new ServletContextHelperInfo(Integer.MAX_VALUE,
                HttpServiceFactory.HTTP_SERVICE_CONTEXT_SERVICE_ID,
                HttpServiceFactory.HTTP_SERVICE_CONTEXT_NAME, "/", null);
        list.add(new HttpServiceContextHandler(info, registry.getRegistry(HttpServiceFactory.HTTP_SERVICE_CONTEXT_SERVICE_ID),
                httpServiceFactory, webContext, this.httpBundleContext.getBundle()));
        this.contextMap.put(HttpServiceFactory.HTTP_SERVICE_CONTEXT_NAME, list);

        this.defaultContextRegistration = httpBundleContext.registerService(
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
                            // nothing to override
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
        addTracker(new FilterTracker(this.httpBundleContext, this));
        addTracker(new ListenersTracker(this.httpBundleContext, this));
        addTracker(new ResourceTracker(this.httpBundleContext, this));
        addTracker(new ServletContextHelperTracker(this.httpBundleContext, this));
        addTracker(new ServletTracker(this.httpBundleContext, this));

        this.plugin.register();
    }

    /**
     * Add a tracker and start it
     * @param tracker The tracker instance
     */
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

        this.contextMap.clear();
        this.servicesMap.clear();
        this.failureStateHandler.clear();
        this.registry.reset();

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
        this.webContext = null;
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
            final WhiteboardContextHandler handler = this.getContextHandler(contextId);
            if ( handler != null )
            {
                final ExtServletContext context = handler.getServletContext(this.httpBundleContext.getBundle());
                new HttpSessionWrapper(contextId, session, context, true).invalidate();
                handler.ungetServletContext(this.httpBundleContext.getBundle());
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
            final WhiteboardContextHandler handler = this.getContextHandler(contextId);
            if ( handler != null )
            {
                handler.getRegistry().getEventListenerRegistry().sessionIdChanged(event, oldSessionId);
            }
        }
    }

    /**
     * Activate a servlet context helper.
     *
     * @param handler The context handler
     * @return {@code true} if activation succeeded.
     */
    private boolean activate(final WhiteboardContextHandler handler)
    {
        if ( !handler.activate(this.registry) )
        {
            return false;
        }

        final List<WhiteboardServiceInfo<?>> services = new ArrayList<WhiteboardServiceInfo<?>>();
        for(final Map.Entry<WhiteboardServiceInfo<?>, List<WhiteboardContextHandler>> entry : this.servicesMap.entrySet())
        {
            if ( entry.getKey().getContextSelectionFilter().match(handler.getContextInfo().getServiceReference()) )
            {
                entry.getValue().add(handler);
                if ( entry.getValue().size() == 1 )
                {
                    this.failureStateHandler.remove(entry.getKey());
                }
                if ( entry.getKey() instanceof ListenerInfo && ((ListenerInfo)entry.getKey()).isListenerType(ServletContextListener.class.getName()) )
                {
                    // servlet context listeners will be registered directly
                    this.registerWhiteboardService(handler, entry.getKey());
                }
                else
                {
                    // registration of other services will be delayed
                    services.add(entry.getKey());
                }
            }
        }
        // notify context listeners first
        handler.getRegistry().getEventListenerRegistry().contextInitialized();

        // register services
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
    private void deactivate(final WhiteboardContextHandler handler)
    {
        // services except context listeners first
        final List<WhiteboardServiceInfo<?>> listeners = new ArrayList<WhiteboardServiceInfo<?>>();
        final Iterator<Map.Entry<WhiteboardServiceInfo<?>, List<WhiteboardContextHandler>>> i = this.servicesMap.entrySet().iterator();
        while ( i.hasNext() )
        {
            final Map.Entry<WhiteboardServiceInfo<?>, List<WhiteboardContextHandler>> entry = i.next();
            if ( entry.getValue().remove(handler) )
            {
                if ( !this.failureStateHandler.remove(entry.getKey(), handler.getContextInfo().getServiceId()) )
                {
                    if ( entry.getKey() instanceof ListenerInfo && ((ListenerInfo)entry.getKey()).isListenerType(ServletContextListener.class.getName()) )
                    {
                        listeners.add(entry.getKey());
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
        // context listeners last
        handler.getRegistry().getEventListenerRegistry().contextDestroyed();
        for(final WhiteboardServiceInfo<?> info : listeners)
        {
            this.unregisterWhiteboardService(handler, info);
        }

        handler.deactivate(this.registry);
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
                    final WhiteboardContextHandler handler = new WhiteboardContextHandler(info,
                            this.webContext,
                            this.httpBundleContext.getBundle());

                    // check for activate/deactivate
                    List<WhiteboardContextHandler> handlerList = this.contextMap.get(info.getName());
                    if ( handlerList == null )
                    {
                        handlerList = new ArrayList<WhiteboardContextHandler>();
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
                                final WhiteboardContextHandler oldHead = handlerList.get(1);
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
                final List<WhiteboardContextHandler> handlerList = this.contextMap.get(info.getName());
                if ( handlerList != null )
                {
                    final Iterator<WhiteboardContextHandler> i = handlerList.iterator();
                    boolean first = true;
                    boolean activateNext = false;
                    while ( i.hasNext() )
                    {
                        final WhiteboardContextHandler handler = i.next();
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
                            final WhiteboardContextHandler newHead = handlerList.get(0);
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
    private List<WhiteboardContextHandler> getMatchingContexts(final WhiteboardServiceInfo<?> info)
    {
        final List<WhiteboardContextHandler> result = new ArrayList<WhiteboardContextHandler>();
        for(final List<WhiteboardContextHandler> handlerList : this.contextMap.values())
        {
            final WhiteboardContextHandler h = handlerList.get(0);
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
            if ( visible )
            {
                if ( h.getContextInfo().getServiceReference() != null )
                {
                    if ( info.getContextSelectionFilter().match(h.getContextInfo().getServiceReference()) )
                    {
                        result.add(h);
                    }
                }
                else
                {
                    final Map<String, String> props = new HashMap<String, String>();
                    props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, h.getContextInfo().getName());
                    props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, h.getContextInfo().getPath());
                    if ( info.getContextSelectionFilter().matches(props) )
                    {
                        result.add(h);
                    }
                }
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
                    final List<WhiteboardContextHandler> handlerList = this.getMatchingContexts(info);
                    this.servicesMap.put(info, handlerList);
                    if (handlerList.isEmpty())
                    {
                        final String type = info.getClass().getSimpleName().substring(0, info.getClass().getSimpleName().length() - 4);
                        SystemLogger.debug("Ignoring unmatched " + type + " service " + info.getServiceReference());
                        this.failureStateHandler.add(info, FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING);
                    }
                    else
                    {
                        for(final WhiteboardContextHandler h : handlerList)
                        {
                            this.registerWhiteboardService(h, info);
                            if ( info instanceof ListenerInfo && ((ListenerInfo)info).isListenerType(ServletContextListener.class.getName()) )
                            {
                                final ListenerHandler handler = h.getRegistry().getEventListenerRegistry().getServletContextListener((ListenerInfo)info);
                                if ( handler != null )
                                {
                                    final ServletContextListener listener = (ServletContextListener)handler.getListener();
                                    if ( listener != null )
                                    {
                                        EventListenerRegistry.contextInitialized(handler.getListenerInfo(), listener, new ServletContextEvent(handler.getContext()));
                                    }
                                }
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
                final List<WhiteboardContextHandler> handlerList = this.servicesMap.remove(info);
                if ( handlerList != null )
                {
                    for(final WhiteboardContextHandler h : handlerList)
                    {
                        if ( !failureStateHandler.remove(info, h.getContextInfo().getServiceId()) )
                        {
                            if ( info instanceof ListenerInfo && ((ListenerInfo)info).isListenerType(ServletContextListener.class.getName()) )
                            {
                                final ListenerHandler handler = h.getRegistry().getEventListenerRegistry().getServletContextListener((ListenerInfo)info);
                                if ( handler != null )
                                {
                                    final ServletContextListener listener = (ServletContextListener) handler.getListener();
                                    if ( listener != null )
                                    {
                                        EventListenerRegistry.contextDestroyed(handler.getListenerInfo(), listener, new ServletContextEvent(handler.getContext()));
                                    }
                                }
                            }
                            this.unregisterWhiteboardService(h, info);
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
    private void registerWhiteboardService(final WhiteboardContextHandler handler, final WhiteboardServiceInfo<?> info)
    {
        try
        {
            int failureCode = -1;
            if ( info instanceof ServletInfo )
            {
                final ExtServletContext servletContext = handler.getServletContext(info.getServiceReference().getBundle());
                if ( servletContext == null )
                {
                    failureCode = DTOConstants.FAILURE_REASON_SERVLET_CONTEXT_FAILURE;
                }
                else
                {
                    final ServletHandler servletHandler = new WhiteboardServletHandler(
                        handler.getContextInfo().getServiceId(),
                        servletContext,
                        (ServletInfo)info,
                        handler.getBundleContext());
                    handler.getRegistry().registerServlet(servletHandler);
                }
            }
            else if ( info instanceof FilterInfo )
            {
                final ExtServletContext servletContext = handler.getServletContext(info.getServiceReference().getBundle());
                if ( servletContext == null )
                {
                    failureCode = DTOConstants.FAILURE_REASON_SERVLET_CONTEXT_FAILURE;
                }
                else
                {
                    final FilterHandler filterHandler = new WhiteboardFilterHandler(
                            handler.getContextInfo().getServiceId(),
                            servletContext,
                            (FilterInfo)info,
                            handler.getBundleContext());
                    handler.getRegistry().registerFilter(filterHandler);
                }
            }
            else if ( info instanceof ResourceInfo )
            {
                final ServletInfo servletInfo = ((ResourceInfo)info).getServletInfo();
                final ExtServletContext servletContext = handler.getServletContext(info.getServiceReference().getBundle());
                if ( servletContext == null )
                {
                    failureCode = DTOConstants.FAILURE_REASON_SERVLET_CONTEXT_FAILURE;
                }
                else
                {
                    final ServletHandler servleHandler = new HttpServiceServletHandler(
                            handler.getContextInfo().getServiceId(),
                            servletContext,
                            servletInfo,
                            new ResourceServlet(servletInfo.getPrefix()));
                    handler.getRegistry().registerServlet(servleHandler);
                }
            }

            else if ( info instanceof ListenerInfo )
            {
                final ExtServletContext servletContext = handler.getServletContext(info.getServiceReference().getBundle());
                if ( servletContext == null )
                {
                    failureCode = DTOConstants.FAILURE_REASON_SERVLET_CONTEXT_FAILURE;
                }
                else
                {
                    final ListenerHandler listenerHandler = new WhiteboardListenerHandler(
                            handler.getContextInfo().getServiceId(),
                            servletContext,
                            (ListenerInfo)info,
                            handler.getBundleContext());
                    handler.getRegistry().registerListeners(listenerHandler);
                }
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
    private void unregisterWhiteboardService(final WhiteboardContextHandler handler, final WhiteboardServiceInfo<?> info)
    {
        try
        {
            if ( info instanceof ServletInfo )
            {
                handler.getRegistry().unregisterServlet((ServletInfo)info, true);
                handler.ungetServletContext(info.getServiceReference().getBundle());
            }
            else if ( info instanceof FilterInfo )
            {
                handler.getRegistry().unregisterFilter((FilterInfo)info, true);
                handler.ungetServletContext(info.getServiceReference().getBundle());
            }
            else if ( info instanceof ResourceInfo )
            {
                handler.getRegistry().unregisterServlet(((ResourceInfo)info).getServletInfo(), true);
                handler.ungetServletContext(info.getServiceReference().getBundle());
            }

            else if ( info instanceof ListenerInfo )
            {
                handler.getRegistry().unregisterListeners((ListenerInfo) info);
                handler.ungetServletContext(info.getServiceReference().getBundle());
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
                final Filter f = this.httpBundleContext.createFilter(target);
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

    private WhiteboardContextHandler getContextHandler(final Long contextId)
    {
        synchronized ( this.contextMap )
        {
            for(final List<WhiteboardContextHandler> handlerList : this.contextMap.values())
            {
                final WhiteboardContextHandler h = handlerList.get(0);
                if ( h.getContextInfo().getServiceId() == contextId )
                {
                    return h;
                }
            }
        }
        return null;
    }

    public RegistryRuntime getRuntimeInfo()
    {
        final FailedDTOHolder failedDTOHolder = new FailedDTOHolder();

        final Collection<ServletContextDTO> contextDTOs = new ArrayList<ServletContextDTO>();
/*
        // add the context for the http service
        final ServletContextHelperInfo info = new ServletContextHelperInfo(Integer.MAX_VALUE,
                HttpServiceFactory.HTTP_SERVICE_CONTEXT_SERVICE_ID,
                HttpServiceFactory.HTTP_SERVICE_CONTEXT_NAME, "/", null);
        final ServletContextDTO dto = ServletContextDTOBuilder.build(info, webContext, -1);
        if ( registry.getRuntimeInfo(dto, failedDTOHolder) )
        {
            contextDTOs.add(dto);
        }
*/
        // get sort list of context handlers
        final List<WhiteboardContextHandler> contextHandlerList = new ArrayList<WhiteboardContextHandler>();
        synchronized ( this.contextMap )
        {
            for (final List<WhiteboardContextHandler> list : this.contextMap.values())
            {
                if ( !list.isEmpty() )
                {
                    contextHandlerList.add(list.get(0));
                }
            }
            this.failureStateHandler.getRuntimeInfo(failedDTOHolder);
        }
        Collections.sort(contextHandlerList);

        for (final WhiteboardContextHandler handler : contextHandlerList)
        {
            final ServletContextDTO scDTO = ServletContextDTOBuilder.build(handler.getContextInfo(), handler.getSharedContext(), -1);

            if ( registry.getRuntimeInfo(scDTO, failedDTOHolder) )
            {
                contextDTOs.add(scDTO);
            }
        }

        return new RegistryRuntime(failedDTOHolder, contextDTOs);
    }
}
