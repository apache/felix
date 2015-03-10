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
package org.apache.felix.http.base.internal.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.DispatcherType;

import org.apache.felix.http.base.internal.runtime.HandlerRuntime;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.osgi.framework.BundleContext;

/**
 * Registry for all services.
 *
 * The registry is organized per servlet context and is dispatching to one
 * of the {@link PerContextHandlerRegistry} registries.
 */
public final class HandlerRegistry
{
    private static FilterHandler[] EMPTY_FILTER_HANDLER = new FilterHandler[0];
    private final BundleContext bundleContext;

    /** Current list of context registrations. */
    private volatile List<PerContextHandlerRegistry> registrations = Collections.emptyList();

    public HandlerRegistry(BundleContext bundleContext)
    {
    	this.bundleContext = bundleContext;
    }
    
    /**
     * Register default context registry for Http Service
     */
    public void init()
    {
        this.add(new PerContextHandlerRegistry(this.bundleContext));
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
        this.add(new PerContextHandlerRegistry(info, this.bundleContext));
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

    /**
     * Get the per context registry.
     * @param info The servlet context helper info or {@code null} for the Http Service context.
     * @return A per context registry or {@code null}
     */
    public PerContextHandlerRegistry getRegistry(final ServletContextHelperInfo info)
    {
        final long key = (info == null ? 0 : info.getServiceId());

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

    public ErrorsMapping getErrorsMapping(final String requestURI, final Long serviceId)
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

    public FilterHandler[] getFilterHandlers(@Nonnull final ServletHandler servletHandler,
            final DispatcherType dispatcherType,
            @Nonnull final String requestURI)
    {
        final long key = servletHandler.getContextServiceId();
        final List<PerContextHandlerRegistry> regs = this.registrations;
        for(final PerContextHandlerRegistry r : regs)
        {
            if ( key == r.getContextServiceId() )
            {
                return r.getFilterHandlers(servletHandler, dispatcherType, requestURI);
            }
        }
        return EMPTY_FILTER_HANDLER;
    }

    /**
     * Get the servlet handler for a servlet by name
     * @param contextId The context id or {@code null}
     * @param name The servlet name
     * @return The servlet handler or {@code null}
     */
    public ServletHandler getServletHandlerByName(final Long contextId, @Nonnull final String name)
    {
        final long key = (contextId == null ? 0 : contextId);
        final List<PerContextHandlerRegistry> regs = this.registrations;
        for(final PerContextHandlerRegistry r : regs)
        {
            if ( key == r.getContextServiceId() )
            {
                return r.getServletHandlerByName(name);
            }
        }

        return null;
    }

    /**
     * Search the servlet handler for the request uri
     * @param requestURI The request uri
     * @return
     */
    public ServletHandler getServletHander(@Nonnull final String requestURI)
    {
        // search the first matching context registry
        final List<PerContextHandlerRegistry> regs = this.registrations;
        for(final PerContextHandlerRegistry r : regs)
        {
        	final String pathInContext = r.isMatching(requestURI);
        	if ( pathInContext != null )
        	{
                final ServletHandler handler = r.getServletHander(pathInContext);
                if ( handler != null )
                {
                    return handler;
                }
            }
        }
        return null;
    }

    public synchronized List<HandlerRuntime> getRuntime()
    {
        List<HandlerRuntime> handlerRuntimes = new ArrayList<HandlerRuntime>();
        for (PerContextHandlerRegistry contextRegistry : this.registrations)
        {
            handlerRuntimes.add(contextRegistry.getRuntime());
        }
        return handlerRuntimes;
    }
}
