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


/**
 * The <code>WebConsoleSecurityProvider</code> is a service interface allowing
 * to use an external system to authenticate users before granting access to the
 * Web Console.
 *
 * @since 3.1.0; Web Console Bundle 3.1.0
 */
public interface WebConsoleSecurityProvider
{

    /**
     * Authenticates the user with the given user name and password.
     *
     * @param username The name of the user presented by the client
     * @param password The password presented by the client
     * @return Some object representing the authenticated user indicating general
     *         access to be granted to the web console. If the user cannot be
     *         authenticated (e.g. unknown user name or wrong password) or the
     *         user must not be allowed access to the web console at all
     *         <code>null</code> must be returned from this method.
     */
    public Object authenticate( String username, String password );


    /**
     * Checks whether bthe authenticated user has the given role permission.
     *
     * @param user The object referring to the authenticated user. This is the
     *      object returned from the {@link #authenticate(String, String)}
     *      method and will never be <code>null</code>.
     * @param role The requested role
     * @return <code>true</code> if the user is given permission for the given
     *      role.
     */
    public boolean authorize( Object user, String role );

}
