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

import javax.annotation.CheckForNull;
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
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.ListenerInfo;
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

    public void cleanup()
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
     * Add  listeners
     *
     * @param listener handler
     */
    public void addListeners(@Nonnull final ListenerHandler handler)
    {
        final int reason = handler.init();

        if ( handler.getListenerInfo().isListenerType(ServletContextListener.class.getName()))
        {
            this.contextListeners.add(handler, reason);
        }
        if ( handler.getListenerInfo().isListenerType(ServletContextAttributeListener.class.getName()))
        {
            this.contextAttributeListeners.add(handler, reason);
        }
        if ( handler.getListenerInfo().isListenerType(HttpSessionListener.class.getName()))
        {
            this.sessionListeners.add(handler, reason);
        }
        if ( handler.getListenerInfo().isListenerType(HttpSessionIdListener.class.getName()))
        {
            this.sessionIdListeners.add(handler, reason);
        }
        if ( handler.getListenerInfo().isListenerType(HttpSessionAttributeListener.class.getName()))
        {
            this.sessionAttributeListeners.add(handler, reason);
        }
        if ( handler.getListenerInfo().isListenerType(ServletRequestListener.class.getName()))
        {
            this.requestListeners.add(handler, reason);
        }
        if ( handler.getListenerInfo().isListenerType(ServletRequestAttributeListener.class.getName()))
        {
            this.requestAttributeListeners.add(handler, reason);
        }
    }

    /**
     * Remove listeners
     *
     * @param info
     */
    public void removeListeners(@Nonnull final ListenerInfo info)
    {
        // each listener map returns the same handler, we just need it once to destroy
        ListenerHandler handler = null;
        if ( info.isListenerType(ServletContextListener.class.getName()))
        {
            handler = this.contextListeners.remove(info);
        }
        if ( info.isListenerType(ServletContextAttributeListener.class.getName()))
        {
            handler = this.contextAttributeListeners.remove(info);
        }
        if ( info.isListenerType(HttpSessionListener.class.getName()))
        {
            handler = this.sessionListeners.remove(info);
        }
        if ( info.isListenerType(HttpSessionIdListener.class.getName()))
        {
            handler = this.sessionIdListeners.remove(info);
        }
        if ( info.isListenerType(HttpSessionAttributeListener.class.getName()))
        {
            handler = this.sessionAttributeListeners.remove(info);
        }
        if ( info.isListenerType(ServletRequestListener.class.getName()))
        {
            handler = this.requestListeners.remove(info);
        }
        if ( info.isListenerType(ServletRequestAttributeListener.class.getName()))
        {
            handler = this.requestAttributeListeners.remove(info);
        }
        if ( handler != null )
        {
            handler.destroy();
        }
    }

    /**
     * Get the listener handler for the listener info
     * @param info The listener info
     * @return The handler or {@code null}.
     */
    public @CheckForNull ListenerHandler getServletContextListener(@Nonnull final ListenerInfo info)
    {
        return this.contextListeners.getListenerHandler(info);
    }

    public void contextInitialized() {
        for (final ListenerHandler l : contextListeners.getActiveHandlers())
        {
            final ServletContextListener listener = (ServletContextListener)l.getListener();
            if ( listener != null )
            {
                contextInitialized(l.getListenerInfo(), listener, new ServletContextEvent(l.getContext()));
            }
        }
    }

    public void contextDestroyed() {
        for (final ListenerHandler l : contextListeners.getActiveHandlers())
        {
            final ServletContextListener listener = (ServletContextListener) l.getListener();
            if ( listener != null )
            {
                contextDestroyed(l.getListenerInfo(), listener, new ServletContextEvent(l.getContext()));
            }
        }
    }

    @Override
    public void attributeReplaced(final HttpSessionBindingEvent event)
    {
        for (final HttpSessionAttributeListener l : sessionAttributeListeners.getActiveListeners())
        {
            try
            {
                l.attributeReplaced(event);
            }
            catch (final Throwable t)
            {
                SystemLogger.error(null, "Exception while calling listener " + l, t);
            }
        }
    }

    @Override
    public void attributeRemoved(final HttpSessionBindingEvent event)
    {
        for (final HttpSessionAttributeListener l : sessionAttributeListeners.getActiveListeners())
        {
            try
            {
                l.attributeRemoved(event);
            }
            catch (final Throwable t)
            {
                SystemLogger.error(null, "Exception while calling listener " + l, t);
            }
        }
    }

    @Override
    public void attributeAdded(final HttpSessionBindingEvent event)
    {
        for (final HttpSessionAttributeListener l : sessionAttributeListeners.getActiveListeners())
        {
            try
            {
                l.attributeAdded(event);
            }
            catch (final Throwable t)
            {
                SystemLogger.error(null, "Exception while calling listener " + l, t);
            }
        }
    }

    @Override
    public void attributeReplaced(final ServletContextAttributeEvent event)
    {
        for (final ServletContextAttributeListener l : contextAttributeListeners.getActiveListeners())
        {
            try
            {
                l.attributeReplaced(event);
            }
            catch (final Throwable t)
            {
                SystemLogger.error(null, "Exception while calling listener " + l, t);
            }
        }
    }

    @Override
    public void attributeRemoved(final ServletContextAttributeEvent event)
    {
        for (final ServletContextAttributeListener l : contextAttributeListeners.getActiveListeners())
        {
            try
            {
                l.attributeRemoved(event);
            }
            catch (final Throwable t)
            {
                SystemLogger.error(null, "Exception while calling listener " + l, t);
            }
        }
    }

    @Override
    public void attributeAdded(final ServletContextAttributeEvent event)
    {
        for (final ServletContextAttributeListener l : contextAttributeListeners.getActiveListeners())
        {
            try
            {
                l.attributeAdded(event);
            }
            catch (final Throwable t)
            {
                SystemLogger.error(null, "Exception while calling listener " + l, t);
            }
        }
    }

    @Override
    public void sessionCreated(final HttpSessionEvent se)
    {
        for (final HttpSessionListener l : sessionListeners.getActiveListeners())
        {
            try
            {
                l.sessionCreated(se);
            }
            catch (final Throwable t)
            {
                SystemLogger.error(null, "Exception while calling listener " + l, t);
            }
        }
    }

    @Override
    public void sessionDestroyed(final HttpSessionEvent se)
    {
        for (final HttpSessionListener l : sessionListeners.getActiveListeners())
        {
            try
            {
                l.sessionDestroyed(se);
            }
            catch (final Throwable t)
            {
                SystemLogger.error(null, "Exception while calling listener " + l, t);
            }
        }
    }

    @Override
    public void requestDestroyed(final ServletRequestEvent sre)
    {
        for (final ServletRequestListener l : requestListeners.getActiveListeners())
        {
            try
            {
                l.requestDestroyed(sre);
            }
            catch (final Throwable t)
            {
                SystemLogger.error(null, "Exception while calling listener " + l, t);
            }
        }
    }

    @Override
    public void requestInitialized(final ServletRequestEvent sre)
    {
        for (final ServletRequestListener l : requestListeners.getActiveListeners())
        {
            try
            {
                l.requestInitialized(sre);
            }
            catch (final Throwable t)
            {
                SystemLogger.error(null, "Exception while calling listener " + l, t);
            }
        }
    }

    @Override
    public void attributeAdded(final ServletRequestAttributeEvent srae)
    {
        for (final ServletRequestAttributeListener l : requestAttributeListeners.getActiveListeners())
        {
            try
            {
                l.attributeAdded(srae);
            }
            catch (final Throwable t)
            {
                SystemLogger.error(null, "Exception while calling listener " + l, t);
            }
        }
    }

    @Override
    public void attributeRemoved(final ServletRequestAttributeEvent srae)
    {
        for (final ServletRequestAttributeListener l : requestAttributeListeners.getActiveListeners())
        {
            try
            {
                l.attributeRemoved(srae);
            }
            catch (final Throwable t)
            {
                SystemLogger.error(null, "Exception while calling listener " + l, t);
            }
        }
    }

    @Override
    public void attributeReplaced(final ServletRequestAttributeEvent srae)
    {
        for (final ServletRequestAttributeListener l : requestAttributeListeners.getActiveListeners())
        {
            try
            {
                l.attributeReplaced(srae);
            }
            catch (final Throwable t)
            {
                SystemLogger.error(null, "Exception while calling listener " + l, t);
            }
        }
    }

    /**
     * @see javax.servlet.http.HttpSessionIdListener#sessionIdChanged(javax.servlet.http.HttpSessionEvent, java.lang.String)
     */
    @Override
    public void sessionIdChanged(@Nonnull final HttpSessionEvent event, @Nonnull final String oldSessionId) {
        for (final HttpSessionIdListener l : sessionIdListeners.getActiveListeners())
        {
            try
            {
                l.sessionIdChanged(event, oldSessionId);
            }
            catch (final Throwable t)
            {
                SystemLogger.error(null, "Exception while calling listener " + l, t);
            }
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

    public static void contextInitialized(
            @Nonnull final ListenerInfo info,
            @Nonnull final ServletContextListener listener,
            @Nonnull final ServletContextEvent event)
    {
        try
        {
            listener.contextInitialized(event);
        }
        catch (final Throwable t)
        {
            SystemLogger.error(info.getServiceReference(), "Exception while calling servlet context listener.", t);
        }
    }

    public static void contextDestroyed(
            @Nonnull final ListenerInfo info,
            @Nonnull final ServletContextListener listener,
            @Nonnull final ServletContextEvent event)
    {
        try
        {
            listener.contextDestroyed(event);
        }
        catch (final Throwable t)
        {
            SystemLogger.error(info.getServiceReference(), "Exception while calling servlet context listener.", t);
        }
    }
}
