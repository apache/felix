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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.registry.EventListenerRegistry;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.service.http.context.ServletContextHelper;

public final class ContextHandler implements Comparable<ContextHandler>
{
    /** The info object for the context. */
    private final ServletContextHelperInfo info;

    /** The shared part of the servlet context. */
    private final ServletContext sharedContext;

    /** The http bundle. */
    private final Bundle bundle;

    /** A map of all created servlet contexts. Each bundle gets it's own instance. */
    private final Map<Long, ContextHolder> perBundleContextMap = new HashMap<Long, ContextHolder>();

    private final EventListenerRegistry eventListener;

    public ContextHandler(final ServletContextHelperInfo info,
            final ServletContext webContext,
            final Bundle bundle)
    {
        this.info = info;
        this.eventListener = new EventListenerRegistry(bundle);
        this.bundle = bundle;
        this.sharedContext = new SharedServletContextImpl(webContext,
                info.getName(),
                info.getPath(),
                info.getInitParameters(),
                eventListener);
    }

    public ServletContextHelperInfo getContextInfo()
    {
        return this.info;
    }

    @Override
    public int compareTo(final ContextHandler o)
    {
        return this.info.compareTo(o.info);
    }

    /**
     * Activate this context.
     * @return {@code true} if it succeeded.
     */
    public boolean activate()
    {
        return getServletContext(bundle) != null;
    }

    /**
     * Deactivate this context.
     */
    public void deactivate()
    {
        this.ungetServletContext(bundle);
    }

    public ServletContext getSharedContext()
    {
        return sharedContext;
    }

    public ExtServletContext getServletContext(@Nonnull final Bundle bundle)
    {
        final Long key = bundle.getBundleId();
        synchronized ( this.perBundleContextMap )
        {
            ContextHolder holder = this.perBundleContextMap.get(key);
            if ( holder == null )
            {
                final BundleContext ctx = bundle.getBundleContext();
                final ServiceObjects<ServletContextHelper> so = (ctx == null ? null : ctx.getServiceObjects(this.info.getServiceReference()));
                if ( so != null )
                {
                    final ServletContextHelper service = so.getService();
                    if ( service != null )
                    {
                        holder = new ContextHolder();
                        holder.servletContextHelper = service;
                        holder.servletContext = new PerBundleServletContextImpl(bundle,
                                this.sharedContext,
                                service,
                                this.eventListener);
                        this.perBundleContextMap.put(key, holder);
                    }
                }
            }
            if ( holder != null )
            {
                holder.counter++;

                return holder.servletContext;
            }
        }
        return null;
    }

    public void ungetServletContext(@Nonnull final Bundle bundle)
    {
        final Long key = bundle.getBundleId();
        synchronized ( this.perBundleContextMap )
        {
            ContextHolder holder = this.perBundleContextMap.get(key);
            if ( holder != null )
            {
                holder.counter--;
                if ( holder.counter == 0 )
                {
                    this.perBundleContextMap.remove(key);
                    if ( holder.servletContextHelper != null )
                    {
                        final BundleContext ctx = bundle.getBundleContext();
                        final ServiceObjects<ServletContextHelper> so = (ctx == null ? null : ctx.getServiceObjects(this.info.getServiceReference()));
                        if ( so != null )
                        {
                            so.ungetService(holder.servletContextHelper);
                        }
                    }
                }
            }
        }
    }

    private static final class ContextHolder
    {
        public long counter;
        public ExtServletContext servletContext;
        public ServletContextHelper servletContextHelper;
    }

    public EventListenerRegistry getListenerRegistry() {
        return this.eventListener;
    }
}
