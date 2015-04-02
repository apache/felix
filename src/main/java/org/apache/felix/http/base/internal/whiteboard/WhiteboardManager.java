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

import java.util.Hashtable;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.handler.HttpSessionWrapper;
import org.apache.felix.http.base.internal.service.HttpServiceFactory;
import org.apache.felix.http.base.internal.service.HttpServiceRuntimeImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;

public final class WhiteboardManager
{
    private final BundleContext bundleContext;

    private final HttpServiceFactory httpServiceFactory;

    private final HttpServiceRuntimeImpl serviceRuntime;

    private final ServletContextHelperManager contextManager;

    private volatile ServiceRegistration<HttpServiceRuntime> runtimeServiceReg;

    /**
     * Create a new whiteboard http manager
     * @param bundleContext
     * @param httpServiceFactory
     * @param registry
     */
    public WhiteboardManager(final BundleContext bundleContext,
            final HttpServiceFactory httpServiceFactory,
            final HandlerRegistry registry)
    {
        this.bundleContext = bundleContext;
        this.httpServiceFactory = httpServiceFactory;
        this.contextManager = new ServletContextHelperManager(bundleContext, registry);
        this.serviceRuntime = new HttpServiceRuntimeImpl(registry, this.contextManager);
    }

    public void start(final ServletContext context)
    {
        // TODO set Endpoint
        this.serviceRuntime.setAttribute(HttpServiceRuntimeConstants.HTTP_SERVICE_ID_ATTRIBUTE,
                this.httpServiceFactory.getHttpServiceServiceId());
        this.runtimeServiceReg = this.bundleContext.registerService(HttpServiceRuntime.class,
                serviceRuntime,
                this.serviceRuntime.getAttributes());

        this.contextManager.start(context, this.runtimeServiceReg.getReference());
    }

    public void stop()
    {
        this.contextManager.stop();

        if ( this.runtimeServiceReg != null )
        {
            this.runtimeServiceReg.unregister();
            this.runtimeServiceReg = null;
        }
    }

    public void setProperties(final Hashtable<String, Object> props)
    {
        // runtime service gets the same props for now
        this.serviceRuntime.setAllAttributes(props);

        if (this.runtimeServiceReg != null)
        {
            this.serviceRuntime.setAttribute(HttpServiceRuntimeConstants.HTTP_SERVICE_ID_ATTRIBUTE,
                    this.httpServiceFactory.getHttpServiceServiceId());
            this.runtimeServiceReg.setProperties(this.serviceRuntime.getAttributes());
        }
    }

    public void sessionDestroyed(@Nonnull final HttpSession session, final Set<Long> contextIds)
    {
        for(final Long contextId : contextIds)
        {
            // TODO - on shutdown context manager is already NULL which shouldn't be the case
            if ( this.contextManager != null )
            {
                final ContextHandler handler = this.contextManager.getContextHandler(contextId);
                if ( handler != null )
                {
                    final ExtServletContext context = handler.getServletContext(this.bundleContext.getBundle());
                    new HttpSessionWrapper(contextId, session, context, true).invalidate();
                    handler.ungetServletContext(this.bundleContext.getBundle());
                }
            }
        }
    }
}
