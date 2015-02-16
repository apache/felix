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
package org.apache.felix.http.base.internal;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The dispatcher servlet is registered in the container.
 *
 * When {@link #init(ServletConfig)} is called, the Http services are registered
 * and started, when {@link #destroy()} is called the services are stopped
 * and unregistered.
 */
public final class DispatcherServlet extends HttpServlet
{
    private final HttpServiceController controller;

    public DispatcherServlet(final HttpServiceController controller)
    {
        this.controller = controller;
    }

    @Override
    public void init(final ServletConfig config) throws ServletException
    {
        super.init(config);
        this.controller.register(getServletContext());
    }

    @Override
    public void destroy()
    {
        this.controller.unregister();
        super.destroy();
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse res)
            throws ServletException, IOException
    {
        this.controller.getDispatcher().dispatch(req, res);
    }
}
