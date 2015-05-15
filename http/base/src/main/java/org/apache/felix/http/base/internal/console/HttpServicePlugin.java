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
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;

/**
 * This is a web console plugin.
 */
@SuppressWarnings("serial")
public class HttpServicePlugin extends HttpServlet
{

    private final HttpServiceRuntime runtime;
    private final BundleContext context;

    private volatile ServiceRegistration<Servlet> serviceReg;

    public HttpServicePlugin(final BundleContext context, final HttpServiceRuntime runtime)
    {
        this.runtime = runtime;
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
        this.serviceReg = context.registerService(Servlet.class, this, props);
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException
    {
        getHtml(resp);
    }

    private void getHtml(final HttpServletResponse resp) throws IOException
    {
        final RuntimeDTO dto = this.runtime.getRuntimeDTO();

        final PrintWriter pw = resp.getWriter();

        printRuntimeDetails(pw, dto.serviceDTO);
        for(final ServletContextDTO ctxDto : dto.servletContextDTOs )
        {
            printContextDetails(pw, ctxDto);
        }
    }

    private String getValueAsString(final Object value)
    {
        if ( value.getClass().isArray() )
        {
            if (value instanceof long[])
            {
                return Arrays.toString((long[])value);
            }
            else if (value instanceof int[])
            {
                return Arrays.toString((int[])value);
            }
            else if (value instanceof double[])
            {
                return Arrays.toString((double[])value);
            }
            else if (value instanceof byte[])
            {
                return Arrays.toString((byte[])value);
            }
            else if (value instanceof float[])
            {
                return Arrays.toString((float[])value);
            }
            else if (value instanceof short[])
            {
                return Arrays.toString((short[])value);
            }
            else if (value instanceof boolean[])
            {
                return Arrays.toString((boolean[])value);
            }
            else if (value instanceof char[])
            {
                return Arrays.toString((char[])value);
            }
            else
            {
                return Arrays.toString((Object[])value);
            }
        }
        return value.toString();
    }

    private void printRuntimeDetails(final PrintWriter pw, final ServiceReferenceDTO dto)
    {
        pw.println("<p class=\"statline ui-state-highlight\">${Runtime Properties}</p>");
        pw.println("<table class=\"nicetable\">");
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Name}</th>");
        pw.println("<th class=\"header\">${Value}</th>");
        pw.println("</tr></thead>");
        boolean odd = true;
        for(final Map.Entry<String, Object> prop : dto.properties.entrySet())
        {
            odd = printRow(pw, odd, prop.getKey(), getValueAsString(prop.getValue()));
        }
        pw.println("</table>");
    }

    private boolean printRow(final PrintWriter pw, final boolean odd, final String...columns)
    {
        pw.print("<tr class=\"");
        if ( odd ) pw.print("odd"); else pw.print("even");
        pw.println(" ui-state-default\">");

        for(final String val : columns)
        {
            pw.print("<td>");
            pw.print(val);
            pw.println("</td>");
        }

        pw.println("</tr>");
        return !odd;
    }

    private void printContextDetails(final PrintWriter pw, final ServletContextDTO dto)
    {
        pw.print("<p class=\"statline ui-state-highlight\">${Servlet Context} '");
        pw.print(dto.name);
        pw.println("'</p>");

        pw.println("<table class=\"nicetable\">");

        boolean odd = true;
        odd = printRow(pw, odd, "${Path}", dto.contextPath);
        odd = printRow(pw, odd, "${service.id}", String.valueOf(dto.serviceId));
        pw.println("</table>");

        printServletDetails(pw, dto);
        printFilterDetails(pw, dto);
    }

    private void printFilterDetails(final PrintWriter pw, final ServletContextDTO dto)
    {
        pw.print("<p class=\"statline ui-state-highlight\">${Servlet Context} '");
        pw.print(dto.name);
        pw.println("' ${Registered Filter Services}</p>");

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

    private void printServletDetails(final PrintWriter pw, final ServletContextDTO dto)
    {
        pw.print("<p class=\"statline ui-state-highlight\">${Servlet Context} '");
        pw.print(dto.name);
        pw.println("' ${Registered Servlet Services}</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Path}</th>");
        pw.println("<th class=\"header\">${Name}</th>");
        pw.println("<th class=\"header\">${Info}</th>");
        pw.println("</tr></thead>");

        boolean odd = true;
        for (ServletDTO servlet : dto.servletDTOs)
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("${service.id} : ").append(String.valueOf(servlet.serviceId)).append("\n");

            odd = printRow(pw, odd, getValueAsString(servlet.patterns), servlet.name, sb.toString());
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
            this.serviceReg = null;
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
