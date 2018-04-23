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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.registry.PerContextHandlerRegistry;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.service.HttpServiceFactory;
import org.apache.felix.http.base.internal.service.PerBundleHttpServiceImpl;
import org.osgi.framework.Bundle;

public class HttpServiceContextHandler extends WhiteboardContextHandler
{
    private final PerContextHandlerRegistry registry;

    private final HttpServiceFactory httpServiceFactory;

    /** A map of all created servlet contexts. Each bundle gets it's own instance. */
    private final Map<Long, ContextHolder> perBundleContextMap = new HashMap<Long, ContextHolder>();

    private final ServletContext sharedContext;

    public HttpServiceContextHandler(final ServletContextHelperInfo info,
            final PerContextHandlerRegistry registry,
            final HttpServiceFactory httpServiceFactory,
            final ServletContext webContext,
            final Bundle httpBundle)
    {
        super(info, webContext, httpBundle);
        this.registry = registry;
        this.httpServiceFactory = httpServiceFactory;
        this.sharedContext = webContext;
    }

    @Override
    public PerContextHandlerRegistry getRegistry()
    {
        return this.registry;
    }

    @Override
    public ServletContext getSharedContext()
    {
        return this.sharedContext;
    }

    @Override
    public @CheckForNull ExtServletContext getServletContext(@CheckForNull final Bundle bundle)
    {
        if ( bundle == null )
        {
            return null;
        }
        final Long key = bundle.getBundleId();
        synchronized ( this.perBundleContextMap )
        {
            ContextHolder holder = this.perBundleContextMap.get(key);
            if ( holder == null )
            {
                holder = new ContextHolder();
                final PerBundleHttpServiceImpl service = (PerBundleHttpServiceImpl)this.httpServiceFactory.getService(bundle, null);
                holder.servletContext = service.getServletContext(service.createDefaultHttpContext());
                holder.httpService = service;
                this.perBundleContextMap.put(key, holder);
            }
            holder.counter++;

            return holder.servletContext;
        }
    }

    @Override
    public void ungetServletContext(@Nonnull final Bundle bundle)
    {
        final Long key = bundle.getBundleId();
        synchronized ( this.perBundleContextMap )
        {
            ContextHolder holder = this.perBundleContextMap.get(key);
            if ( holder != null )
            {
                holder.counter--;
                if ( holder.counter == 0 )
                {
                    this.perBundleContextMap.remove(key);
                    holder.httpService.unregisterAll();
                }
            }
        }
    }

    private static final class ContextHolder
    {
        public long counter;
        public ExtServletContext servletContext;
        public PerBundleHttpServiceImpl httpService;
    }
}
