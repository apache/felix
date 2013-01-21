/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Based on http://antony.lesuisse.org/software/ajaxterm/
 *  Public Domain License
 */

package org.apache.felix.webconsole.plugins.gogo.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.osgi.framework.BundleContext;

/**
 * The <code>GogoPlugin</code>
 */
public class GogoPlugin extends SimpleWebConsolePlugin {

    /** Pseudo class version ID to keep the IDE quite. */
    private static final long serialVersionUID = 1L;

    public static final String LABEL = "gogo";

    public static final String TITLE = "Gogo";

    public static final String CATEGORY = "Web Console";

    public static final int TERM_WIDTH = 120;

    public static final int TERM_HEIGHT = 39;

    private final SessionTerminalManager terminalManager;

    public GogoPlugin(final SessionTerminalManager terminalManager) {
        super(LABEL, TITLE, null);
        this.terminalManager = terminalManager;
    }

    @Override
    public void activate(BundleContext bundleContext) {
        super.activate(bundleContext);
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    public String getCategory()
    {
        return CATEGORY;
    }

    protected void renderContent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter pw = response.getWriter();

        String appRoot = request.getContextPath() + request.getServletPath();
        pw.println("<link href=\"" + appRoot + "/gogo/res/ui/gogo.css\" rel=\"stylesheet\" type=\"text/css\" />");
        pw.println("<script src=\"" + appRoot + "/gogo/res/ui/gogo.js\" type=\"text/javascript\"></script>");
        pw.println("<p id=\"statline\" class=\"statline\">&nbsp;</p>");
        pw.println("<div id='console'><div id='term'></div></div>");
        pw.println("<script type=\"text/javascript\"><!--");
        pw.println("window.onload = function() { gogo.Terminal(document.getElementById(\"term\"), " + TERM_WIDTH + ", "
            + TERM_HEIGHT + "); }");
        pw.println("--></script>");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String encoding = request.getHeader("Accept-Encoding");
        boolean supportsGzip = (encoding != null && encoding.toLowerCase().indexOf("gzip") > -1);
        SessionTerminal st = terminalManager.getSessionTerminal(request);
        if (st != null) {
            String str = request.getParameter("k");
            String f = request.getParameter("f");
            String dump = st.handle(str, f != null && f.length() > 0);
            if (dump != null) {
                if (supportsGzip) {
                    response.setHeader("Content-Encoding", "gzip");
                    response.setHeader("Content-Type", "text/html; charset=UTF-8");
                    try {
                        GZIPOutputStream gzos = new GZIPOutputStream(response.getOutputStream());
                        gzos.write(dump.getBytes("UTF-8"));
                        gzos.close();
                    } catch (IOException ie) {
                        // handle the error here
                        ie.printStackTrace();
                    }
                } else {
                    response.setContentType("text/html; charset=UTF-8");
                    response.getOutputStream().write(dump.getBytes("UTF-8"));
                }
            }
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        response.flushBuffer();
    }

}
