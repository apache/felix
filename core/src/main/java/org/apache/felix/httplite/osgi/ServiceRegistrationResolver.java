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
package org.apache.felix.httplite.osgi;

import java.io.OutputStream;
import java.net.Socket;

import org.apache.felix.httplite.servlet.HttpServletRequestImpl;
import org.apache.felix.httplite.servlet.HttpServletResponseImpl;

/**
 * An interface to provide internal classes with access to client service registration state.
 * This interface serves as the connection between the internal server and the HTTP Service.
 *
 */
public interface ServiceRegistrationResolver
{
    /**
     * Given a socket connection return a HttpServletRequestImpl.
     * 
     * @param socket socket connection to be associated with HTTP Request
     * @return HttpServletRequestImpl instance
     */
    HttpServletRequestImpl getServletRequest(Socket socket);

    /**
     * Resolve the requestPath to a Resource or Servlet registration with the closest (deepest) match.
     * 
     * @param requestPath URI of request
     * @return A ServiceRegistration or null if no match was found.
     */
    ServiceRegistration getServiceRegistration(String requestPath);

    /**
     * Given a HttpRequest and an output stream, return a HttpServletResponseImpl.
     * 
     * @param request HttpRequest
     * @param output output stream associated with socket connection.
     * @return A HttpServletResponseImpl instance
     */
    HttpServletResponseImpl getServletResponse(OutputStream output);

    /**
     * For a request, response, and requestPath, return a ServiceRegistrationHandler.
     * 
     * @param request HttpRequest
     * @param response HttpResponse
     * @param requestPath The request URI
     * @return A ServiceRegistrationHandler corresponding to the requestPath, or null if no match.
     */
    ServiceRegistrationHandler getProcessor(HttpServletRequestImpl request,
        HttpServletResponseImpl response, String requestPath);
}
