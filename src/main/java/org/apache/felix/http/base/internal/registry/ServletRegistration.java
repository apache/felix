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
package org.apache.felix.http.base.internal.registry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.util.UriUtils;

/**
 * Servlet is registered with a pattern and a servlet handler
 */
public class ServletRegistration
{
    private final ServletHandler handler;
    private final Pattern pattern;

    public ServletRegistration(@Nonnull final ServletHandler handler, @Nonnull final Pattern pattern)
    {
        this.handler = handler;
        this.pattern = pattern;
    }

    public ServletHandler getServletHandler()
    {
        return this.handler;
    }

    public PathResolution resolve(@Nonnull final String requestURI)
    {
        final Matcher matcher = pattern.matcher(requestURI);
        if (matcher.find(0))
        {
            final PathResolution pr = new PathResolution();
            pr.servletPath = matcher.groupCount() > 0 ? matcher.group(1) : matcher.group();
            pr.pathInfo = UriUtils.compactPath(UriUtils.relativePath(pr.servletPath, requestURI));
            pr.handler = this.handler;

            return pr;
        }

        return null;
    }
}
