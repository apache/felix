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


/**
 * The <code>WebConsoleSecurityProvider2</code> extends the
 * {@link WebConsoleSecurityProvider} interface allowing for full control of
 * the authentication process to access the Web Console.
 * <p>
 * If a registered {@link WebConsoleSecurityProvider} service implements this
 * interface the {@link #authenticate(HttpServletRequest, HttpServletResponse)}
 * method is called instead of the
 * {@link WebConsoleSecurityProvider#authenticate(String, String)} method.
 *
 * @since 3.1.2; Web Console Bundle 3.1.4
 */
public interface WebConsoleSecurityProvider2 extends WebConsoleSecurityProvider
{

    /**
     * The name of the request attribute providing the object representing the
     * authenticated user. This object is used to call the
     * {@link WebConsoleSecurityProvider#authorize(Object, String)} to
     * authorize access for certain roles.
     */
    String USER_ATTRIBUTE = "org.apache.felix.webconsole.user"; //$NON-NLS-1$


    /**
     * Authenticates the given request or asks the client for credentials.
     * <p>
     * Implementations of this method are expected to respect and implement
     * the semantics of the <code>HttpContext.handleSecurity</code> method
     * as specified in the OSGi HTTP Service specification.
     * <p>
     * If this method returns <code>true</code> it is assumed the request
     * provided valid credentials identifying the user as accepted to access
     * the web console. In addition, the {@link #USER_ATTRIBUTE} request
     * attribute must be set to a non-<code>null</code> object reference
     * identifying the authenticated user.
     * <p>
     * If this method returns <code>false</code> the request to the web console
     * is terminated without any more response sent back to the client. That is
     * the implementation is expected to have informed the client in case of
     * non-granted access.
     *
     * @param request The request object
     * @param response The response object
     * @return <code>true</code> If the request provided valid credentials.
     */
    public boolean authenticate( HttpServletRequest request, HttpServletResponse response );

}
