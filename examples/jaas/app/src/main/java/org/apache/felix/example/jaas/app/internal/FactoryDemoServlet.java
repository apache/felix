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
import java.io.PrintWriter;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.jaas.LoginContextFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

@Component
@Service(value = Servlet.class)
@Property(name = "alias",value = "/jaas/factory")
public class FactoryDemoServlet extends HttpServlet
{
    @Reference
    private LoginContextFactory loginContextFactory;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {

        //Demonstrates the JAAS authentication if the
        // 1. Felix JAAS LoginContextFactory is used.
        //In that case client code does not have to
        //manage Thread Context Classloader or provide explicit configuration

        PrintWriter pw = resp.getWriter();
        CallbackHandler handler = new ServletRequestCallbackHandler(req);
        Subject subject = new Subject();
        try
        {
            LoginContext lc = loginContextFactory.createLoginContext("sample",subject,handler);
            lc.login();

            pw.println("Principal authentication successful");
            pw.println(subject);
        }
        catch (LoginException e)
        {
            handleAuthenticationFailure(e,pw);
        }

    }

    private void handleAuthenticationFailure(LoginException e, PrintWriter pw)
    {
        pw.println("Authentication Failed");
        pw.println(e);
    }

}
