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

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.util.PatternUtil;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServletHandler extends AbstractHandler<ServletHandler>
{
    private final ServletInfo servletInfo;

    private Servlet servlet;

    private final Pattern[] patterns;

    private final long contextServiceId;
    
    private final boolean isWhiteboardService;

    public ServletHandler(final ServletContextHelperInfo contextInfo,
                          final ExtServletContext context,
                          final ServletInfo servletInfo,
                          final Servlet servlet)
    {
    	this(contextInfo, context, servletInfo, servlet, false);
    }
    
    public ServletHandler(final ServletContextHelperInfo contextInfo,
            final ExtServletContext context,
            final ServletInfo servletInfo,
            final Servlet servlet, 
            final boolean isWhiteboardService)
    {
    	super(context, servletInfo.getInitParameters(), servletInfo.getName());
    	
        this.servlet = servlet;
        this.servletInfo = servletInfo;
        this.isWhiteboardService = isWhiteboardService;

        // Can be null in case of error-handling servlets...
        String[] patterns = this.servletInfo.getPatterns();
        final int length = patterns == null ? 0 : patterns.length;

        this.patterns = new Pattern[length];
        for (int i = 0; i < length; i++)
        {
            final String pattern = patterns[i];
            this.patterns[i] = Pattern.compile(PatternUtil.convertToRegEx(pattern));
        }
        if ( contextInfo != null )
        {
            this.contextServiceId = contextInfo.getServiceId();
        }
        else
        {
            this.contextServiceId = 0;
        }
    }
    

    @Override
    public int compareTo(final ServletHandler other)
    {
        return this.servletInfo.compareTo(other.servletInfo);
    }

    public String determineServletPath(String uri)
    {
        if (uri == null)
        {
            uri = "/";
        }

        // Patterns are sorted on length in descending order, so we should get the longest match first...
        for (int i = 0; i < this.patterns.length; i++)
        {
            Matcher matcher = this.patterns[i].matcher(uri);
            if (matcher.find(0))
            {
                return matcher.groupCount() > 0 ? matcher.group(1) : matcher.group();
            }
        }

        return null;
    }

    @Override
    public void destroy()
    {
        this.servlet.destroy();
    }

    public Servlet getServlet()
    {
        return this.servlet;
    }
    
    void setServlet(Servlet servlet)
    {
    	this.servlet = servlet;
    }
    
    public boolean isWhiteboardService()
    {
    	return this.isWhiteboardService;
    }

    @Override
    public Pattern[] getPatterns()
    {
        return this.patterns;
    }

    @Override
    public void init() throws ServletException
    {
        this.servlet.init(new ServletConfigImpl(getName(), getContext(), getInitParams()));
    }

    public boolean handle(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        if (getContext().handleSecurity(req, res))
        {
            this.servlet.service(req, res);

            return true;
        }

        // FELIX-3988: If the response is not yet committed and still has the default
        // status, we're going to override this and send an error instead.
        if (!res.isCommitted() && (res.getStatus() == SC_OK || res.getStatus() == 0))
        {
            res.sendError(SC_FORBIDDEN);
        }

        return false;
    }

    public ServletInfo getServletInfo()
    {
        return this.servletInfo;
    }

    @Override
    protected Object getSubject()
    {
        return this.servlet;
    }

    public long getContextServiceId()
    {
        return this.contextServiceId;
    }

    @Override
    protected long getServiceId()
    {
        return this.servletInfo.getServiceId();
    }
}
