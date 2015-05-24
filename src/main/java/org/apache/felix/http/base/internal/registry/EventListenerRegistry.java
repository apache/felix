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
package org.apache.felix.http.base.internal.registry;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
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
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.felix.http.base.internal.handler.ListenerHandler;
import org.apache.felix.http.base.internal.runtime.HttpSessionAttributeListenerInfo;
import org.apache.felix.http.base.internal.runtime.HttpSessionIdListenerInfo;
import org.apache.felix.http.base.internal.runtime.HttpSessionListenerInfo;
import org.apache.felix.http.base.internal.runtime.ListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextAttributeListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletRequestAttributeListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletRequestListenerInfo;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;

/**
 * Per context event listener registry.
 */
public final class EventListenerRegistry implements
        HttpSessionListener,
        HttpSessionAttributeListener,
        HttpSessionIdListener,
        ServletContextAttributeListener,
        ServletRequestListener,
        ServletRequestAttributeListener
{
    /** Servlet context listeners. */
    private final ListenerMap<ServletContextListener> contextListeners = new ListenerMap<ServletContextListener>();

    /** Servlet context attribute listeners. */
    private final ListenerMap<ServletContextAttributeListener> contextAttributeListeners = new ListenerMap<ServletContextAttributeListener>();

    /** Session attribute listeners. */
    private final ListenerMap<HttpSessionAttributeListener> sessionAttributeListeners = new ListenerMap<HttpSessionAttributeListener>();

    /** Session listeners. */
    private final ListenerMap<HttpSessionListener> sessionListeners = new ListenerMap<HttpSessionListener>();

    /** Session id listeners. */
    private final ListenerMap<HttpSessionIdListener> sessionIdListeners = new ListenerMap<HttpSessionIdListener>();

    /** Request listeners. */
    private final ListenerMap<ServletRequestListener> requestListeners = new ListenerMap<ServletRequestListener>();

    /** Request attribute listeners. */
    private final ListenerMap<ServletRequestAttributeListener> requestAttributeListeners = new ListenerMap<ServletRequestAttributeListener>();

    public synchronized void cleanup()
    {
        this.contextListeners.cleanup();
        this.contextAttributeListeners.cleanup();
        this.sessionAttributeListeners.cleanup();
        this.sessionListeners.cleanup();
        this.sessionIdListeners.cleanup();
        this.requestListeners.cleanup();
        this.requestAttributeListeners.cleanup();
    }

    /**
     * Add servlet context listener
     *
     * @param handler
     */
    public void addServletContextListener(@Nonnull final ListenerHandler<ServletContextListener> handler)
    {
        this.contextListeners.add(handler);
    }

    /**
     * Remove servlet context listener
     *
     * @param info
     */
    public void removeServletContextListener(@Nonnull final ServletContextListenerInfo info)
    {
        this.contextListeners.remove(info);
    }

    /**
     * Add servlet context attribute listener
     *
     * @param handler
     */
    public void addServletContextAttributeListener(@Nonnull final ListenerHandler<ServletContextAttributeListener> handler)
    {
        this.contextAttributeListeners.add(handler);
    }

    /**
     * Remove servlet context attribute listener
     *
     * @param info
     */
    public void removeServletContextAttributeListener(@Nonnull final ServletContextAttributeListenerInfo info)
    {
        this.contextAttributeListeners.remove(info);
    }

    /**
     * Add session attribute listener
     *
     * @param handler
     */
    public void addSessionAttributeListener(@Nonnull final ListenerHandler<HttpSessionAttributeListener> handler)
    {
        this.sessionAttributeListeners.add(handler);
    }

    /**
     * Remove session attribute listener
     *
     * @param info
     */
    public void removeSessionAttributeListener(@Nonnull final HttpSessionAttributeListenerInfo info)
    {
        this.sessionAttributeListeners.remove(info);
    }

    /**
     * Add session listener
     *
     * @param handler
     */
    public void addSessionListener(@Nonnull final ListenerHandler<HttpSessionListener> handler)
    {
        this.sessionListeners.add(handler);
    }

    /**
     * Remove session listener
     *
     * @param info
     */
    public void removeSessionListener(@Nonnull final HttpSessionListenerInfo info)
    {
        this.sessionListeners.remove(info);
    }

    /**
     * Add session id listener
     *
     * @param handler
     */
    public void addSessionIdListener(@Nonnull final ListenerHandler<HttpSessionIdListener> handler)
    {
        this.sessionIdListeners.add(handler);
    }

    /**
     * Remove session id listener
     *
     * @param info
     */
    public void removeSessionIdListener(@Nonnull final HttpSessionIdListenerInfo info)
    {
        this.sessionIdListeners.remove(info);
    }

    /**
     * Add request listener
     *
     * @param handler
     */
    public void addServletRequestListener(@Nonnull final ListenerHandler<ServletRequestListener> handler)
    {
        this.requestListeners.add(handler);
    }

    /**
     * Remove request listener
     *
     * @param info
     */
    public void removeServletRequestListener(@Nonnull final ServletRequestListenerInfo info)
    {
        this.requestListeners.remove(info);
    }

    /**
     * Add request attribute listener
     *
     * @param handler
     */
    public void addServletRequestAttributeListener(@Nonnull final ListenerHandler<ServletRequestAttributeListener> handler)
    {
        this.requestAttributeListeners.add(handler);
    }

    /**
     * Remove request attribute listener
     *
     * @param info
     */
    public void removeServletRequestAttributeListener(@Nonnull final ServletRequestAttributeListenerInfo info)
    {
        this.requestAttributeListeners.remove(info);
    }

    public ListenerHandler<ServletContextListener> getServletContextListener(@Nonnull final ListenerInfo<ServletContextListener> info)
    {
        return this.contextListeners.getListenerHandler(info);
    }

    public void contextInitialized() {
        for (final ListenerHandler<ServletContextListener> l : contextListeners.getActiveHandlers())
        {
            final ServletContextListener listener = l.getListener();
            if ( listener != null )
            {
                listener.contextInitialized(new ServletContextEvent(l.getContext()));
            }
        }
    }

    public void contextDestroyed() {
        for (final ListenerHandler<ServletContextListener> l : contextListeners.getActiveHandlers())
        {
            final ServletContextListener listener = l.getListener();
            if ( listener != null )
            {
                listener.contextDestroyed(new ServletContextEvent(l.getContext()));
            }
        }
    }

    @Override
    public void attributeReplaced(final HttpSessionBindingEvent event)
    {
        for (final HttpSessionAttributeListener l : sessionAttributeListeners.getActiveListeners())
        {
            l.attributeReplaced(event);
        }
    }

    @Override
    public void attributeRemoved(final HttpSessionBindingEvent event)
    {
        for (final HttpSessionAttributeListener l : sessionAttributeListeners.getActiveListeners())
        {
            l.attributeReplaced(event);
        }
    }

    @Override
    public void attributeAdded(final HttpSessionBindingEvent event)
    {
        for (final HttpSessionAttributeListener l : sessionAttributeListeners.getActiveListeners())
        {
            l.attributeReplaced(event);
        }
    }

    @Override
    public void attributeReplaced(final ServletContextAttributeEvent event)
    {
        for (final ServletContextAttributeListener l : contextAttributeListeners.getActiveListeners())
        {
            l.attributeReplaced(event);
        }
    }

    @Override
    public void attributeRemoved(final ServletContextAttributeEvent event)
    {
        for (final ServletContextAttributeListener l : contextAttributeListeners.getActiveListeners())
        {
            l.attributeReplaced(event);
        }
    }

    @Override
    public void attributeAdded(final ServletContextAttributeEvent event)
    {
        for (final ServletContextAttributeListener l : contextAttributeListeners.getActiveListeners())
        {
            l.attributeReplaced(event);
        }
    }

    @Override
    public void sessionCreated(final HttpSessionEvent se)
    {
        for (final HttpSessionListener l : sessionListeners.getActiveListeners())
        {
            l.sessionCreated(se);
        }
    }

    @Override
    public void sessionDestroyed(final HttpSessionEvent se)
    {
        for (final HttpSessionListener l : sessionListeners.getActiveListeners())
        {
            l.sessionDestroyed(se);
        }
    }

    @Override
    public void requestDestroyed(final ServletRequestEvent sre)
    {
        for (final ServletRequestListener l : requestListeners.getActiveListeners())
        {
            l.requestDestroyed(sre);
        }
    }

    @Override
    public void requestInitialized(final ServletRequestEvent sre)
    {
        for (final ServletRequestListener l : requestListeners.getActiveListeners())
        {
            l.requestInitialized(sre);
        }
    }

    @Override
    public void attributeAdded(final ServletRequestAttributeEvent srae)
    {
        for (final ServletRequestAttributeListener l : requestAttributeListeners.getActiveListeners())
        {
            l.attributeAdded(srae);
        }
    }

    @Override
    public void attributeRemoved(final ServletRequestAttributeEvent srae)
    {
        for (final ServletRequestAttributeListener l : requestAttributeListeners.getActiveListeners())
        {
            l.attributeRemoved(srae);
        }
    }

    @Override
    public void attributeReplaced(final ServletRequestAttributeEvent srae)
    {
        for (final ServletRequestAttributeListener l : requestAttributeListeners.getActiveListeners())
        {
            l.attributeReplaced(srae);
        }
    }

    /**
     * @see javax.servlet.http.HttpSessionIdListener#sessionIdChanged(javax.servlet.http.HttpSessionEvent, java.lang.String)
     */
    @Override
    public void sessionIdChanged(@Nonnull final HttpSessionEvent event, @Nonnull final String oldSessionId) {
        for (final HttpSessionIdListener l : sessionIdListeners.getActiveListeners())
        {
            l.sessionIdChanged(event, oldSessionId);
        }
    }

    public void getRuntimeInfo(final ServletContextDTO dto, final List<FailedListenerDTO> failedListenerDTOs)
    {
        final List<ListenerDTO> listenerDTOs = new ArrayList<ListenerDTO>();
        this.contextListeners.getRuntimeInfo(listenerDTOs, failedListenerDTOs);
        this.contextAttributeListeners.getRuntimeInfo(listenerDTOs, failedListenerDTOs);
        this.requestListeners.getRuntimeInfo(listenerDTOs, failedListenerDTOs);
        this.requestAttributeListeners.getRuntimeInfo(listenerDTOs, failedListenerDTOs);
        this.sessionListeners.getRuntimeInfo(listenerDTOs, failedListenerDTOs);
        this.sessionAttributeListeners.getRuntimeInfo(listenerDTOs, failedListenerDTOs);
        this.sessionIdListeners.getRuntimeInfo(listenerDTOs, failedListenerDTOs);

        if ( listenerDTOs.size() > 0 )
        {
            dto.listenerDTOs = listenerDTOs.toArray(new ListenerDTO[listenerDTOs.size()]);
        }
    }
}
