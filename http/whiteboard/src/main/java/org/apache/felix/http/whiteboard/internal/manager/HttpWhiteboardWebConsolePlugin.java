/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.whiteboard.internal.manager;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.whiteboard.internal.manager.HttpContextManager.HttpContextHolder;

@SuppressWarnings("serial")
public class HttpWhiteboardWebConsolePlugin extends HttpServlet
{

    private final ExtenderManager extMgr;

    public String getLabel()
    {
        return "httpwhiteboard";
    }

    public String getTitle()
    {
        return "Http Whiteboard";
    }

    public HttpWhiteboardWebConsolePlugin(final ExtenderManager extMgr)
    {
        this.extMgr = extMgr;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // only handle GET requests, ensure no error message for other requests
        if ("GET".equals(req.getMethod()) || "HEAD".equals(req.getMethod()))
        {
            super.service(req, resp);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {

        PrintWriter pw = resp.getWriter();

        pw.println("<table class='content' width='100%' cellspacing='0' cellpadding='0'>");

        printHttpContextServices(pw);

        pw.println("<tr><td colspan='2'>&nbsp;</td></tr>");

        final Map<Object, AbstractMapping> mappings = extMgr.getMappings();
        printServletMappings(pw, mappings);

        pw.println("<tr><td colspan='2'>&nbsp;</td></tr>");

        printFilterMappings(pw, mappings);

        pw.println("</table>");
    }

    private void printHttpContextServices(PrintWriter pw)
    {
        pw.println("<tr>");
        pw.println("<th class='content container' colspan='4'>Registered HttpContext Services</td>");
        pw.println("</tr>");
        pw.println("<tr>");
        pw.println("<th class='content'>Context ID</td>");
        pw.println("<th class='content' colspan='3'>HttpContext</td>");
        pw.println("</tr>");

        final Map<String, HttpContextHolder> contexts = extMgr.getHttpContexts();
        for (Map.Entry<String, HttpContextHolder> handler : contexts.entrySet())
        {
            pw.println("<tr class='content'>");
            pw.println("<td class='content'>" + handler.getKey() + "</td>");
            pw.println("<td class='content' colspan='3'>" + handler.getValue().getContext() + "</td>");
            pw.println("</tr>");
        }
    }

    private void printServletMappings(PrintWriter pw, Map<Object, AbstractMapping> mappings)
    {
        pw.println("<tr>");
        pw.println("<th class='content container' colspan='4'>Registered Servlet Services</td>");
        pw.println("</tr>");
        pw.println("<tr>");
        pw.println("<th class='content'>Alias</td>");
        pw.println("<th class='content'>Servlet</td>");
        pw.println("<th class='content'>Init Parameter</td>");
        pw.println("<th class='content'>HttpContext</td>");
        pw.println("</tr>");

        for (Map.Entry<Object, AbstractMapping> handler : mappings.entrySet())
        {
            if (handler.getValue() instanceof ServletMapping)
            {
                ServletMapping sm = (ServletMapping) handler.getValue();
                pw.println("<tr class='content'>");
                pw.println("<td class='content'>" + sm.getAlias() + "</td>");
                pw.println("<td class='content'>" + sm.getServlet() + "</td>");
                pw.println("<td class='content'>" + sm.getInitParams() + "</td>");
                pw.println("<td class='content'>" + sm.getContext() + "</td>");
                pw.println("</tr>");
            }
        }
    }

    private void printFilterMappings(PrintWriter pw, Map<Object, AbstractMapping> mappings)
    {
        pw.println("<tr>");
        pw.println("<th class='content container' colspan='4'>Registered Filter Services</td>");
        pw.println("</tr>");
        pw.println("<tr>");
        pw.println("<th class='content'>Pattern</td>");
        pw.println("<th class='content'>Filter (Ranking)</td>");
        pw.println("<th class='content'>Init Parameter</td>");
        pw.println("<th class='content'>HttpContext</td>");
        pw.println("</tr>");

        for (Map.Entry<Object, AbstractMapping> handler : mappings.entrySet())
        {
            if (handler.getValue() instanceof FilterMapping)
            {
                FilterMapping fm = (FilterMapping) handler.getValue();
                pw.println("<tr class='content'>");
                pw.println("<td class='content'>" + fm.getPattern() + "</td>");
                pw.println("<td class='content'>" + fm.getFilter() + " (" + fm.getRanking() + ")</td>");
                pw.println("<td class='content'>" + fm.getInitParams() + "</td>");
                pw.println("<td class='content'>" + fm.getContext() + "</td>");
                pw.println("</tr>");
            }
        }
    }

    public void printConfiguration(final PrintWriter pw)
    {
        printHttpContextServicesTxt(pw);
        final Map<Object, AbstractMapping> mappings = extMgr.getMappings();
        printServletMappingsTxt(pw, mappings);
        printFilterMappingsTxt(pw, mappings);
    }

    private void printHttpContextServicesTxt(PrintWriter pw)
    {
        pw.println("Registered HttpContext Services");
        final Map<String, HttpContextHolder> contexts = extMgr.getHttpContexts();
        for (Map.Entry<String, HttpContextHolder> handler : contexts.entrySet())
        {
            pw.println("  " + handler.getKey() + " ==> " + handler.getValue().getContext() + "</td>");
        }
        pw.println();
    }

    private void printServletMappingsTxt(PrintWriter pw, Map<Object, AbstractMapping> mappings)
    {
        pw.println("Registered Servlet Services");
        for (Map.Entry<Object, AbstractMapping> handler : mappings.entrySet())
        {
            if (handler.getValue() instanceof ServletMapping)
            {
                ServletMapping sm = (ServletMapping) handler.getValue();
                pw.printf("  %s ==> %s (%s, %s, %s)%n", sm.getAlias(), sm.getServlet(),
                    sm.isRegistered() ? "registered" : "unregistered", sm.getInitParams(), sm.getContext());
            }
        }
        pw.println();
    }

    private void printFilterMappingsTxt(PrintWriter pw, Map<Object, AbstractMapping> mappings)
    {
        pw.println("Registered Filter Services");
        for (Map.Entry<Object, AbstractMapping> handler : mappings.entrySet())
        {
            if (handler.getValue() instanceof FilterMapping)
            {
                FilterMapping fm = (FilterMapping) handler.getValue();
                pw.printf("  %s ==> %s (%s, %s, %s, %s)%n", fm.getPattern(), fm.getFilter(),
                    fm.isRegistered() ? "registered" : "unregistered", fm.getRanking(), fm.getInitParams(),
                    fm.getContext());
            }
        }
        pw.println();
    }
}
