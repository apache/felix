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

import org.apache.felix.http.base.internal.runtime.AbstractInfo;
import org.apache.felix.http.base.internal.runtime.ContextInfo;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ResourceInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.service.InternalHttpService;
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
    private final Map<String, List<ContextInfo>> contextMap = new HashMap<String, List<ContextInfo>>();

    /** A map with all servlet/filter registrations, mapped by abstract info. */
    private final Map<AbstractInfo<?>, List<ContextInfo>> servicesMap = new HashMap<AbstractInfo<?>, List<ContextInfo>>();

    private final InternalHttpService httpService;

    private final ServiceRegistration<ServletContextHelper> defaultContextRegistration;

    /**
     * Create a new servlet context helper manager
     * and the default context
     */
    public ServletContextHelperManager(final BundleContext bundleContext, final InternalHttpService httpService)
    {
        this.httpService = httpService;

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME);
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
        props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);

        this.defaultContextRegistration = bundleContext.registerService(ServletContextHelper.class,
                new ServiceFactory<ServletContextHelper>() {

                    @Override
                    public ServletContextHelper getService(
                            final Bundle bundle,
                            final ServiceRegistration<ServletContextHelper> registration) {
                        return new ServletContextHelper(bundle) {
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

    public void close()
    {
        // TODO cleanup

        this.defaultContextRegistration.unregister();
    }

    private void activate(final ContextInfo holder)
    {
        for(final Map.Entry<AbstractInfo<?>, List<ContextInfo>> entry : this.servicesMap.entrySet())
        {
            if ( entry.getKey().getContextSelectionFilter().match(holder.getServiceReference()) )
            {
                entry.getValue().add(holder);
                if ( entry.getKey() instanceof ServletInfo )
                {
                    this.registerServlet((ServletInfo)entry.getKey(), holder);
                }
                else if ( entry.getKey() instanceof FilterInfo )
                {
                    this.registerFilter((FilterInfo)entry.getKey(), holder);
                }
                else if ( entry.getKey() instanceof ResourceInfo )
                {
                    this.registerResource((ResourceInfo)entry.getKey(), holder);
                }
            }
        }
    }

    private void deactivate(final ContextInfo holder)
    {
        final Iterator<Map.Entry<AbstractInfo<?>, List<ContextInfo>>> i = this.servicesMap.entrySet().iterator();
        while ( i.hasNext() )
        {
            final Map.Entry<AbstractInfo<?>, List<ContextInfo>> entry = i.next();
            if ( entry.getValue().remove(holder) )
            {
                if ( entry.getKey() instanceof ServletInfo )
                {
                    this.unregisterServlet((ServletInfo)entry.getKey(), holder);
                }
                else if ( entry.getKey() instanceof FilterInfo )
                {
                    this.unregisterFilter((FilterInfo)entry.getKey(), holder);
                }
                else if ( entry.getKey() instanceof ResourceInfo )
                {
                    this.unregisterResource((ResourceInfo)entry.getKey(), holder);
                }
                if ( entry.getValue().isEmpty() ) {
                    i.remove();
                }
            }
        }
    }

    /**
     * Add a servlet context helper.
     */
    public void addContextHelper(final ContextInfo info)
    {
        synchronized ( this.contextMap )
        {
            List<ContextInfo> holderList = this.contextMap.get(info.getName());
            if ( holderList == null )
            {
                holderList = new ArrayList<ContextInfo>();
                this.contextMap.put(info.getName(), holderList);
            }
            holderList.add(info);
            Collections.sort(holderList);
            // check for activate/deactivate
            if ( holderList.get(0) == info )
            {
                // check for deactivate
                if ( holderList.size() > 1 )
                {
                    this.deactivate(holderList.get(1));
                }
                this.activate(info);
            }
        }
    }

    /**
     * Remove a servlet context helper
     */
    public void removeContextHelper(final ContextInfo info)
    {
        synchronized ( this.contextMap )
        {
            final List<ContextInfo> holderList = this.contextMap.get(info.getName());
            if ( holderList != null )
            {
                final Iterator<ContextInfo> i = holderList.iterator();
                boolean first = true;
                boolean activateNext = false;
                while ( i.hasNext() )
                {
                    final ContextInfo holder = i.next();
                    if ( holder.compareTo(info) == 0 )
                    {
                        i.remove();
                        // check for deactivate
                        if ( first )
                        {
                            this.deactivate(holder);
                            activateNext = true;
                        }
                        break;
                    }
                    first = false;
                }
                if ( holderList.isEmpty() )
                {
                    this.contextMap.remove(info.getName());
                }
                else if ( activateNext )
                {
                    this.activate(holderList.get(0));
                }
            }
        }
    }

    private List<ContextInfo> getMatchingContexts(final AbstractInfo<?> info)
    {
        final List<ContextInfo> result = new ArrayList<ContextInfo>();
        for(final List<ContextInfo> holders : this.contextMap.values()) {
            final ContextInfo h = holders.get(0);
            if ( info.getContextSelectionFilter().match(h.getServiceReference()) )
            {
                result.add(h);
            }
        }
        return result;
    }

    private void registerServlet(final ServletInfo servletInfo, final ContextInfo holder)
    {
        this.httpService.registerServlet(holder, servletInfo);
    }

    private void unregisterServlet(final ServletInfo servletInfo, final ContextInfo holder)
    {
        this.httpService.unregisterServlet(holder, servletInfo);
    }

    private void registerFilter(final FilterInfo filterInfo, final ContextInfo holder)
    {
        this.httpService.registerFilter(holder, filterInfo);
    }

    private void unregisterFilter(final FilterInfo filterInfo, final ContextInfo holder)
    {
        this.httpService.unregisterFilter(holder, filterInfo);
    }

    private void registerResource(final ResourceInfo resourceInfo, final ContextInfo holder)
    {
        this.httpService.registerResource(holder, resourceInfo);
    }

    private void unregisterResource(final ResourceInfo resourceInfo, final ContextInfo holder)
    {
        this.httpService.unregisterResource(holder, resourceInfo);
    }

    /**
     * Add a new servlet.
     * @param servletInfo The servlet info
     */
    public void addServlet(final ServletInfo servletInfo)
    {
        synchronized ( this.contextMap )
        {
            final List<ContextInfo> holderList = this.getMatchingContexts(servletInfo);
            this.servicesMap.put(servletInfo, holderList);
            for(final ContextInfo h : holderList)
            {
            	this.registerServlet(servletInfo, h);
            }
        }
    }

    /**
     * Remove a servlet
     * @param servletInfo The servlet info
     */
    public void removeServlet(final ServletInfo servletInfo)
    {
        synchronized ( this.contextMap )
        {
            final List<ContextInfo> holderList = this.servicesMap.remove(servletInfo);
            if ( holderList != null )
            {
                for(final ContextInfo h : holderList)
                {
                    this.unregisterServlet(servletInfo, h);
                }
            }
        }
    }

    public void addFilter(final FilterInfo info) {
        synchronized ( this.contextMap )
        {
            final List<ContextInfo> holderList = this.getMatchingContexts(info);
            this.servicesMap.put(info, holderList);
            for(final ContextInfo h : holderList)
            {
                this.registerFilter(info, h);
            }
        }
    }

    public void removeFilter(final FilterInfo info) {
        synchronized ( this.contextMap )
        {
            final List<ContextInfo> holderList = this.servicesMap.remove(info);
            if ( holderList != null )
            {
                for(final ContextInfo h : holderList)
                {
                    this.unregisterFilter(info, h);
                }
            }
        }
    }

    public void addResource(final ResourceInfo info) {
        synchronized ( this.contextMap )
        {
            final List<ContextInfo> holderList = this.getMatchingContexts(info);
            this.servicesMap.put(info, holderList);
            for(final ContextInfo h : holderList)
            {
                this.registerResource(info, h);
            }
        }
    }

    public void removeResource(final ResourceInfo info) {
        synchronized ( this.contextMap )
        {
            final List<ContextInfo> holderList = this.servicesMap.remove(info);
            if ( holderList != null )
            {
                for(final ContextInfo h : holderList)
                {
                    this.unregisterResource(info, h);
                }
            }
        }
    }
}
