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

package org.apache.felix.jaas.boot;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;


public class ProxyLoginModule implements LoginModule
{
    public static final String PROP_LOGIN_MODULE_FACTORY = "org.apache.felix.jaas.LoginModuleFactory";

    private LoginModule delegate;

    public void initialize(Subject subject, CallbackHandler callbackHandler,
        Map<String, ?> sharedState, Map<String, ?> options)
    {
        BootLoginModuleFactory factory = (BootLoginModuleFactory) options.get(PROP_LOGIN_MODULE_FACTORY);
        if (factory == null)
        {
            throw new IllegalStateException("Specify LoginModuleFactory through ["
                + PROP_LOGIN_MODULE_FACTORY
                + "] property as part of configuration options");
        }
        delegate = factory.createLoginModule();
        delegate.initialize(subject, callbackHandler, sharedState, options);
    }

    public boolean login() throws LoginException
    {
        return delegate.login();
    }

    public boolean commit() throws LoginException
    {
        return delegate.commit();
    }

    public boolean abort() throws LoginException
    {
        return delegate.abort();
    }

    public boolean logout() throws LoginException
    {
        return delegate.logout();
    }

    /**
     * Factory interface to create LoginModule instance. This is exactly same as
     * LoginModuleFactory. This is done to keep the boot package self sufficient
     */
    public static interface BootLoginModuleFactory
    {
        LoginModule createLoginModule();
    }
}
