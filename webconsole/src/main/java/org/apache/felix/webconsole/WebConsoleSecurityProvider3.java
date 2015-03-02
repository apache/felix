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
package org.apache.felix.webconsole;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpContext;

/**
 * The <code>WebConsoleSecurityProvider3</code> extends the
 * {@link WebConsoleSecurityProvider2} interface and adds the ability to perform log-out operation.
 * <p>
 * If a registered {@link WebConsoleSecurityProvider} service implements this
 * interface the {@link #logout(HttpServletRequest, HttpServletResponse)}
 * method is called when the user clicks the logout button.
 * 
 * If this service is missing and basic authentication is used, then new authentication is requested.
 * 
 * In any case, the logout procedure will invalidate the current session and will remove the 
 * {@link HttpContext#REMOTE_USER}, {@link HttpContext#AUTHORIZATION} attributes from the request and the session.
 * 
 * @since 4.2.8; Web Console Bundle 4.2.8
 */
public interface WebConsoleSecurityProvider3 extends WebConsoleSecurityProvider2
{

    /**
     * This method will be called by the web console when the user clicks the logout button. The security provider
     * shouldn't invalidate the session, it will be invalidated after this method exits.
     * 
     * However the security provider must delete any cookies or objects, that matters during the authorization process.
     * 
     * @param request the request
     * @param response the response
     */
    void logout(HttpServletRequest request, HttpServletResponse response);
}

