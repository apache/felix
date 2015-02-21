/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.whiteboard;

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
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;

import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.AbstractInfo;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.HandlerRuntime;
import org.apache.felix.http.base.internal.runtime.ListenerInfo;
import org.apache.felix.http.base.internal.runtime.RegistryRuntime;
import org.apache.felix.http.base.internal.runtime.ResourceInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.WhiteboardServiceInfo;
import org.apache.felix.http.base.internal.util.MimeTypes;
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
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public final class ServletContextHelperManager
{
    /** A map containing all servlet context registrations. Mapped by context name */
    private final Map<String, List<ContextHandler>> contextMap = new HashMap<String, List<ContextHandler>>();

    /** A map with all servlet/filter registrations, mapped by abstract info. */
    private final Map<WhiteboardServiceInfo<?>, List<ContextHandler>> servicesMap = new HashMap<WhiteboardServiceInfo<?>, List<ContextHandler>>();

    private final WhiteboardHttpService httpService;

    private final ListenerRegistry listenerRegistry;

    private final BundleContext bundleContext;

    private final Set<AbstractInfo<?>> invalidRegistrations = new ConcurrentSkipListSet<AbstractInfo<?>>();

    private volatile ServletContext webContext;

    private volatile ServiceReference<HttpServiceRuntime> httpServiceRuntime;

    private volatile ServiceRegistration<ServletContextHelper> defaultContextRegistration;

    /**
     * Create a new servlet context helper manager
     * and the default context
     */
    public ServletContextHelperManager(final BundleContext bundleContext,
            final WhiteboardHttpService httpService,
            final ListenerRegistry listenerRegistry)
    {
        this.bundleContext = bundleContext;
        this.httpService = httpService;
        this.listenerRegistry = listenerRegistry;
    }

    public void start(ServletContext webContext, ServiceReference<HttpServiceRuntime> httpServiceRuntime)
    {
        this.webContext = webContext;
        this.httpServiceRuntime = httpServiceRuntime;

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
    }

    /**
     * Clean up the instance
     */
    public void close()
    {
        // TODO cleanup
        if (this.defaultContextRegistration != null)
        {
            this.defaultContextRegistration.unregister();
            this.defaultContextRegistration = null;
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
            }
        }
        // context listeners first
        for(final ServletContextListenerInfo info : listeners.values())
        {
            this.listenerRegistry.initialized(info, handler);
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
            this.listenerRegistry.destroyed(info, handler);
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
                    PerContextEventListener contextEventListener = listenerRegistry.addContext(info);
                    ContextHandler handler = new ContextHandler(info,
                            this.webContext,
                            contextEventListener,
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
                            this.deactivate(handlerList.get(1));
                        }
                        this.activate(handler);
                    }
                }
            }
            else
            {
                final String type = info.getClass().getSimpleName().substring(0, info.getClass().getSimpleName().length() - 4);
                SystemLogger.debug("Ignoring " + type + " service " + info.getServiceReference());
                this.invalidRegistrations.add(info);
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
                            this.activate(handlerList.get(0));
                        }
                        listenerRegistry.removeContext(info);
                    }
                }
            }
            else
            {
                this.invalidRegistrations.remove(info);
            }
        }
    }

    /**
     * Find the list of matching contexts for the whiteboard service
     */
    private List<ContextHandler> getMatchingContexts(final WhiteboardServiceInfo<?> info)
    {
        final List<ContextHandler> result = new ArrayList<ContextHandler>();
        for(final List<ContextHandler> handlerList : this.contextMap.values()) {
            final ContextHandler h = handlerList.get(0);
            if ( info.getContextSelectionFilter().match(h.getContextInfo().getServiceReference()) )
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
                    for(final ContextHandler h : handlerList)
                    {
                        this.registerWhiteboardService(h, info);
                    }
                }
            }
            else
            {
                final String type = info.getClass().getSimpleName().substring(0, info.getClass().getSimpleName().length() - 4);
                SystemLogger.debug("Ignoring " + type + " service " + info.getServiceReference());
                this.invalidRegistrations.add(info);
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
                            this.unregisterWhiteboardService(h, info);
                        }
                    }
                }
            }
            else
            {
                this.invalidRegistrations.remove(info);
            }
        }
    }

    /**
     * Register whiteboard service in the http service
     * @param contextInfo Context info
     * @param info Whiteboard service info
     */
    private void registerWhiteboardService(final ContextHandler handler, final WhiteboardServiceInfo<?> info)
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
        else if ( info instanceof ListenerInfo )
        {
            this.listenerRegistry.addListener((ListenerInfo<?>)info, handler);
        }
    }

    /**
     * Unregister whiteboard service from the http service
     * @param contextInfo Context info
     * @param info Whiteboard service info
     */
    private void unregisterWhiteboardService(final ContextHandler handler, final WhiteboardServiceInfo<?> info)
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
        else if ( info instanceof ListenerInfo )
        {
            this.listenerRegistry.removeListener((ListenerInfo<?>)info, handler);
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
                return f.match(this.httpServiceRuntime);
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

    public RegistryRuntime getRuntime(HandlerRegistry registry)
    {
        List<HandlerRuntime> handlerRuntimes;
        Map<Long, Collection<ServiceReference<?>>> listenerRuntimes;
        Set<ContextHandler> contextHandlers = new TreeSet<ContextHandler>();
        synchronized ( this.contextMap )
        {
            for (List<ContextHandler> contextHandlerList : this.contextMap.values())
            {
                if ( !contextHandlerList.isEmpty() )
                {
                    contextHandlers.add(contextHandlerList.get(0));
                }
            }
            handlerRuntimes = registry.getRuntime();
            listenerRuntimes = listenerRegistry.getContextRuntimes();
        }
        return new RegistryRuntime(contextHandlers, handlerRuntimes, listenerRuntimes);
    }
}
