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

import javax.annotation.Nonnull;

import org.apache.felix.http.base.internal.runtime.PreprocessorInfo;
import org.osgi.service.http.runtime.dto.FailedPreprocessorDTO;
import org.osgi.service.http.runtime.dto.PreprocessorDTO;

public final class PreprocessorDTOBuilder
{

    /**
     * Build a preprocessor DTO from a filter info
     * @param info The preprocessor info
     * @return A preprocessor DTO
     */
    public static @Nonnull PreprocessorDTO build(@Nonnull final PreprocessorInfo info, final int reason)
    {
        final PreprocessorDTO dto = (reason != -1 ? new FailedPreprocessorDTO() : new PreprocessorDTO());

        dto.initParams = info.getInitParameters();
        dto.serviceId = info.getServiceId();

        if ( reason != -1 )
        {
            ((FailedPreprocessorDTO)dto).failureReason = reason;
        }

        return dto;
    }
}
