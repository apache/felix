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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.apache.felix.http.base.internal.context.ServletContextImpl;
import org.apache.felix.http.base.internal.dispatch.Dispatcher;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.http.HttpContext;

public class ServletHandlerRequestTest
{
    private HttpServletRequest superReq1;
    private HttpServletRequest superReq2;
    private HttpServletRequest superReq3;
    private HttpServletRequest superReq4;

    private HttpServletRequest req1;
    private HttpServletRequest req2;
    private HttpServletRequest req3;
    private HttpServletRequest req4;

    @Before
    public void setUp()
    {
        ServletContextImpl context = mock(ServletContextImpl.class);

        this.superReq1 = mock(HttpServletRequest.class);
        when(this.superReq1.getContextPath()).thenReturn("/mycontext");
        when(this.superReq1.getServletPath()).thenReturn("");
        when(this.superReq1.getRequestURI()).thenReturn("/mycontext/request/to/resource");
        when(this.superReq1.getPathInfo()).thenReturn("/request/to/resource");
        when(this.superReq1.getAttribute(HttpContext.AUTHENTICATION_TYPE)).thenReturn(HttpServletRequest.BASIC_AUTH);
        when(this.superReq1.getAttribute(HttpContext.REMOTE_USER)).thenReturn("felix");
        this.req1 = new ServletHandlerRequest(this.superReq1, context, "/");

        this.superReq2 = mock(HttpServletRequest.class);
        when(this.superReq2.getContextPath()).thenReturn("/mycontext");
        when(this.superReq2.getServletPath()).thenReturn("");
        when(this.superReq2.getRequestURI()).thenReturn("/mycontext/myservlet/request/to/resource;jsession=123");
        when(this.superReq2.getPathInfo()).thenReturn("/myservlet/request/to/resource");
        when(this.superReq2.getAttribute(HttpContext.AUTHENTICATION_TYPE)).thenReturn(null);
        when(this.superReq2.getAuthType()).thenReturn(HttpServletRequest.DIGEST_AUTH);
        when(this.superReq2.getAttribute(HttpContext.REMOTE_USER)).thenReturn(null);
        when(this.superReq2.getRemoteUser()).thenReturn("sling");
        this.req2 = new ServletHandlerRequest(this.superReq2, context, "/myservlet");

        this.superReq3 = mock(HttpServletRequest.class);
        when(this.superReq3.getContextPath()).thenReturn("/mycontext");
        when(this.superReq3.getServletPath()).thenReturn("/proxyservlet");
        when(this.superReq3.getRequestURI()).thenReturn("/mycontext/proxyservlet/request/to/resource");
        when(this.superReq3.getPathInfo()).thenReturn("/request/to/resource");
        when(this.superReq3.getAttribute(HttpContext.AUTHENTICATION_TYPE)).thenReturn(HttpServletRequest.BASIC_AUTH);
        when(this.superReq3.getAttribute(HttpContext.REMOTE_USER)).thenReturn("felix");
        this.req3 = new ServletHandlerRequest(this.superReq3, context, "/");

        this.superReq4 = mock(HttpServletRequest.class);
        when(this.superReq4.getContextPath()).thenReturn("/mycontext");
        when(this.superReq4.getServletPath()).thenReturn("/proxyservlet");
        when(this.superReq4.getRequestURI()).thenReturn("/mycontext/proxyservlet/myservlet/request/to/resource;jsession=123");
        when(this.superReq4.getPathInfo()).thenReturn("/myservlet/request/to/resource");
        when(this.superReq4.getAttribute(HttpContext.AUTHENTICATION_TYPE)).thenReturn(null);
        when(this.superReq4.getAuthType()).thenReturn(HttpServletRequest.DIGEST_AUTH);
        when(this.superReq4.getAttribute(HttpContext.REMOTE_USER)).thenReturn(null);
        when(this.superReq4.getRemoteUser()).thenReturn("sling");
        this.req4 = new ServletHandlerRequest(this.superReq4, context, "/myservlet");
    }

    @Test
    public void testPathInfo()
    {
        assertEquals("/request/to/resource", this.req1.getPathInfo());
        assertEquals("/request/to/resource", this.req2.getPathInfo());
        assertEquals("/request/to/resource", this.req3.getPathInfo());
        assertEquals("/request/to/resource", this.req4.getPathInfo());
    }

    @Test
    public void testSuperGetServletPath()
    {
        assertEquals("", this.superReq1.getServletPath());
        assertEquals("", this.superReq2.getServletPath());
        assertEquals("/proxyservlet", this.superReq3.getServletPath());
        assertEquals("/proxyservlet", this.superReq4.getServletPath());
    }

    @Test
    public void testServletPath()
    {
        assertEquals("", this.req1.getServletPath());
        assertEquals("/myservlet", this.req2.getServletPath());
        assertEquals("", this.req3.getServletPath());
        assertEquals("/myservlet", this.req4.getServletPath());
    }

    @Test
    public void testContextPath()
    {
        assertEquals("/mycontext", this.req1.getContextPath());
        assertEquals("/mycontext", this.req2.getContextPath());
        assertEquals("/mycontext/proxyservlet", this.req3.getContextPath());
        assertEquals("/mycontext/proxyservlet", this.req4.getContextPath());
    }

    @Test
    public void testGetAuthType()
    {
        assertEquals(HttpServletRequest.BASIC_AUTH, this.req1.getAuthType());
        verify(this.superReq1).getAttribute(Dispatcher.REQUEST_DISPATCHER_PROVIDER);
        verify(this.superReq1).getAttribute(HttpContext.AUTHENTICATION_TYPE);
        verifyNoMoreInteractions(this.superReq1);

        assertEquals(HttpServletRequest.DIGEST_AUTH, this.req2.getAuthType());
        verify(this.superReq2).getAttribute(Dispatcher.REQUEST_DISPATCHER_PROVIDER);
        verify(this.superReq2).getAttribute(HttpContext.AUTHENTICATION_TYPE);
        verify(this.superReq2).getAuthType();
        verifyNoMoreInteractions(this.superReq2);
    }

    @Test
    public void testGetRemoteUser()
    {
        assertEquals("felix", this.req1.getRemoteUser());
        verify(this.superReq1).getAttribute(Dispatcher.REQUEST_DISPATCHER_PROVIDER);
        verify(this.superReq1).getAttribute(HttpContext.REMOTE_USER);
        verifyNoMoreInteractions(this.superReq1);

        assertEquals("sling", this.req2.getRemoteUser());
        verify(this.superReq2).getAttribute(Dispatcher.REQUEST_DISPATCHER_PROVIDER);
        verify(this.superReq2).getAttribute(HttpContext.REMOTE_USER);
        verify(this.superReq2).getRemoteUser();
        verifyNoMoreInteractions(this.superReq2);
    }
}
