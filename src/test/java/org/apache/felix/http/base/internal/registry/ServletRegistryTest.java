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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.HttpServiceServletHandler;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.FailedDTOHolder;
import org.junit.Test;
import org.mockito.Matchers;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class ServletRegistryTest {

    private final ServletRegistry reg = new ServletRegistry();

    private void assertEmpty(final ServletContextDTO dto, final FailedDTOHolder holder)
    {
        assertNull(dto.servletDTOs);
        assertNull(dto.resourceDTOs);
        assertTrue(holder.failedResourceDTOs.isEmpty());
        assertTrue(holder.failedServletDTOs.isEmpty());
    }

    private void clear(final ServletContextDTO dto, final FailedDTOHolder holder)
    {
        dto.servletDTOs = null;
        dto.resourceDTOs = null;
        holder.failedResourceDTOs.clear();
        holder.failedServletDTOs.clear();
    }

    @Test public void testSingleServlet() throws InvalidSyntaxException, ServletException
    {
        final FailedDTOHolder holder = new FailedDTOHolder();
        final ServletContextDTO dto = new ServletContextDTO();

        final Map<ServletInfo, ServletRegistry.ServletRegistrationStatus> status = reg.getServletStatusMapping();
        // empty reg
        assertEquals(0, status.size());
        // check DTO
        reg.getRuntimeInfo(dto, holder.failedServletDTOs, holder.failedResourceDTOs);
        assertEmpty(dto, holder);

        // register servlet
        final ServletHandler h1 = createServletHandler(1L, 0, "/foo");
        reg.addServlet(h1);

        verify(h1.getServlet()).init(Matchers.any(ServletConfig.class));

        // one entry in reg
        assertEquals(1, status.size());
        assertNotNull(status.get(h1.getServletInfo()));
        assertNotNull(status.get(h1.getServletInfo()).pathToStatus.get("/foo"));
        final int code = status.get(h1.getServletInfo()).pathToStatus.get("/foo");
        assertEquals(-1, code);

        // check DTO
        clear(dto, holder);
        reg.getRuntimeInfo(dto, holder.failedServletDTOs, holder.failedResourceDTOs);
        assertNull(dto.resourceDTOs);
        assertTrue(holder.failedResourceDTOs.isEmpty());
        assertTrue(holder.failedServletDTOs.isEmpty());
        assertNotNull(dto.servletDTOs);
        assertEquals(1, dto.servletDTOs.length);
        assertEquals(1, dto.servletDTOs[0].patterns.length);
        assertEquals("/foo", dto.servletDTOs[0].patterns[0]);

        // remove servlet
        final Servlet s = h1.getServlet();
        reg.removeServlet(h1.getServletInfo(), true);
        verify(s).destroy();

        // empty again
        assertEquals(0, status.size());
        // check DTO
        clear(dto, holder);
        reg.getRuntimeInfo(dto, holder.failedServletDTOs, holder.failedResourceDTOs);
        assertEmpty(dto, holder);
    }

    @Test public void testSimpleHiding() throws InvalidSyntaxException, ServletException
    {
        final FailedDTOHolder holder = new FailedDTOHolder();
        final ServletContextDTO dto = new ServletContextDTO();

        final Map<ServletInfo, ServletRegistry.ServletRegistrationStatus> status = reg.getServletStatusMapping();
        // empty reg
        assertEquals(0, status.size());
        // check DTO
        reg.getRuntimeInfo(dto, holder.failedServletDTOs, holder.failedResourceDTOs);
        assertEmpty(dto, holder);

        // register servlets
        final ServletHandler h1 = createServletHandler(1L, 10, "/foo");
        reg.addServlet(h1);
        verify(h1.getServlet()).init(Matchers.any(ServletConfig.class));

        final ServletHandler h2 = createServletHandler(2L, 0, "/foo");
        reg.addServlet(h2);
        verify(h2.getServlet(), never()).init(Matchers.any(ServletConfig.class));
        verify(h1.getServlet(), never()).destroy();

        // two entries in reg
        assertEquals(2, status.size());
        assertNotNull(status.get(h1.getServletInfo()));
        assertNotNull(status.get(h2.getServletInfo()));

        // h1 is active
        assertNotNull(status.get(h1.getServletInfo()).pathToStatus.get("/foo"));
        final int code1 = status.get(h1.getServletInfo()).pathToStatus.get("/foo");
        assertEquals(-1, code1);

        // h2 is hidden
        assertNotNull(status.get(h2.getServletInfo()).pathToStatus.get("/foo"));
        final int code2 = status.get(h2.getServletInfo()).pathToStatus.get("/foo");
        assertEquals(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, code2);

        // check DTO
        clear(dto, holder);
        reg.getRuntimeInfo(dto, holder.failedServletDTOs, holder.failedResourceDTOs);
        assertNull(dto.resourceDTOs);
        assertTrue(holder.failedResourceDTOs.isEmpty());
        assertFalse(holder.failedServletDTOs.isEmpty());
        assertNotNull(dto.servletDTOs);
        assertEquals(1, dto.servletDTOs.length);
        assertEquals(1, dto.servletDTOs[0].patterns.length);
        assertEquals("/foo", dto.servletDTOs[0].patterns[0]);
        assertEquals(1, dto.servletDTOs[0].serviceId);
        assertEquals(1, holder.failedServletDTOs.size());
        final FailedServletDTO failedDTO = holder.failedServletDTOs.iterator().next();
        assertEquals(1, failedDTO.patterns.length);
        assertEquals("/foo", failedDTO.patterns[0]);
        assertEquals(2, failedDTO.serviceId);

        // remove servlet 1
        final Servlet s1 = h1.getServlet();
        reg.removeServlet(h1.getServletInfo(), true);
        verify(s1).destroy();
        verify(h2.getServlet()).init(Matchers.any(ServletConfig.class));

        // h2 is active
        assertEquals(1, status.size());
        assertNotNull(status.get(h2.getServletInfo()).pathToStatus.get("/foo"));
        final int code3 = status.get(h2.getServletInfo()).pathToStatus.get("/foo");
        assertEquals(-1, code3);

        // check DTO
        clear(dto, holder);
        reg.getRuntimeInfo(dto, holder.failedServletDTOs, holder.failedResourceDTOs);
        assertNull(dto.resourceDTOs);
        assertTrue(holder.failedResourceDTOs.isEmpty());
        assertTrue(holder.failedServletDTOs.isEmpty());
        assertNotNull(dto.servletDTOs);
        assertEquals(1, dto.servletDTOs.length);
        assertEquals(1, dto.servletDTOs[0].patterns.length);
        assertEquals("/foo", dto.servletDTOs[0].patterns[0]);
        assertEquals(2, dto.servletDTOs[0].serviceId);

        // remove servlet 2
        final Servlet s2 = h2.getServlet();
        reg.removeServlet(h2.getServletInfo(), true);
        verify(s2).destroy();

        // empty again
        assertEquals(0, status.size());
        // check DTO
        clear(dto, holder);
        reg.getRuntimeInfo(dto, holder.failedServletDTOs, holder.failedResourceDTOs);
        assertEmpty(dto, holder);
    }

    private static ServletInfo createServletInfo(final long id, final int ranking, final String... paths) throws InvalidSyntaxException
    {
        final BundleContext bCtx = mock(BundleContext.class);
        when(bCtx.createFilter(Matchers.anyString())).thenReturn(null);
        final Bundle bundle = mock(Bundle.class);
        when(bundle.getBundleContext()).thenReturn(bCtx);

        final ServiceReference<Servlet> ref = mock(ServiceReference.class);
        when(ref.getBundle()).thenReturn(bundle);
        when(ref.getProperty(Constants.SERVICE_ID)).thenReturn(id);
        when(ref.getProperty(Constants.SERVICE_RANKING)).thenReturn(ranking);
        when(ref.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN)).thenReturn(paths);
        when(ref.getPropertyKeys()).thenReturn(new String[0]);
        final ServletInfo si = new ServletInfo(ref);

        return si;
    }

    private static ServletHandler createServletHandler(final long id, final int ranking, final String... paths) throws InvalidSyntaxException
    {
        final ServletInfo si = createServletInfo(id, ranking, paths);
        final ExtServletContext ctx = mock(ExtServletContext.class);
        final Servlet servlet = mock(Servlet.class);

        return new HttpServiceServletHandler(7, ctx, si, servlet);
    }
}
