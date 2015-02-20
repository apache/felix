/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.base.internal.runtime;

import java.util.Collection;
import java.util.Collections;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.ServletHandler;

public final class HandlerRuntime
{
    private final Collection<ServletHandler> servletHandlers;
    private final Collection<FilterHandler> filterHandlers;
    private final Collection<ErrorPage> errorPages;
    private final long serviceId;

    public HandlerRuntime(Collection<ServletHandler> servletHandlers,
            Collection<FilterHandler> filterHandlers,
            Collection<ErrorPage> errorPages,
            long serviceId)
    {
        this.servletHandlers = servletHandlers;
        this.filterHandlers = filterHandlers;
        this.errorPages = errorPages;
        this.serviceId = serviceId;
    }

    public static HandlerRuntime empty(long serviceId)
    {
        return new HandlerRuntime(Collections.<ServletHandler>emptyList(),
                Collections.<FilterHandler>emptyList(),
                Collections.<ErrorPage> emptyList(),
                serviceId);
    }

    public Collection<ServletHandler> getServletHandlers()
    {
        return servletHandlers;
    }

    public Collection<FilterHandler> getFilterHandlers()
    {
        return filterHandlers;
    }

    public Collection<ErrorPage> getErrorPages()
    {
        return errorPages;
    }

    public long getServiceId()
    {
        return serviceId;
    }

    public static class ErrorPage {
        private final ServletHandler servletHandler;
        private final Collection<Integer> errorCodes;
        private final Collection<String> exceptions;

        public ErrorPage(ServletHandler servletHandler,
                Collection<Integer> errorCodes,
                Collection<String> exceptions)
        {
            this.servletHandler = servletHandler;
            this.errorCodes = errorCodes;
            this.exceptions = exceptions;
        }

        public ServletHandler getServletHandler()
        {
            return servletHandler;
        }

        public Collection<Integer> getErrorCodes()
        {
            return errorCodes;
        }

        public Collection<String> getExceptions()
        {
            return exceptions;
        }
    }
}
