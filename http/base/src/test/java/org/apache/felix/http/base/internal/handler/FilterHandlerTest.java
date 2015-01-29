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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.junit.Before;
import org.junit.Test;

public class FilterHandlerTest extends AbstractHandlerTest
{
    private Filter filter;

    @Override
    @Before
    public void setUp()
    {
        super.setUp();
        this.filter = mock(Filter.class);
    }

    @Test
    public void testCompare()
    {
        FilterHandler h1 = createHandler("a", 0);
        FilterHandler h2 = createHandler("b", 10);

        assertEquals(1, h1.compareTo(h2));
        assertEquals(-1, h2.compareTo(h1));
    }

    @Test
    public void testDestroy()
    {
        FilterHandler h1 = createHandler("/a", 0);
        h1.destroy();
        verify(this.filter).destroy();
    }

    @Test
    public void testHandleFound() throws Exception
    {
        FilterHandler h1 = createHandler("/a", 0);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(this.context.handleSecurity(req, res)).thenReturn(true);

        when(req.getPathInfo()).thenReturn("/a");
        h1.handle(req, res, chain);

        verify(this.filter).doFilter(req, res, chain);
        verify(chain, never()).doFilter(req, res);
    }

    @Test
    public void testHandleFoundContextRoot() throws Exception
    {
        FilterHandler h1 = createHandler("/", 0);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(this.context.handleSecurity(req, res)).thenReturn(true);

        when(req.getPathInfo()).thenReturn(null);
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
        FilterHandler h1 = createHandler("/a", 0);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getPathInfo()).thenReturn("/a");
        // Default behaviour: uncomitted response and default status code...
        when(res.isCommitted()).thenReturn(false);
        when(res.getStatus()).thenReturn(SC_OK);

        when(this.context.handleSecurity(req, res)).thenReturn(false);

        h1.handle(req, res, chain);

        verify(this.filter, never()).doFilter(req, res, chain);
        verify(chain, never()).doFilter(req, res);
        verify(res).sendError(SC_FORBIDDEN);
    }

    /**
     * FELIX-3988: do not try to write to an already committed response.
     */
    @Test
    public void testHandleFoundForbiddenCommittedOwnResponse() throws Exception
    {
        FilterHandler h1 = createHandler("/a", 0);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getPathInfo()).thenReturn("/a");
        // Simulate an already committed response...
        when(res.isCommitted()).thenReturn(true);
        when(res.getStatus()).thenReturn(SC_OK);

        when(this.context.handleSecurity(req, res)).thenReturn(false);

        h1.handle(req, res, chain);

        verify(this.filter, never()).doFilter(req, res, chain);
        verify(chain, never()).doFilter(req, res);
        // Should not be called from our handler...
        verify(res, never()).sendError(SC_FORBIDDEN);
    }

    /**
     * FELIX-3988: do not overwrite custom set status code.
     */
    @Test
    public void testHandleFoundForbiddenCustomStatusCode() throws Exception
    {
        FilterHandler h1 = createHandler("/a", 0);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getPathInfo()).thenReturn("/a");
        // Simulate an uncommitted response with a non-default status code...
        when(res.isCommitted()).thenReturn(false);
        when(res.getStatus()).thenReturn(SC_PAYMENT_REQUIRED);

        when(this.context.handleSecurity(req, res)).thenReturn(false);

        h1.handle(req, res, chain);

        verify(this.filter, never()).doFilter(req, res, chain);
        verify(chain, never()).doFilter(req, res);
        // Should not be called from our handler...
        verify(res, never()).sendError(SC_FORBIDDEN);
    }

    @Test
    public void testHandleNotFound() throws Exception
    {
        FilterHandler h1 = createHandler("/a", 0);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getPathInfo()).thenReturn("/");
        h1.handle(req, res, chain);

        verify(this.filter, never()).doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test
    public void testHandleNotFoundContextRoot() throws Exception
    {
        FilterHandler h1 = createHandler("/a", 0);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getPathInfo()).thenReturn(null);
        h1.handle(req, res, chain);

        verify(this.filter, never()).doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test
    public void testInit() throws Exception
    {
        FilterHandler h1 = createHandler("/a", 0);
        h1.init();
        verify(this.filter).init(any(FilterConfig.class));
    }

    @Test
    public void testMatches()
    {
        FilterHandler h1 = createHandler("/a/b", 0);
        FilterHandler h2 = createHandler("/a/b/.+", 0);
        FilterHandler h3 = createHandler("/", 0);
        FilterHandler h4 = createHandler("/.*", 0);

        assertFalse(h1.matches(null));
        assertFalse(h1.matches("/a"));
        assertTrue(h1.matches("/a/b"));
        assertFalse(h1.matches("/a/b/c"));
        assertFalse(h2.matches(null));
        assertFalse(h1.matches("/a"));
        assertTrue(h2.matches("/a/b/c"));
        assertFalse(h2.matches("/a/b/"));
        assertTrue(h3.matches(null));
        assertTrue(h3.matches("/"));
        assertFalse(h3.matches("/a/b/"));
        assertTrue(h4.matches(null));
        assertTrue(h4.matches("/"));
        assertTrue(h4.matches("/a/b/"));
    }

    @Override
    protected AbstractHandler createHandler()
    {
        return createHandler("dummy", 0);
    }

    private FilterHandler createHandler(String pattern, int ranking)
    {
        final FilterInfo info = new FilterInfo();
        info.regexs = new String[] {pattern};
        info.ranking = ranking;
        return new FilterHandler(this.context, this.filter, info);
    }
}
