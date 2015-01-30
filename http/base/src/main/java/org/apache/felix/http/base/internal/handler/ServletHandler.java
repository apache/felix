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
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.felix.http.base.internal.util.UriUtils.concat;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.runtime.ServletInfo;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class ServletHandler extends AbstractHandler implements Comparable<ServletHandler>
{
    private class RequestDispatcherImpl implements RequestDispatcher
    {
        final String servletPath = "/".equals(getAlias()) ? "" : getAlias(); // XXX handle wildcard aliases!
        final String requestURI;
        final String pathInfo;
        final String query;
        final boolean named;

        public RequestDispatcherImpl()
        {
            this.requestURI = null;
            this.pathInfo = null;
            this.query = null;
            this.named = true;
        }

        public RequestDispatcherImpl(String uri, String pathInContext, String query)
        {
            this.requestURI = uri;
            this.pathInfo = this.servletPath.equals(pathInContext) ? null : pathInContext;
            this.query = query;
            this.named = false;
        }

        @Override
        public void forward(ServletRequest req, ServletResponse res) throws ServletException, IOException
        {
            if (res.isCommitted())
            {
                throw new ServletException("Response has been committed");
            }
            else
            {
                // See section 9.4 of Servlet 3.0 spec
                res.resetBuffer();
            }

            // Since we're already created this RequestDispatcher for *this* servlet handler, we do not need to
            // recheck whether its patch matches, but instead can directly handle the forward-request...
            doHandle(new ServletRequestWrapper((HttpServletRequest) req, this, DispatcherType.FORWARD), (HttpServletResponse) res);

            // After a forward has taken place, the results should be committed,
            // see section 9.4 of Servlet 3.0 spec...
            if (!req.isAsyncStarted())
            {
                res.flushBuffer();
                res.getWriter().close();
            }
        }

        @Override
        public void include(ServletRequest req, ServletResponse res) throws ServletException, IOException
        {
            // Since we're already created this RequestDispatcher for *this* servlet handler, we do not need to
            // recheck whether its patch matches, but instead can directly handle the include-request...
            doHandle(new ServletRequestWrapper((HttpServletRequest) req, this, DispatcherType.INCLUDE), (HttpServletResponse) res);
        }

        boolean isNamedDispatcher()
        {
            return this.named;
        }
    }

    private static class ServletRequestWrapper extends HttpServletRequestWrapper
    {
        private final RequestDispatcherImpl dispatcher;
        private final DispatcherType type;

        public ServletRequestWrapper(HttpServletRequest req, RequestDispatcherImpl dispatcher, DispatcherType type)
        {
            super(req);
            this.dispatcher = dispatcher;
            this.type = type;
        }

        @Override
        public Object getAttribute(String name)
        {
            HttpServletRequest request = (HttpServletRequest) getRequest();
            if (isInclusionDispatcher())
            {
                if (INCLUDE_REQUEST_URI.equals(name))
                {
                    return concat(request.getContextPath(), this.dispatcher.requestURI);
                }
                else if (INCLUDE_CONTEXT_PATH.equals(name))
                {
                    return request.getContextPath();
                }
                else if (INCLUDE_SERVLET_PATH.equals(name))
                {
                    return this.dispatcher.servletPath;
                }
                else if (INCLUDE_PATH_INFO.equals(name))
                {
                    return this.dispatcher.pathInfo;
                }
                else if (INCLUDE_QUERY_STRING.equals(name))
                {
                    return this.dispatcher.query;
                }
            }
            else if (isForwardingDispatcher())
            {
                // NOTE: the forward.* attributes *always* yield the *original* values...
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
        public DispatcherType getDispatcherType()
        {
            return this.type;
        }

        @Override
        public String getPathInfo()
        {
            if (isForwardingDispatcher())
            {
                return this.dispatcher.pathInfo;
            }
            return super.getPathInfo();
        }

        @Override
        public String getRequestURI()
        {
            if (isForwardingDispatcher())
            {
                return concat(getContextPath(), this.dispatcher.requestURI);
            }
            return super.getRequestURI();
        }

        @Override
        public String getServletPath()
        {
            if (isForwardingDispatcher())
            {
                return this.dispatcher.servletPath;
            }
            return super.getServletPath();
        }

        @Override
        public String toString()
        {
            return getClass().getSimpleName() + "->" + super.getRequest();
        }

        private boolean isForwardingDispatcher()
        {
            return DispatcherType.FORWARD == this.type && !this.dispatcher.isNamedDispatcher();
        }

        private boolean isInclusionDispatcher()
        {
            return DispatcherType.INCLUDE == this.type && !this.dispatcher.isNamedDispatcher();
        }
    }

    private final ServletInfo servletInfo;

    private final Servlet servlet;

    private final Pattern pattern;

    private final String alias;

    public ServletHandler(final ExtServletContext context,
                          final ServletInfo servletInfo,
                          final Servlet servlet,
                          final String alias)
    {
        super(context, servletInfo.getInitParams(), servletInfo.getName());
        this.servlet = servlet;
        this.pattern = Pattern.compile(alias.replace(".", "\\.").replace("*", ".*"));
        this.alias = alias;
        this.servletInfo = servletInfo;
    }

    @Override
    public int compareTo(ServletHandler other)
    {
        int result = other.alias.length() - this.alias.length();
        if ( result == 0 )
        {
            result = this.alias.compareTo(other.alias);
        }
        return result;
    }

    public RequestDispatcher createNamedRequestDispatcher()
    {
        return new RequestDispatcherImpl();
    }

    public RequestDispatcher createRequestDispatcher(String path, String pathInContext, String query)
    {
        return new RequestDispatcherImpl(path, pathInContext, query);
    }

    @Override
    public void destroy()
    {
        this.servlet.destroy();
    }

    public String getAlias()
    {
        return this.alias;
    }

    public Servlet getServlet()
    {
        return this.servlet;
    }

    public boolean handle(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        String path;
        if (DispatcherType.INCLUDE == req.getDispatcherType())
        {
            path = (String) req.getAttribute(INCLUDE_SERVLET_PATH);
        }
        else if (DispatcherType.FORWARD == req.getDispatcherType())
        {
            path = (String) req.getAttribute(FORWARD_SERVLET_PATH);
        }
        else if (DispatcherType.ASYNC == req.getDispatcherType())
        {
            path = (String) req.getAttribute("javax.servlet.async.path_info");
        }
        else
        {
            path = req.getPathInfo();
        }

        final boolean matches = matches(path);
        if (matches)
        {
            doHandle(req, res);
        }

        return matches;
    }

    @Override
    public void init() throws ServletException
    {
        this.servlet.init(new ServletConfigImpl(getName(), getContext(), getInitParams()));
    }

    public boolean matches(String uri)
    {
        // TODO handle wildcard aliases and extension specs...
        if (uri == null)
        {
            return this.alias.equals("/");
        }
        else if (this.alias.equals("/"))
        {
            return uri.startsWith(this.alias);
        }
        else if ( uri.equals(this.alias) || uri.startsWith(this.alias + "/") )
        {
            return true;
        }
        return this.pattern.matcher(uri).matches();
    }

    public ServletInfo getServletInfo()
    {
        return this.servletInfo;
    }

    final void doHandle(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        // Only wrap the original ServletRequest in case we're handling plain requests,
        // not inclusions or forwards from servlets. Should solve FELIX-2774 and FELIX-3054...
        if (DispatcherType.REQUEST == req.getDispatcherType())
        {
            req = new ServletHandlerRequest(req, getContext(), this.alias);
        }

        if (getContext().handleSecurity(req, res))
        {
            this.servlet.service(req, res);
        }
        else
        {
            // FELIX-3988: If the response is not yet committed and still has the default
            // status, we're going to override this and send an error instead.
            if (!res.isCommitted() && res.getStatus() == SC_OK)
            {
                res.sendError(SC_FORBIDDEN);
            }
        }
    }

    @Override
    protected Object getSubject()
    {
        return this.servlet;
    }
}
