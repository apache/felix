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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.ServletRuntime;
import org.apache.felix.http.base.internal.util.PatternUtil;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class ServletHandler extends AbstractHandler<ServletHandler> implements ServletRuntime
{
    private final ServletInfo servletInfo;
    private final Pattern[] patterns;
    private final long contextServiceId;

    public ServletHandler(final long contextServiceId,
            final ExtServletContext context,
            final ServletInfo servletInfo)
    {
        super(context, servletInfo.getInitParameters(), servletInfo.getName());

        this.servletInfo = servletInfo;

        // Can be null in case of error-handling servlets...
        String[] patterns = this.servletInfo.getPatterns();
        final int length = patterns == null ? 0 : patterns.length;

        this.patterns = new Pattern[length];
        for (int i = 0; i < length; i++)
        {
            final String pattern = patterns[i];
            this.patterns[i] = Pattern.compile(PatternUtil.convertToRegEx(pattern));
        }

        this.contextServiceId = contextServiceId;
    }

    @Override
    public int compareTo(final ServletHandler other)
    {
        return this.servletInfo.compareTo(other.servletInfo);
    }

    public void handle(ServletRequest req, ServletResponse res) throws ServletException, IOException
    {
        getServlet().service(req, res);
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
    public Pattern[] getPatterns()
    {
        return this.patterns;
    }

    @Override
    public ServletInfo getServletInfo()
    {
        return this.servletInfo;
    }

    @Override
    public long getContextServiceId()
    {
        return this.contextServiceId;
    }

    @Override
    protected long getServiceId()
    {
        return this.servletInfo.getServiceId();
    }

    @Override
    protected Object getSubject()
    {
        return getServlet();
    }

    protected static ServletInfo checkIsResource(ServletInfo servletInfo, boolean checkTrue)
    {
        if (checkTrue != servletInfo.isResource())
        {
            String message = "ServletInfo must " + (checkTrue ? "" : "not") + " represent a resource";
            throw new IllegalArgumentException(message);
        }
        return servletInfo;
    }
}
