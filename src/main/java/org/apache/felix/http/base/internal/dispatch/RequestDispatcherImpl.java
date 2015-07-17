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
package org.apache.felix.http.base.internal.dispatch;

import java.io.IOException;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.registry.ServletResolution;
import org.apache.felix.http.base.internal.util.UriUtils;

/**
 * Wrapper implementation for {@link RequestDispatcher}.
 */
public final class RequestDispatcherImpl implements RequestDispatcher
{
    private final RequestInfo requestInfo;
    private final ServletResolution resolution;

    public RequestDispatcherImpl(final ServletResolution resolution, 
    		final RequestInfo requestInfo)
    {
    	System.out.println("New dispatcher with " + requestInfo);
        this.resolution = resolution;
        this.requestInfo = requestInfo;
    }

    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException
    {
        if (response.isCommitted())
        {
            throw new ServletException("Response has been committed");
        }
        else
        {
            // See section 9.4 of Servlet 3.0 spec
            response.resetBuffer();
        }

        try
        {
            final ServletRequestWrapper req = new ServletRequestWrapper((HttpServletRequest) request,
                    this.resolution.handler.getContext(),
                    this.requestInfo,
                    DispatcherType.FORWARD,
                    this.resolution.handler.getContextServiceId(),
                    this.resolution.handler.getServletInfo().isAsyncSupported());
            final String requestURI = UriUtils.relativePath(req.getContextPath(), req.getRequestURI());
            final FilterHandler[] filterHandlers = this.resolution.handlerRegistry.getFilterHandlers(this.resolution.handler, DispatcherType.FORWARD, requestURI);

            final FilterChain filterChain = new InvocationChain(resolution.handler, filterHandlers);
            filterChain.doFilter( req, (HttpServletResponse) response);
        }
        finally
        {
            // After a forward has taken place, the results should be committed,
            // see section 9.4 of Servlet 3.0 spec...
            if (!request.isAsyncStarted())
            {
                response.flushBuffer();
                response.getWriter().close();
            }
        }
    }

    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException
    {
        final ServletRequestWrapper req = new ServletRequestWrapper((HttpServletRequest) request,
                this.resolution.handler.getContext(),
                this.requestInfo,
                DispatcherType.INCLUDE,
                this.resolution.handler.getContextServiceId(),
                this.resolution.handler.getServletInfo().isAsyncSupported());
        final String requestURI = UriUtils.relativePath(req.getContextPath(), req.getRequestURI());
        final FilterHandler[] filterHandlers = this.resolution.handlerRegistry.getFilterHandlers(this.resolution.handler, DispatcherType.INCLUDE, requestURI);

        final FilterChain filterChain = new InvocationChain(resolution.handler, filterHandlers);
        filterChain.doFilter( req, (HttpServletResponse) response);
    }
}
