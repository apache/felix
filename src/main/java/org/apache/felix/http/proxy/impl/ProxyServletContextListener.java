/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.proxy.impl;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.felix.http.proxy.AbstractProxyListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

/**
 * The ProxyServletContextListener is a servlet context listener which will setup
 * all required listeners for the http service implementation.
 *
 * @since 3.0.0
 */
@WebListener
public class ProxyServletContextListener
    implements ServletContextListener
{

    private volatile ServletContext servletContext;

    private volatile EventDispatcherTracker eventDispatcherTracker;

    // ---------- ServletContextListener

    @Override
    public void contextInitialized(final ServletContextEvent sce)
    {
        this.servletContext = sce.getServletContext();

        // add all required listeners

        this.servletContext.addListener(new AbstractProxyListener() {
            
            @Override
            protected void stopTracking() {
                ProxyServletContextListener.this.stopTracking();
            }
            
            @Override
            protected void startTracking(final Object bundleContextAttr) {
                ProxyServletContextListener.this.startTracking(bundleContextAttr);
            }
            
            @Override
            protected EventDispatcherTracker getEventDispatcherTracker() {
                return eventDispatcherTracker;
            }
        });
    }

    private void stopTracking() {
        if (eventDispatcherTracker != null)
        {
            eventDispatcherTracker.close();
            eventDispatcherTracker = null;
        }
    }

    protected void startTracking(final Object bundleContextAttr) {
        if (bundleContextAttr instanceof BundleContext)
        {
            try
            {
                final BundleContext bundleContext = (BundleContext) bundleContextAttr;
                eventDispatcherTracker = new EventDispatcherTracker(bundleContext);
                eventDispatcherTracker.open();
            }
            catch (final InvalidSyntaxException e)
            {
                // not expected for our simple filter
            }
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce)
    {
        this.stopTracking();
        this.servletContext = null;
    }
}
