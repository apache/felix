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

package org.apache.felix.example.jaas.app.internal;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.servlet.http.HttpServletRequest;

public class ServletRequestCallbackHandler implements CallbackHandler
{

    private final HttpServletRequest request;

    public ServletRequestCallbackHandler(HttpServletRequest request)
    {
        this.request = request;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException,
        UnsupportedCallbackException
    {
        for (Callback c : callbacks)
        {
            if (c instanceof NameCallback)
            {
                ((NameCallback) c).setName(getParam("j_username"));
            }
            if (c instanceof PasswordCallback)
            {
                ((PasswordCallback) c).setPassword(getParam("j_password").toCharArray());
            }
        }
    }

    private String getParam(String name)
    {
        String value = request.getParameter(name);
        if (value == null)
        {
            throw new IllegalArgumentException("No parameter with name [" + name
                + "] found");
        }
        return value.trim();
    }
}
