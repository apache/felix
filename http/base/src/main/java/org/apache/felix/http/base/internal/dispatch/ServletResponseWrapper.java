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
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.registry.PerContextHandlerRegistry;

final class ServletResponseWrapper extends HttpServletResponseWrapper
{

    private final HttpServletRequest request;

    private final AtomicInteger invocationCount = new AtomicInteger();

    private final PerContextHandlerRegistry errorRegistry;

    private final String servletName;

    public ServletResponseWrapper(@Nonnull final HttpServletRequest req,
            @Nonnull final HttpServletResponse res,
            @CheckForNull final String servletName,
            @CheckForNull final PerContextHandlerRegistry errorRegistry)
    {
        super(res);
        this.request = req;
        this.servletName = servletName;
        this.errorRegistry = errorRegistry;
    }

    @Override
    public void sendError(int sc) throws IOException
    {
        sendError(sc, null);
    }

    @Override
    public void sendError(final int code, final String message) throws IOException
    {
        resetBuffer();

        setStatus(code);

        boolean invokeSuper = true;

        if ( invocationCount.incrementAndGet() == 1 )
        {
            // If we are allowed to have a body
            if (code != SC_NO_CONTENT &&
                code != SC_NOT_MODIFIED &&
                code != SC_PARTIAL_CONTENT &&
                code >= SC_OK)
            {
                final Throwable exception = (Throwable)request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
                final ServletHandler errorResolution = (errorRegistry == null ? null :
                        errorRegistry.getErrorHandler(code, exception));

                if ( errorResolution != null )
                {
                    try
                    {
                        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, new Integer(code));
                        if ( message != null )
                        {
                            request.setAttribute(RequestDispatcher.ERROR_MESSAGE, message);
                        }
                        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, request.getRequestURI());
                        if ( this.servletName != null )
                        {
                            request.setAttribute(RequestDispatcher.ERROR_SERVLET_NAME, this.servletName);
                        }

                        final String servletPath = null;
                        final String pathInfo = request.getRequestURI();
                        final String queryString = null; // XXX

                        final RequestInfo requestInfo = new RequestInfo(servletPath, pathInfo, queryString, pathInfo);

                        final FilterHandler[] filterHandlers = errorRegistry.getFilterHandlers(errorResolution, DispatcherType.ERROR, request.getRequestURI());

                        // TODO - is async = false correct?
                        final ServletRequestWrapper reqWrapper = new ServletRequestWrapper(request, errorResolution.getContext(), requestInfo, null, errorResolution.getContextServiceId(), false);
                        final FilterChain filterChain = new InvocationChain(errorResolution, filterHandlers);
                        filterChain.doFilter(reqWrapper, this);

                        invokeSuper = false;
                    }
                    catch (final ServletException e)
                    {
                        // ignore
                    }
                    finally
                    {
                        request.removeAttribute(RequestDispatcher.ERROR_STATUS_CODE);
                        request.removeAttribute(RequestDispatcher.ERROR_MESSAGE);
                        request.removeAttribute(RequestDispatcher.ERROR_REQUEST_URI);
                        request.removeAttribute(RequestDispatcher.ERROR_SERVLET_NAME);
                        request.removeAttribute(RequestDispatcher.ERROR_EXCEPTION);
                        request.removeAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
                    }
                }
            }
        }
        if ( invokeSuper )
        {
            super.sendError(code, message);
        }
    }
}
