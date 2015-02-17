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

package org.apache.felix.http.base.internal.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

@SuppressWarnings("serial")
public class HttpServicePlugin extends HttpServlet
{

    private final HandlerRegistry registry;
    private final BundleContext context;

    private ServiceRegistration serviceReg;

    public HttpServicePlugin(BundleContext context, HandlerRegistry registry)
    {
        this.registry = registry;
        this.context = context;
    }

    public void register()
    {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        props.put(Constants.SERVICE_DESCRIPTION, "HTTP Service Web Console Plugin");
        props.put("felix.webconsole.label", "httpservice");
        props.put("felix.webconsole.title", "HTTP Service");
        props.put("felix.webconsole.configprinter.modes", "always");
        this.serviceReg = context.registerService(Servlet.class.getName(), this, props);
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException
    {
        getHtml(resp);
    }

    private void getHtml(HttpServletResponse resp) throws IOException
    {
        final PrintWriter pw = resp.getWriter();

        printServletDetails(pw);
        printFilterDetails(pw);

    }

    private void printFilterDetails(PrintWriter pw)
    {
        pw.println("<p class=\"statline ui-state-highlight\">${Registered Filter Services}</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Pattern}</th>");
        pw.println("<th class=\"header\">${Filter(Ranking)}</th>");
        pw.println("<th class=\"header\">${Bundle}</th>");
        pw.println("</tr></thead>");

        FilterHandler[] filters = new FilterHandler[0]; // XXX was: registry.getFilters();
        Arrays.sort(filters);
        String rowClass = "odd";
        for (FilterHandler filter : filters)
        {
            pw.println("<tr class=\"" + rowClass + " ui-state-default\">");
//            pw.println("<td>" + Arrays.toString(filter.getPatternStrings()) + "</td>"); // XXX
//            pw.println("<td>" + filter.getFilter().getClass().getName() + "(" + filter.getRanking() + ")" + "</td>");

            printBundleDetails(pw, filter.getFilter().getClass());

            if (rowClass.equals("odd"))
            {
                rowClass = "even";
            }
            else
            {
                rowClass = "odd";
            }
        }
        pw.println("</table>");
    }

    private void printServletDetails(PrintWriter pw)
    {
        pw.println("<p class=\"statline ui-state-highlight\">${Registered Servlet Services}</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Alias}</th>");
        pw.println("<th class=\"header\">${Servlet}</th>");
        pw.println("<th class=\"header\">${Bundle}</th>");
        pw.println("</tr></thead>");

        ServletHandler[] servlets = new ServletHandler[0]; // XXX was: registry.getServlets();
        String rowClass = "odd";
        for (ServletHandler servlet : servlets)
        {

            pw.println("<tr class=\"" + rowClass + " ui-state-default\">");
//            pw.println("<td>" + Arrays.toString(servlet.getPatternStrings()) + "</td>"); // XXX
//            pw.println("<td>" + servlet.getServlet().getClass().getName() + "</td>");

            printBundleDetails(pw, servlet.getServlet().getClass());

            pw.println("</tr>");
            if (rowClass.equals("odd"))
            {
                rowClass = "even";
            }
            else
            {
                rowClass = "odd";
            }
        }
        pw.println("</table>");
    }

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration(final PrintWriter pw)
    {
        pw.println("HTTP Service Details:");
        pw.println();
        pw.println("Registered Servlet Services");
        ServletHandler[] servlets = new ServletHandler[0]; // XXX was: registry.getServlets();
        for (ServletHandler servlet : servlets)
        {
//            pw.println("Patterns : " + Arrays.toString(servlet.getPatternStrings())); // XXX
            addSpace(pw, 1);
            pw.println("Class    : " + servlet.getServlet().getClass().getName());
            addSpace(pw, 1);
            pw.println("Bundle   : " + getBundleDetails(servlet.getServlet().getClass()));
        }

        pw.println();

        pw.println("Registered Filter Services");
        FilterHandler[] filters = new FilterHandler[0]; // XXX was: registry.getFilters();
        Arrays.sort(filters);
        for (FilterHandler filter : filters)
        {
//            pw.println("Patterns : " + Arrays.toString(filter.getPatternStrings())); // XXX
            addSpace(pw, 1);
//            pw.println("Ranking  : " + filter.getRanking()); // XXX
            addSpace(pw, 1);
            pw.println("Class    : " + filter.getFilter().getClass().getName());
            addSpace(pw, 1);
            pw.println("Bundle   : " + getBundleDetails(filter.getFilter().getClass()));
        }
    }

    public void unregister()
    {
        if (this.serviceReg != null)
        {
            this.serviceReg.unregister();
        }
    }

    private void printBundleDetails(PrintWriter pw, Class<?> c)
    {
        Bundle b = getBundle(c);
        pw.println("<td>");
        if (b == null)
        {
            pw.print("UNKNOWN");
        }
        else
        {
            String details = b.getSymbolicName();
            pw.print("<a href=\"${appRoot}/bundles/" + b.getBundleId() + "\">" + details + "</a>");
        }
        pw.println("</td>");
    }

    private String getBundleDetails(Class<?> c)
    {
        Bundle b = getBundle(c);
        return (b == null) ? "UNKNOWN" : b.getSymbolicName();
    }

    private static void addSpace(PrintWriter pw, int count)
    {
        for (int i = 0; i < count; i++)
        {
            pw.print("  ");
        }
    }

    private Bundle getBundle(Class<?> clazz)
    {
        return FrameworkUtil.getBundle(clazz);
    }
}
