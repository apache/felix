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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
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
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class ErrorPageRegistryTest {

    private final ErrorPageRegistry reg = new ErrorPageRegistry();

    private void assertEmpty(final ServletContextDTO dto, final FailedDTOHolder holder)
    {
        assertNull(dto.servletDTOs);
        assertNull(dto.resourceDTOs);
        assertNull(dto.errorPageDTOs);
        assertTrue(holder.failedErrorPageDTOs.isEmpty());
    }

    private void clear(final ServletContextDTO dto, final FailedDTOHolder holder)
    {
        dto.servletDTOs = null;
        dto.resourceDTOs = null;
        dto.errorPageDTOs = null;
        holder.failedErrorPageDTOs.clear();
    }

    @Test public void testSingleErrorPage() throws InvalidSyntaxException, ServletException
    {
        final FailedDTOHolder holder = new FailedDTOHolder();
        final ServletContextDTO dto = new ServletContextDTO();

        final Map<ServletInfo, ErrorPageRegistry.ErrorRegistrationStatus> status = reg.getStatusMapping();
        // empty reg
        assertEquals(0, status.size());
        // check DTO
        reg.getRuntimeInfo(dto, holder.failedErrorPageDTOs);
        assertEmpty(dto, holder);

        // register error page
        final ServletHandler h1 = createServletHandler(1L, 0, "404", "java.io.IOException");
        reg.addServlet(h1);

        verify(h1.getServlet()).init(Matchers.any(ServletConfig.class));

        // one entry in reg
        assertEquals(1, status.size());
        assertNotNull(status.get(h1.getServletInfo()));
        assertEquals(1, status.get(h1.getServletInfo()).exceptionMapping.size());
        assertEquals(-1, (int)status.get(h1.getServletInfo()).exceptionMapping.get("java.io.IOException"));
        assertEquals(1, status.get(h1.getServletInfo()).errorCodeMapping.size());
        assertEquals(-1, (int)status.get(h1.getServletInfo()).errorCodeMapping.get(404L));

        // check DTO
        clear(dto, holder);
        reg.getRuntimeInfo(dto, holder.failedErrorPageDTOs);
        assertNull(dto.resourceDTOs);
        assertNull(dto.servletDTOs);
        assertNotNull(dto.errorPageDTOs);
        assertEquals(1, dto.errorPageDTOs.length);
        assertEquals(1, dto.errorPageDTOs[0].errorCodes.length);
        assertEquals(404, dto.errorPageDTOs[0].errorCodes[0]);
        assertTrue(holder.failedErrorPageDTOs.isEmpty());

        // test error handling
        assertNotNull(reg.get(new IOException(), 404));
        assertNotNull(reg.get(new RuntimeException(), 404));
        assertNotNull(reg.get(new IOException(), 500));
        assertNotNull(reg.get(new FileNotFoundException(), 500));
        assertNull(reg.get(new RuntimeException(), 500));

        // remove servlet
        final Servlet s = h1.getServlet();
        reg.removeServlet(h1.getServletInfo(), true);
        verify(s).destroy();

        // empty again
        assertEquals(0, status.size());
        // check DTO
        clear(dto, holder);
        reg.getRuntimeInfo(dto, holder.failedErrorPageDTOs);
        assertEmpty(dto, holder);
    }

    @Test public void testSimpleHiding() throws InvalidSyntaxException, ServletException
    {
        final FailedDTOHolder holder = new FailedDTOHolder();
        final ServletContextDTO dto = new ServletContextDTO();

        final Map<ServletInfo, ErrorPageRegistry.ErrorRegistrationStatus> status = reg.getStatusMapping();
        // empty reg
        assertEquals(0, status.size());
        // check DTO
        reg.getRuntimeInfo(dto, holder.failedErrorPageDTOs);
        assertEmpty(dto, holder);

        // register error pages
        final ServletHandler h1 = createServletHandler(1L, 0, "404", "java.io.IOException");
        reg.addServlet(h1);
        final ServletHandler h2 = createServletHandler(2L, 10, "404", "some.other.Exception");
        reg.addServlet(h2);

        verify(h1.getServlet()).init(Matchers.any(ServletConfig.class));
        verify(h2.getServlet()).init(Matchers.any(ServletConfig.class));

        // two entries in reg
        assertEquals(2, status.size());
        assertNotNull(status.get(h1.getServletInfo()));
        assertEquals(1, status.get(h1.getServletInfo()).exceptionMapping.size());
        assertEquals(-1, (int)status.get(h1.getServletInfo()).exceptionMapping.get("java.io.IOException"));
        assertEquals(1, status.get(h1.getServletInfo()).errorCodeMapping.size());
        assertEquals(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, (int)status.get(h1.getServletInfo()).errorCodeMapping.get(404L));
        assertNotNull(status.get(h2.getServletInfo()));
        assertEquals(1, status.get(h2.getServletInfo()).exceptionMapping.size());
        assertEquals(-1, (int)status.get(h2.getServletInfo()).exceptionMapping.get("some.other.Exception"));
        assertEquals(1, status.get(h2.getServletInfo()).errorCodeMapping.size());
        assertEquals(-1, (int)status.get(h2.getServletInfo()).errorCodeMapping.get(404L));

        // check DTO
        clear(dto, holder);
        reg.getRuntimeInfo(dto, holder.failedErrorPageDTOs);
        assertNull(dto.resourceDTOs);
        assertNull(dto.servletDTOs);
        assertNotNull(dto.errorPageDTOs);
        assertEquals(2, dto.errorPageDTOs.length);
        assertEquals(0, dto.errorPageDTOs[0].errorCodes.length);
        assertEquals(1, dto.errorPageDTOs[1].errorCodes.length);
        assertEquals(404, dto.errorPageDTOs[1].errorCodes[0]);
        assertEquals(1, dto.errorPageDTOs[0].exceptions.length);
        assertEquals(1, dto.errorPageDTOs[1].exceptions.length);
        assertEquals("java.io.IOException", dto.errorPageDTOs[0].exceptions[0]);
        assertEquals("some.other.Exception", dto.errorPageDTOs[1].exceptions[0]);
        assertEquals(1, holder.failedErrorPageDTOs.size());
        assertEquals(1L, holder.failedErrorPageDTOs.iterator().next().serviceId);
        assertEquals(1, holder.failedErrorPageDTOs.iterator().next().errorCodes.length);
        assertEquals(404, holder.failedErrorPageDTOs.iterator().next().errorCodes[0]);
        assertEquals(0, holder.failedErrorPageDTOs.iterator().next().exceptions.length);

        // remove second page
        final Servlet s2 = h2.getServlet();
        reg.removeServlet(h2.getServletInfo(), true);
        verify(s2).destroy();

        // one entry in reg
        assertEquals(1, status.size());
        assertNotNull(status.get(h1.getServletInfo()));
        assertEquals(1, status.get(h1.getServletInfo()).exceptionMapping.size());
        assertEquals(-1, (int)status.get(h1.getServletInfo()).exceptionMapping.get("java.io.IOException"));
        assertEquals(1, status.get(h1.getServletInfo()).errorCodeMapping.size());
        assertEquals(-1, (int)status.get(h1.getServletInfo()).errorCodeMapping.get(404L));

        // check DTO
        clear(dto, holder);
        reg.getRuntimeInfo(dto, holder.failedErrorPageDTOs);
        assertNull(dto.resourceDTOs);
        assertNull(dto.servletDTOs);
        assertNotNull(dto.errorPageDTOs);
        assertEquals(1, dto.errorPageDTOs.length);
        assertEquals(1, dto.errorPageDTOs[0].errorCodes.length);
        assertEquals(404, dto.errorPageDTOs[0].errorCodes[0]);
        assertTrue(holder.failedErrorPageDTOs.isEmpty());

        // test error handling
        assertNotNull(reg.get(new IOException(), 404));
        assertNotNull(reg.get(new RuntimeException(), 404));
        assertNotNull(reg.get(new IOException(), 500));
        assertNotNull(reg.get(new FileNotFoundException(), 500));
        assertNull(reg.get(new RuntimeException(), 500));

        // remove first page
        final Servlet s1 = h1.getServlet();
        reg.removeServlet(h1.getServletInfo(), true);
        verify(s1).destroy();

        // empty again
        assertEquals(0, status.size());
        // check DTO
        clear(dto, holder);
        reg.getRuntimeInfo(dto, holder.failedErrorPageDTOs);
        assertEmpty(dto, holder);
    }

    private static ServletInfo createServletInfo(final long id, final int ranking, final String... codes) throws InvalidSyntaxException
    {
        final BundleContext bCtx = mock(BundleContext.class);
        when(bCtx.createFilter(Matchers.anyString())).thenReturn(null);
        final Bundle bundle = mock(Bundle.class);
        when(bundle.getBundleContext()).thenReturn(bCtx);

        @SuppressWarnings("unchecked")
        final ServiceReference<Servlet> ref = mock(ServiceReference.class);
        when(ref.getBundle()).thenReturn(bundle);
        when(ref.getProperty(Constants.SERVICE_ID)).thenReturn(id);
        when(ref.getProperty(Constants.SERVICE_RANKING)).thenReturn(ranking);
        when(ref.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE)).thenReturn(codes);
        when(ref.getPropertyKeys()).thenReturn(new String[0]);
        final ServletInfo si = new ServletInfo(ref);

        return si;
    }

    private static ServletHandler createServletHandler(final long id, final int ranking, final String... codes) throws InvalidSyntaxException
    {
        final ServletInfo si = createServletInfo(id, ranking, codes);
        final ExtServletContext ctx = mock(ExtServletContext.class);
        final Servlet servlet = mock(Servlet.class);

        return new HttpServiceServletHandler(7, ctx, si, servlet);
    }
}
