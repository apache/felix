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
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

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
        this.servletContext.addListener(new HttpSessionListener()
        {
            private HttpSessionListener getHttpSessionListener()
            {
                final EventDispatcherTracker tracker = eventDispatcherTracker;
                if ( tracker != null )
                {
                    return tracker.getHttpSessionListener();
                }
                return null;
            }

            @Override
            public void sessionCreated(final HttpSessionEvent se)
            {
                final HttpSessionListener sessionDispatcher = getHttpSessionListener();
                if (sessionDispatcher != null)
                {
                    sessionDispatcher.sessionCreated(se);
                }
            }

            @Override
            public void sessionDestroyed(final HttpSessionEvent se)
            {
                final HttpSessionListener sessionDispatcher = getHttpSessionListener();
                if (sessionDispatcher != null)
                {
                    sessionDispatcher.sessionDestroyed(se);
                }
            }
        });
        this.servletContext.addListener(new HttpSessionIdListener()
        {
            private HttpSessionIdListener getHttpSessionIdListener()
            {
                final EventDispatcherTracker tracker = eventDispatcherTracker;
                if ( tracker != null )
                {
                    return tracker.getHttpSessionIdListener();
                }
                return null;
            }

            @Override
            public void sessionIdChanged(final HttpSessionEvent event, final String oldSessionId)
            {
                final HttpSessionIdListener sessionIdDispatcher = getHttpSessionIdListener();
                if (sessionIdDispatcher != null)
                {
                    sessionIdDispatcher.sessionIdChanged(event, oldSessionId);
                }
            }
        });
        this.servletContext.addListener(new HttpSessionAttributeListener()
        {
            private HttpSessionAttributeListener getHttpSessionAttributeListener()
            {
                final EventDispatcherTracker tracker = eventDispatcherTracker;
                if ( tracker != null )
                {
                    return tracker.getHttpSessionAttributeListener();
                }
                return null;
            }

            @Override
            public void attributeAdded(final HttpSessionBindingEvent se)
            {
                final HttpSessionAttributeListener attributeDispatcher = getHttpSessionAttributeListener();
                if (attributeDispatcher != null)
                {
                    attributeDispatcher.attributeAdded(se);
                }
            }

            @Override
            public void attributeRemoved(final HttpSessionBindingEvent se)
            {
                final HttpSessionAttributeListener attributeDispatcher = getHttpSessionAttributeListener();
                if (attributeDispatcher != null)
                {
                    attributeDispatcher.attributeRemoved(se);
                }
            }

            @Override
            public void attributeReplaced(final HttpSessionBindingEvent se)
            {
                final HttpSessionAttributeListener attributeDispatcher = getHttpSessionAttributeListener();
                if (attributeDispatcher != null)
                {
                    attributeDispatcher.attributeReplaced(se);
                }
            }
        });
        this.servletContext.addListener(new ServletContextAttributeListener()
        {
            @Override
            public void attributeAdded(final ServletContextAttributeEvent event)
            {
                if ( event.getName().equals(BundleContext.class.getName()) )
                {
                    startTracker(event.getValue());
                }
            }

            @Override
            public void attributeRemoved(final ServletContextAttributeEvent event)
            {
                if ( event.getName().equals(BundleContext.class.getName()) )
                {
                    stopTracker();
                }
            }

            @Override
            public void attributeReplaced(final ServletContextAttributeEvent event)
            {
                if ( event.getName().equals(BundleContext.class.getName()) )
                {
                    stopTracker();
                    startTracker(event.getServletContext().getAttribute(event.getName()));
                }
            }
        });
    }

    private void startTracker(final Object bundleContextAttr)
    {
        if (bundleContextAttr instanceof BundleContext)
        {
            try
            {
                final BundleContext bundleContext = (BundleContext) bundleContextAttr;
                this.eventDispatcherTracker = new EventDispatcherTracker(bundleContext);
                this.eventDispatcherTracker.open();
            }
            catch (final InvalidSyntaxException e)
            {
                // not expected for our simple filter
            }
        }
    }

    private void stopTracker()
    {
        if (this.eventDispatcherTracker != null)
        {
            this.eventDispatcherTracker.close();
            this.eventDispatcherTracker = null;
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce)
    {
        this.stopTracker();
        this.servletContext = null;
    }
}
