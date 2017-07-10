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
package org.apache.felix.http.whiteboard.internal.manager;

import javax.servlet.Servlet;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpService;

public final class ServletMapping extends AbstractMapping
{
    private final Servlet servlet;
    private final String alias;

    public ServletMapping(Bundle bundle, Servlet servlet, String alias)
    {
        super(bundle);
        this.servlet = servlet;
        this.alias = alias;
    }

    public void register(HttpService httpService)
    {
        if (!isRegistered() && getContext() != null)
        {
            try
            {
                httpService.registerServlet(this.alias, this.servlet, getInitParams(), getContext());
                setRegistered(true);
            }
            catch (Exception e)
            {
                // Warn that something might have gone astray...
                SystemLogger.warning("Failed to register servlet for " + this.alias, e);
            }
        }
    }

    public void unregister(HttpService httpService)
    {
        if (isRegistered())
        {
            try
            {
                httpService.unregister(this.alias);
            }
            catch (Exception e)
            {
                // Warn that something might have gone astray...
                SystemLogger.debug("Failed to unregister servlet for " + this.alias, e);
            }
            finally
            {
                // Best effort: avoid mappings that are registered which is reality aren't registered...
                setRegistered(false);
            }
        }
    }

    String getAlias()
    {
        return this.alias;
    }

    Servlet getServlet()
    {
        return this.servlet;
    }
}
