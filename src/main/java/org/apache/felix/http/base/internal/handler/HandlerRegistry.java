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
import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.DispatcherType;

import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;

public final class HandlerRegistry
{
    private volatile List<PerContextHandlerRegistry> registrations = Collections.emptyList();

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

    public void remove(@Nonnull PerContextHandlerRegistry registry)
    {
        synchronized ( this )
        {
            final List<PerContextHandlerRegistry> updatedList = new ArrayList<PerContextHandlerRegistry>(this.registrations);
            updatedList.remove(registry);

            this.registrations = updatedList;
        }
    }

    public PerContextHandlerRegistry getRegistry(final ServletContextHelperInfo info)
    {
        synchronized ( this )
        {
            for(final PerContextHandlerRegistry r : this.registrations)
            {
                if ( info == null )
                {
                    if ( r.getContextServiceid() == -1)
                    {
                        return r;
                    }
                }
                else
                {
                    if ( info.getServiceId() == r.getContextServiceid())
                    {
                        return r;
                    }
                }
            }
            final PerContextHandlerRegistry reg = new PerContextHandlerRegistry(info);
            this.add(reg);

            return reg;
        }
    }

    public ErrorsMapping getErrorsMapping(final String requestURI, final Long serviceId)
    {
        final List<PerContextHandlerRegistry> regs = this.registrations;
        for(final PerContextHandlerRegistry r : regs)
        {
            if ( serviceId != null && serviceId == r.getContextServiceid() )
            {
                return r.getErrorsMapping();
            }
            else if ( serviceId == null && requestURI.startsWith(r.getPrefixPath()) )
            {
                return r.getErrorsMapping();
            }
        }

        return null;
    }

    private static FilterHandler[] EMPTY_FILTER_HANDLER = new FilterHandler[0];

    public FilterHandler[] getFilterHandlers(final ServletHandler servletHandler,
            final DispatcherType dispatcherType,
            final String requestURI)
    {
        final long id = servletHandler.getContextServiceId();
        final List<PerContextHandlerRegistry> regs = this.registrations;
        for(final PerContextHandlerRegistry r : regs)
        {
            if ( id == r.getContextServiceid() )
            {
                return r.getFilterHandlers(servletHandler, dispatcherType, requestURI);
            }
        }
        return EMPTY_FILTER_HANDLER;
    }
/*
    public synchronized Servlet getServletByAlias(String alias)
    {
        return this.servletPatternMap.get(alias);
    }
*/
    public ServletHandler getServletHandlerByName(final String name)
    {
        // TODO
        return null;
    }

    public ServletHandler getServletHander(final String requestURI)
    {
        final List<PerContextHandlerRegistry> regs = this.registrations;
        for(final PerContextHandlerRegistry r : regs)
        {
            if ( requestURI.startsWith(r.getPrefixPath()))
            {
                // TODO - we could stip of the prefix and simplify handler registration
                final ServletHandler handler = r.getServletHander(requestURI);
                if ( handler != null )
                {
                    return handler;
                }
            }
        }
        return null;
    }

    public synchronized void removeAll()
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
}
