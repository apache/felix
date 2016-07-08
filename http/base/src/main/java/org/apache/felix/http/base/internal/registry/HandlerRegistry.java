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
package org.apache.felix.http.base.internal.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.DispatcherType;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.dto.FailedDTOHolder;
import org.osgi.service.http.runtime.dto.ServletContextDTO;

/**
 * Registry for all services.
 *
 * The registry is organized per servlet context and is dispatching to one
 * of the {@link PerContextHandlerRegistry} registries.
 */
public final class HandlerRegistry
{
    private static FilterHandler[] EMPTY_FILTER_HANDLER = new FilterHandler[0];

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
     * Reset to initial state
     */
    public void reset()
    {
        this.registrations.clear();
        this.init();
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
    public void add(@Nonnull PerContextHandlerRegistry registry)
    {
        synchronized ( this )
        {
            final List<PerContextHandlerRegistry> updatedList = new ArrayList<PerContextHandlerRegistry>(this.registrations);
            updatedList.add(registry);
            Collections.sort(updatedList);

            this.registrations = updatedList;
        }
    }

    public PerContextHandlerRegistry getRegistry(final long key)
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

    public @CheckForNull ServletResolution getErrorHandler(@Nonnull final String requestURI,
            final Long serviceId,
            final int code,
            final Throwable exception)
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
            final ServletHandler handler = reg.getErrorHandler(code, exception);
            if ( handler != null )
            {
                final ServletResolution res = new ServletResolution();
                res.handler = handler;
                res.handlerRegistry = reg;

                return res;
            }
        }
        return null;
    }

    public FilterHandler[] getFilters(@Nonnull final ServletResolution pr,
            @Nonnull final DispatcherType dispatcherType,
            @Nonnull String requestURI)
    {
        if ( pr != null && pr.handlerRegistry != null )
        {
            return pr.handlerRegistry.getFilterHandlers(pr.handler, dispatcherType, requestURI);
        }
        return EMPTY_FILTER_HANDLER;
    }

    public PathResolution resolveServlet(@Nonnull final String requestURI)
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
     * Get the servlet handler for a servlet by name
     * @param contextId The context id
     * @param name The servlet name
     * @return The servlet handler or {@code null}
     */
    public ServletResolution resolveServletByName(final long contextId, @Nonnull final String name)
    {
        final PerContextHandlerRegistry reg = this.getRegistry(contextId);
        if ( reg != null )
        {
            final ServletHandler handler = reg.resolveServletByName(name);
            if ( handler != null )
            {
                final ServletResolution resolution = new ServletResolution();
                resolution.handler = handler;
                resolution.handlerRegistry = reg;

                return resolution;
            }
        }
        return null;
    }

    public boolean getRuntimeInfo(@Nonnull final ServletContextDTO dto,
            @Nonnull final FailedDTOHolder failedDTOHolder)
    {
        final PerContextHandlerRegistry reg = this.getRegistry(dto.serviceId);
        if ( reg != null )
        {
            reg.getRuntime(dto, failedDTOHolder);
            return true;
        }
        return false;
    }

    public PerContextHandlerRegistry getBestMatchingRegistry(String requestURI)
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
        return found;
    }
}
