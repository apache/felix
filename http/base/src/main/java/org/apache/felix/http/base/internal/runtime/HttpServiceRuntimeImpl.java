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
package org.apache.felix.http.base.internal.runtime;

import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;

public final class HttpServiceRuntimeImpl implements HttpServiceRuntime
{

    @Override
    public RuntimeDTO getRuntimeDTO()
    {
        // create new DTO on every call
        final RuntimeDTO runtime = new RuntimeDTO();

        return runtime;
    }

    public void registerServlet(ServletDTO servletDTO)
    {
        // TODO
    }

    public void registerFailedServlet(FailedServletDTO failedServletDTO)
    {
        // TODO
    }

    public void registerErrorPage(ErrorPageDTO errorPageDTO)
    {
        // TODO
    }

    public void registerFailedErrorPage(FailedErrorPageDTO failedErrorPageDTO)
    {
        // TODO
    }

    public void registerFilter(FilterDTO filterDTO)
    {
        // TODO
    }

    public void registerFailedFilter(FailedFilterDTO failedFilterDTO)
    {
        // TODO
    }

    @Override
    public RequestInfoDTO calculateRequestInfoDTO(String path) {
        // TODO
        return null;
    }

}
