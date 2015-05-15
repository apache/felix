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

import static javax.servlet.RequestDispatcher.FORWARD_CONTEXT_PATH;
import static javax.servlet.RequestDispatcher.FORWARD_PATH_INFO;
import static javax.servlet.RequestDispatcher.FORWARD_QUERY_STRING;
import static javax.servlet.RequestDispatcher.FORWARD_REQUEST_URI;
import static javax.servlet.RequestDispatcher.FORWARD_SERVLET_PATH;
import static javax.servlet.RequestDispatcher.INCLUDE_CONTEXT_PATH;
import static javax.servlet.RequestDispatcher.INCLUDE_PATH_INFO;
import static javax.servlet.RequestDispatcher.INCLUDE_QUERY_STRING;
import static javax.servlet.RequestDispatcher.INCLUDE_REQUEST_URI;
import static javax.servlet.RequestDispatcher.INCLUDE_SERVLET_PATH;
import static org.apache.felix.http.base.internal.util.UriUtils.concat;
import static org.apache.felix.http.base.internal.util.UriUtils.decodePath;
import static org.apache.felix.http.base.internal.util.UriUtils.removeDotSegments;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.handler.HttpSessionWrapper;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.util.UriUtils;
import org.apache.felix.http.base.internal.whiteboard.WhiteboardManager;
import org.osgi.service.http.HttpContext;
import org.osgi.service.useradmin.Authorization;

public final class Dispatcher implements RequestDispatcherProvider
{
    /**
     * Wrapper implementation for {@link RequestDispatcher}.
     */
    final class RequestDispatcherImpl implements RequestDispatcher
    {
        private final RequestInfo requestInfo;
        private final ServletHandler handler;

        public RequestDispatcherImpl(ServletHandler handler, RequestInfo requestInfo)
        {
            this.handler = handler;
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
                ServletRequestWrapper req = new ServletRequestWrapper((HttpServletRequest) request, this.handler.getContext(), this.requestInfo, DispatcherType.FORWARD, this.handler.getContextServiceId(),
                        handler.getServletInfo().isAsyncSupported());
                Dispatcher.this.forward(this.handler, req, (HttpServletResponse) response);
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
            ServletRequestWrapper req = new ServletRequestWrapper((HttpServletRequest) request, this.handler.getContext(), this.requestInfo, DispatcherType.INCLUDE,
                    this.handler.getContextServiceId(), handler.getServletInfo().isAsyncSupported());
            Dispatcher.this.include(this.handler, req, (HttpServletResponse) response);
        }
    }

    final class ServletResponseWrapper extends HttpServletResponseWrapper
    {

        private final HttpServletRequest request;

        private final AtomicInteger invocationCount = new AtomicInteger();

        private final Long serviceId;

        private final String servletName;

        public ServletResponseWrapper(final HttpServletRequest req, final HttpServletResponse res, final ServletHandler servletHandler)
        {
            super(res);
            this.request = req;
            if ( servletHandler != null )
            {
                this.serviceId = servletHandler.getContextServiceId();
                this.servletName = servletHandler.getName();
            }
            else
            {
                this.serviceId = null;
                this.servletName = null;
            }
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
                    final ServletHandler errorHandler = handlerRegistry.getErrorsHandler(request.getRequestURI(), this.serviceId, code, exception);

                    if ( errorHandler != null )
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

                            final RequestInfo requestInfo = new RequestInfo(servletPath, pathInfo, queryString);

                            final FilterHandler[] filterHandlers = handlerRegistry.getFilterHandlers(errorHandler, DispatcherType.ERROR, request.getRequestURI());

                            // TODO - is async = false correct?
                            invokeChain(filterHandlers, errorHandler, new ServletRequestWrapper(request, errorHandler.getContext(), requestInfo, this.serviceId, false), this);

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

    final class ServletRequestWrapper extends HttpServletRequestWrapper
    {
        private final DispatcherType type;
        private final RequestInfo requestInfo;
        private final ExtServletContext servletContext;
        private final Long contextId;
        private final boolean asyncSupported;

        public ServletRequestWrapper(HttpServletRequest req, ExtServletContext servletContext, RequestInfo requestInfo, final Long contextId,
                final boolean asyncSupported)
        {
            this(req, servletContext, requestInfo, null /* type */, contextId, asyncSupported);
        }

        public ServletRequestWrapper(HttpServletRequest req, ExtServletContext servletContext, RequestInfo requestInfo,
                DispatcherType type, final Long contextId, final boolean asyncSupported)
        {
            super(req);

            this.asyncSupported = asyncSupported;
            this.servletContext = servletContext;
            this.requestInfo = requestInfo;
            this.type = type;
            this.contextId = contextId;
        }

        @Override
        public Object getAttribute(String name)
        {
            HttpServletRequest request = (HttpServletRequest) getRequest();
            if (isInclusionDispatcher())
            {
                // The javax.servlet.include.* attributes refer to the information of the *included* request,
                // meaning that the request information comes from the *original* request...
                if (INCLUDE_REQUEST_URI.equals(name))
                {
                    return concat(request.getContextPath(), this.requestInfo.requestURI);
                }
                else if (INCLUDE_CONTEXT_PATH.equals(name))
                {
                    return request.getContextPath();
                }
                else if (INCLUDE_SERVLET_PATH.equals(name))
                {
                    return this.requestInfo.servletPath;
                }
                else if (INCLUDE_PATH_INFO.equals(name))
                {
                    return this.requestInfo.pathInfo;
                }
                else if (INCLUDE_QUERY_STRING.equals(name))
                {
                    return this.requestInfo.queryString;
                }
            }
            else if (isForwardingDispatcher())
            {
                // The javax.servlet.forward.* attributes refer to the information of the *original* request,
                // meaning that the request information comes from the *forwarded* request...
                if (FORWARD_REQUEST_URI.equals(name))
                {
                    return super.getRequestURI();
                }
                else if (FORWARD_CONTEXT_PATH.equals(name))
                {
                    return request.getContextPath();
                }
                else if (FORWARD_SERVLET_PATH.equals(name))
                {
                    return super.getServletPath();
                }
                else if (FORWARD_PATH_INFO.equals(name))
                {
                    return super.getPathInfo();
                }
                else if (FORWARD_QUERY_STRING.equals(name))
                {
                    return super.getQueryString();
                }
            }
            return super.getAttribute(name);
        }

        @Override
        public String getAuthType()
        {
            String authType = (String) getAttribute(HttpContext.AUTHENTICATION_TYPE);
            if (authType == null)
            {
                authType = super.getAuthType();
            }
            return authType;
        }

        @Override
        public String getContextPath()
        {
            return this.getServletContext().getContextPath();
        }

        @Override
        public DispatcherType getDispatcherType()
        {
            return (this.type == null) ? super.getDispatcherType() : this.type;
        }

        @Override
        public String getPathInfo()
        {
            if ( this.isInclusionDispatcher() )
            {
                return super.getPathInfo();
            }
            return this.requestInfo.pathInfo;
        }

        @Override
        @SuppressWarnings("deprecation")
        public String getPathTranslated()
        {
            final String info = getPathInfo();
            return (null == info) ? null : getRealPath(info);
        }

        @Override
        public String getRemoteUser()
        {
            String remoteUser = (String) getAttribute(HttpContext.REMOTE_USER);
            if (remoteUser != null)
            {
                return remoteUser;
            }

            return super.getRemoteUser();
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String path)
        {
            if ( this.contextId == null )
            {
                return null;
            }
            // See section 9.1 of Servlet 3.0 specification...
            if (path == null)
            {
                return null;
            }
            // Handle relative paths, see Servlet 3.0 spec, section 9.1 last paragraph.
            boolean relPath = !path.startsWith("/") && !"".equals(path);
            if (relPath)
            {
                path = concat(getServletPath(), path);
            }
            return Dispatcher.this.getRequestDispatcher(this.contextId, path);
        }

        @Override
        public String getRequestURI()
        {
            if ( isInclusionDispatcher() )
            {
                return super.getRequestURI();
            }
            return concat(getContextPath(), this.requestInfo.requestURI);
        }

        @Override
        public ServletContext getServletContext()
        {
            return new ServletContextWrapper(this.contextId, this.servletContext, Dispatcher.this);
        }

        @Override
        public String getServletPath()
        {
            if ( isInclusionDispatcher() )
            {
                return super.getServletPath();
            }
            return this.requestInfo.servletPath;
        }

        @Override
        public HttpSession getSession() {
            return this.getSession(true);
        }

        @Override
        public HttpSession getSession(boolean create)
        {
            // FELIX-2797: wrap the original HttpSession to provide access to the correct ServletContext...
            final HttpSession session = super.getSession(create);
            if (session == null)
            {
                return null;
            }
            // check if internal session is available
            if ( !create && !HttpSessionWrapper.hasSession(this.contextId, session) )
            {
                return null;
            }
            return new HttpSessionWrapper(this.contextId, session, this.servletContext, false);
        }

        @Override
        public boolean isUserInRole(String role)
        {
            Authorization authorization = (Authorization) getAttribute(HttpContext.AUTHORIZATION);
            if (authorization != null)
            {
                return authorization.hasRole(role);
            }

            return super.isUserInRole(role);
        }

        @Override
        public void setAttribute(final String name, final Object value)
        {
            if ( value == null )
            {
                this.removeAttribute(name);
            }
            final Object oldValue = this.getAttribute(name);
            super.setAttribute(name, value);
            if ( this.servletContext.getServletRequestAttributeListener() != null )
            {
                if ( oldValue == null )
                {
                    this.servletContext.getServletRequestAttributeListener().attributeAdded(new ServletRequestAttributeEvent(this.servletContext, this, name, value));
                }
                else
                {
                    this.servletContext.getServletRequestAttributeListener().attributeReplaced(new ServletRequestAttributeEvent(this.servletContext, this, name, oldValue));
                }
            }
        }

        @Override
        public void removeAttribute(final String name) {
            final Object oldValue = this.getAttribute(name);
            if ( oldValue != null )
            {
                super.removeAttribute(name);
                if ( this.servletContext.getServletRequestAttributeListener() != null )
                {
                    this.servletContext.getServletRequestAttributeListener().attributeRemoved(new ServletRequestAttributeEvent(this.servletContext, this, name, oldValue));
                }
            }
        }

        @Override
        public String toString()
        {
            return getClass().getSimpleName() + "->" + super.getRequest();
        }

        private boolean isForwardingDispatcher()
        {
            return (DispatcherType.FORWARD == this.type) && (this.requestInfo != null);
        }

        private boolean isInclusionDispatcher()
        {
            return (DispatcherType.INCLUDE == this.type) && (this.requestInfo != null);
        }

        @Override
        public AsyncContext startAsync() throws IllegalStateException
        {
            if ( !this.asyncSupported )
            {
                throw new IllegalStateException();
            }
            return super.startAsync();
        }

        @Override
        public AsyncContext startAsync(final ServletRequest servletRequest,
                final ServletResponse servletResponse) throws IllegalStateException
        {
            if ( !this.asyncSupported )
            {
                throw new IllegalStateException();
            }
            return super.startAsync(servletRequest, servletResponse);
        }

        @Override
        public boolean isAsyncSupported()
        {
            return this.asyncSupported;
        }
    }

    private static class RequestInfo
    {
        final String servletPath;
        final String pathInfo;
        final String queryString;
        final String requestURI;

        public RequestInfo(String servletPath, String pathInfo, String queryString)
        {
            this.servletPath = servletPath;
            this.pathInfo = pathInfo;
            this.queryString = queryString;
            this.requestURI = UriUtils.compactPath(concat(servletPath, pathInfo));
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder("RequestInfo[servletPath =");
            sb.append(this.servletPath).append(", pathInfo = ").append(this.pathInfo);
            sb.append(", queryString = ").append(this.queryString).append("]");
            return sb.toString();
        }
    }

    private final HandlerRegistry handlerRegistry;

    private WhiteboardManager whiteboardManager;

    public Dispatcher(final HandlerRegistry handlerRegistry)
    {
        this.handlerRegistry = handlerRegistry;
    }

    public void setWhiteboardManager(final WhiteboardManager service)
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
            this.whiteboardManager.sessionDestroyed(session, ids);
        }
        String requestURI = getRequestURI(req);
        if ( requestURI == null )
        {
            requestURI = "";
        }

        // Determine which servlets we should forward the request to...
        final ServletHandler servletHandler = this.handlerRegistry.getServletHandler(requestURI);

        final HttpServletResponse wrappedResponse = new ServletResponseWrapper(req, res, servletHandler);
        if ( servletHandler == null )
        {
            wrappedResponse.sendError(404);
            return;
        }

        // strip of context path
        requestURI = requestURI.substring(servletHandler.getContext().getContextPath().length() - req.getContextPath().length());

        final String servletPath = servletHandler.determineServletPath(requestURI);
        String pathInfo = UriUtils.compactPath(UriUtils.relativePath(servletPath, requestURI));
        String queryString = null; // XXX

        final ExtServletContext servletContext = servletHandler.getContext();
        final RequestInfo requestInfo = new RequestInfo(servletPath, pathInfo, queryString);

        final HttpServletRequest wrappedRequest = new ServletRequestWrapper(req, servletContext, requestInfo, servletHandler.getContextServiceId(),
                servletHandler.getServletInfo().isAsyncSupported());
        final FilterHandler[] filterHandlers = this.handlerRegistry.getFilterHandlers(servletHandler, req.getDispatcherType(), requestURI);

        try
        {
            if ( servletContext.getServletRequestListener() != null )
            {
                servletContext.getServletRequestListener().requestInitialized(new ServletRequestEvent(servletContext, wrappedRequest));
            }
            invokeChain(filterHandlers, servletHandler, wrappedRequest, wrappedResponse);
        }
        catch ( final Exception e)
        {
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

    @Override
    public RequestDispatcher getNamedDispatcher(final Long contextId, final String name)
    {
        ServletHandler handler = this.handlerRegistry.getServletHandlerByName(contextId, name);
        return handler != null ? new RequestDispatcherImpl(handler, null) : null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(final Long contextId, String path)
    {
        // See section 9.1 of Servlet 3.x specification...
        if (path == null || (!path.startsWith("/") && !"".equals(path)))
        {
            return null;
        }

        String query = null;
        int q = 0;
        if ((q = path.indexOf('?')) > 0)
        {
            query = path.substring(q + 1);
            path = path.substring(0, q);
        }
        // TODO remove path parameters...
        String requestURI = decodePath(removeDotSegments(path));
        if ( requestURI == null )
        {
            requestURI = "";
        }

        ServletHandler handler = this.handlerRegistry.getServletHandler(requestURI);
        if (handler == null)
        {
            return null;
        }

        String servletPath = handler.determineServletPath(requestURI);
        String pathInfo = UriUtils.relativePath(servletPath, path);

        RequestInfo requestInfo = new RequestInfo(servletPath, pathInfo, query);
        return new RequestDispatcherImpl(handler, requestInfo);
    }

    /**
     * @param servletHandler the servlet that should handle the forward request;
     * @param request the {@link HttpServletRequest};
     * @param response the {@link HttpServletResponse};
     */
    void forward(ServletHandler servletHandler, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String requestURI = getRequestURI(request);
        FilterHandler[] filterHandlers = this.handlerRegistry.getFilterHandlers(servletHandler, DispatcherType.FORWARD, requestURI);

        invokeChain(filterHandlers, servletHandler, request, response);
    }

    /**
     * @param servletHandler the servlet that should handle the include request;
     * @param request the {@link HttpServletRequest};
     * @param response the {@link HttpServletResponse};
     */
    void include(ServletHandler servletHandler, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String requestURI = getRequestURI(request);
        FilterHandler[] filterHandlers = this.handlerRegistry.getFilterHandlers(servletHandler, DispatcherType.INCLUDE, requestURI);

        invokeChain(filterHandlers, servletHandler, request, response);
    }

    private String getRequestURI(HttpServletRequest req)
    {
        return UriUtils.relativePath(req.getContextPath(), req.getRequestURI());
    }

    private void invokeChain(FilterHandler[] filterHandlers, ServletHandler servletHandler, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        final FilterChain filterChain = new InvocationChain(servletHandler, filterHandlers);
        filterChain.doFilter(request, response);
    }
}
