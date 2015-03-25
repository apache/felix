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

import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;

final class DTOFactories
{
    static final DTOFactory<ServletDTO> SERVLET = new DTOFactory<ServletDTO>()
    {
        @Override
        public ServletDTO get()
        {
            return new ServletDTO();
        }
    };

    static final DTOFactory<FailedServletDTO> FAILED_SERVLET = new DTOFactory<FailedServletDTO>()
    {
        @Override
        public FailedServletDTO get()
        {
            return new FailedServletDTO();
        }
    };

    static final DTOFactory<FilterDTO> FILTER = new DTOFactory<FilterDTO>()
    {
        @Override
        public FilterDTO get()
        {
            return new FilterDTO();
        }
    };

    static final DTOFactory<FailedFilterDTO> FAILED_FILTER = new DTOFactory<FailedFilterDTO>()
    {
        @Override
        public FailedFilterDTO get()
        {
            return new FailedFilterDTO();
        }
    };

    static final DTOFactory<ResourceDTO> RESOURCE = new DTOFactory<ResourceDTO>()
    {
        @Override
        public ResourceDTO get()
        {
            return new ResourceDTO();
        }
    };

    static final DTOFactory<FailedResourceDTO> FAILED_RESOURCE = new DTOFactory<FailedResourceDTO>()
    {
        @Override
        public FailedResourceDTO get()
        {
            return new FailedResourceDTO();
        }
    };

    static final DTOFactory<ListenerDTO> LISTENER = new DTOFactory<ListenerDTO>()
    {
        @Override
        public ListenerDTO get()
        {
            return new ListenerDTO();
        }
    };

    static final DTOFactory<FailedListenerDTO> FAILED_LISTENER = new DTOFactory<FailedListenerDTO>()
    {
        @Override
        public FailedListenerDTO get()
        {
            return new FailedListenerDTO();
        }
    };

    static final DTOFactory<ErrorPageDTO> ERROR_PAGE = new DTOFactory<ErrorPageDTO>()
    {
        @Override
        public ErrorPageDTO get()
        {
            return new ErrorPageDTO();
        }
    };

    static final DTOFactory<FailedErrorPageDTO> FAILED_ERROR_PAGE = new DTOFactory<FailedErrorPageDTO>()
    {
        @Override
        public FailedErrorPageDTO get()
        {
            return new FailedErrorPageDTO();
        }
    };
}
