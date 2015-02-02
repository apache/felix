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
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ResourceInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextAttributeListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.WhiteboardServiceInfo;
import org.apache.felix.http.base.internal.util.MimeTypes;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public final class ServletContextHelperManager
{
    /** A map containing all servlet context registrations. Mapped by context name */
    private final Map<String, List<ContextHandler>> contextMap = new HashMap<String, List<ContextHandler>>();

    /** A map with all servlet/filter registrations, mapped by abstract info. */
    private final Map<WhiteboardServiceInfo<?>, List<ContextHandler>> servicesMap = new HashMap<WhiteboardServiceInfo<?>, List<ContextHandler>>();

    private final WhiteboardHttpService httpService;

    private final ServiceRegistration<ServletContextHelper> defaultContextRegistration;

    private final ServletContext webContext;

    private final Bundle bundle;

    /**
     * Create a new servlet context helper manager
     * and the default context
     */
    public ServletContextHelperManager(final BundleContext bundleContext, final ServletContext webContext, final WhiteboardHttpService httpService)
    {
        this.httpService = httpService;
        this.webContext = webContext;
        this.bundle = bundleContext.getBundle();

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME);
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
        props.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);

        this.defaultContextRegistration = bundleContext.registerService(ServletContextHelper.class,
                new ServiceFactory<ServletContextHelper>() {

                    @Override
                    public ServletContextHelper getService(
                            final Bundle bundle,
                            final ServiceRegistration<ServletContextHelper> registration) {
                        return new ServletContextHelper(bundle) {

                            @Override
                            public String getMimeType(final String file) {
                                return MimeTypes.get().getByFile(file);
                            }
                        };
                    }

                    @Override
                    public void ungetService(
                            final Bundle bundle,
                            final ServiceRegistration<ServletContextHelper> registration,
                            final ServletContextHelper service) {
                        // nothing to do
                    }
                },
                props);
    }

    /**
     * Clean up the instance
     */
    public void close()
    {
        // TODO cleanup

        this.defaultContextRegistration.unregister();
    }

    /**
     * Activate a servlet context helper.
     * @param contextInfo A context info
     */
    private void activate(final ContextHandler handler)
    {
        handler.activate();
        // context listeners first
        final List<WhiteboardServiceInfo<?>> services = new ArrayList<WhiteboardServiceInfo<?>>();
        for(final Map.Entry<WhiteboardServiceInfo<?>, List<ContextHandler>> entry : this.servicesMap.entrySet())
        {
            if ( entry.getKey().getContextSelectionFilter().match(handler.getContextInfo().getServiceReference()) )
            {
                entry.getValue().add(handler);
                if ( entry.getKey() instanceof ServletContextListenerInfo )
                {
                    handler.initialized((ServletContextListenerInfo)entry.getKey());
                }
                else
                {
                    services.add(entry.getKey());
                }
            }
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
        final List<ServletContextListenerInfo> listeners = new ArrayList<ServletContextListenerInfo>();
        final Iterator<Map.Entry<WhiteboardServiceInfo<?>, List<ContextHandler>>> i = this.servicesMap.entrySet().iterator();
        while ( i.hasNext() )
        {
            final Map.Entry<WhiteboardServiceInfo<?>, List<ContextHandler>> entry = i.next();
            if ( entry.getValue().remove(handler) )
            {
                if ( entry.getKey() instanceof ServletContextListenerInfo )
                {
                    listeners.add((ServletContextListenerInfo)entry.getKey());
                }
                else
                {
                    this.unregisterWhiteboardService(handler, entry.getKey());
                }
                if ( entry.getValue().isEmpty() ) {
                    i.remove();
                }
            }
        }
        for(final ServletContextListenerInfo info : listeners)
        {
            handler.destroyed(info);
        }
        handler.deactivate();
    }

    /**
     * Add a servlet context helper.
     */
    public void addContextHelper(final ServletContextHelperInfo info)
    {
        if ( info.isValid() )
        {
            final ContextHandler handler = new ContextHandler(info, this.webContext, this.bundle);
            synchronized ( this.contextMap )
            {
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
            // TODO - failure DTO
            final String type = info.getClass().getSimpleName().substring(0, info.getClass().getSimpleName().length() - 4);
            SystemLogger.debug("Ignoring " + type + " service " + info.getServiceReference());
        }
    }

    /**
     * Remove a servlet context helper
     */
    public void removeContextHelper(final ServletContextHelperInfo info)
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
            // TODO - failure DTO
            final String type = info.getClass().getSimpleName().substring(0, info.getClass().getSimpleName().length() - 4);
            SystemLogger.debug("Ignoring " + type + " service " + info.getServiceReference());
        }
    }

    /**
     * Remove whiteboard service from the registry
     * @param info Whiteboard service info
     */
    public void removeWhiteboardService(@Nonnull final WhiteboardServiceInfo<?> info)
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
        else if ( info instanceof ServletContextAttributeListenerInfo )
        {
            handler.addListener((ServletContextAttributeListenerInfo)info );
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
        else if ( info instanceof ServletContextAttributeListenerInfo )
        {
            handler.removeListener((ServletContextAttributeListenerInfo)info );
        }
    }
}
