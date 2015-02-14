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

import org.apache.felix.http.base.internal.dispatch.Dispatcher;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.handler.HttpServicePlugin;
import org.apache.felix.http.base.internal.handler.HttpSessionWrapper;
import org.apache.felix.http.base.internal.runtime.HttpServiceRuntimeImpl;
import org.apache.felix.http.base.internal.service.HttpServiceFactory;
import org.apache.felix.http.base.internal.service.listener.ServletContextAttributeListenerManager;
import org.apache.felix.http.base.internal.service.listener.ServletRequestAttributeListenerManager;
import org.apache.felix.http.base.internal.service.listener.ServletRequestListenerManager;
import org.apache.felix.http.base.internal.whiteboard.WhiteboardHttpService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;

public final class HttpServiceController
{
    private final BundleContext bundleContext;
    private final HandlerRegistry registry;
    private final Dispatcher dispatcher;
    private final HttpServicePlugin plugin;
    private final HttpServiceFactory httpServiceFactory;
    private volatile WhiteboardHttpService whiteboardHttpService;

    private volatile ServiceRegistration<HttpServiceRuntime> runtimeServiceReg;
    private final Hashtable<String, Object> runtimeServiceProps = new Hashtable<String, Object>();;

    public HttpServiceController(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
        this.registry = new HandlerRegistry();
        this.dispatcher = new Dispatcher(this.registry);
        this.plugin = new HttpServicePlugin(bundleContext, registry);
        this.httpServiceFactory = new HttpServiceFactory(this.bundleContext, this.registry);
    }

    public Dispatcher getDispatcher()
    {
        return this.dispatcher;
    }

    ServletContextAttributeListenerManager getContextAttributeListener()
    {
        return this.httpServiceFactory.getContextAttributeListener();
    }

    ServletRequestListenerManager getRequestListener()
    {
        return this.httpServiceFactory.getRequestListener();
    }

    ServletRequestAttributeListenerManager getRequestAttributeListener()
    {
        return this.httpServiceFactory.getRequestAttributeListener();
    }

    HttpSessionListener getSessionListener()
    {
        return new HttpSessionListener() {

            @Override
            public void sessionDestroyed(final HttpSessionEvent se) {
                httpServiceFactory.getSessionListener().sessionDestroyed(se);
                whiteboardHttpService.sessionDestroyed(se.getSession(), HttpSessionWrapper.getSessionContextIds(se.getSession()));
            }

            @Override
            public void sessionCreated(final HttpSessionEvent se) {
                httpServiceFactory.getSessionListener().sessionCreated(se);
            }
        };
    }

    HttpSessionAttributeListener getSessionAttributeListener()
    {
        return httpServiceFactory.getSessionAttributeListener();
    }

    public void setProperties(final Hashtable<String, Object> props)
    {
        this.httpServiceFactory.setProperties(props);

        // runtime service gets the same props for now
        this.runtimeServiceProps.clear();
        this.runtimeServiceProps.putAll(props);

        if (this.runtimeServiceReg != null)
        {
            this.runtimeServiceProps.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ID_ATTRIBUTE,
                    this.httpServiceFactory.getHttpServiceServiceId());
            this.runtimeServiceReg.setProperties(this.runtimeServiceProps);
        }
    }

    public void register(final ServletContext servletContext)
    {
        this.plugin.register();

        this.httpServiceFactory.start(servletContext);

        this.runtimeServiceProps.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ID_ATTRIBUTE,
                this.httpServiceFactory.getHttpServiceServiceId());
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

        this.plugin.unregister();

        if ( this.httpServiceFactory != null )
        {
            this.httpServiceFactory.stop();
        }

        this.registry.shutdown();
    }
}
