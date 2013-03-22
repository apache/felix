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

package org.apache.felix.example.jaas.config.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class SampleConfigLoginModule implements LoginModule
{
    private Subject subject;
    private CallbackHandler handler;
    private Map<String, ?> options;
    private Map<String, ?> sharedState;
    private boolean succeeded;
    private String name;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
        Map<String, ?> sharedState, Map<String, ?> options)
    {
        this.subject = subject;
        this.options = options;
        this.handler = callbackHandler;
        this.sharedState = sharedState;
    }

    @Override
    public boolean login() throws LoginException
    {
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("Name");
        callbacks[1] = new PasswordCallback("Password", false);

        try
        {
            handler.handle(callbacks);
        }
        catch (IOException e)
        {
            throw new LoginException(e.getMessage());
        }
        catch (UnsupportedCallbackException e)
        {
            throw new LoginException(e.getMessage());
        }

        String name = ((NameCallback) callbacks[0]).getName();
        char[] password = ((PasswordCallback) callbacks[1]).getPassword();

        boolean result = Arrays.equals(name.toCharArray(), password);
        succeeded = result;
        this.name = name;
        return result;
    }

    @Override
    public boolean commit() throws LoginException
    {
        if (succeeded)
        {
            subject.getPrincipals().add(new SamplePrincipal(name, "SampleConfigLoginModule"));
            return true;
        }
        return false;
    }

    @Override
    public boolean abort() throws LoginException
    {
       return true;
    }

    @Override
    public boolean logout() throws LoginException
    {
        return false;
    }
}
