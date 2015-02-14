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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.http.base.internal.dispatch.Dispatcher;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.handler.HttpServicePlugin;
import org.apache.felix.http.base.internal.handler.HttpSessionWrapper;
import org.apache.felix.http.base.internal.runtime.HttpServiceRuntimeImpl;
import org.apache.felix.http.base.internal.service.HttpServiceFactory;
import org.apache.felix.http.base.internal.service.listener.HttpSessionAttributeListenerManager;
import org.apache.felix.http.base.internal.service.listener.HttpSessionListenerManager;
import org.apache.felix.http.base.internal.service.listener.ServletContextAttributeListenerManager;
import org.apache.felix.http.base.internal.service.listener.ServletRequestAttributeListenerManager;
import org.apache.felix.http.base.internal.service.listener.ServletRequestListenerManager;
import org.apache.felix.http.base.internal.whiteboard.WhiteboardHttpService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;

public final class HttpServiceController
{
    /**
     * Name of the Framework property indicating whether the servlet context
     * attributes of the ServletContext objects created for each HttpContext
     * used to register servlets and resources share their attributes or not.
     * By default (if this property is not specified or it's value is not
     * <code>true</code> (case-insensitive)) servlet context attributes are not
     * shared. To have servlet context attributes shared amongst servlet context
     * and also with the ServletContext provided by the servlet container ensure
     * setting the property as follows:
     * <pre>
     * org.apache.felix.http.shared_servlet_context_attributes = true
     * </pre>
     * <p>
     * <b>WARNING:</b> Only set this property if absolutely needed (for example
     * you implement an HttpSessionListener and want to access servlet context
     * attributes of the ServletContext to which the HttpSession is linked).
     * Otherwise leave this property unset.
     */
    private static final String FELIX_HTTP_SHARED_SERVLET_CONTEXT_ATTRIBUTES = "org.apache.felix.http.shared_servlet_context_attributes";

    /** Compat property for previous versions. */
    private static final String OBSOLETE_REG_PROPERTY_ENDPOINTS = "osgi.http.service.endpoints";

    private final BundleContext bundleContext;
    private final HandlerRegistry registry;
    private final Dispatcher dispatcher;
    private final ServletContextAttributeListenerManager contextAttributeListener;
    private final ServletRequestListenerManager requestListener;
    private final ServletRequestAttributeListenerManager requestAttributeListener;
    private final HttpSessionListenerManager sessionListener;
    private final HttpSessionAttributeListenerManager sessionAttributeListener;
    private final boolean sharedContextAttributes;
    private final HttpServicePlugin plugin;
    private volatile WhiteboardHttpService whiteboardHttpService;

    private final Hashtable<String, Object> httpServiceProps = new Hashtable<String, Object>();;
    private volatile ServiceRegistration httpServiceReg;

    private volatile ServiceRegistration<HttpServiceRuntime> runtimeServiceReg;
    private final Hashtable<String, Object> runtimeServiceProps = new Hashtable<String, Object>();;

    public HttpServiceController(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
        this.registry = new HandlerRegistry();
        this.dispatcher = new Dispatcher(this.registry);
        this.contextAttributeListener = new ServletContextAttributeListenerManager(bundleContext);
        this.requestListener = new ServletRequestListenerManager(bundleContext);
        this.requestAttributeListener = new ServletRequestAttributeListenerManager(bundleContext);
        this.sessionListener = new HttpSessionListenerManager(bundleContext);
        this.sessionAttributeListener = new HttpSessionAttributeListenerManager(bundleContext);
        this.sharedContextAttributes = getBoolean(FELIX_HTTP_SHARED_SERVLET_CONTEXT_ATTRIBUTES);
        this.plugin = new HttpServicePlugin(bundleContext, registry);
    }

    public Dispatcher getDispatcher()
    {
        return this.dispatcher;
    }

    public ServletContextAttributeListenerManager getContextAttributeListener()
    {
        return contextAttributeListener;
    }

    public ServletRequestListenerManager getRequestListener()
    {
        return requestListener;
    }

    public ServletRequestAttributeListenerManager getRequestAttributeListener()
    {
        return requestAttributeListener;
    }

    public HttpSessionListener getSessionListener()
    {
        return new HttpSessionListener() {

            @Override
            public void sessionDestroyed(final HttpSessionEvent se) {
                sessionListener.sessionDestroyed(se);
                whiteboardHttpService.sessionDestroyed(se.getSession(), HttpSessionWrapper.getSessionContextIds(se.getSession()));
            }

            @Override
            public void sessionCreated(final HttpSessionEvent se) {
                sessionListener.sessionCreated(se);
            }
        };
    }

    public HttpSessionAttributeListener getSessionAttributeListener()
    {
        return sessionAttributeListener;
    }

    public void setProperties(Hashtable<String, Object> props)
    {
        this.httpServiceProps.clear();
        this.httpServiceProps.putAll(props);

        if ( this.httpServiceProps.get(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT_ATTRIBUTE) != null )
        {
            this.httpServiceProps.put(OBSOLETE_REG_PROPERTY_ENDPOINTS,
                    this.httpServiceProps.get(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT_ATTRIBUTE));
        }

        // runtime service gets the same props for now
        this.runtimeServiceProps.clear();
        this.runtimeServiceProps.putAll(this.httpServiceProps);

        if (this.httpServiceReg != null)
        {
            this.httpServiceReg.setProperties(this.httpServiceProps);

            this.runtimeServiceProps.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ID_ATTRIBUTE,
                    this.httpServiceReg.getReference().getProperty(Constants.SERVICE_ID));
            this.runtimeServiceReg.setProperties(this.runtimeServiceProps);
        }
    }

    public void register(ServletContext servletContext)
    {
        this.contextAttributeListener.open();
        this.requestListener.open();
        this.requestAttributeListener.open();
        this.sessionListener.open();
        this.sessionAttributeListener.open();
        this.plugin.register();

        String[] ifaces = new String[] { HttpService.class.getName(), ExtHttpService.class.getName() };
        HttpServiceFactory factory = new HttpServiceFactory(servletContext, this.registry, this.contextAttributeListener, this.sharedContextAttributes);

        this.httpServiceReg = this.bundleContext.registerService(ifaces, factory, this.httpServiceProps);

        this.runtimeServiceProps.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ID_ATTRIBUTE,
                this.httpServiceReg.getReference().getProperty(Constants.SERVICE_ID));
        this.runtimeServiceReg = this.bundleContext.registerService(HttpServiceRuntime.class,
                new HttpServiceRuntimeImpl(),
                this.runtimeServiceProps);

        this.whiteboardHttpService = new WhiteboardHttpService(this.bundleContext,
                servletContext,
                this.registry,
                this.runtimeServiceReg.getReference());
        this.dispatcher.setWhiteboardHttpService(this.whiteboardHttpService);
    }

    public void unregister()
    {
        this.dispatcher.setWhiteboardHttpService(null);
        if ( this.whiteboardHttpService != null )
        {
            this.whiteboardHttpService.close();
            this.whiteboardHttpService = null;
        }

        if ( this.runtimeServiceReg != null )
        {
            this.runtimeServiceReg.unregister();
            this.runtimeServiceReg = null;
        }

        this.sessionAttributeListener.close();
        this.sessionListener.close();
        this.contextAttributeListener.close();
        this.requestListener.close();
        this.requestAttributeListener.close();

        this.plugin.unregister();

        if ( this.httpServiceReg != null )
        {
            try
            {
                this.httpServiceReg.unregister();
                this.registry.shutdown();
            }
            finally
            {
                this.httpServiceReg = null;
            }
        }
    }

    private boolean getBoolean(final String property)
    {
        String prop = this.bundleContext.getProperty(property);
        return (prop != null) ? Boolean.valueOf(prop).booleanValue() : false;
    }
}
