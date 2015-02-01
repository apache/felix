/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.jetty.internal;

import static javax.servlet.RequestDispatcher.ERROR_EXCEPTION_TYPE;
import static javax.servlet.RequestDispatcher.ERROR_MESSAGE;
import static javax.servlet.RequestDispatcher.ERROR_STATUS_CODE;
import static org.eclipse.jetty.http.HttpMethod.GET;
import static org.eclipse.jetty.http.HttpMethod.HEAD;
import static org.eclipse.jetty.http.HttpMethod.POST;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.DispatcherServlet;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.ByteArrayISO8859Writer;

/**
 * Provides a hook into the error-handling of Jetty, allowing us to handle all status codes &gt;= 400 and/or exceptions thrown by servlets/filters.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CustomErrorHandler extends ErrorHandler
{
    private final DispatcherServlet dispatcher;

    /**
     * Creates a new {@link CustomErrorHandler} instance.
     * 
     * @param dispatcher the dispatcher servlet that is going to retrieve the error page for us.
     */
    public CustomErrorHandler(DispatcherServlet dispatcher)
    {
        this.dispatcher = dispatcher;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        // We're handling this request either by delegating it to a specific error handler, or by writing a custom error page ourselves...
        baseRequest.setHandled(true);

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        if (!GET.equals(method) && !POST.equals(method) && !HEAD.equals(method))
        {
            return;
        }

        int exceptionRC = (Integer) request.getAttribute(ERROR_STATUS_CODE);
        String reason = (String) request.getAttribute(ERROR_MESSAGE);
        Class<?> exceptionType = (Class<?>) request.getAttribute(ERROR_EXCEPTION_TYPE);

        if (!this.dispatcher.handleError(request, response, exceptionRC, (exceptionType == null) ? null : exceptionType.getName()))
        {
            if (getCacheControl() != null)
            {
                response.setHeader(HttpHeader.CACHE_CONTROL.asString(), getCacheControl());
            }

            ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(4096);
            response.setContentType(MimeTypes.Type.TEXT_HTML_8859_1.asString());

            handleErrorPage(request, writer, exceptionRC, reason);
            writer.flush();
            response.setContentLength(writer.size());
            writer.writeTo(response.getOutputStream());
            writer.destroy();
        }
    }
}
