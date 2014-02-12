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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.dispatch.Dispatcher;
import org.apache.felix.http.base.internal.dispatch.RequestDispatcherProvider;
import org.apache.felix.http.base.internal.util.UriUtils;
import org.osgi.service.http.HttpContext;
import org.osgi.service.useradmin.Authorization;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings("deprecation")
class ServletHandlerRequest extends HttpServletRequestWrapper
{
    private final String alias;
    private final ServletContextWrapper context;
    private String contextPath;
    private String pathInfo;
    private boolean pathInfoCalculated = false;

    public ServletHandlerRequest(HttpServletRequest req, ExtServletContext context, String alias)
    {
        super(req);
        this.context = new ServletContextWrapper(context, (RequestDispatcherProvider) req.getAttribute(Dispatcher.REQUEST_DISPATCHER_PROVIDER));
        this.alias = alias;
    }

    @Override
    public String getAuthType()
    {
        String authType = (String) getAttribute(HttpContext.AUTHENTICATION_TYPE);
        if (authType != null)
        {
            return authType;
        }

        return super.getAuthType();
    }

    @Override
    public String getContextPath()
    {
        /*
         * FELIX-2030 Calculate the context path for the Http Service
         * registered servlets from the container context and servlet paths
         */
        if (contextPath == null)
        {
            final String context = super.getContextPath();
            final String servlet = super.getServletPath();
            if (context == null || context.length() == 0)
            {
                contextPath = servlet;
            }
            else if (servlet == null || servlet.length() == 0)
            {
                contextPath = context;
            }
            else
            {
                contextPath = context + servlet;
            }
        }

        return contextPath;
    }

    @Override
    public String getPathInfo()
    {
        if (!this.pathInfoCalculated)
        {
            this.pathInfo = calculatePathInfo();
            this.pathInfoCalculated = true;
        }

        return this.pathInfo;
    }

    @Override
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
        // See section 9.1 of Servlet 3.0 specification...
        if (path == null)
        {
            return null;
        }
        // Handle relative paths, see Servlet 3.0 spec, section 9.1 last paragraph.
        boolean relPath = !path.startsWith("/") && !"".equals(path);
        if (relPath)
        {
            path = UriUtils.concat(this.alias, path);
        }
        return super.getRequestDispatcher(path);
    }

    @Override
    public ServletContext getServletContext()
    {
        return this.context;
    }

    @Override
    public String getServletPath()
    {
        if ("/".equals(this.alias))
        {
            return "";
        }
        return this.alias;
    }

    @Override
    public HttpSession getSession(boolean create)
    {
        // FELIX-2797: wrap the original HttpSession to provide access to the correct ServletContext...
        HttpSession session = super.getSession(create);
        if (session == null)
        {
            return null;
        }
        return new HttpSessionWrapper(session, this.context);
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
    public String toString()
    {
        return getClass().getSimpleName() + "->" + super.getRequest();
    }

    private String calculatePathInfo()
    {
        /*
         * The pathInfo from the servlet container is
         *       servletAlias + pathInfo
         * where pathInfo is either an empty string (in which case the
         * client directly requested the servlet) or starts with a slash
         * (in which case the client requested a child of the servlet).
         *
         * Note, the servlet container pathInfo may also be null if the
         * servlet is registered as the root servlet
         */
        String pathInfo = super.getPathInfo();
        if (pathInfo != null)
        {
            // cut off alias of this servlet (if not the root servlet)
            if (!"/".equals(this.alias) && pathInfo.startsWith(this.alias))
            {
                pathInfo = pathInfo.substring(alias.length());
            }

            // ensure empty string is coerced to null
            if (pathInfo.length() == 0)
            {
                pathInfo = null;
            }
        }

        return pathInfo;
    }
}
