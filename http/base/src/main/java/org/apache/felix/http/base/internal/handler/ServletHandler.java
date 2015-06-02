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

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.service.http.runtime.dto.DTOConstants;

/**
 * The servlet handler handles the initialization and destruction of
 * a servlet.
 */
public abstract class ServletHandler implements Comparable<ServletHandler>
{
    private final long contextServiceId;

    private final ServletInfo servletInfo;

    private final ExtServletContext context;

    private volatile Servlet servlet;

    protected volatile int useCount;

    public ServletHandler(final long contextServiceId,
            final ExtServletContext context,
            final ServletInfo servletInfo)
    {
        this.contextServiceId = contextServiceId;
        this.context = context;
        this.servletInfo = servletInfo;
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
    }

    public Servlet getServlet()
    {
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

}
