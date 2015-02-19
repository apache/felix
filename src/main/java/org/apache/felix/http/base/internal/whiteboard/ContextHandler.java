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
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.runtime.HttpSessionAttributeListenerInfo;
import org.apache.felix.http.base.internal.runtime.HttpSessionListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextAttributeListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletRequestAttributeListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletRequestListenerInfo;
import org.apache.felix.http.base.internal.runtime.WhiteboardServiceInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
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

    /** Servlet context listeners. */
    private final Map<Long, ServletContextListener> listeners = new HashMap<Long, ServletContextListener>();

    /** Servlet context attribute listeners. */
    private final Map<ServiceReference<ServletContextAttributeListener>, ServletContextAttributeListener> contextAttributeListeners =
                      new ConcurrentSkipListMap<ServiceReference<ServletContextAttributeListener>, ServletContextAttributeListener>();

    /** Session attribute listeners. */
    private final Map<ServiceReference<HttpSessionAttributeListener>, HttpSessionAttributeListener> sessionAttributeListeners =
            new ConcurrentSkipListMap<ServiceReference<HttpSessionAttributeListener>, HttpSessionAttributeListener>();

    /** Session listeners. */
    private final Map<ServiceReference<HttpSessionListener>, HttpSessionListener> sessionListeners =
            new ConcurrentSkipListMap<ServiceReference<HttpSessionListener>, HttpSessionListener>();

    /** Request listeners. */
    private final Map<ServiceReference<ServletRequestListener>, ServletRequestListener> requestListeners =
            new ConcurrentSkipListMap<ServiceReference<ServletRequestListener>, ServletRequestListener>();

    /** Request attribute listeners. */
    private final Map<ServiceReference<ServletRequestAttributeListener>, ServletRequestAttributeListener> requestAttributeListeners =
            new ConcurrentSkipListMap<ServiceReference<ServletRequestAttributeListener>, ServletRequestAttributeListener>();

    /** All whiteboard services - servlets, filters, resources. */
    private final Set<WhiteboardServiceInfo<?>> whiteboardServices = new ConcurrentSkipListSet<WhiteboardServiceInfo<?>>();

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
                info.getPath(),
                info.getInitParameters(),
                getContextAttributeListener());
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
        this.whiteboardServices.clear();
    }

    public void initialized(@Nonnull final ServletContextListenerInfo listenerInfo)
    {
        final ServletContextListener listener = listenerInfo.getService(bundle);
        if ( listener != null)
        {
            // no need to sync map - initialized is called in sync
            this.listeners.put(listenerInfo.getServiceId(), listener);

            final ServletContext context = this.getServletContext(listenerInfo.getServiceReference().getBundle());

            listener.contextInitialized(new ServletContextEvent(context));
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
            listenerInfo.ungetService(bundle, listener);
        }
    }

    public ExtServletContext getServletContext(@Nonnull final Bundle bundle)
    {
        final Long key = bundle.getBundleId();
        synchronized ( this.perBundleContextMap )
        {
            ContextHolder holder = this.perBundleContextMap.get(key);
            if ( holder == null )
            {
                final ServiceObjects<ServletContextHelper> so = bundle.getBundleContext().getServiceObjects(this.info.getServiceReference());
                if ( so != null )
                {
                    holder = new ContextHolder();
                    // TODO check for null of getService()
                    holder.servletContextHelper = so.getService();
                    holder.servletContext = new PerBundleServletContextImpl(bundle,
                            this.sharedContext,
                            holder.servletContextHelper,
                            this.getSessionListener(),
                            this.getSessionAttributeListener(),
                            this.getServletRequestListener(),
                            this.getServletRequestAttributeListener());
                    this.perBundleContextMap.put(key, holder);
                }
                // TODO - check null for so
            }
            holder.counter++;

            return holder.servletContext;
        }
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
                        final ServiceObjects<ServletContextHelper> so = bundle.getBundleContext().getServiceObjects(this.info.getServiceReference());
                        if ( so != null )
                        {
                            try
                            {
                                so.ungetService(holder.servletContextHelper);
                            }
                            catch ( final IllegalArgumentException iae)
                            {
                                // this seems to be thrown sometimes on shutdown; we have to evaluate
                            }
                        }
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
        final  ServletContextAttributeListener service = info.getService(bundle);
        if ( service != null )
        {
            this.contextAttributeListeners.put(info.getServiceReference(), service);
        }
    }

    /**
     * Remove servlet context attribute listener
     * @param info
     */
    public void removeListener(@Nonnull final ServletContextAttributeListenerInfo info)
    {
        final  ServletContextAttributeListener service = this.contextAttributeListeners.remove(info.getServiceReference());
        if ( service != null )
        {
            info.ungetService(bundle, service);
        }
    }

    /**
     * Add session attribute listener
     * @param info
     */
    public void addListener(@Nonnull final HttpSessionAttributeListenerInfo info)
    {
        final  HttpSessionAttributeListener service = info.getService(bundle);
        if ( service != null )
        {
            this.sessionAttributeListeners.put(info.getServiceReference(), service);
        }
    }

    /**
     * Remove session attribute listener
     * @param info
     */
    public void removeListener(@Nonnull final HttpSessionAttributeListenerInfo info)
    {
        final  HttpSessionAttributeListener service = this.sessionAttributeListeners.remove(info.getServiceReference());
        if ( service != null )
        {
            info.ungetService(bundle, service);
        }
    }

    /**
     * Add session listener
     * @param info
     */
    public void addListener(@Nonnull final HttpSessionListenerInfo info)
    {
        final  HttpSessionListener service = info.getService(bundle);
        if ( service != null )
        {
            this.sessionListeners.put(info.getServiceReference(), service);
        }
    }

    /**
     * Remove session listener
     * @param info
     */
    public void removeListener(@Nonnull final HttpSessionListenerInfo info)
    {
        final  HttpSessionListener service = this.sessionListeners.remove(info.getServiceReference());
        if ( service != null )
        {
            info.ungetService(bundle, service);
        }
    }

    /**
     * Add request listener
     * @param info
     */
    public void addListener(@Nonnull final ServletRequestListenerInfo info)
    {
        final  ServletRequestListener service = info.getService(bundle);
        if ( service != null )
        {
            this.requestListeners.put(info.getServiceReference(), service);
        }
    }

    /**
     * Remove request listener
     * @param info
     */
    public void removeListener(@Nonnull final ServletRequestListenerInfo info)
    {
        final ServletRequestListener service = this.requestListeners.remove(info.getServiceReference());
        if ( service != null )
        {
            info.ungetService(bundle, service);
        }
    }

    /**
     * Add request attribute listener
     * @param info
     */
    public void addListener(@Nonnull final ServletRequestAttributeListenerInfo info)
    {
        final  ServletRequestAttributeListener service = info.getService(bundle);
        if ( service != null )
        {
            this.requestAttributeListeners.put(info.getServiceReference(), service);
        }
    }

    /**
     * Remove request attribute listener
     * @param info
     */
    public void removeListener(@Nonnull final ServletRequestAttributeListenerInfo info)
    {
        final ServletRequestAttributeListener service = this.requestAttributeListeners.remove(info.getServiceReference());
        if ( service != null )
        {
            info.ungetService(bundle, service);
        }
    }

    private static final class ContextHolder
    {
        public long counter;
        public ExtServletContext servletContext;
        public ServletContextHelper servletContextHelper;
    }

    public HttpSessionAttributeListener getSessionAttributeListener()
    {
        return new HttpSessionAttributeListener() {

            @Override
            public void attributeReplaced(final HttpSessionBindingEvent event) {
                for(final HttpSessionAttributeListener l : sessionAttributeListeners.values())
                {
                    l.attributeReplaced(event);
                }
            }

            @Override
            public void attributeRemoved(final HttpSessionBindingEvent event) {
                for(final HttpSessionAttributeListener l : sessionAttributeListeners.values())
                {
                    l.attributeReplaced(event);
                }
            }

            @Override
            public void attributeAdded(final HttpSessionBindingEvent event) {
                for(final HttpSessionAttributeListener l : sessionAttributeListeners.values())
                {
                    l.attributeReplaced(event);
                }
            }
        };
    }

    private ServletContextAttributeListener getContextAttributeListener()
    {
        return new ServletContextAttributeListener() {

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
        };
    }

    public HttpSessionListener getSessionListener()
    {
        return new HttpSessionListener() {

            @Override
            public void sessionCreated(final HttpSessionEvent se) {
                for(final HttpSessionListener l : sessionListeners.values())
                {
                    l.sessionCreated(se);
                }
            }

            @Override
            public void sessionDestroyed(final HttpSessionEvent se) {
                for(final HttpSessionListener l : sessionListeners.values())
                {
                    l.sessionDestroyed(se);
                }
            }
        };
    }

    private ServletRequestListener getServletRequestListener()
    {
        return new ServletRequestListener() {

            @Override
            public void requestDestroyed(final ServletRequestEvent sre) {
                for(final ServletRequestListener l : requestListeners.values())
                {
                    l.requestDestroyed(sre);
                }
            }

            @Override
            public void requestInitialized(final ServletRequestEvent sre) {
                for(final ServletRequestListener l : requestListeners.values())
                {
                    l.requestInitialized(sre);
                }
            }
        };
    }

    private ServletRequestAttributeListener getServletRequestAttributeListener()
    {
        return new ServletRequestAttributeListener() {

            @Override
            public void attributeAdded(final ServletRequestAttributeEvent srae) {
                for(final ServletRequestAttributeListener l : requestAttributeListeners.values())
                {
                    l.attributeAdded(srae);
                }
            }

            @Override
            public void attributeRemoved(final ServletRequestAttributeEvent srae) {
                for(final ServletRequestAttributeListener l : requestAttributeListeners.values())
                {
                    l.attributeRemoved(srae);
                }
            }

            @Override
            public void attributeReplaced(final ServletRequestAttributeEvent srae) {
                for(final ServletRequestAttributeListener l : requestAttributeListeners.values())
                {
                    l.attributeReplaced(srae);
                }
            }
        };
    }

    public void addWhiteboardService(final WhiteboardServiceInfo<?> info)
    {
        this.whiteboardServices.add(info);
    }

    public void removeWhiteboardService(final WhiteboardServiceInfo<?> info)
    {
        this.whiteboardServices.remove(info);
    }

    public Set<WhiteboardServiceInfo<?>> getWhiteboardServices()
    {
        return this.whiteboardServices;
    }
}
