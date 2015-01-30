/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.whiteboard;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.context.ServletContextHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

public class HttpContextBridge extends ServletContextHelper implements HttpContext {

    private final ServletContextHelper delegatee;

    public HttpContextBridge(final ServletContextHelper delegatee)
    {
        this.delegatee = delegatee;
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        return delegatee.handleSecurity(request, response);
    }

    @Override
    public URL getResource(String name)
    {
        return delegatee.getResource(name);
    }

    @Override
    public String getMimeType(String name)
    {
        return delegatee.getMimeType(name);
    }

    @Override
    public Set<String> getResourcePaths(String path)
    {
        return delegatee.getResourcePaths(path);
    }

    @Override
    public String getRealPath(String path)
    {
        return delegatee.getRealPath(path);
    }

    public ServletContextHelper getDelegatee()
    {
        return delegatee;
    }
}
