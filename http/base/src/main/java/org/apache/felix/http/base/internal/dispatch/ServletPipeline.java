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

import static org.apache.felix.http.base.internal.util.UriUtils.decodePath;
import static org.apache.felix.http.base.internal.util.UriUtils.removeDotSegments;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.handler.ServletHandler;

public final class ServletPipeline implements RequestDispatcherProvider
{
    private final ServletHandler[] handlers;

    public ServletPipeline(ServletHandler[] handlers)
    {
        this.handlers = handlers;
    }

    public RequestDispatcher getNamedDispatcher(String name)
    {
        // See section 9.1 of Servlet 3.x specification...
        if (name == null)
        {
            return null;
        }

        for (ServletHandler handler : this.handlers)
        {
            if (name.equals(handler.getName()))
            {
                return handler.createNamedRequestDispatcher();
            }
        }

        return null;
    }

    public RequestDispatcher getRequestDispatcher(String path)
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
        String pathInContext = decodePath(removeDotSegments(path));

        for (ServletHandler handler : this.handlers)
        {
            if (handler.matches(pathInContext))
            {
                return handler.createRequestDispatcher(path, pathInContext, query);
            }
        }

        return null;
    }

    public boolean handle(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        // NOTE: this code assumes that HttpServletRequest#getRequestDispatcher() is properly mapped, see FilterPipeline.FilterRequestWrapper!
        for (ServletHandler handler : this.handlers)
        {
            if (handler.handle(req, res))
            {
                return true;
            }
        }

        return false;
    }

    public boolean hasServletsMapped()
    {
        return this.handlers.length > 0;
    }
}
