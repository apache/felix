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
package org.apache.felix.http.base.internal.handler.holder;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.http.base.internal.handler.ServletConfigImpl;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.service.http.runtime.dto.DTOConstants;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class ServletHolder extends AbstractHolder<ServletHolder>
{
    private final ServletInfo servletInfo;

    private volatile Servlet servlet;

    public ServletHolder(final ServletContext context,
            final ServletInfo servletInfo)
    {
        super(context);

        this.servletInfo = servletInfo;
    }

    protected Servlet getServlet()
    {
        return servlet;
    }

    protected void setServlet(final Servlet s)
    {
        this.servlet = s;
    }

    @Override
    public int compareTo(final ServletHolder other)
    {
        return this.servletInfo.compareTo(other.servletInfo);
    }

    public void handle(final ServletRequest req, final ServletResponse res)
            throws ServletException, IOException
    {
        this.servlet.service(req, res);
    }

    protected ServletInfo getServletInfo()
    {
        return this.servletInfo;
    }

    protected String getName()
    {
        String name = this.servletInfo.getName();
        if (name == null)
        {
            name = servlet.getClass().getName();
        }
        return name;
    }

    @Override
    public int init()
    {
        if (this.servlet == null)
        {
            return DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE;
        }

        try {
            servlet.init(new ServletConfigImpl(getName(), getContext(), getServletInfo().getInitParameters()));
        } catch (final ServletException e) {
            SystemLogger.error(this.getServletInfo().getServiceReference(),
                    "Error during calling init() on servlet " + this.servlet,
                    e);
            return DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT;
        }
        return -1;
    }

    @Override
    public void destroy()
    {
        if (this.servlet == null)
        {
            return;
        }

        try {
            servlet.destroy();
        } catch ( final Exception ignore ) {
            // we ignore this
            SystemLogger.error(this.getServletInfo().getServiceReference(),
                    "Error during calling destroy() on servlet " + this.servlet,
                    ignore);
        }

        servlet = null;
    }
}
