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
package org.apache.felix.http.base.internal.whiteboard;

import javax.servlet.Servlet;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.service.HttpServiceImpl;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpService;

public final class ServletMapping extends AbstractMapping
{
    private final Servlet servlet;
    private final ServletInfo servletInfo;

    public ServletMapping(Bundle bundle, Servlet servlet, ServletInfo servletInfo)
    {
        super(bundle);
        this.servlet = servlet;
        this.servletInfo = servletInfo;
    }

    @Override
    public void register(HttpService httpService)
    {
        if (!isRegistered() && (httpService instanceof HttpServiceImpl) && getContext() != null)
        {
            this.servletInfo.context = getContext(); // XXX
            try
            {
                ((HttpServiceImpl) httpService).registerServlet(this.servlet, this.servletInfo);
                setRegistered(true);
            }
            catch (Exception e)
            {
                // Warn that something might have gone astray...
                SystemLogger.warning("Failed to register servlet for " + this.servletInfo.name, e);
            }
            setRegistered(true);
        }
    }

    @Override
    public void unregister(HttpService httpService)
    {
        if (isRegistered() && (httpService instanceof HttpServiceImpl))
        {
            try
            {
                ((HttpServiceImpl) httpService).unregisterServlet(this.servlet);
            }
            catch (Exception e)
            {
                // Warn that something might have gone astray...
                SystemLogger.debug("Failed to unregister servlet for " + this.servletInfo.name, e);
            }
            finally
            {
                // Best effort: avoid mappings that are registered which is reality aren't registered...
                setRegistered(false);
            }
        }
    }

    Servlet getServlet()
    {
        return this.servlet;
    }
}
