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
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.PerContextHandlerRegistry;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.service.http.NamespaceException;

public final class SharedHttpServiceImpl
{
    private final PerContextHandlerRegistry handlerRegistry;

    private final Map<String, ServletHandler> aliasMap = new HashMap<String, ServletHandler>();

    public SharedHttpServiceImpl(final PerContextHandlerRegistry handlerRegistry)
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
    public boolean registerFilter(@Nonnull final ExtServletContext httpContext,
            @Nonnull final Filter filter,
            @Nonnull final FilterInfo filterInfo)
    {
        final FilterHandler handler = new FilterHandler(null, httpContext, filter, filterInfo);
        try
        {
            this.handlerRegistry.addFilter(handler);
            return true;
        }
        catch (final ServletException e)
        {
            // TODO create failure DTO
        }
        return false;
    }

    /**
     * Register a servlet
     */
    public void registerServlet(@Nonnull final String alias,
            @Nonnull final ExtServletContext httpContext,
            @Nonnull final Servlet servlet,
            @Nonnull final ServletInfo servletInfo) throws ServletException, NamespaceException
    {
        final ServletHandler handler = new ServletHandler(null, httpContext, servletInfo, servlet);

        synchronized (this.aliasMap)
        {
            if (this.aliasMap.containsKey(alias))
            {
                throw new NamespaceException("Alias " + alias + " is already in use.");
            }
            this.handlerRegistry.addServlet(handler);

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
            return this.handlerRegistry.removeServlet(handler.getServletInfo(), true);
        }
    }

    public void unregisterServlet(final Servlet servlet, final boolean destroy)
    {
        if (servlet != null)
        {
            this.handlerRegistry.removeServlet(servlet, destroy);
            synchronized (this.aliasMap)
            {
                final Iterator<Map.Entry<String, ServletHandler>> i = this.aliasMap.entrySet().iterator();
                while (i.hasNext())
                {
                    final Map.Entry<String, ServletHandler> entry = i.next();
                    if (entry.getValue().getServlet() == servlet)
                    {
                        i.remove();
                        break;
                    }

                }
            }
        }
    }

    public void unregisterFilter(final Filter filter, final boolean destroy)
    {
        if (filter != null)
        {
            this.handlerRegistry.removeFilter(filter, destroy);
        }
    }
}
