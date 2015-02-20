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
import java.util.Iterator;

import org.apache.felix.http.base.internal.runtime.HandlerRuntime.ErrorPage;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;

final class ErrorPageDTOBuilder extends BaseServletDTOBuilder<ErrorPage, ErrorPageDTO>
{
    private static final String[] STRING_ARRAY = new String[0];

    @Override
    ErrorPageDTO buildDTO(ErrorPage errorPage, long servletConextId)
    {
        ErrorPageDTO errorPageDTO = new ErrorPageDTO();
        setBaseFields(errorPageDTO, errorPage.getServletHandler(), servletConextId);
        errorPageDTO.errorCodes = getErrorCodes(errorPage.getErrorCodes());
        errorPageDTO.exceptions = errorPage.getExceptions().toArray(STRING_ARRAY);
        return errorPageDTO;
    }

    private long[] getErrorCodes(Collection<Integer> errorCodes)
    {
        Iterator<Integer> itr = errorCodes.iterator();
        long[] result = new long[errorCodes.size()];
        for (int i = 0; i < result.length; i++)
        {
            result[i] = (long) itr.next();
        }
        return result;
    }
}
