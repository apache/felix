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
package org.apache.felix.http.base.internal.handler;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PAYMENT_REQUIRED;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.junit.Before;
import org.junit.Test;

public class SimpleServletHandlerTest extends AbstractHandlerTest
{
    private Servlet servlet;

    @Override
    @Before
    public void setUp()
    {
        super.setUp();
        this.servlet = mock(Servlet.class);
    }

    @Test
    public void testDestroy()
    {
        ServletHandler h1 = createHandler("/a");
        h1.destroy();
        verify(this.servlet).destroy();
    }

    @Test
    public void testHandleFound() throws Exception
    {
        ServletHandler h1 = createHandler("/a");
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        when(this.context.handleSecurity(req, res)).thenReturn(true);

        when(req.getPathInfo()).thenReturn("/a/b");
        boolean result = h1.handle(req, res);

        assertTrue(result);
        verify(this.servlet).service(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    public void testHandleFoundContextRoot() throws Exception
    {
        ServletHandler h1 = createHandler("/");
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        when(this.context.handleSecurity(req, res)).thenReturn(true);

        when(req.getPathInfo()).thenReturn(null);
        boolean result = h1.handle(req, res);

        assertTrue(result);
        verify(this.servlet).service(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    /**
     * FELIX-3988: only send an error for uncomitted responses with default status codes.
     */
    @Test
    public void testHandleFoundForbidden() throws Exception
    {
        ServletHandler h1 = createHandler("/a");
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        when(req.getPathInfo()).thenReturn("/a");
        // Default behaviour: uncomitted response and default status code...
        when(res.isCommitted()).thenReturn(false);
        when(res.getStatus()).thenReturn(SC_OK);

        when(this.context.handleSecurity(req, res)).thenReturn(false);

        when(req.getPathInfo()).thenReturn("/a/b");
        boolean result = h1.handle(req, res);

        assertFalse(result);
        verify(this.servlet, never()).service(req, res);
        verify(res).sendError(SC_FORBIDDEN);
    }

    /**
     * FELIX-3988: do not try to write to an already committed response.
     */
    @Test
    public void testHandleFoundForbiddenCommittedOwnResponse() throws Exception
    {
        ServletHandler h1 = createHandler("/a");
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        when(req.getPathInfo()).thenReturn("/a");
        // Comitted response with default status code...
        when(res.isCommitted()).thenReturn(true);
        when(res.getStatus()).thenReturn(SC_OK);

        when(this.context.handleSecurity(req, res)).thenReturn(false);

        when(req.getPathInfo()).thenReturn("/a/b");
        boolean result = h1.handle(req, res);

        assertFalse(result);
        verify(this.servlet, never()).service(req, res);
        verify(res, never()).sendError(SC_FORBIDDEN);
    }

    /**
     * FELIX-3988: do not overwrite custom set status code.
     */
    @Test
    public void testHandleFoundForbiddenCustomStatusCode() throws Exception
    {
        ServletHandler h1 = createHandler("/a");
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        when(req.getPathInfo()).thenReturn("/a");
        // Unomitted response with default status code...
        when(res.isCommitted()).thenReturn(false);
        when(res.getStatus()).thenReturn(SC_PAYMENT_REQUIRED);

        when(this.context.handleSecurity(req, res)).thenReturn(false);

        when(req.getPathInfo()).thenReturn("/a/b");
        boolean result = h1.handle(req, res);

        assertFalse(result);
        verify(this.servlet, never()).service(req, res);
        verify(res, never()).sendError(SC_FORBIDDEN);
    }

    @Test
    public void testHandleNotFound() throws Exception
    {
        ServletHandler h1 = createHandler("/a");
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        when(req.getPathInfo()).thenReturn("/");
        boolean result = h1.handle(req, res);

        assertFalse(result);
        verify(this.servlet, never()).service(req, res);
    }

    @Test
    public void testHandleNotFoundContextRoot() throws Exception
    {
        ServletHandler h1 = createHandler("/a");
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        when(this.context.handleSecurity(req, res)).thenReturn(true);

        when(req.getRequestURI()).thenReturn(null);
        boolean result = h1.handle(req, res);

        assertTrue(result);
        verify(this.servlet).service(req, res);
    }

    @Test
    public void testInit() throws Exception
    {
        ServletHandler h1 = createHandler("/a");
        h1.init();
        verify(this.servlet).init(any(ServletConfig.class));
    }

    @Override
    protected AbstractHandler createHandler()
    {
        return createHandler("/dummy", null);
    }

    @Override
    protected AbstractHandler createHandler(Map<String, String> map)
    {
        return createHandler("/dummy", map);
    }

    private ServletHandler createHandler(String alias)
    {
        return createHandler(alias, null);
    }

    private ServletHandler createHandler(String alias, Map<String, String> map)
    {
        if ( map == null )
        {
            map = Collections.emptyMap();
        }
        final ServletInfo info = new ServletInfo(null, alias, 0, map);
        return new SimpleServletHandler(this.context, info, this.servlet);
    }
}
