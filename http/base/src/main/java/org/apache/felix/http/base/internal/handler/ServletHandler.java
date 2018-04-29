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

<<<<<<< HEAD
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.felix.http.base.internal.context.ExtServletContext;
import java.io.IOException;

public final class ServletHandler
    extends AbstractHandler implements Comparable<ServletHandler>
{
    private final String alias;
    private final Servlet servlet;

    public ServletHandler(ExtServletContext context, Servlet servlet, String alias)
    {
        super(context);
        this.alias = alias;
        this.servlet = servlet;
    }

    public String getAlias()
    {
        return this.alias;
=======
import java.io.File;
import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.dispatch.MultipartConfig;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.framework.Bundle;
import org.osgi.service.http.runtime.dto.DTOConstants;

/**
 * The servlet handler handles the initialization and destruction of
 * a servlet.
 */
public abstract class ServletHandler implements Comparable<ServletHandler>
{
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    private static final String JAVA_SERVLET_TEMP_DIR_PROP = "javax.servlet.content.tempdir";

    private final long contextServiceId;

    private final ServletInfo servletInfo;

    private final ExtServletContext context;

    private volatile Servlet servlet;

    protected volatile int useCount;

    private final MultipartConfig mpConfig;

    public ServletHandler(final long contextServiceId,
            final ExtServletContext context,
            final ServletInfo servletInfo)
    {
        this.contextServiceId = contextServiceId;
        this.context = context;
        this.servletInfo = servletInfo;
        final MultipartConfig origConfig = servletInfo.getMultipartConfig();
        if ( origConfig != null )
        {
            String location = origConfig.multipartLocation;
            if ( location == null ) {
                final Object obj = context == null ? null : context.getAttribute(JAVA_SERVLET_TEMP_DIR_PROP);
                if ( obj != null ) {
                    if ( obj instanceof File ) {
                        location = ((File)obj).getAbsolutePath();
                    } else {
                        location = obj.toString();
                    }
                }
            }
            if ( location == null ) {
                location = TEMP_DIR;
            }
            this.mpConfig = new MultipartConfig(origConfig.multipartThreshold,
                    location,
                    origConfig.multipartMaxFileSize,
                    origConfig.multipartMaxRequestSize);
        }
        else
        {
            this.mpConfig = null;
        }
    }

    @Override
    public int compareTo(final ServletHandler other)
    {
        return this.servletInfo.compareTo(other.servletInfo);
    }

    public long getContextServiceId()
    {
        return this.contextServiceId;
    }

    public ExtServletContext getContext()
    {
        return this.context;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    public Servlet getServlet()
    {
<<<<<<< HEAD
        return this.servlet;
    }

    public void init()
        throws ServletException
    {
        String name = "servlet_" + getId();
        ServletConfig config = new ServletConfigImpl(name, getContext(), getInitParams());
        this.servlet.init(config);
    }

    public void destroy()
    {
        this.servlet.destroy();
    }

    public boolean matches(String uri)
    {
        if (uri == null) {
            return this.alias.equals("/");
        } else if (this.alias.equals("/")) {
            return uri.startsWith(this.alias);
        } else {
            return uri.equals(this.alias) || uri.startsWith(this.alias + "/");
        }
    }

    public boolean handle(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException
    {
        final boolean matches = matches(req.getPathInfo());
        if (matches) {
            doHandle(req, res);
        }

        return matches;
    }

    private void doHandle(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException
    {
        // set a sensible status code in case handleSecurity returns false
        // but fails to send a response
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        if (getContext().handleSecurity(req, res))
        {
            // reset status to OK for further processing
            res.setStatus(HttpServletResponse.SC_OK);

            this.servlet.service(new ServletHandlerRequest(req, this.alias), res);
        }
    }

    public int compareTo(ServletHandler other)
    {
        return other.alias.length() - this.alias.length();
=======
        return servlet;
    }

    protected void setServlet(final Servlet s)
    {
        this.servlet = s;
    }

    public void handle(final ServletRequest req, final ServletResponse res)
            throws ServletException, IOException
    {
        this.servlet.service(req, res);
    }

    public ServletInfo getServletInfo()
    {
        return this.servletInfo;
    }

    public String getName()
    {
        String name = this.servletInfo.getName();
        if (name == null && servlet != null )
        {
            name = servlet.getClass().getName();
        }
        return name;
    }

    /**
     * Initialize the object
     * @return {code -1} on success, a failure reason according to {@link DTOConstants} otherwise.
     */
    public int init()
    {
        if ( this.useCount > 0 )
        {
            this.useCount++;
            return -1;
        }

        if (this.servlet == null)
        {
            return DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE;
        }

        try
        {
            servlet.init(new ServletConfigImpl(getName(), getContext(), getServletInfo().getInitParameters()));
        }
        catch (final Exception e)
        {
            SystemLogger.error(this.getServletInfo().getServiceReference(),
                    "Error during calling init() on servlet " + this.servlet,
                    e);
            return DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT;
        }
        this.useCount++;
        return -1;
    }


    public boolean destroy()
    {
        if (this.servlet == null)
        {
            return false;
        }

        this.useCount--;
        if ( this.useCount == 0 )
        {
            try
            {
                servlet.destroy();
            }
            catch ( final Exception ignore )
            {
                // we ignore this
                SystemLogger.error(this.getServletInfo().getServiceReference(),
                        "Error during calling destroy() on servlet " + this.servlet,
                        ignore);
            }

            servlet = null;
            return true;
        }
        return false;
    }

    public boolean dispose()
    {
        // fully destroy the servlet
        this.useCount = 1;
        return this.destroy();
    }

    @Override
    public int hashCode()
    {
        return 31 + servletInfo.hashCode();
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || getClass() != obj.getClass() )
        {
            return false;
        }
        final ServletHandler other = (ServletHandler) obj;
        return servletInfo.equals(other.servletInfo);
    }

    public MultipartConfig getMultipartConfig()
    {
        return mpConfig;
    }

    public Bundle getMultipartSecurityContext()
    {
        return null;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }
}
