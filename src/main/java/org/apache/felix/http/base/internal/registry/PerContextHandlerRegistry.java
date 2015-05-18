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
package org.apache.felix.http.base.internal.registry;

import javax.annotation.Nonnull;
import javax.servlet.DispatcherType;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.ContextRuntime;
import org.apache.felix.http.base.internal.runtime.dto.FailureRuntime;

/**
 * This registry keeps track of all processing components per context:
 * - servlets
 * - filters
 * - error pages
 */
public final class PerContextHandlerRegistry implements Comparable<PerContextHandlerRegistry>
{
    /** Service id of the context. */
    private final long serviceId;

    /** Ranking of the context. */
    private final int ranking;

    /** The context path. */
    private final String path;

    /** The context prefix. */
    private final String prefix;

    private final ServletRegistry servletRegistry = new ServletRegistry();

    private final FilterRegistry filterRegistry = new FilterRegistry();

    private final ErrorPageRegistry errorPageRegistry = new ErrorPageRegistry();

    /**
     * Default http service registry
     */
    public PerContextHandlerRegistry()
    {
        this.serviceId = 0;
        this.ranking = Integer.MAX_VALUE;
        this.path = "/";
        this.prefix = null;
    }

    /**
     * Registry for a servlet context helper (whiteboard support)
     * @param info The servlet context helper info
     */
    public PerContextHandlerRegistry(@Nonnull final ServletContextHelperInfo info)
    {
        this.serviceId = info.getServiceId();
        this.ranking = info.getRanking();
        this.path = info.getPath();
        if ( this.path.equals("/") )
        {
            this.prefix = null;
        }
        else
        {
            this.prefix = this.path + "/";
        }
    }

    public long getContextServiceId()
    {
        return this.serviceId;
    }

    public void removeAll()
    {
        // TODO - implement
    }

    @Override
    public int compareTo(@Nonnull final PerContextHandlerRegistry other)
    {
        // the context of the HttpService is the least element
        if (this.serviceId == 0 ^ other.serviceId == 0)
        {
            return this.serviceId == 0 ? -1 : 1;
        }

        final int result = Integer.compare(other.path.length(), this.path.length());
        if ( result == 0 ) {
            if (this.ranking == other.ranking)
            {
                // Service id's can be negative. Negative id's follow the reverse natural ordering of integers.
                int reverseOrder = ( this.serviceId <= 0 && other.serviceId <= 0 ) ? -1 : 1;
                return reverseOrder * Long.compare(this.serviceId, other.serviceId);
            }

            return Integer.compare(other.ranking, this.ranking);
        }
        return result;
    }

    public String isMatching(final String requestURI)
    {
        if (requestURI.equals(this.path))
        {
            return "";
        }
        if (this.prefix == null)
        {
            return requestURI;
        }
        if (requestURI.startsWith(this.prefix))
        {
            return requestURI.substring(this.prefix.length() - 1);
        }
        return null;
    }

    public PathResolution resolve(final String relativeRequestURI)
    {
        return this.servletRegistry.resolve(relativeRequestURI);
    }

    public ServletHandler resolveServletByName(final String name)
    {
        return this.servletRegistry.resolveByName(name);
    }

    /**
     * Add a servlet
     * @param holder The servlet holder
     * @param info The servlet info
     */
    public void addServlet(@Nonnull final ServletHandler holder)
    {
        this.servletRegistry.addServlet(holder);
        this.errorPageRegistry.addServlet(holder);
    }

    /**
     * Remove a servlet
     * @param info The servlet info
     */
    public void removeServlet(@Nonnull final ServletInfo info, final boolean destroy)
    {
        this.servletRegistry.removeServlet(info, destroy);
        this.errorPageRegistry.removeServlet(info, destroy);
    }

    public void addFilter(@Nonnull final FilterHandler holder)
    {
        this.filterRegistry.addFilter(holder);
    }

    public void removeFilter(@Nonnull final FilterInfo info, final boolean destroy)
    {
        this.filterRegistry.removeFilter(info, destroy);
    }

    public FilterHandler[] getFilterHandlers(final ServletHandler servletHandler,
            DispatcherType dispatcherType, String requestURI)
    {
        return this.filterRegistry.getFilterHandlers(servletHandler, dispatcherType, requestURI);
    }

    public ServletHandler getErrorHandler(int code, Throwable exception)
    {
        return this.errorPageRegistry.get(exception, code);
    }

    public synchronized ContextRuntime getRuntime(final FailureRuntime.Builder failureRuntimeBuilder)
    {
        // TODO - add servlets
        // TODO - add failures from filters and error pages
        return new ContextRuntime(this.filterRegistry.getFilterRuntimes(failureRuntimeBuilder),
                this.errorPageRegistry.getErrorPageRuntimes(),
                null, this.serviceId);
    }
}
