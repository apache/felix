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

import static org.apache.felix.http.base.internal.runtime.dto.BuilderConstants.CONTEXT_FAILURE_DTO_ARRAY;
import static org.apache.felix.http.base.internal.runtime.dto.BuilderConstants.ERROR_PAGE_FAILURE_DTO_ARRAY;
import static org.apache.felix.http.base.internal.runtime.dto.BuilderConstants.FILTER_FAILURE_DTO_ARRAY;
import static org.apache.felix.http.base.internal.runtime.dto.BuilderConstants.LISTENER_FAILURE_DTO_ARRAY;
import static org.apache.felix.http.base.internal.runtime.dto.BuilderConstants.RESOURCE_FAILURE_DTO_ARRAY;
import static org.apache.felix.http.base.internal.runtime.dto.BuilderConstants.SERVLET_FAILURE_DTO_ARRAY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.felix.http.base.internal.registry.ErrorPageRegistry;
import org.apache.felix.http.base.internal.runtime.AbstractInfo;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ListenerInfo;
import org.apache.felix.http.base.internal.runtime.ResourceInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.state.FailureFilterState;
import org.apache.felix.http.base.internal.runtime.dto.state.FailureServletState;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.runtime.dto.FailedServletContextDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;

public final class FailureRuntime
{
    private static final FailureComparator<ServletContextHelperRuntime> CONTEXT_COMPARATOR = FailureComparator.create(ServletContextHelperRuntime.COMPARATOR);
    private static final Comparator<Failure<ServiceReference<?>>> REFERENCE_COMPARATOR = new Comparator<Failure<ServiceReference<?>>>()
    {
        @Override
        public int compare(Failure<ServiceReference<?>> o1, Failure<ServiceReference<?>> o2)
        {
            return o1.service.compareTo(o2.service);
        }
    };

    private final List<Failure<ServletContextHelperRuntime>> contextRuntimes;
    private final List<FailureServletState> servletRuntimes;
    private final List<FailureFilterState> filterRuntimes;
    private final List<FailureServletState> resourceRuntimes;
    private final List<FailureServletState> errorPageRuntimes;
    private final List<Failure<ServiceReference<?>>> listenerRuntimes;

    private FailureRuntime(List<Failure<ServletContextHelperRuntime>> contextRuntimes,
            List<Failure<ServiceReference<?>>> listenerRuntimes,
            List<FailureServletState> servletRuntimes,
            List<FailureFilterState> filterRuntimes,
            List<FailureServletState> resourceRuntimes,
            List<FailureServletState> errorPageRuntimes)
    {
        this.contextRuntimes = contextRuntimes;
        this.servletRuntimes = servletRuntimes;
        this.filterRuntimes = filterRuntimes;
        this.resourceRuntimes = resourceRuntimes;
        this.listenerRuntimes = listenerRuntimes;
        this.errorPageRuntimes = errorPageRuntimes;
    }

    public static FailureRuntime.Builder builder()
    {
        return new Builder();
    }

    public FailedServletDTO[] getServletDTOs()
    {
        final List<FailedServletDTO> servletDTOs = new ArrayList<FailedServletDTO>();
        for (final FailureServletState failure : servletRuntimes)
        {
            servletDTOs.add(getServletDTO(failure));
        }
        return servletDTOs.toArray(SERVLET_FAILURE_DTO_ARRAY);
    }

    private FailedServletDTO getServletDTO(FailureServletState failedServlet)
    {
        ServletDTOBuilder<FailedServletDTO> dtoBuilder = new ServletDTOBuilder<FailedServletDTO>(DTOFactories.FAILED_SERVLET);
        FailedServletDTO servletDTO = dtoBuilder.buildDTO(failedServlet, 0);
        servletDTO.failureReason = failedServlet.getReason();
        return servletDTO;
    }

    public FailedFilterDTO[] getFilterDTOs()
    {
        List<FailedFilterDTO> filterDTOs = new ArrayList<FailedFilterDTO>();
        for (FailureFilterState failure : filterRuntimes)
        {
            filterDTOs.add(getFilterDTO(failure));
        }
        return filterDTOs.toArray(FILTER_FAILURE_DTO_ARRAY);
    }

    private FailedFilterDTO getFilterDTO(FailureFilterState failedFilter)
    {
        FilterDTOBuilder<FailedFilterDTO> dtoBuilder = new FilterDTOBuilder<FailedFilterDTO>(DTOFactories.FAILED_FILTER);
        FailedFilterDTO filterDTO = dtoBuilder.buildDTO(failedFilter, 0);
        filterDTO.failureReason = failedFilter.getReason();
        return filterDTO;
    }

    public FailedResourceDTO[] getResourceDTOs()
    {
        List<FailedResourceDTO> resourceDTOs = new ArrayList<FailedResourceDTO>();
        for (FailureServletState failure : resourceRuntimes)
        {
            resourceDTOs.add(getResourceDTO(failure));
        }
        return resourceDTOs.toArray(RESOURCE_FAILURE_DTO_ARRAY);
    }

    private FailedResourceDTO getResourceDTO(FailureServletState failedResource)
    {
        ResourceDTOBuilder<FailedResourceDTO> dtoBuilder = new ResourceDTOBuilder<FailedResourceDTO>(DTOFactories.FAILED_RESOURCE);
        FailedResourceDTO resourceDTO = dtoBuilder.buildDTO(failedResource, 0);
        resourceDTO.failureReason = failedResource.getReason();
        return resourceDTO;
    }

    public FailedErrorPageDTO[] getErrorPageDTOs()
    {
        List<FailedErrorPageDTO> errorPageDTOs = new ArrayList<FailedErrorPageDTO>();
        for (FailureServletState failure : errorPageRuntimes)
        {
            errorPageDTOs.add(getErrorPageDTO(failure));
        }
        return errorPageDTOs.toArray(ERROR_PAGE_FAILURE_DTO_ARRAY);
    }

    private FailedErrorPageDTO getErrorPageDTO(FailureServletState failedErrorPage)
    {
        ErrorPageDTOBuilder<FailedErrorPageDTO> dtoBuilder = new ErrorPageDTOBuilder<FailedErrorPageDTO>(DTOFactories.FAILED_ERROR_PAGE);
        FailedErrorPageDTO errorPageDTO = dtoBuilder.buildDTO(failedErrorPage, 0);
        errorPageDTO.failureReason = failedErrorPage.getReason();
        return errorPageDTO;
    }

    public FailedListenerDTO[] getListenerDTOs()
    {
        List<FailedListenerDTO> listenerDTOs = new ArrayList<FailedListenerDTO>();
        for (Failure<ServiceReference<?>> failure : listenerRuntimes)
        {
            listenerDTOs.add(getListenerDTO(failure.service, failure.failureCode));
        }
        return listenerDTOs.toArray(LISTENER_FAILURE_DTO_ARRAY);
    }

    private FailedListenerDTO getListenerDTO(ServiceReference<?> failedListener, int failureCode)
    {
        ListenerDTOBuilder<FailedListenerDTO> dtoBuilder = new ListenerDTOBuilder<FailedListenerDTO>(DTOFactories.FAILED_LISTENER);
        FailedListenerDTO errorPageDTO = dtoBuilder.buildDTO(failedListener, 0);
        errorPageDTO.failureReason = failureCode;
        return errorPageDTO;
    }

    public FailedServletContextDTO[] getServletContextDTOs()
    {
        List<FailedServletContextDTO> contextDTOs = new ArrayList<FailedServletContextDTO>();
        for (Failure<ServletContextHelperRuntime> failure : contextRuntimes)
        {
            contextDTOs.add(getServletContextDTO(failure.service, failure.failureCode));
        }
        return contextDTOs.toArray(CONTEXT_FAILURE_DTO_ARRAY);
    }

    private FailedServletContextDTO getServletContextDTO(ServletContextHelperRuntime failedContext, int failureCode)
    {
        ServletContextDTOBuilder dtoBuilder = new ServletContextDTOBuilder(new FailedServletContextDTO(), failedContext);
        FailedServletContextDTO servletContextDTO = (FailedServletContextDTO) dtoBuilder.build();
        servletContextDTO.failureReason = failureCode;
        return servletContextDTO;
    }

    public static class Builder
    {
        private final List<Failure<ServletContextHelperRuntime>> contextRuntimes = new ArrayList<FailureRuntime.Failure<ServletContextHelperRuntime>>();
        private final List<FailureServletState> servletRuntimes = new ArrayList<FailureServletState>();
        private final List<FailureFilterState> filterRuntimes = new ArrayList<FailureFilterState>();
        private final List<FailureServletState> resourceRuntimes = new ArrayList<FailureServletState>();
        private final List<FailureServletState> errorPageRuntimes = new ArrayList<FailureServletState>();
        private final List<Failure<ServiceReference<?>>> listenerRuntimes = new ArrayList<Failure<ServiceReference<?>>>();

        public FailureRuntime.Builder add(Map<AbstractInfo<?>, Integer> failureInfos)
        {
            for (Map.Entry<AbstractInfo<?>, Integer> failureEntry : failureInfos.entrySet())
            {
                add(failureEntry.getKey(), failureEntry.getValue());
            }
            return this;
        }

        private FailureRuntime.Builder add(final AbstractInfo<?> info, final int failureCode)
        {
            if (info instanceof ServletContextHelperInfo)
            {
                ServletContextHelperRuntime servletRuntime = new ServletContextHelperRuntime()
                {

                    @Override
                    public ServletContext getSharedContext() {
                        return null;
                    }

                    @Override
                    public ServletContextHelperInfo getContextInfo() {
                        return (ServletContextHelperInfo) info;
                    }

                    @Override
                    public ContextRuntime getContextRuntime() {
                        return null;
                    }

                    @Override
                    public Collection<ServiceReference<?>> getListeners() {
                        return null;
                    }
                };
                contextRuntimes.add(new Failure<ServletContextHelperRuntime>(servletRuntime, failureCode));
            }
            else if (info instanceof ServletInfo )
            {
                boolean isError = false;
                if ( ((ServletInfo) info).getErrorPage() != null)
                {
                    isError = true;
                    final FailureServletState servletRuntime = new FailureServletState((ServletInfo) info, failureCode);
                    ErrorPageRegistry.ErrorRegistration  reg = ErrorPageRegistry.getErrorRegistration((ServletInfo)info);
                    if ( !reg.errorCodes.isEmpty() )
                    {
                        final long[] codes = new long[reg.errorCodes.size()];
                        int index = 0;
                        final Iterator<Long> i = reg.errorCodes.iterator();
                        while ( i.hasNext() )
                        {
                            codes[index++] = i.next();
                        }
                        servletRuntime.setErrorCodes(codes);
                    }
                    if ( !reg.exceptions.isEmpty() )
                    {
                        servletRuntime.setErrorExceptions(reg.exceptions.toArray(new String[reg.exceptions.size()]));
                    }
                    errorPageRuntimes.add(servletRuntime);
                }
                if ( ((ServletInfo) info).getPatterns() != null || !isError )
                {
                    FailureServletState servletRuntime = new FailureServletState((ServletInfo) info, failureCode);
                    if ( ((ServletInfo) info).getPatterns() != null )
                    {
                        servletRuntime.setPatterns(((ServletInfo) info).getPatterns());
                    }
                    servletRuntimes.add(servletRuntime);
                }
            }
            else if (info instanceof FilterInfo)
            {
                FailureFilterState filterRuntime = new FailureFilterState() {

                    @Override
                    public FilterInfo getFilterInfo() {
                        return (FilterInfo)info;
                    }

                    @Override
                    public int getReason() {
                        return failureCode;
                    }

                };
                filterRuntimes.add(filterRuntime);
            }
            else if (info instanceof ResourceInfo)
            {
                FailureServletState servletRuntime = new FailureServletState(new ServletInfo((ResourceInfo) info), failureCode);
                resourceRuntimes.add(servletRuntime);
            }
            else if (info instanceof ListenerInfo)
            {
                ServiceReference<?> serviceReference = ((ListenerInfo<?>) info).getServiceReference();
                listenerRuntimes.add(new Failure<ServiceReference<?>>(serviceReference, failureCode));
            }
            else
            {
                throw new IllegalArgumentException("Unsupported info type: " + info.getClass());
            }
            return this;
        }

        public FailureRuntime build()
        {
            Collections.sort(contextRuntimes, CONTEXT_COMPARATOR);
            Collections.sort(listenerRuntimes, REFERENCE_COMPARATOR);
            Collections.sort(servletRuntimes, FailureServletState.COMPARATOR);
            Collections.sort(filterRuntimes, FailureFilterState.COMPARATOR);
            Collections.sort(resourceRuntimes, FailureServletState.COMPARATOR);
            Collections.sort(errorPageRuntimes, FailureServletState.COMPARATOR);

            return new FailureRuntime(contextRuntimes,
                    listenerRuntimes,
                    servletRuntimes,
                    filterRuntimes,
                    resourceRuntimes,
                    errorPageRuntimes);
        }
    }

    private static class Failure<T>
    {

        final T service;
        final int failureCode;

        Failure(T service, int failureCode)
        {
            this.service = service;
            this.failureCode = failureCode;
        }
    }

    private static class FailureComparator<T> implements Comparator<Failure<T>>
    {
        final Comparator<? super T> serviceComparator;

        FailureComparator(Comparator<? super T> serviceComparator)
        {
            this.serviceComparator = serviceComparator;
        }

        static <T> FailureComparator<T> create(Comparator<? super T> serviceComparator)
        {
            return new FailureComparator<T>(serviceComparator);
        }

        @Override
        public int compare(Failure<T> o1, Failure<T> o2)
        {
            return serviceComparator.compare(o1.service, o2.service);
        }
    }
}

