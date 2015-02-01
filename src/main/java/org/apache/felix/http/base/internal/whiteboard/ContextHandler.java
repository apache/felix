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
import org.apache.felix.http.base.internal.runtime.ContextInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextAttributeListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextListenerInfo;
import org.osgi.framework.Bundle;
import org.osgi.service.http.context.ServletContextHelper;

public final class ContextHandler implements Comparable<ContextHandler>
{
    /** The info object for the context. */
    private final ContextInfo info;

    /** A map of all created servlet contexts. */
    private final Map<Long, ContextHolder> contextMap = new HashMap<Long, ContextHolder>();

    /** The shared part of the servlet context. */
    private final ServletContext sharedContext;

    /** Servlet context listeners. */
    private final Map<Long, ServletContextListener> listeners = new HashMap<Long, ServletContextListener>();

    /** Servlet context attribute listeners. */
    private final Map<Long, ServletContextAttributeListener> contextAttributeListeners = new ConcurrentHashMap<Long, ServletContextAttributeListener>();

    /**
     * Create new handler.
     * @param info
     * @param webContext
     */
    public ContextHandler(final ContextInfo info, final ServletContext webContext)
    {
        this.info = info;
        this.sharedContext = new SharedServletContextImpl(webContext,
                info.getPrefix(),
                info.getName(),
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
    public ContextInfo getContextInfo()
    {
        return this.info;
    }

    @Override
    public int compareTo(final ContextHandler o)
    {
        return this.info.compareTo(o.info);
    }

    public void activate(final Bundle bundle)
    {
        getServletContext(bundle);
    }

    public void deactivate(final Bundle bundle)
    {
        this.ungetServletContext(bundle);
    }

    public void initialized(final Bundle bundle, final ServletContextListenerInfo listenerInfo)
    {
        final ServletContextListener listener = bundle.getBundleContext().getServiceObjects(listenerInfo.getServiceReference()).getService();
        if ( listener != null)
        {
            // no need to sync map - initialized is called in sync
            this.listeners.put(listenerInfo.getServiceId(), listener);

            final ServletContext context = this.getServletContext(listenerInfo.getServiceReference().getBundle());

            listener.contextInitialized(new ServletContextEvent(context));
        }
    }

    public void destroyed(final ServletContextListenerInfo listenerInfo)
    {
        // no need to sync map - destroyed is called in sync
        final ServletContextListener listener = this.listeners.remove(listenerInfo.getServiceId());
        if ( listener != null )
        {
            final ServletContext context = this.getServletContext(listenerInfo.getServiceReference().getBundle());
            listener.contextDestroyed(new ServletContextEvent(context));
            this.ungetServletContext(listenerInfo.getServiceReference().getBundle());
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
                holder = new ContextHolder();
                // TODO check for null
                holder.servletContextHelper = bundle.getBundleContext().getServiceObjects(this.info.getServiceReference()).getService();
                holder.servletContext = new PerBundleServletContextImpl(bundle,
                        this.sharedContext,
                        holder.servletContextHelper);
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
                    bundle.getBundleContext().getServiceObjects(this.info.getServiceReference()).ungetService(holder.servletContextHelper);
                }
            }
        }
    }

    public void addListener(@Nonnull final Bundle bundle, @Nonnull final ServletContextAttributeListenerInfo info)
    {
        final  ServletContextAttributeListener service = bundle.getBundleContext().getServiceObjects(info.getServiceReference()).getService();
        if ( service != null )
        {
            this.contextAttributeListeners.put(info.getServiceId(), service);
        }
    }

    public void removeListener(@Nonnull final Bundle bundle, @Nonnull final ServletContextAttributeListenerInfo info)
    {
        final  ServletContextAttributeListener service = this.contextAttributeListeners.remove(info.getServiceId());
        if ( service != null )
        {
            bundle.getBundleContext().getServiceObjects(info.getServiceReference()).ungetService(service);
        }
    }

    private static final class ContextHolder
    {
        public long counter;
        public ExtServletContext servletContext;
        public ServletContextHelper servletContextHelper;
    }

}
