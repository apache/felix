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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.apache.felix.http.base.internal.handler.holder.HttpServiceServletHolder;
import org.apache.felix.http.base.internal.handler.holder.ServletHolder;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.junit.Test;
import org.mockito.Matchers;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class ServletRegistryTest {

    private final ServletRegistry reg = new ServletRegistry();

    @Test public void testSingleServlet() throws InvalidSyntaxException
    {
        final Map<ServletInfo, ServletRegistry.ServletRegistrationStatus> status = reg.getServletStatusMapping();
        // empty reg
        assertEquals(0, status.size());

        // register servlet
        final ServletHolder h1 = createServletHolder(1L, 0, "/foo");
        reg.addServlet(h1);

        // one entry in reg
        assertEquals(1, status.size());
        assertNotNull(status.get(h1.getServletInfo()));
        assertNotNull(status.get(h1.getServletInfo()).pathToStatus.get("/foo"));
        final int code = status.get(h1.getServletInfo()).pathToStatus.get("/foo");
        assertEquals(-1, code);

        // remove servlet
        reg.removeServlet(h1.getServletInfo());

        // empty again
        assertEquals(0, status.size());
    }

    @Test public void testSimpleHiding() throws InvalidSyntaxException
    {
        final Map<ServletInfo, ServletRegistry.ServletRegistrationStatus> status = reg.getServletStatusMapping();
        // empty reg
        assertEquals(0, status.size());

        // register servlets
        final ServletHolder h1 = createServletHolder(1L, 10, "/foo");
        reg.addServlet(h1);

        final ServletHolder h2 = createServletHolder(2L, 0, "/foo");
        reg.addServlet(h2);

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

        // remove servlet 1
        reg.removeServlet(h1.getServletInfo());

        // h2 is active
        assertEquals(1, status.size());
        assertNotNull(status.get(h2.getServletInfo()).pathToStatus.get("/foo"));
        final int code3 = status.get(h2.getServletInfo()).pathToStatus.get("/foo");
        assertEquals(-1, code3);

        // remove servlet 2
        reg.removeServlet(h2.getServletInfo());

        // empty again
        assertEquals(0, status.size());
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

    private static ServletHolder createServletHolder(final long id, final int ranking, final String... paths) throws InvalidSyntaxException
    {
        final ServletInfo si = createServletInfo(id, ranking, paths);
        final ServletContext ctx = mock(ServletContext.class);
        final Servlet servlet = mock(Servlet.class);

        return new HttpServiceServletHolder(ctx, si, servlet);
    }
}
