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

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.handler.HandlerRegistry;

public final class Dispatcher
{
    public static final String REQUEST_DISPATCHER_PROVIDER = "org.apache.felix.http.requestDispatcherProvider";

    private static final FilterChain DEFAULT_CHAIN = new NotFoundFilterChain();

    private final HandlerRegistry handlerRegistry;

    public Dispatcher(HandlerRegistry handlerRegistry)
    {
        this.handlerRegistry = handlerRegistry;
    }

    public void dispatch(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        ServletPipeline servletPipeline = new ServletPipeline(this.handlerRegistry.getServlets());
        // Provides access to the correct request dispatcher...
        req.setAttribute(REQUEST_DISPATCHER_PROVIDER, servletPipeline);

        try
        {
            new FilterPipeline(this.handlerRegistry.getFilters(), servletPipeline).dispatch(req, res, DEFAULT_CHAIN);
        }
        finally
        {
            req.removeAttribute(REQUEST_DISPATCHER_PROVIDER);
        }
    }
}
