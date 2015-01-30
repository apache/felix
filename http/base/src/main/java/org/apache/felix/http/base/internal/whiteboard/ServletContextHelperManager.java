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
import org.apache.felix.http.base.internal.service.HttpServiceImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public final class ServletContextHelperManager
{
    /** A map containing all servlet context registrations. Mapped by context name */
    private final Map<String, List<ContextHolder>> contextMap = new HashMap<String, List<ContextHolder>>();

    /** A map with all servlet/filter registrations, mapped by abstract info. */
    private final Map<AbstractInfo<?>, List<ContextHolder>> servicesMap = new HashMap<AbstractInfo<?>, List<ContextHolder>>();

    private final HttpServiceImpl httpService;

    private final ServiceRegistration<ServletContextHelper> defaultContextRegistration;

    /**
     * Create a new servlet context helper manager
     * and the default context
     */
    public ServletContextHelperManager(final BundleContext bundleContext, final HttpServiceImpl httpService)
    {
        this.httpService = httpService;

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME);
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");

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

    private void activate(final ContextHolder holder)
    {
        for(final Map.Entry<AbstractInfo<?>, List<ContextHolder>> entry : this.servicesMap.entrySet())
        {
            if ( entry.getKey().getContextSelectionFilter().match(holder.getInfo().getServiceReference()) )
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

    private void deactivate(final ContextHolder holder)
    {
        final Iterator<Map.Entry<AbstractInfo<?>, List<ContextHolder>>> i = this.servicesMap.entrySet().iterator();
        while ( i.hasNext() )
        {
            final Map.Entry<AbstractInfo<?>, List<ContextHolder>> entry = i.next();
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
        final ContextHolder holder = new ContextHolder(info);
        synchronized ( this.contextMap )
        {
            List<ContextHolder> holderList = this.contextMap.get(info.getName());
            if ( holderList == null )
            {
                holderList = new ArrayList<ContextHolder>();
                this.contextMap.put(info.getName(), holderList);
            }
            holderList.add(holder);
            Collections.sort(holderList);
            // check for activate/deactivate
            if ( holderList.get(0) == holder )
            {
                // check for deactivate
                if ( holderList.size() > 1 )
                {
                    this.deactivate(holderList.get(1));
                }
                this.activate(holder);
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
            final List<ContextHolder> holderList = this.contextMap.get(info.getName());
            if ( holderList != null )
            {
                final Iterator<ContextHolder> i = holderList.iterator();
                boolean first = true;
                boolean activateNext = false;
                while ( i.hasNext() )
                {
                    final ContextHolder holder = i.next();
                    if ( holder.getInfo().compareTo(info) == 0 )
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

    private List<ContextHolder> getMatchingContexts(final AbstractInfo<?> info)
    {
        final List<ContextHolder> result = new ArrayList<ContextHolder>();
        for(final List<ContextHolder> holders : this.contextMap.values()) {
            final ContextHolder h = holders.get(0);
            if ( info.getContextSelectionFilter().match(h.getInfo().getServiceReference()) )
            {
                result.add(h);
            }
        }
        return result;
    }

    private void registerServlet(final ServletInfo servletInfo, final ContextHolder holder)
    {
    	final ServletContextHelper helper = holder.getContext(servletInfo.getServiceReference().getBundle());
        this.httpService.registerServlet(helper, holder.getInfo(), servletInfo);
    }

    private void unregisterServlet(final ServletInfo servletInfo, final ContextHolder holder)
    {
        this.httpService.unregisterServlet(holder.getInfo(), servletInfo);
    }

    private void registerFilter(final FilterInfo filterInfo, final ContextHolder holder)
    {
        final ServletContextHelper helper = holder.getContext(filterInfo.getServiceReference().getBundle());
        this.httpService.registerFilter(helper, holder.getInfo(), filterInfo);
    }

    private void unregisterFilter(final FilterInfo filterInfo, final ContextHolder holder)
    {
        this.httpService.unregisterFilter(holder.getInfo(), filterInfo);
    }

    private void registerResource(final ResourceInfo resourceInfo, final ContextHolder holder)
    {
        final ServletContextHelper helper = holder.getContext(resourceInfo.getServiceReference().getBundle());
        this.httpService.registerResource(helper, holder.getInfo(), resourceInfo);
    }

    private void unregisterResource(final ResourceInfo resourceInfo, final ContextHolder holder)
    {
        this.httpService.unregisterResource(holder.getInfo(), resourceInfo);
    }

    /**
     * Add a new servlet.
     * @param servletInfo The servlet info
     */
    public void addServlet(final ServletInfo servletInfo)
    {
        synchronized ( this.contextMap )
        {
            final List<ContextHolder> holderList = this.getMatchingContexts(servletInfo);
            this.servicesMap.put(servletInfo, holderList);
            for(final ContextHolder h : holderList)
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
            final List<ContextHolder> holderList = this.servicesMap.remove(servletInfo);
            if ( holderList != null )
            {
                for(final ContextHolder h : holderList)
                {
                    this.unregisterServlet(servletInfo, h);
                }
            }
        }
    }

    /**
     * Hold information about a context.
     */
    private final static class ContextHolder implements Comparable<ContextHolder>
    {
        private final ContextInfo info;

        public ContextHolder(final ContextInfo info)
        {
            this.info = info;
        }

        public ContextInfo getInfo()
        {
            return this.info;
        }

        @Override
        public int compareTo(final ContextHolder o)
        {
            return this.info.compareTo(o.info);
        }

        public ServletContextHelper getContext(final Bundle b)
        {
        	// TODO - we should somehow keep track of these objects to later on dispose them
        	return b.getBundleContext().getServiceObjects(this.info.getServiceReference()).getService();
        }
    }

    public void addFilter(final FilterInfo info) {
        synchronized ( this.contextMap )
        {
            final List<ContextHolder> holderList = this.getMatchingContexts(info);
            this.servicesMap.put(info, holderList);
            for(final ContextHolder h : holderList)
            {
                this.registerFilter(info, h);
            }
        }
    }

    public void removeFilter(final FilterInfo info) {
        synchronized ( this.contextMap )
        {
            final List<ContextHolder> holderList = this.servicesMap.remove(info);
            if ( holderList != null )
            {
                for(final ContextHolder h : holderList)
                {
                    this.unregisterFilter(info, h);
                }
            }
        }
    }

    public void addResource(final ResourceInfo info) {
        synchronized ( this.contextMap )
        {
            final List<ContextHolder> holderList = this.getMatchingContexts(info);
            this.servicesMap.put(info, holderList);
            for(final ContextHolder h : holderList)
            {
                this.registerResource(info, h);
            }
        }
    }

    public void removeResource(final ResourceInfo info) {
        synchronized ( this.contextMap )
        {
            final List<ContextHolder> holderList = this.servicesMap.remove(info);
            if ( holderList != null )
            {
                for(final ContextHolder h : holderList)
                {
                    this.unregisterResource(info, h);
                }
            }
        }
    }
}
