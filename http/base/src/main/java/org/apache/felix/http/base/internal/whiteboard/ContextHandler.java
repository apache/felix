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
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.runtime.ServletContextAttributeListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextListenerInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceObjects;
import org.osgi.service.http.context.ServletContextHelper;

public final class ContextHandler implements Comparable<ContextHandler>
{
    /** The info object for the context. */
    private final ServletContextHelperInfo info;

    /** A map of all created servlet contexts. */
    private final Map<Long, ContextHolder> contextMap = new HashMap<Long, ContextHolder>();

    /** The shared part of the servlet context. */
    private final ServletContext sharedContext;

    /** Servlet context listeners. */
    private final Map<Long, ServletContextListener> listeners = new HashMap<Long, ServletContextListener>();

    /** Servlet context attribute listeners. */
    private final Map<Long, ServletContextAttributeListener> contextAttributeListeners = new ConcurrentHashMap<Long, ServletContextAttributeListener>();

    /** The http bundle. */
    private final Bundle bundle;

    /**
     * Create new handler.
     * @param info
     * @param webContext
     */
    public ContextHandler(final ServletContextHelperInfo info,
            final ServletContext webContext,
            final Bundle bundle)
    {
        this.info = info;
        this.bundle = bundle;
        this.sharedContext = new SharedServletContextImpl(webContext,
                info.getName(),
                info.getPrefix(),
                info.getInitParams(),
                new ServletContextAttributeListener() {

                    @Override
                    public void attributeReplaced(final ServletContextAttributeEvent event) {
                        for(final ServletContextAttributeListener l : contextAttributeListeners.values())
                        {
                            l.attributeReplaced(event);
                        }
                    }

                    @Override
                    public void attributeRemoved(final ServletContextAttributeEvent event) {
                        for(final ServletContextAttributeListener l : contextAttributeListeners.values())
                        {
                            l.attributeReplaced(event);
                        }
                    }

                    @Override
                    public void attributeAdded(final ServletContextAttributeEvent event) {
                        for(final ServletContextAttributeListener l : contextAttributeListeners.values())
                        {
                            l.attributeReplaced(event);
                        }
                    }
                });
    }

    /**
     * Get the context info
     */
    public ServletContextHelperInfo getContextInfo()
    {
        return this.info;
    }

    @Override
    public int compareTo(final ContextHandler o)
    {
        return this.info.compareTo(o.info);
    }

    public void activate()
    {
        getServletContext(bundle);
    }

    public void deactivate()
    {
        this.ungetServletContext(bundle);
    }

    public void initialized(@Nonnull final ServletContextListenerInfo listenerInfo)
    {
        final ServiceObjects<ServletContextListener> so = bundle.getBundleContext().getServiceObjects(listenerInfo.getServiceReference());
        if ( so != null )
        {
            final ServletContextListener listener = so.getService();
            if ( listener != null)
            {
                // no need to sync map - initialized is called in sync
                this.listeners.put(listenerInfo.getServiceId(), listener);

                final ServletContext context = this.getServletContext(listenerInfo.getServiceReference().getBundle());

                listener.contextInitialized(new ServletContextEvent(context));
            }
        }
    }

    public void destroyed(@Nonnull final ServletContextListenerInfo listenerInfo)
    {
        // no need to sync map - destroyed is called in sync
        final ServletContextListener listener = this.listeners.remove(listenerInfo.getServiceId());
        if ( listener != null )
        {
            final ServletContext context = this.getServletContext(listenerInfo.getServiceReference().getBundle());
            listener.contextDestroyed(new ServletContextEvent(context));
            // call unget twice, once for the call in initialized and once for the call in this method(!)
            this.ungetServletContext(listenerInfo.getServiceReference().getBundle());
            this.ungetServletContext(listenerInfo.getServiceReference().getBundle());
            final ServiceObjects<ServletContextListener> so = bundle.getBundleContext().getServiceObjects(listenerInfo.getServiceReference());
            if ( so != null )
            {
                so.ungetService(listener);
            }
        }
    }

    public ExtServletContext getServletContext(@Nonnull final Bundle bundle)
    {
        final Long key = bundle.getBundleId();
        synchronized ( this.contextMap )
        {
            ContextHolder holder = this.contextMap.get(key);
            if ( holder == null )
            {
                final ServiceObjects<ServletContextHelper> so = bundle.getBundleContext().getServiceObjects(this.info.getServiceReference());
                if ( so != null )
                {
                    holder = new ContextHolder();
                    // TODO check for null
                    holder.servletContextHelper = so.getService();
                    holder.servletContext = new PerBundleServletContextImpl(bundle,
                            this.sharedContext,
                            holder.servletContextHelper);
                    this.contextMap.put(key, holder);
                }
            }
            holder.counter++;

            return holder.servletContext;
        }
    }

    public void ungetServletContext(@Nonnull final Bundle bundle)
    {
        final Long key = bundle.getBundleId();
        synchronized ( this.contextMap )
        {
            ContextHolder holder = this.contextMap.get(key);
            if ( holder != null )
            {
                holder.counter--;
                if ( holder.counter <= 0 )
                {
                    this.contextMap.remove(key);
                    final ServiceObjects<ServletContextHelper> so = bundle.getBundleContext().getServiceObjects(this.info.getServiceReference());
                    if ( so != null )
                    {
                        so.ungetService(holder.servletContextHelper);
                    }
                }
            }
        }
    }

    /**
     * Add servlet context attribute listener
     * @param info
     */
    public void addListener(@Nonnull final ServletContextAttributeListenerInfo info)
    {
        final ServiceObjects<ServletContextAttributeListener> so =  bundle.getBundleContext().getServiceObjects(info.getServiceReference());
        if ( so != null )
        {
            final  ServletContextAttributeListener service = bundle.getBundleContext().getServiceObjects(info.getServiceReference()).getService();
            if ( service != null )
            {
                this.contextAttributeListeners.put(info.getServiceId(), service);
            }
        }
    }

    /**
     * Remove servlet context attribute listener
     * @param info
     */
    public void removeListener(@Nonnull final ServletContextAttributeListenerInfo info)
    {
        final  ServletContextAttributeListener service = this.contextAttributeListeners.remove(info.getServiceId());
        if ( service != null )
        {
            final ServiceObjects<ServletContextAttributeListener> so =  bundle.getBundleContext().getServiceObjects(info.getServiceReference());
            if ( so != null ) {
                so.ungetService(service);
            }
        }
    }

    private static final class ContextHolder
    {
        public long counter;
        public ExtServletContext servletContext;
        public ServletContextHelper servletContextHelper;
    }

}
