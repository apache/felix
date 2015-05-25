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
package org.apache.felix.http.base.internal.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EventListener;

import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.ListenerHandler;
import org.apache.felix.http.base.internal.handler.WhiteboardListenerHandler;
import org.apache.felix.http.base.internal.runtime.ListenerInfo;
import org.apache.felix.http.base.internal.runtime.dto.FailedDTOHolder;
import org.junit.Test;
import org.mockito.Matchers;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.ServletContextDTO;

public class FilterRegistryTest {

    private void assertEmpty(final ServletContextDTO dto, final FailedDTOHolder holder)
    {
        assertNull(dto.listenerDTOs);
        assertTrue(holder.failedListenerDTOs.isEmpty());
    }

    private void clear(final ServletContextDTO dto, final FailedDTOHolder holder)
    {
        dto.listenerDTOs = null;
        holder.failedListenerDTOs.clear();
    }

    @Test public void testSingleListener() throws InvalidSyntaxException, ServletException
    {
        final EventListenerRegistry reg = new EventListenerRegistry();
        final FailedDTOHolder holder = new FailedDTOHolder();
        final ServletContextDTO dto = new ServletContextDTO();

        // check DTO
        reg.getRuntimeInfo(dto, holder.failedListenerDTOs);
        assertEmpty(dto, holder);

        // register listener
        final ListenerHandler h1 = createListenerHandler(1L, 0, ServletContextListener.class);
        reg.addListeners(h1);

        // one entry in DTO
        clear(dto, holder);
        reg.getRuntimeInfo(dto, holder.failedListenerDTOs);
        assertTrue(holder.failedListenerDTOs.isEmpty());
        assertNotNull(dto.listenerDTOs);
        assertEquals(1, dto.listenerDTOs.length);
        assertEquals(1, dto.listenerDTOs[0].types.length);
        assertEquals(ServletContextListener.class.getName(), dto.listenerDTOs[0].types[0]);

        // remove listener
        reg.removeListeners(h1.getListenerInfo());

        // empty again
        clear(dto, holder);
        reg.getRuntimeInfo(dto, holder.failedListenerDTOs);
        assertEmpty(dto, holder);
    }

    private static ListenerInfo createListenerInfo(final long id, final int ranking, final Class<? extends EventListener> type) throws InvalidSyntaxException
    {
        final String[] typeNames = new String[1];
        int index = 0;
        typeNames[index++] = type.getName();

        final BundleContext bCtx = mock(BundleContext.class);
        when(bCtx.createFilter(Matchers.anyString())).thenReturn(null);
        final Bundle bundle = mock(Bundle.class);
        when(bundle.getBundleContext()).thenReturn(bCtx);

        final ServiceReference<EventListener> ref = mock(ServiceReference.class);
        when(ref.getBundle()).thenReturn(bundle);
        when(ref.getProperty(Constants.SERVICE_ID)).thenReturn(id);
        when(ref.getProperty(Constants.SERVICE_RANKING)).thenReturn(ranking);
        when(ref.getProperty(Constants.OBJECTCLASS)).thenReturn(typeNames);
        when(ref.getPropertyKeys()).thenReturn(new String[0]);

        final EventListener listener = mock(type);
        final ServiceObjects<EventListener> so = mock(ServiceObjects.class);
        when(bCtx.getServiceObjects(ref)).thenReturn(so);
        when(so.getService()).thenReturn(listener);

        final ListenerInfo info = new ListenerInfo(ref);

        return info;
    }

    private static ListenerHandler createListenerHandler(final long id, final int ranking, final Class<? extends EventListener> type) throws InvalidSyntaxException
    {
        final ListenerInfo info = createListenerInfo(id, ranking, type);
        final ExtServletContext ctx = mock(ExtServletContext.class);

        return new WhiteboardListenerHandler(1L, ctx, info, info.getServiceReference().getBundle().getBundleContext());
    }
}
