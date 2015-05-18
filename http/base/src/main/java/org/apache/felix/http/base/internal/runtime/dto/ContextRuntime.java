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

import org.apache.felix.http.base.internal.runtime.dto.state.FailureFilterState;
import org.apache.felix.http.base.internal.runtime.dto.state.FailureServletState;
import org.apache.felix.http.base.internal.runtime.dto.state.FilterState;
import org.apache.felix.http.base.internal.runtime.dto.state.ServletState;

/**
 * Contains all information about a context wrt to the servlet/filter registry.
 */
public final class ContextRuntime
{
    private final Collection<FilterState> filterRuntimes;
    private final Collection<ServletState> errorPageRuntimes;
    private final Collection<ServletState> servletRuntimes;
    private final Collection<ServletState> resourceRuntimes;

    private final Collection<FailureFilterState> failureFilterRuntimes;
    private final Collection<FailureServletState> failureErrorPageRuntimes;
    private final Collection<FailureServletState> failureServletRuntimes;
    private final Collection<FailureServletState> failureResourceRuntimes;

    public ContextRuntime(final Collection<FilterState> filterRuntimes,
            final Collection<ServletState> errorPageRuntimes,
            final Collection<ServletState> servletRuntimes,
            final Collection<ServletState> resourceRuntimes,
            final Collection<FailureFilterState> failureFilterRuntimes,
            final Collection<FailureServletState> failureErrorPageRuntimes,
            final Collection<FailureServletState> failureServletRuntimes,
            final Collection<FailureServletState> failureResourceRuntimes)
    {
        this.filterRuntimes = filterRuntimes;
        this.errorPageRuntimes = errorPageRuntimes;
        this.servletRuntimes = servletRuntimes;
        this.resourceRuntimes = resourceRuntimes;
        this.failureFilterRuntimes = failureFilterRuntimes;
        this.failureErrorPageRuntimes = failureErrorPageRuntimes;
        this.failureServletRuntimes = failureServletRuntimes;
        this.failureResourceRuntimes = failureResourceRuntimes;
    }

    Collection<ServletState> getServletRuntimes()
    {
        return servletRuntimes;
    }

    Collection<ServletState> getResourceRuntimes()
    {
        return resourceRuntimes;
    }

    Collection<FilterState> getFilterRuntimes()
    {
        return filterRuntimes;
    }

    Collection<ServletState> getErrorPageRuntimes()
    {
        return errorPageRuntimes;
    }

    Collection<FailureServletState> getFailureServletRuntimes()
    {
        return failureServletRuntimes;
    }

    Collection<FailureServletState> getFailureResourceRuntimes()
    {
        return failureResourceRuntimes;
    }

    Collection<FailureFilterState> getFailureFilterRuntimes()
    {
        return failureFilterRuntimes;
    }

    Collection<FailureServletState> getFailureErrorPageRuntimes()
    {
        return failureErrorPageRuntimes;
    }
}
