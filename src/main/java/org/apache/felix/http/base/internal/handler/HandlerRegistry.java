/*
 * Licensed to the Apaanche Software Foundation (ASF) under one or more
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
package org.apache.felix.http.base.internal.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.DispatcherType;

import org.apache.felix.http.base.internal.registry.PathResolution;
import org.apache.felix.http.base.internal.registry.PerContextHandlerRegistry;
import org.apache.felix.http.base.internal.registry.ServletResolution;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.ContextRuntime;
import org.apache.felix.http.base.internal.runtime.dto.FailureRuntime;
import org.apache.felix.http.base.internal.runtime.dto.HandlerRegistryRuntime;

/**
 * Registry for all services.
 *
 * The registry is organized per servlet context and is dispatching to one
 * of the {@link PerContextHandlerRegistry} registries.
 */
public final class HandlerRegistry
{
    private static FilterHandler[] EMPTY_FILTER_HOLDER = new FilterHandler[0];

    /** Current list of context registrations. */
    private volatile List<PerContextHandlerRegistry> registrations = Collections.emptyList();

    /**
     * Register default context registry for Http Service
     */
    public void init()
    {
        this.add(new PerContextHandlerRegistry());
    }

    /**
     * Shutdown
     */
    public void shutdown()
    {
        final List<PerContextHandlerRegistry> list;

        synchronized ( this )
        {
            list = new ArrayList<PerContextHandlerRegistry>(this.registrations);
            this.registrations = Collections.emptyList();

        }

        for(final PerContextHandlerRegistry r : list)
        {
            r.removeAll();
        }
    }

    /**
     * Add a context registration.
     * @param info The servlet context helper info
     */
    public void add(@Nonnull ServletContextHelperInfo info)
    {
        synchronized ( this )
        {
            this.add(new PerContextHandlerRegistry(info));
        }
    }

    /**
     * Remove a context registration.
     * @param info The servlet context helper info
     */
    public void remove(@Nonnull ServletContextHelperInfo info)
    {
        synchronized ( this )
        {
            final List<PerContextHandlerRegistry> updatedList = new ArrayList<PerContextHandlerRegistry>(this.registrations);
            final Iterator<PerContextHandlerRegistry> i = updatedList.iterator();
            while ( i.hasNext() )
            {
                final PerContextHandlerRegistry reg = i.next();
                if ( reg.getContextServiceId() == info.getServiceId() )
                {
                    i.remove();
                    this.registrations = updatedList;
                    break;
                }
            }
        }
    }


    /**
     * Add a new context registration.
     */
    private void add(@Nonnull PerContextHandlerRegistry registry)
    {
        synchronized ( this )
        {
            final List<PerContextHandlerRegistry> updatedList = new ArrayList<PerContextHandlerRegistry>(this.registrations);
            updatedList.add(registry);
            Collections.sort(updatedList);

            this.registrations = updatedList;
        }
    }

    public void addFilter(@Nonnull final FilterHandler holder)
    {
        final PerContextHandlerRegistry reg = this.getRegistry(holder.getContextServiceId());
        // TODO - check whether we need to handle the null case as well
        //        it shouldn't be required as we only get here if the context exists
        if ( reg != null )
        {
            reg.addFilter(holder);
        }
    }

    public void removeFilter(final long contextId, @Nonnull final FilterInfo info, final boolean destroy)
    {
        final PerContextHandlerRegistry reg = this.getRegistry(contextId);
        // TODO - check whether we need to handle the null case as well
        //        it shouldn't be required as we only get here if the context exists
        if ( reg != null )
        {
            reg.removeFilter(info, destroy);
        }
    }

    private PerContextHandlerRegistry getRegistry(final long key)
    {
        final List<PerContextHandlerRegistry> list = this.registrations;
        for(final PerContextHandlerRegistry r : list)
        {
            if ( key == r.getContextServiceId())
            {
                return r;
            }
        }
        return null;
    }

    public ServletResolution getErrorHandler(String requestURI, Long serviceId, int code, Throwable exception)
    {
        final PerContextHandlerRegistry reg;
        if ( serviceId == null )
        {
            // if the context is unknown, we use the first matching one!
            PerContextHandlerRegistry found = null;
            final List<PerContextHandlerRegistry> regs = this.registrations;
            for(final PerContextHandlerRegistry r : regs)
            {
                final String path = r.isMatching(requestURI);
                if ( path != null )
                {
                    found = r;
                    break;
                }
            }
            reg = found;
        }
        else
        {
            reg = this.getRegistry(serviceId);
        }
        if ( reg != null )
        {
            final ServletHandler holder = reg.getErrorHandler(code, exception);
            if ( holder != null )
            {
                final ServletResolution res = new ServletResolution();
                res.holder = holder;
                res.handlerRegistry = reg;

                return res;
            }
        }
        return null;
    }

    public FilterHandler[] getFilters(@Nonnull final ServletResolution pr,
            final DispatcherType dispatcherType,
            @Nonnull String requestURI)
    {
        if ( pr != null && pr.handlerRegistry != null )
        {
            return pr.handlerRegistry.getFilterHandlers(pr.holder, dispatcherType, requestURI);
        }
        return EMPTY_FILTER_HOLDER;
    }

    public void addServlet(final ServletHandler holder)
    {
        final PerContextHandlerRegistry reg = this.getRegistry(holder.getContextServiceId());
        // TODO - check whether we need to handle the null case as well
        //        it shouldn't be required as we only get here if the context exists
        if ( reg != null )
        {
            reg.addServlet(holder);
        }
    }

    public void removeServlet(final long contextId, final ServletInfo info, final boolean destroy)
    {
        final PerContextHandlerRegistry reg = this.getRegistry(contextId);
        // TODO - check whether we need to handle the null case as well
        //        it shouldn't be required as we only get here if the context exists
        if ( reg != null )
        {
            reg.removeServlet(info, destroy);
        }

    }

    public PathResolution resolveServlet(final String requestURI)
    {
        final List<PerContextHandlerRegistry> regs = this.registrations;
        for(final PerContextHandlerRegistry r : regs)
        {
            final String path = r.isMatching(requestURI);
            if ( path != null )
            {
                final PathResolution ps = r.resolve(path);
                if ( ps != null )
                {
                    // remove context path from request URI and add registry object
                    ps.requestURI = path;
                    ps.handlerRegistry = r;
                    return ps;
                }
            }
        }

        return null;
    }

    /**
     * Get the servlet holder for a servlet by name
     * @param contextId The context id or {@code null}
     * @param name The servlet name
     * @return The servlet holder or {@code null}
     */
    public ServletResolution resolveServletByName(final Long contextId, @Nonnull final String name)
    {
        final PerContextHandlerRegistry reg = (contextId == null ? null : this.getRegistry(contextId));
        if ( reg != null )
        {
            final ServletHandler holder = reg.resolveServletByName(name);
            if ( holder != null )
            {
                final ServletResolution resolution = new ServletResolution();
                resolution.holder = holder;
                resolution.handlerRegistry = reg;

                return resolution;
            }
        }
        return null;
    }

    public HandlerRegistryRuntime getRuntime(FailureRuntime.Builder failureRuntimeBuilder)
    {
        final List<ContextRuntime> handlerRuntimes = new ArrayList<ContextRuntime>();

        final List<PerContextHandlerRegistry> regs = this.registrations;
        for (final PerContextHandlerRegistry contextRegistry : regs)
        {
            handlerRuntimes.add(contextRegistry.getRuntime(failureRuntimeBuilder));
        }

        return new HandlerRegistryRuntime(handlerRuntimes);
    }
}
