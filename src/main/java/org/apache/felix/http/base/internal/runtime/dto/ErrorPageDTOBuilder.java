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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.felix.http.base.internal.runtime.dto.state.ServletState;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;

final class ErrorPageDTOBuilder<T extends ErrorPageDTO> extends BaseServletDTOBuilder<ServletState, T>
{
    static ErrorPageDTOBuilder<ErrorPageDTO> create()
    {
        return new ErrorPageDTOBuilder<ErrorPageDTO>(DTOFactories.ERROR_PAGE);
    }

    ErrorPageDTOBuilder(DTOFactory<T> dtoFactory)
    {
        super(dtoFactory);
    }

    @Override
    Collection<T> build(Collection<? extends ServletState> whiteboardServices,
            long servletContextId) {
        List<T> dtoList = new ArrayList<T>();
        for (ServletState whiteboardService : whiteboardServices)
        {
            if ( whiteboardService.getErrorCodes().length > 0 || whiteboardService.getErrorExceptions().length > 0 )
            {
                dtoList.add(buildDTO(whiteboardService, servletContextId));
            }
        }
        return dtoList;
    }

    @Override
    T buildDTO(ServletState errorPage, long servletConextId)
    {
        T errorPageDTO = super.buildDTO(errorPage, servletConextId);
        errorPageDTO.errorCodes = errorPage.getErrorCodes();
        errorPageDTO.exceptions = errorPage.getErrorExceptions();
        return errorPageDTO;
    }
}
