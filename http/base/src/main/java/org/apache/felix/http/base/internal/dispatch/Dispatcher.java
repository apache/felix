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
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.HttpSessionWrapper;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.registry.HandlerRegistry;
import org.apache.felix.http.base.internal.registry.PathResolution;
import org.apache.felix.http.base.internal.registry.PerContextHandlerRegistry;
import org.apache.felix.http.base.internal.whiteboard.WhiteboardManager;

public final class Dispatcher
{
    private final HandlerRegistry handlerRegistry;

    private volatile WhiteboardManager whiteboardManager;

    public Dispatcher(final HandlerRegistry handlerRegistry)
    {
        this.handlerRegistry = handlerRegistry;
    }

    /**
     * Set or unset the whiteboard manager.
     * @param service The whiteboard manager or {@code null}
     */
    public void setWhiteboardManager(@CheckForNull final WhiteboardManager service)
    {
        this.whiteboardManager = service;
    }

    /**
     * Responsible for dispatching a given request to the actual applicable servlet and/or filters in the local registry.
     *
     * @param req the {@link ServletRequest} to dispatch;
     * @param res the {@link ServletResponse} to dispatch.
     * @throws ServletException in case of exceptions during the actual dispatching;
     * @throws IOException in case of I/O problems.
     */
    public void dispatch(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException
    {
        // check for invalidating session(s) first
        final HttpSession session = req.getSession(false);
        if ( session != null )
        {
            final Set<Long> ids = HttpSessionWrapper.getExpiredSessionContextIds(session);
            final WhiteboardManager mgr = this.whiteboardManager;
            if ( mgr != null )
            {
                this.whiteboardManager.sessionDestroyed(session, ids);
            }
        }

        // get full decoded path for dispatching
        // we can't use req.getRequestURI() or req.getRequestURL() as these are returning the encoded path
        String path = req.getServletPath();
        if ( path == null )
        {
            path = "";
        }
        if ( req.getPathInfo() != null )
        {
            path = path.concat(req.getPathInfo());
        }
        final String requestURI = path;

        // Determine which servlet we should forward the request to...
        final PathResolution pr = this.handlerRegistry.resolveServlet(requestURI);

        final PerContextHandlerRegistry errorRegistry = (pr != null ? pr.handlerRegistry : this.handlerRegistry.getBestMatchingRegistry(requestURI));
        final String servletName = (pr != null ? pr.handler.getName() : null);
        final HttpServletResponse wrappedResponse = new ServletResponseWrapper(req, res, servletName, errorRegistry);
        if ( pr == null )
        {
            wrappedResponse.sendError(404);
            return;
        }

        final ExtServletContext servletContext = pr.handler.getContext();
        final RequestInfo requestInfo = new RequestInfo(pr.servletPath, pr.pathInfo, null, req.getRequestURI());

        final HttpServletRequest wrappedRequest = new ServletRequestWrapper(req, servletContext, requestInfo, null,
                pr.handler.getContextServiceId(),
                pr.handler.getServletInfo().isAsyncSupported());
        final FilterHandler[] filterHandlers = this.handlerRegistry.getFilters(pr, req.getDispatcherType(), pr.requestURI);

        try
        {
            if ( servletContext.getServletRequestListener() != null )
            {
                servletContext.getServletRequestListener().requestInitialized(new ServletRequestEvent(servletContext, wrappedRequest));
            }
            final FilterChain filterChain = new InvocationChain(pr.handler, filterHandlers);
            filterChain.doFilter(wrappedRequest, wrappedResponse);

        }
        catch ( final Exception e)
        {
            SystemLogger.error("Exception while processing request to " + requestURI, e);
            req.setAttribute(RequestDispatcher.ERROR_EXCEPTION, e);
            req.setAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE, e.getClass().getName());

            wrappedResponse.sendError(500);
        }
        finally
        {
            if ( servletContext.getServletRequestListener() != null )
            {
                servletContext.getServletRequestListener().requestDestroyed(new ServletRequestEvent(servletContext, wrappedRequest));
            }
        }
    }
}
