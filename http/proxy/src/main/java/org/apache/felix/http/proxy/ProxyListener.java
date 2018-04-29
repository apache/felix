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
package org.apache.felix.http.proxy;

<<<<<<< HEAD
import java.util.EventListener;

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
<<<<<<< HEAD
import javax.servlet.http.HttpSessionListener;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
=======
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.felix.http.proxy.impl.EventDispatcherTracker;
import org.apache.felix.http.proxy.impl.ProxyServletContextListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

/**
 * The <code>ProxyListener</code> implements a Servlet API listener for HTTP
 * Session related events. These events are provided by the servlet container
 * and forwarded to the event dispatcher.
 *
 * @since 2.1.0
<<<<<<< HEAD
 */
public class ProxyListener implements HttpSessionAttributeListener, HttpSessionListener, ServletContextListener
{

    private ServletContext servletContext;

    private ServiceTracker eventDispatcherTracker;

    private HttpSessionListener sessionDispatcher;

    private HttpSessionAttributeListener attributeDispatcher;

    // ---------- ServletContextListener

=======
 * @deprecated Use the {@link ProxyServletContextListener} instead.
 */
@Deprecated
public class ProxyListener
    implements HttpSessionAttributeListener,
               HttpSessionListener,
               HttpSessionIdListener,
               ServletContextListener
{

    private volatile ServletContext servletContext;

    private volatile EventDispatcherTracker eventDispatcherTracker;

    // ---------- ServletContextListener

    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void contextInitialized(final ServletContextEvent sce)
    {
        this.servletContext = sce.getServletContext();
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void contextDestroyed(final ServletContextEvent sce)
    {
        if (this.eventDispatcherTracker != null)
        {
            this.eventDispatcherTracker.close();
            this.eventDispatcherTracker = null;
        }
        this.servletContext = null;
    }

    // ---------- HttpSessionListener

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void sessionCreated(final HttpSessionEvent se)
    {
        final HttpSessionListener sessionDispatcher = getSessionDispatcher();
        if (sessionDispatcher != null)
        {
            sessionDispatcher.sessionCreated(se);
        }
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void sessionDestroyed(final HttpSessionEvent se)
    {
        final HttpSessionListener sessionDispatcher = getSessionDispatcher();
        if (sessionDispatcher != null)
        {
            sessionDispatcher.sessionDestroyed(se);
        }
    }

<<<<<<< HEAD
    // ---------- HttpSessionAttributeListener

=======
    // ---------- HttpSessionIdListener

    @Override
    public void sessionIdChanged(final HttpSessionEvent event, final String oldSessionId)
    {
        final HttpSessionIdListener sessionIdDispatcher = getSessionIdDispatcher();
        if (sessionIdDispatcher != null)
        {
            sessionIdDispatcher.sessionIdChanged(event, oldSessionId);
        }
    }

    // ---------- HttpSessionAttributeListener

    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void attributeAdded(final HttpSessionBindingEvent se)
    {
        final HttpSessionAttributeListener attributeDispatcher = getAttributeDispatcher();
        if (attributeDispatcher != null)
        {
            attributeDispatcher.attributeAdded(se);
        }
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void attributeRemoved(final HttpSessionBindingEvent se)
    {
        final HttpSessionAttributeListener attributeDispatcher = getAttributeDispatcher();
        if (attributeDispatcher != null)
        {
            attributeDispatcher.attributeRemoved(se);
        }
    }

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void attributeReplaced(final HttpSessionBindingEvent se)
    {
        final HttpSessionAttributeListener attributeDispatcher = getAttributeDispatcher();
        if (attributeDispatcher != null)
        {
            attributeDispatcher.attributeReplaced(se);
        }
    }

    // ---------- internal

    private Object getDispatcher()
    {
        if (this.eventDispatcherTracker == null)
        {
            // the bundle context may or may not be already provided;
            // if not we cannot access the dispatcher yet
            Object bundleContextAttr = this.servletContext.getAttribute(BundleContext.class.getName());
            if (!(bundleContextAttr instanceof BundleContext))
            {
                return null;
            }

            try
            {
                BundleContext bundleContext = (BundleContext) bundleContextAttr;
<<<<<<< HEAD
                Filter filter = createFilter(bundleContext, null);
                this.eventDispatcherTracker = new ServiceTracker(bundleContext, filter, null)
                {
                    public void removedService(ServiceReference reference, Object service)
                    {
                        ProxyListener.this.sessionDispatcher = null;
                        ProxyListener.this.attributeDispatcher = null;
                        super.removedService(reference, service);
                    }
                };
=======
                this.eventDispatcherTracker = new EventDispatcherTracker(bundleContext);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                this.eventDispatcherTracker.open();
            }
            catch (InvalidSyntaxException e)
            {
                // not expected for our simple filter
            }

        }
        return this.eventDispatcherTracker.getService();
    }

    private HttpSessionListener getSessionDispatcher()
    {
<<<<<<< HEAD
        if (this.sessionDispatcher == null)
        {
            final Object dispatcher = getDispatcher();
            if (dispatcher instanceof HttpSessionListener)
            {
                this.sessionDispatcher = (HttpSessionListener) dispatcher;
            }
        }
        return this.sessionDispatcher;
    }

    private HttpSessionAttributeListener getAttributeDispatcher()
    {
        if (this.attributeDispatcher == null)
        {
            final Object dispatcher = getDispatcher();
            if (dispatcher instanceof HttpSessionAttributeListener)
            {
                this.attributeDispatcher = (HttpSessionAttributeListener) dispatcher;
            }
        }
        return this.attributeDispatcher;
    }

    private static Filter createFilter(BundleContext context, String filter) throws InvalidSyntaxException
    {
        StringBuffer str = new StringBuffer();
        str.append("(&(").append(Constants.OBJECTCLASS).append("=");
        str.append(EventListener.class.getName()).append(")");
        str.append(filter != null ? filter : DispatcherTracker.DEFAULT_FILTER).append(")");
        return context.createFilter(str.toString());
=======
        if (this.eventDispatcherTracker != null)
        {
            return this.eventDispatcherTracker.getHttpSessionListener();
        }
        return null;
    }

    private HttpSessionIdListener getSessionIdDispatcher()
    {
        if (this.eventDispatcherTracker != null)
        {
            return this.eventDispatcherTracker.getHttpSessionIdListener();
        }
        return null;
    }

    private HttpSessionAttributeListener getAttributeDispatcher()
    {
        if (this.eventDispatcherTracker != null)
        {
            return this.eventDispatcherTracker.getHttpSessionAttributeListener();
        }
        return null;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }
}
