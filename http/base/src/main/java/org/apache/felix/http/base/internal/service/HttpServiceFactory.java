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
package org.apache.felix.http.base.internal.service;

import java.util.Hashtable;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.http.base.internal.registry.HandlerRegistry;
import org.apache.felix.http.base.internal.service.listener.HttpSessionAttributeListenerManager;
import org.apache.felix.http.base.internal.service.listener.HttpSessionListenerManager;
import org.apache.felix.http.base.internal.service.listener.ServletContextAttributeListenerManager;
import org.apache.felix.http.base.internal.service.listener.ServletRequestAttributeListenerManager;
import org.apache.felix.http.base.internal.service.listener.ServletRequestListenerManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;

public final class HttpServiceFactory
    implements ServiceFactory<HttpService>
{
    public static final String HTTP_SERVICE_CONTEXT_NAME = "org.osgi.service.http";

    public static final long HTTP_SERVICE_CONTEXT_SERVICE_ID = -1;


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

    /** Compatibility property with previous versions. */
    private static final String OBSOLETE_REG_PROPERTY_ENDPOINTS = "osgi.http.service.endpoints";

    private volatile boolean active = false;
    private final BundleContext bundleContext;
    private final boolean sharedContextAttributes;

    private final ServletContextAttributeListenerManager contextAttributeListenerManager;
    private final ServletRequestListenerManager requestListenerManager;
    private final ServletRequestAttributeListenerManager requestAttributeListenerManager;
    private final HttpSessionListenerManager sessionListenerManager;
    private final HttpSessionAttributeListenerManager sessionAttributeListenerManager;

    private final Hashtable<String, Object> httpServiceProps = new Hashtable<String, Object>();
    private volatile ServletContext context;
    private volatile ServiceRegistration<?> httpServiceReg;

    private final HandlerRegistry handlerRegistry;
    private volatile SharedHttpServiceImpl sharedHttpService;

    public HttpServiceFactory(final BundleContext bundleContext,
            final HandlerRegistry handlerRegistry)
    {
        this.bundleContext = bundleContext;
        this.handlerRegistry = handlerRegistry;
        this.sharedContextAttributes = getBoolean(FELIX_HTTP_SHARED_SERVLET_CONTEXT_ATTRIBUTES);

        this.contextAttributeListenerManager = new ServletContextAttributeListenerManager(bundleContext);
        this.requestListenerManager = new ServletRequestListenerManager(bundleContext);
        this.requestAttributeListenerManager = new ServletRequestAttributeListenerManager(bundleContext);
        this.sessionListenerManager = new HttpSessionListenerManager(bundleContext);
        this.sessionAttributeListenerManager = new HttpSessionAttributeListenerManager(bundleContext);
    }

    public void start(final ServletContext context)
    {
        this.context = context;
        this.contextAttributeListenerManager.open();
        this.requestListenerManager.open();
        this.requestAttributeListenerManager.open();
        this.sessionListenerManager.open();
        this.sessionAttributeListenerManager.open();

        this.sharedHttpService = new SharedHttpServiceImpl(handlerRegistry);

        final String[] ifaces = new String[] { HttpService.class.getName(), ExtHttpService.class.getName() };
        this.active = true;
        this.httpServiceReg = bundleContext.registerService(ifaces, this, this.httpServiceProps);
    }

    public void stop()
    {
        this.active = false;

        if ( this.httpServiceReg != null )
        {
            this.httpServiceReg.unregister();
            this.httpServiceReg = null;
        }

        this.context = null;
        this.sharedHttpService = null;

        this.contextAttributeListenerManager.close();
        this.requestListenerManager.close();
        this.requestAttributeListenerManager.close();
        this.sessionListenerManager.close();
        this.sessionAttributeListenerManager.close();
    }

    @Override
    public HttpService getService(final Bundle bundle, final ServiceRegistration<HttpService> reg)
    {
        // Only return a service after start() has been called.
        if (active) {
            // Store everything that we want to pass to the PerBundleHttpServiceImpl in local vars to avoid
            // a race condition where the service might be stopped while this method is executing.
            SharedHttpServiceImpl sharedHttpSvc = this.sharedHttpService;
            ServletContext servletCtx = this.context;
            ServletContextAttributeListenerManager servletCtxAttrListenerMgr = this.contextAttributeListenerManager;
            boolean sharedCtxAttrs = this.sharedContextAttributes;
            ServletRequestListenerManager reqListenerMgr = this.requestListenerManager;
            ServletRequestAttributeListenerManager reqAttrListenerMgr = this.requestAttributeListenerManager;

            if ( active ) {
                // Only return the service if we're still active
                return new PerBundleHttpServiceImpl(bundle,
                        sharedHttpSvc,
                        servletCtx,
                        servletCtxAttrListenerMgr,
                        sharedCtxAttrs,
                        reqListenerMgr,
                        reqAttrListenerMgr);
            }
        }
        return null;
    }

    @Override
    public void ungetService(final Bundle bundle, final ServiceRegistration<HttpService> reg,
            final HttpService service)
    {
        if ( service instanceof PerBundleHttpServiceImpl )
        {
            ((PerBundleHttpServiceImpl)service).unregisterAll();
        }
    }

    public ServletContextAttributeListenerManager getContextAttributeListener()
    {
        return contextAttributeListenerManager;
    }

    public HttpSessionListener getSessionListener()
    {
        return sessionListenerManager;
    }

    public HttpSessionAttributeListener getSessionAttributeListener()
    {
        return sessionAttributeListenerManager;
    }

    public long getHttpServiceServiceId()
    {
        return (Long) this.httpServiceReg.getReference().getProperty(Constants.SERVICE_ID);
    }

    public void setProperties(final Hashtable<String, Object> props)
    {
        this.httpServiceProps.clear();
        this.httpServiceProps.putAll(props);

        if ( this.httpServiceProps.get(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT) != null )
        {
            this.httpServiceProps.put(OBSOLETE_REG_PROPERTY_ENDPOINTS,
                    this.httpServiceProps.get(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT));
        }

        if (this.httpServiceReg != null)
        {
            this.httpServiceReg.setProperties(this.httpServiceProps);
        }
    }

    private boolean getBoolean(final String property)
    {
        String prop = this.bundleContext.getProperty(property);
        return (prop != null) ? Boolean.valueOf(prop).booleanValue() : false;
    }
}
