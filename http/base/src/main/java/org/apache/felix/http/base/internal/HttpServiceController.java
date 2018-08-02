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
package org.apache.felix.http.base.internal;

import java.util.Hashtable;

import org.jetbrains.annotations.NotNull;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.felix.http.base.internal.dispatch.Dispatcher;
import org.apache.felix.http.base.internal.dispatch.DispatcherServlet;
import org.apache.felix.http.base.internal.handler.HttpSessionWrapper;
import org.apache.felix.http.base.internal.registry.HandlerRegistry;
import org.apache.felix.http.base.internal.service.HttpServiceFactory;
import org.apache.felix.http.base.internal.whiteboard.WhiteboardManager;
import org.osgi.framework.BundleContext;

public final class HttpServiceController
{
    private final BundleContext bundleContext;
    private final HandlerRegistry registry;
    private final Dispatcher dispatcher;
    private final EventDispatcher eventDispatcher;
    private final HttpServiceFactory httpServiceFactory;
    private final WhiteboardManager whiteboardManager;

    private volatile HttpSessionListener httpSessionListener;

    public HttpServiceController(final BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
        this.registry = new HandlerRegistry();
        this.dispatcher = new Dispatcher(this.registry);
        this.eventDispatcher = new EventDispatcher(this);
        this.httpServiceFactory = new HttpServiceFactory(this.bundleContext, this.registry);
        this.whiteboardManager = new WhiteboardManager(bundleContext, this.httpServiceFactory, this.registry);
    }

    public void stop()
    {
        this.unregister();
    }

    /**
     * Create a new dispatcher servlet
     * @return The dispatcher servlet.
     */
    public @NotNull Servlet createDispatcherServlet()
    {
        return new DispatcherServlet(this.dispatcher);
    }

    public EventDispatcher getEventDispatcher()
    {
        return this.eventDispatcher;
    }

    HttpSessionListener getSessionListener()
    {
        // we don't need to sync here, if the object gets created several times
        // its not a problem
        if ( httpSessionListener == null )
        {
            httpSessionListener = new HttpSessionListener() {

                @Override
                public void sessionDestroyed(final HttpSessionEvent se) {
                    whiteboardManager.sessionDestroyed(se.getSession(), HttpSessionWrapper.getSessionContextNames(se.getSession()));
                }

                @Override
                public void sessionCreated(final HttpSessionEvent se) {
                    // nothing to do, session created event is sent from within the session
                }
            };
        }
        return httpSessionListener;
    }

    HttpSessionIdListener getSessionIdListener()
    {
        return new HttpSessionIdListener() {

            @Override
            public void sessionIdChanged(final HttpSessionEvent event, final String oldSessionId) {
                whiteboardManager.sessionIdChanged(event, oldSessionId, HttpSessionWrapper.getSessionContextNames(event.getSession()));
            }
        };
    }

    /**
     * Start the http and http whiteboard service in the provided context.
     * @param containerContext The container context.
     */
    public void register(@NotNull final ServletContext containerContext, @NotNull final Hashtable<String, Object> props)
    {
        this.registry.init();

        this.httpServiceFactory.start(containerContext, props);
        this.whiteboardManager.start(containerContext, props);

        this.dispatcher.setWhiteboardManager(this.whiteboardManager);
    }

    /**
     * Stops the http and http whiteboard service.
     */
    public void unregister()
    {
        this.dispatcher.setWhiteboardManager(null);

        this.httpServiceFactory.stop();
        this.whiteboardManager.stop();

        this.registry.shutdown();
        this.httpSessionListener = null;
    }
}
