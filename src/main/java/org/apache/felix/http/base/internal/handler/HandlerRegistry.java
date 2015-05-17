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

import org.apache.felix.http.base.internal.handler.holder.FilterHolder;
import org.apache.felix.http.base.internal.handler.holder.ServletHolder;
import org.apache.felix.http.base.internal.registry.PathResolution;
import org.apache.felix.http.base.internal.registry.PerContextHandlerRegistry;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.ContextRuntime;
import org.apache.felix.http.base.internal.runtime.dto.FailureRuntime;
import org.apache.felix.http.base.internal.runtime.dto.HandlerRegistryRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ServletRegistryRuntime;

/**
 * Registry for all services.
 *
 * The registry is organized per servlet context and is dispatching to one
 * of the {@link PerContextHandlerRegistry} registries.
 */
public final class HandlerRegistry
{
    private static FilterHolder[] EMPTY_FILTER_HOLDER = new FilterHolder[0];

    /** Current list of context registrations. */
    private volatile List<PerContextHandlerRegistry> registrations = Collections.emptyList();

    private final ServletHandlerRegistry servletRegistry = new ServletHandlerRegistry();

    /**
     * Register default context registry for Http Service
     */
    public void init()
    {
        this.add(new PerContextHandlerRegistry());
        servletRegistry.init();
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

        servletRegistry.shutdown();
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
            this.servletRegistry.add(info);
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
            this.servletRegistry.remove(info);
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

    public void addFilter(@Nonnull final FilterHolder holder)
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
        synchronized ( this )
        {
            for(final PerContextHandlerRegistry r : this.registrations)
            {
                if ( key == r.getContextServiceId())
                {
                    return r;
                }
            }
        }
        return null;
    }

    public ServletHolder getErrorHandler(String requestURI, Long serviceId, int code, Throwable exception)
    {
        ErrorsMapping errorsMapping = getErrorsMapping(requestURI, serviceId);
        if (errorsMapping == null)
        {
            return null;
        }

        // TODO
        return null;
        //return errorsMapping.get(exception, code);
    }

    private ErrorsMapping getErrorsMapping(final String requestURI, final Long serviceId)
    {
        final List<PerContextHandlerRegistry> regs = this.registrations;
        for(final PerContextHandlerRegistry r : regs)
        {
            if ( serviceId != null && serviceId == r.getContextServiceId() )
            {
                return r.getErrorsMapping();
            }
            else if ( serviceId == null && r.isMatching(requestURI) != null )
            {
                return r.getErrorsMapping();
            }
        }

        return null;
    }

    public FilterHolder[] getFilters(@Nonnull final ServletHolder servletHolder,
            final DispatcherType dispatcherType,
            @Nonnull final String requestURI)
    {
        final long key = servletHolder.getContextServiceId();
        final PerContextHandlerRegistry reg = this.getRegistry(key);
        if ( reg != null )
        {
            return reg.getFilterHolders(servletHolder, dispatcherType, requestURI);
        }
        return EMPTY_FILTER_HOLDER;
    }

    public void addServlet(final ServletHolder holder)
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
                    // remove context path from request URI
                    ps.requestURI = path;
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
    public ServletHolder resolveServletByName(final Long contextId, @Nonnull final String name)
    {
        final PerContextHandlerRegistry reg = (contextId == null ? null : this.getRegistry(contextId));
        if ( reg != null )
        {
            return reg.resolveServletByName(name);
        }
        return null;
    }

    public synchronized HandlerRegistryRuntime getRuntime(FailureRuntime.Builder failureRuntimeBuilder)
    {
        List<ContextRuntime> handlerRuntimes = new ArrayList<ContextRuntime>();
        for (PerContextHandlerRegistry contextRegistry : this.registrations)
        {
            // TODO
            // handlerRuntimes.add(contextRegistry.getRuntime(failureRuntimeBuilder));
        }
        ServletRegistryRuntime servletRegistryRuntime = servletRegistry.getRuntime(failureRuntimeBuilder);
        return new HandlerRegistryRuntime(handlerRuntimes, servletRegistryRuntime);
    }
}
