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

import java.util.Arrays;

import org.apache.felix.http.base.internal.handler.ListenerHandler;
import org.apache.felix.http.base.internal.runtime.ListenerInfo;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;

public final class ListenerDTOBuilder
{
   public static ListenerDTO build(final ListenerInfo info, final int reason)
    {
        final ListenerDTO dto = (reason == -1 ? new ListenerDTO() : new FailedListenerDTO());

        dto.serviceId = info.getServiceId();
        dto.types = Arrays.copyOf(info.getListenerTypes(), info.getListenerTypes().length);

        if ( reason != -1 )
        {
            ((FailedListenerDTO)dto).failureReason = reason;
        }

        return dto;
    }

    public static ListenerDTO build(final ListenerHandler handler, final int reason)
    {
        final ListenerDTO dto = build(handler.getListenerInfo(), reason);
        dto.servletContextId = handler.getContextServiceId();
        return dto;
    }
}
