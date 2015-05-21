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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.dispatch.InvocationChain;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class FilterHandlerTest
{
    private Filter filter;

    private ExtServletContext context;

    @Before
    public void setUp()
    {
        this.context = Mockito.mock(ExtServletContext.class);
        this.filter = mock(Filter.class);
    }

    @Test
    public void testCompare()
    {
        FilterHandler h1 = createHandler(0, "a");
        FilterHandler h2 = createHandler(10, "b");
        FilterHandler h3 = createHandler(10, "c");

        assertTrue(h1.compareTo(h1) == 0);

        assertTrue(h1.compareTo(h2) > 0);
        assertTrue(h2.compareTo(h1) < 0);

        // h2 is actually registered first, so should be called first...
        assertTrue(h2.compareTo(h3) < 0);
        assertTrue(h3.compareTo(h2) > 0);
    }

    @Test
    public void testDestroy()
    {
        FilterHandler h1 = createHandler(0, "/a");
        h1.init();
        h1.destroy();
        verify(this.filter).destroy();
    }

    @Test
    public void testHandleFound() throws Exception
    {
        FilterHandler h1 = createHandler(0, "/a");
        HttpServletRequest req = createServletRequest();
        HttpServletResponse res = createServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(this.context.handleSecurity(req, res)).thenReturn(true);

        when(req.getRequestURI()).thenReturn("/a");
        h1.handle(req, res, chain);

        verify(this.filter).doFilter(req, res, chain);
        verify(chain, never()).doFilter(req, res);
    }

    @Test
    public void testHandleFoundContextRoot() throws Exception
    {
        FilterHandler h1 = createHandler(0, "/");
        HttpServletRequest req = createServletRequest();
        HttpServletResponse res = createServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(this.context.handleSecurity(req, res)).thenReturn(true);

        when(req.getRequestURI()).thenReturn(null);
        h1.handle(req, res, chain);

        verify(this.filter).doFilter(req, res, chain);
        verify(chain, never()).doFilter(req, res);
    }

    /**
     * FELIX-3988: only send an error for uncomitted responses with default status codes.
     */
    @Test
    public void testHandleFoundForbidden() throws Exception
    {
        FilterHandler h1 = createHandler(0, "/a");
        final ServletHandler sc = mock(ServletHandler.class);
        when(sc.getContext()).thenReturn(this.context);
        final InvocationChain ic = new InvocationChain(sc, new FilterHandler[] {h1});
        HttpServletRequest req = createServletRequest();
        HttpServletResponse res = createServletResponse();

        when(req.getRequestURI()).thenReturn("/a");
        // Default behaviour: uncomitted response and default status code...
        when(res.isCommitted()).thenReturn(false);
        when(res.getStatus()).thenReturn(SC_OK);

        when(this.context.handleSecurity(req, res)).thenReturn(false);

        ic.doFilter(req, res);

        verify(this.filter, never()).doFilter(req, res, ic);
        verify(res).sendError(SC_FORBIDDEN);
    }

    /**
     * FELIX-3988: do not try to write to an already committed response.
     */
    @Test
    public void testHandleFoundForbiddenCommittedOwnResponse() throws Exception
    {
        FilterHandler h1 = createHandler(0, "/a");
        final ServletHandler sc = mock(ServletHandler.class);
        when(sc.getContext()).thenReturn(this.context);
        final InvocationChain ic = new InvocationChain(sc, new FilterHandler[] {h1});
        HttpServletRequest req = createServletRequest();
        HttpServletResponse res = createServletResponse();

        when(req.getRequestURI()).thenReturn("/a");
        // Simulate an already committed response...
        when(res.isCommitted()).thenReturn(true);
        when(res.getStatus()).thenReturn(SC_OK);

        when(this.context.handleSecurity(req, res)).thenReturn(false);

        ic.doFilter(req, res);

        verify(this.filter, never()).doFilter(req, res, ic);
        // Should not be called from our handler...
        verify(res, never()).sendError(SC_FORBIDDEN);
    }

    /**
     * FELIX-3988: do not overwrite custom set status code.
     */
    @Test
    public void testHandleFoundForbiddenCustomStatusCode() throws Exception
    {
        FilterHandler h1 = createHandler(0, "/a");
        final ServletHandler sc = mock(ServletHandler.class);
        when(sc.getContext()).thenReturn(this.context);
        final InvocationChain ic = new InvocationChain(sc, new FilterHandler[] {h1});
        HttpServletRequest req = createServletRequest();
        HttpServletResponse res = createServletResponse();

        when(req.getRequestURI()).thenReturn("/a");
        // Simulate an uncommitted response with a non-default status code...
        when(res.isCommitted()).thenReturn(false);
        when(res.getStatus()).thenReturn(SC_PAYMENT_REQUIRED);

        when(this.context.handleSecurity(req, res)).thenReturn(false);

        ic.doFilter(req, res);

        verify(this.filter, never()).doFilter(req, res, ic);
        // Should not be called from our handler...
        verify(res, never()).sendError(SC_FORBIDDEN);
    }

    @Test
    public void testHandleNotFound() throws Exception
    {
        FilterHandler h1 = createHandler(0, "/a");
        final ServletHandler sc = mock(ServletHandler.class);
        when(sc.getContext()).thenReturn(this.context);
        final InvocationChain ic = new InvocationChain(sc, new FilterHandler[] {h1});
        HttpServletRequest req = createServletRequest();
        HttpServletResponse res = createServletResponse();

        when(req.getRequestURI()).thenReturn("/");
        ic.doFilter(req, res);

        verify(this.filter, never()).doFilter(req, res, ic);
    }

    @Test
    public void testHandleNotFoundContextRoot() throws Exception
    {
        FilterHandler h1 = createHandler(0, "/a");
        final ServletHandler sc = mock(ServletHandler.class);
        when(sc.getContext()).thenReturn(this.context);
        final InvocationChain ic = new InvocationChain(sc, new FilterHandler[] {h1});
        HttpServletRequest req = createServletRequest();
        HttpServletResponse res = createServletResponse();

        when(req.getRequestURI()).thenReturn(null);
        ic.doFilter(req, res);

        verify(this.filter, never()).doFilter(req, res, ic);
    }

    @Test
    public void testInit() throws Exception
    {
        FilterHandler h1 = createHandler(0, "/a");
        h1.init();
        verify(this.filter).init(any(FilterConfig.class));
    }

    private FilterHandler createHandler(int ranking, String pattern)
    {
        return createHandler(pattern, ranking, null);
    }

    private FilterHandler createHandler(String pattern, int ranking, Map<String, String> initParams)
    {
        if ( initParams == null )
        {
            initParams = Collections.emptyMap();
        }
        final FilterInfo info = new FilterInfo(null, pattern, ranking, initParams);
        return new HttpServiceFilterHandler(this.context, info, this.filter);
    }

    private HttpServletRequest createServletRequest()
    {
        return createServletRequest(DispatcherType.REQUEST);
    }

    private HttpServletRequest createServletRequest(DispatcherType type)
    {
        HttpServletRequest result = mock(HttpServletRequest.class);
        when(result.getDispatcherType()).thenReturn(type);
        return result;
    }

    private HttpServletResponse createServletResponse()
    {
        return mock(HttpServletResponse.class);
    }
}
