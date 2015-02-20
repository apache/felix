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

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.ListenerDTO;

final class ListenerDTOBuilder extends BaseDTOBuilder<ServiceReference<?>, ListenerDTO>
{
    @Override
    ListenerDTO buildDTO(ServiceReference<?> listenerRef, long servletContextId)
    {
        ListenerDTO listenerDTO = new ListenerDTO();
        listenerDTO.serviceId = (Long) listenerRef.getProperty(Constants.SERVICE_ID);
        listenerDTO.servletContextId = servletContextId;
        // TODO Is this the desired value?
        listenerDTO.types = (String[]) listenerRef.getProperty(Constants.OBJECTCLASS);
        return listenerDTO;
    }
}
