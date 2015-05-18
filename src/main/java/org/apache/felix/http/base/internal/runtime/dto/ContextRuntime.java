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
package org.apache.felix.http.base.internal.runtime.dto;

import java.util.Collection;
import java.util.Collections;

public final class ContextRuntime
{
    private final Collection<FilterRuntime> filterRuntimes;
    private final Collection<ErrorPageRuntime> errorPageRuntimes;
    private final long serviceId;
    private final Collection<ServletRuntime> servletRuntimes;
    private final Collection<ServletRuntime> resourceRuntimes;


    public ContextRuntime(Collection<FilterRuntime> filterRuntimes,
            Collection<ErrorPageRuntime> errorPageRuntimes,
            Collection<ServletRuntime> servletRuntimes,
            Collection<ServletRuntime> resourceRuntimes,
            long serviceId)
    {
        this.filterRuntimes = filterRuntimes;
        this.errorPageRuntimes = errorPageRuntimes;
        this.serviceId = serviceId;
        this.servletRuntimes = servletRuntimes;
        this.resourceRuntimes = resourceRuntimes;
    }

    public static ContextRuntime empty(long serviceId)
    {
        return new ContextRuntime(Collections.<FilterRuntime>emptyList(),
                Collections.<ErrorPageRuntime> emptyList(),
                Collections.<ServletRuntime> emptyList(),
                Collections.<ServletRuntime> emptyList(),
                serviceId);
    }

    Collection<ServletRuntime> getServletRuntimes()
    {
        return servletRuntimes;
    }

    Collection<ServletRuntime> getResourceRuntimes()
    {
        return resourceRuntimes;
    }

    Collection<FilterRuntime> getFilterRuntimes()
    {
        return filterRuntimes;
    }

    Collection<ErrorPageRuntime> getErrorPageRuntimes()
    {
        return errorPageRuntimes;
    }

    public long getServiceId()
    {
        return serviceId;
    }
}
