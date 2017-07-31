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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.registry.HandlerRegistry;
import org.apache.felix.http.base.internal.registry.PerContextHandlerRegistry;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.service.http.context.ServletContextHelper;

/**
 * The whiteboard context handler provides some information and functionality for
 * handling servlet context (helpers).
 */
public class WhiteboardContextHandler implements Comparable<WhiteboardContextHandler>
{
    /** The info object for the context. */
    private final ServletContextHelperInfo info;

    /** The web context. */
    private final ServletContext webContext;

    /** The http bundle. */
    private final Bundle httpBundle;

    /** A map of all created servlet contexts. Each bundle gets it's own instance. */
    private final Map<Long, ContextHolder> perBundleContextMap = new HashMap<Long, ContextHolder>();

    /** The corresponding handler registry. */
    private volatile PerContextHandlerRegistry registry;

    /** The shared part of the servlet context. */
    private volatile ServletContext sharedContext;

    public WhiteboardContextHandler(@Nonnull final ServletContextHelperInfo info,
            @Nonnull final ServletContext webContext,
            @Nonnull final Bundle httpBundle)
    {
        this.webContext = webContext;
        this.info = info;
        this.httpBundle = httpBundle;
    }

    public @Nonnull BundleContext getBundleContext()
    {
        return this.httpBundle.getBundleContext();
    }

    public @Nonnull ServletContextHelperInfo getContextInfo()
    {
        return this.info;
    }

    @Override
    public int compareTo(@Nonnull final WhiteboardContextHandler o)
    {
        return this.info.compareTo(o.getContextInfo());
    }

    /**
     * Activate this context.
     * @return {@code true} if it succeeded.
     */
    public boolean activate(@Nonnull final HandlerRegistry registry)
    {
        this.registry = new PerContextHandlerRegistry(this.info);
        this.sharedContext = new SharedServletContextImpl(webContext,
                info.getName(),
                info.getPath(),
                info.getInitParameters(),
                this.registry);
        final boolean activate = getServletContext(httpBundle) != null;
        if ( !activate )
        {
            this.registry = null;
            this.sharedContext = null;
        }
        else
        {
            registry.add(this.registry);
        }
        return activate;
    }

    /**
     * Deactivate this context.
     */
    public void deactivate(@Nonnull final HandlerRegistry registry)
    {
        registry.remove(this.info);
        this.registry = null;
        this.sharedContext = null;
        this.ungetServletContext(httpBundle);
        this.perBundleContextMap.clear();
    }

    public @CheckForNull ServletContext getSharedContext()
    {
        return sharedContext;
    }

    public @CheckForNull ExtServletContext getServletContext(@CheckForNull final Bundle bundle)
    {
        if ( bundle == null )
        {
            return null;
        }
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
                                this.registry.getEventListenerRegistry());
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

    public @CheckForNull PerContextHandlerRegistry getRegistry()
    {
        return this.registry;
    }

    private static final class ContextHolder
    {
        public long counter;
        public ExtServletContext servletContext;
        public ServletContextHelper servletContextHelper;
    }
}
