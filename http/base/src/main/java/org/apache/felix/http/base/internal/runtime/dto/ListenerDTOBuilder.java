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

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.felix.http.base.internal.runtime.ListenerInfo;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;

public final class ListenerDTOBuilder
{
    private static final Set<String> ALLOWED_INTERFACES;
    static {
        ALLOWED_INTERFACES = new HashSet<String>();
        ALLOWED_INTERFACES.add(HttpSessionAttributeListener.class.getName());
        ALLOWED_INTERFACES.add(HttpSessionIdListener.class.getName());
        ALLOWED_INTERFACES.add(HttpSessionListener.class.getName());
        ALLOWED_INTERFACES.add(ServletContextAttributeListener.class.getName());
        ALLOWED_INTERFACES.add(ServletContextListener.class.getName());
        ALLOWED_INTERFACES.add(ServletRequestAttributeListener.class.getName());
        ALLOWED_INTERFACES.add(ServletRequestListener.class.getName());
    }

    public static ListenerDTO build(final ListenerInfo<?> info, final int reason)
    {
        final ListenerDTO dto = (reason == -1 ? new ListenerDTO() : new FailedListenerDTO());

        dto.serviceId = info.getServiceId();

        if ( reason != -1 )
        {
            ((FailedListenerDTO)dto).failureReason = reason;
        }

        return dto;
    }

    public static ListenerDTO build(final ServiceReference<?> listenerRef, final long servletContextId)
    {
        final ListenerDTO listenerDTO = new ListenerDTO();
        listenerDTO.serviceId = (Long) listenerRef.getProperty(Constants.SERVICE_ID);
        listenerDTO.servletContextId = servletContextId;

        final String[] objectClass = (String[])listenerRef.getProperty(Constants.OBJECTCLASS);
        final Set<String> names = new HashSet<String>();
        for(final String name : objectClass)
        {
            if ( ALLOWED_INTERFACES.contains(name) )
            {
                names.add(name);
            }
        }
        listenerDTO.types = names.toArray(new String[names.size()]);
        return listenerDTO;
    }
}
