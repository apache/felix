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
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
        return "Felix Http Whiteboard";
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

        printServletMappings(pw, this.getServlets());

        pw.println("<tr><td colspan='2'>&nbsp;</td></tr>");

        printFilterMappings(pw, this.getFilters());

        pw.println("<tr><td colspan='2'>&nbsp;</td></tr>");

        printOrphanMappings(pw, this.getOrphanMappings());

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

        final Map<String, HttpContextHolder> contexts = getHttpContexts();
        for (Map.Entry<String, HttpContextHolder> handler : contexts.entrySet())
        {
            pw.println("<tr class='content'>");
            pw.println("<td class='content'>" + handler.getKey() + "</td>");
            pw.println("<td class='content' colspan='3'>" + handler.getValue().getContext() + "</td>");
            pw.println("</tr>");
        }
    }

    private void printServletMappings(PrintWriter pw, Map<String, ServletMapping> mappings)
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

        for (ServletMapping sm : mappings.values())
        {
            pw.println("<tr class='content'>");
            pw.println("<td class='content'>" + sm.getAlias() + "</td>");
            pw.println("<td class='content'>" + sm.getServlet() + "</td>");
            pw.println("<td class='content'>" + sm.getInitParams() + "</td>");
            pw.println("<td class='content'>" + sm.getContext() + "</td>");
            pw.println("</tr>");
        }
    }

    private void printFilterMappings(PrintWriter pw, Set<FilterMapping> mappings)
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

        for (FilterMapping fm : mappings)
        {
            pw.println("<tr class='content'>");
            pw.println("<td class='content'>" + fm.getPattern() + "</td>");
            pw.println("<td class='content'>" + fm.getFilter() + " (" + fm.getRanking() + ")</td>");
            pw.println("<td class='content'>" + fm.getInitParams() + "</td>");
            pw.println("<td class='content'>" + fm.getContext() + "</td>");
            pw.println("</tr>");
        }
    }

    private void printOrphanMappings(PrintWriter pw, Map<String, Set<AbstractMapping>> mappings)
    {
        pw.println("<tr>");
        pw.println("<th class='content container' colspan='4'>Orphan Servlets and Filters</td>");
        pw.println("</tr>");
        if (mappings.isEmpty())
        {
            pw.println("<tr>");
            pw.println("<td class='content' colspan='4'><i>none</i></td>");
            pw.println("</tr>");
            pw.println("");
        }
        else
        {
            pw.println("<tr>");
            pw.println("<th class='content'>Context ID</td>");
            pw.println("<th class='content' colspan='3'>Servlets and Filters</td>");
            pw.println("</tr>");

            for (Entry<String, Set<AbstractMapping>> entry : mappings.entrySet())
            {
                pw.println("<tr class='content'>");
                pw.println("<td class='content'>" + entry.getKey() + "</td>");
                pw.println("<td class='content' colspan='3'>");
                for (AbstractMapping mapping : entry.getValue())
                {
                    if (mapping instanceof ServletMapping)
                    {
                        pw.printf("Servlet %s (%s)", ((ServletMapping) mapping).getAlias(),
                            ((ServletMapping) mapping).getServlet());
                    }
                    else if (mapping instanceof FilterMapping)
                    {
                        pw.printf("Filter %s (%s)", ((FilterMapping) mapping).getPattern(),
                            ((FilterMapping) mapping).getFilter());
                    }
                    pw.println("<br/>");
                }
                pw.println("</td>");
                pw.println("</tr>");
            }
        }
    }

    public void printConfiguration(final PrintWriter pw)
    {
        printHttpContextServicesTxt(pw);
        printServletMappingsTxt(pw, getServlets());
        printFilterMappingsTxt(pw, getFilters());
        printOrphanMappingsTxt(pw, getOrphanMappings());
    }

    private void printHttpContextServicesTxt(PrintWriter pw)
    {
        pw.println("Registered HttpContext Services");
        final Map<String, HttpContextHolder> contexts = getHttpContexts();
        for (Map.Entry<String, HttpContextHolder> handler : contexts.entrySet())
        {
            pw.println("  " + handler.getKey() + " ==> " + handler.getValue().getContext());
        }
        pw.println();
    }

    private void printServletMappingsTxt(PrintWriter pw, Map<String, ServletMapping> mappings)
    {
        pw.println("Registered Servlet Services");
        for (ServletMapping sm : mappings.values())
        {
            pw.printf("  %s ==> %s (%s, %s, %s)%n", sm.getAlias(), sm.getServlet(), sm.isRegistered() ? "registered"
                : "unregistered", sm.getInitParams(), sm.getContext());
        }
        pw.println();
    }

    private void printFilterMappingsTxt(PrintWriter pw, Set<FilterMapping> mappings)
    {
        pw.println("Registered Filter Services");
        for (FilterMapping fm : mappings)
        {
            pw.printf("  %s ==> %s (%s, %s, %s, %s)%n", fm.getPattern(), fm.getFilter(),
                fm.isRegistered() ? "registered" : "unregistered", fm.getRanking(), fm.getInitParams(), fm.getContext());
        }
        pw.println();
    }

    private void printOrphanMappingsTxt(PrintWriter pw, Map<String, Set<AbstractMapping>> mappings)
    {
        pw.println("Orphan Servlets and Filters");
        if (mappings.isEmpty())
        {
            pw.println("  none");
        }
        else
        {

            for (Entry<String, Set<AbstractMapping>> entry : mappings.entrySet())
            {
                pw.printf("  %s ==> { ", entry.getKey());
                boolean cont = false;
                for (AbstractMapping mapping : entry.getValue())
                {
                    if (cont)
                    {
                        pw.print(", ");
                    }
                    else
                    {
                        cont = true;
                    }

                    if (mapping instanceof ServletMapping)
                    {
                        pw.printf("Servlet %s (%s)", ((ServletMapping) mapping).getAlias(),
                            ((ServletMapping) mapping).getServlet());
                    }
                    else if (mapping instanceof FilterMapping)
                    {
                        pw.printf("Filter %s (%s)", ((FilterMapping) mapping).getPattern(),
                            ((FilterMapping) mapping).getFilter());
                    }
                }
                pw.println(" }");
            }
        }
    }

    private Map<String, HttpContextHolder> getHttpContexts()
    {
        return new TreeMap<String, HttpContextManager.HttpContextHolder>(this.extMgr.getHttpContexts());
    }

    private Map<String, ServletMapping> getServlets()
    {
        Map<String, ServletMapping> mappings = new TreeMap<String, ServletMapping>();
        for (AbstractMapping mapping : this.extMgr.getMappings().values())
        {
            if (mapping instanceof ServletMapping)
            {
                mappings.put(((ServletMapping) mapping).getAlias(), (ServletMapping) mapping);
            }
        }
        return mappings;
    }

    private Set<FilterMapping> getFilters()
    {
        Set<FilterMapping> mappings = new TreeSet<FilterMapping>(new Comparator<FilterMapping>()
        {
            @Override
            public int compare(FilterMapping o1, FilterMapping o2)
            {
                if (o1 == o2)
                {
                    return 0;
                }
                int res = o1.getPattern().compareTo(o2.getPattern());
                if (res == 0)
                {
                    if (o1.getRanking() > o2.getRanking())
                    {
                        res = -1;
                    }
                    else if (o1.getRanking() < o2.getRanking())
                    {
                        res = 1;
                    }
                    else
                    {
                        res = o1.getFilter().toString().compareTo(o2.getFilter().toString());
                    }
                }
                return res;
            }
        });
        for (AbstractMapping mapping : this.extMgr.getMappings().values())
        {
            if (mapping instanceof FilterMapping)
            {
                mappings.add((FilterMapping) mapping);
            }
        }
        return mappings;
    }

    private Map<String, Set<AbstractMapping>> getOrphanMappings()
    {
        return new TreeMap<String, Set<AbstractMapping>>(this.extMgr.getOrphanMappings());
    }
}
