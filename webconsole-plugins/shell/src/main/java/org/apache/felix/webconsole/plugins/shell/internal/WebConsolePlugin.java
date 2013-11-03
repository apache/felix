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
package org.apache.felix.webconsole.plugins.shell.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.shell.ShellService;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * ShellServlet provides a Web bases interface to the Apache shell service, allowing
 * the user to execute shell commands from the browser.
 */
class WebConsolePlugin extends SimpleWebConsolePlugin
{

    private static final String LABEL = "shell"; //$NON-NLS-1$
    private static final String TITLE = "%shell.pluginTitle"; //$NON-NLS-1$
    private static final String CATEGORY = "Web Console";
    private static final String CSS[] = { "/" + LABEL + "/res/plugin.css" }; //$NON-NLS-1$ //$NON-NLS-2$

    private final ServiceTracker tracker;

    // templates
    private final String TEMPLATE;

    WebConsolePlugin(ServiceTracker tracker)
    {
        super(LABEL, TITLE, CSS);

        // load templates
        TEMPLATE = readTemplateFile("/res/plugin.html"); //$NON-NLS-1$
        this.tracker = tracker;
    }


    public String getCategory()
    {
        return CATEGORY;
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        response.setCharacterEncoding("utf-8"); //$NON-NLS-1$
        response.setContentType("text/html"); //$NON-NLS-1$

        PrintWriter pw = response.getWriter();

        try
        {
            String command = request.getParameter("command"); //$NON-NLS-1$
            if (command != null)
            {
                command = WebConsoleUtil.urlDecode(command);
            }

            pw.print("<span class=\"consolecommand\">-&gt; "); //$NON-NLS-1$
            pw.print(command == null ? "" : WebConsoleUtil.escapeHtml(command)); //$NON-NLS-1$
            pw.println("</span><br />"); //$NON-NLS-1$

            if (command != null && command.length() > 0)
            {
                ShellService shellService = getShellService();
                if (shellService != null)
                {
                    ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
                    ByteArrayOutputStream baosErr = new ByteArrayOutputStream();

                    shellService.executeCommand(command, new PrintStream(baosOut, true),
                        new PrintStream(baosErr, true));
                    if (baosOut.size() > 0)
                    {
                        pw.print(WebConsoleUtil.escapeHtml(new String(
                            baosOut.toByteArray())));
                    }
                    if (baosErr.size() > 0)
                    {
                        pw.print("<span class=\"error\">"); //$NON-NLS-1$
                        pw.print(WebConsoleUtil.escapeHtml(new String(
                            baosErr.toByteArray())));
                        pw.println("</span>"); //$NON-NLS-1$
                    }
                }
                else
                {
                    pw.print("<span class=\"error\">"); //$NON-NLS-1$
                    pw.print("Error: No shell service available<br />");
                    pw.println("</span>"); //$NON-NLS-1$
                }
            }
        }
        catch (Throwable t)
        {
            pw.print("<span class=\"error\">"); //$NON-NLS-1$
            StringWriter out = new StringWriter();
            t.printStackTrace(new PrintWriter(out, true));
            pw.print(WebConsoleUtil.escapeHtml(out.toString()));
            pw.println("</span>"); //$NON-NLS-1$
        }
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        DefaultVariableResolver vr = (DefaultVariableResolver) WebConsoleUtil.getVariableResolver(request);
        if (getShellService() == null)
        {
            vr.put("shell.status", "Shell Service not available"); //$NON-NLS-1$
            vr.put("shell.disabled", "true"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else
        {
            vr.put("shell.disabled", "false"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        response.getWriter().print(TEMPLATE);
    }

    private final ShellService getShellService()
    {
        return (ShellService) tracker.getService();
    }

}
