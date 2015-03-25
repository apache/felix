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
    private final Collection<ServletRuntime> servletRuntimes;
    private final Collection<FilterRuntime> filterRuntimes;
    private final Collection<ServletRuntime> resourceRuntimes;
    private final Collection<ErrorPageRuntime> errorPageRuntimes;
    private final long serviceId;

    public ContextRuntime(Collection<ServletRuntime> servletRuntimes,
            Collection<FilterRuntime> filterRuntimes,
            Collection<ServletRuntime> resourceRuntimes,
            Collection<ErrorPageRuntime> errorPageRuntimes,
            long serviceId)
    {
        this.servletRuntimes = servletRuntimes;
        this.filterRuntimes = filterRuntimes;
        this.resourceRuntimes = resourceRuntimes;
        this.errorPageRuntimes = errorPageRuntimes;
        this.serviceId = serviceId;
    }

    public static ContextRuntime empty(long serviceId)
    {
        return new ContextRuntime(Collections.<ServletRuntime>emptyList(),
                Collections.<FilterRuntime>emptyList(),
                Collections.<ServletRuntime>emptyList(),
                Collections.<ErrorPageRuntime> emptyList(),
                serviceId);
    }

    public Collection<ServletRuntime> getServletRuntimes()
    {
        return servletRuntimes;
    }

    public Collection<FilterRuntime> getFilterRuntimes()
    {
        return filterRuntimes;
    }

    public Collection<ServletRuntime> getResourceRuntimes()
    {
        return resourceRuntimes;
    }

    public Collection<ErrorPageRuntime> getErrorPageRuntimes()
    {
        return errorPageRuntimes;
    }

    public long getServiceId()
    {
        return serviceId;
    }
}
