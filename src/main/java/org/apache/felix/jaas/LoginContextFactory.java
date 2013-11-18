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

package org.apache.felix.jaas;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import aQute.bnd.annotation.ProviderType;


/**
 * A factory for creating {@link LoginContext} instances.
 *
 * <p>Instead of directly creating {@link LoginContext} this factory can be used. It takes care of
 * locating the {@link javax.security.auth.login.Configuration} instance and switching of Thread's context
 * classloader.
 */
@ProviderType
public interface LoginContextFactory
{
    /**
     * Instantiate a new <code>LoginContext</code> object with a name, a <code>Subject</code> to be authenticated,
     * and a <code>CallbackHandler</code> object.
     *
     * @param realm realm or application name
     * @param subject the <code>Subject</code> to authenticate.
     * @param handler  the <code>CallbackHandler</code> object used by
     *		LoginModules to communicate with the user.
     *
     * @return created LoginContext
     *
     * @exception LoginException if the caller-specified <code>name</code>  does not appear in
     *          the <code>Configuration</code>  and there is no <code>Configuration</code> entry
     *          for "<i>other</i>", or if the caller-specified <code>subject</code> is <code>null</code>,
     *		    or if the caller-specified <code>callbackHandler</code> is <code>null</code>.
     */
    LoginContext createLoginContext(String realm, Subject subject, CallbackHandler handler) throws LoginException;

}
