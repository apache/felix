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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.HttpServiceServletHandler;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.registry.HandlerRegistry;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.service.http.NamespaceException;

public final class SharedHttpServiceImpl
{
    private final HandlerRegistry handlerRegistry;

    private final Map<String, ServletHandler> aliasMap = new HashMap<String, ServletHandler>();

    public SharedHttpServiceImpl(final HandlerRegistry handlerRegistry)
    {
        if (handlerRegistry == null)
        {
            throw new IllegalArgumentException("HandlerRegistry cannot be null!");
        }

        this.handlerRegistry = handlerRegistry;
    }

    /**
     * Register a filter
     */
    public boolean registerFilter(@Nonnull final FilterHandler handler)
    {
        this.handlerRegistry.getRegistry(handler.getContextServiceId()).registerFilter(handler);
        return true;
    }

    /**
     * Register a servlet
     */
    public void registerServlet(@Nonnull final String alias,
            @Nonnull final ExtServletContext httpContext,
            @Nonnull final Servlet servlet,
            @Nonnull final ServletInfo servletInfo) throws ServletException, NamespaceException
    {
        final ServletHandler handler = new HttpServiceServletHandler(httpContext, servletInfo, servlet);

        synchronized (this.aliasMap)
        {
            if (this.aliasMap.containsKey(alias))
            {
                throw new NamespaceException("Alias " + alias + " is already in use.");
            }
            this.handlerRegistry.getRegistry(handler.getContextServiceId()).registerServlet(handler);

            this.aliasMap.put(alias, handler);
        }
    }

    /**
     * @see org.osgi.service.http.HttpService#unregister(java.lang.String)
     */
    public Servlet unregister(final String alias)
    {
        synchronized (this.aliasMap)
        {
            final ServletHandler handler = this.aliasMap.remove(alias);
            if (handler == null)
            {
                throw new IllegalArgumentException("Nothing registered at " + alias);
            }

            final Servlet s = handler.getServlet();
            this.handlerRegistry.getRegistry(handler.getContextServiceId()).unregisterServlet(handler.getServletInfo(), true);
            return s;
        }
    }

    public void unregisterServlet(final Servlet servlet, final boolean destroy)
    {
        if (servlet != null)
        {
            synchronized (this.aliasMap)
            {
                final Iterator<Map.Entry<String, ServletHandler>> i = this.aliasMap.entrySet().iterator();
                while (i.hasNext())
                {
                    final Map.Entry<String, ServletHandler> entry = i.next();
                    if (entry.getValue().getServlet() == servlet)
                    {
                        this.handlerRegistry.getRegistry(entry.getValue().getContextServiceId()).unregisterServlet(entry.getValue().getServletInfo(), destroy);

                        i.remove();
                        break;
                    }

                }
            }
        }
    }

    public void unregisterFilter(final FilterHandler handler, final boolean destroy)
    {
        if (handler != null)
        {
            this.handlerRegistry.getRegistry(handler.getContextServiceId()).unregisterFilter(handler.getFilterInfo(), destroy);
        }
    }

	public HandlerRegistry getHandlerRegistry() 
	{
		return this.handlerRegistry;
	}
}
