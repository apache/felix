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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
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

    /** Escape xml text */
    private static String escapeXml(final String input) {
        if (input == null) {
            return null;
        }

        final StringBuilder b = new StringBuilder(input.length());
        for(int i = 0;i  < input.length(); i++) {
            final char c = input.charAt(i);
            if(c == '&') {
                b.append("&amp;");
            } else if(c == '<') {
                b.append("&lt;");
            } else if(c == '>') {
                b.append("&gt;");
            } else if(c == '"') {
                b.append("&quot;");
            } else if(c == '\'') {
                b.append("&apos;");
            } else {
                b.append(c);
            }
        }
        return b.toString();
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
        pw.println("<br/>");
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
            String text = escapeXml(val).replace("\n", "<br/>");
            int pos;
            while ( (pos = text.indexOf("${#link:")) != -1) {
                final int endPos = text.indexOf("}", pos);
                final int bundleId = Integer.valueOf(text.substring(pos + 8, endPos));
                final int tokenEndPos = text.indexOf("${link#}", pos);

                text = text.substring(0, pos) + "<a href=\"${appRoot}/bundles/" + String.valueOf(bundleId) + "\">" +
                       text.substring(endPos + 1, tokenEndPos) + "</a>" + text.substring(tokenEndPos + 8);
            }
            pw.print(text);
            pw.println("</td>");
        }

        pw.println("</tr>");
        return !odd;
    }

    private String getContextPath(final String path)
    {
        if ( path.length() == 0 )
        {
            return "<root>";
        }
        return path;
    }

    private void printContextDetails(final PrintWriter pw, final ServletContextDTO dto)
    {
        pw.print("<p class=\"statline ui-state-highlight\">${Servlet Context} '");
        pw.print(escapeXml(dto.name));
        pw.println("'</p>");

        pw.println("<table class=\"nicetable\">");

        boolean odd = true;
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Name}</th>");
        pw.println("<th class=\"header\">${Value)}</th>");
        pw.println("</tr></thead>");
        odd = printRow(pw, odd, "${Path}", getContextPath(dto.contextPath));
        odd = printRow(pw, odd, "${service.id}", String.valueOf(dto.serviceId));
        pw.println("</table>");

        printServletDetails(pw, dto);
        printFilterDetails(pw, dto);
        printResourceDetails(pw, dto);
        printErrorPageDetails(pw, dto);
        printListenerDetails(pw, dto);
    }

    private void printFilterDetails(final PrintWriter pw, final ServletContextDTO dto)
    {
        if ( dto.filterDTOs.length == 0 )
        {
            return;
        }
        pw.print("<p class=\"statline ui-state-highlight\">${Servlet Context} '");
        pw.print(escapeXml(dto.name));
        pw.println("' ${Registered Filter Services}</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Pattern}</th>");
        pw.println("<th class=\"header\">${Filter}</th>");
        pw.println("<th class=\"header\">${Info}</th>");
        pw.println("</tr></thead>");

        boolean odd = true;
        for (final FilterDTO filter : dto.filterDTOs)
        {
            final StringBuilder sb = new StringBuilder();
            final ServiceReference<?> ref = this.getServiceReference(filter.serviceId);
            if ( ref != null )
            {
                int ranking = 0;
                final Object obj = ref.getProperty(Constants.SERVICE_RANKING);
                if ( obj instanceof Integer)
                {
                    ranking = (Integer)obj;
                }
                sb.append("${ranking} : ").append(String.valueOf(ranking)).append("\n");
            }
            sb.append("${async} : ").append(String.valueOf(filter.asyncSupported)).append("\n");
            sb.append("${dispatcher} : ").append(getValueAsString(filter.dispatcher)).append("\n");
            sb.append("${service.id} : ").append(String.valueOf(filter.serviceId)).append("\n");
            if ( ref != null )
            {
                sb.append("${bundle} : ");
                sb.append("${#link:");
                sb.append(ref.getBundle().getBundleId());
                sb.append("}");
                sb.append(ref.getBundle().getSymbolicName());
                sb.append("${link#}\n");
            }

            final List<String> patterns = new ArrayList<String>();
            patterns.addAll(Arrays.asList(filter.patterns));
            patterns.addAll(Arrays.asList(filter.regexs));
            for(final String name : filter.servletNames)
            {
                patterns.add("Servlet : " + name);
            }
            Collections.sort(patterns);
            final StringBuilder psb = new StringBuilder();
            for(final String p : patterns)
            {
                psb.append(p).append('\n');
            }
            odd = printRow(pw, odd, psb.toString(), filter.name, sb.toString());
        }
        pw.println("</table>");
    }

    private ServiceReference<?> getServiceReference(final long serviceId)
    {
        if ( serviceId > 0 )
        {
            try
            {
                final ServiceReference<?>[] ref = this.context.getServiceReferences((String)null, "(" + Constants.SERVICE_ID + "=" + String.valueOf(serviceId));
                if ( ref != null && ref.length > 0 )
                {
                    return ref[0];
                }
            }
            catch (final InvalidSyntaxException e)
            {
                // ignore
            }
        }
        return null;
    }

    private void printServletDetails(final PrintWriter pw, final ServletContextDTO dto)
    {
        if ( dto.servletDTOs.length == 0 )
        {
            return;
        }
        pw.print("<p class=\"statline ui-state-highlight\">${Servlet Context} '");
        pw.print(escapeXml(dto.name));
        pw.println("' ${Registered Servlet Services}</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Path}</th>");
        pw.println("<th class=\"header\">${Name}</th>");
        pw.println("<th class=\"header\">${Info}</th>");
        pw.println("</tr></thead>");

        boolean odd = true;
        for (final ServletDTO servlet : dto.servletDTOs)
        {
            final StringBuilder sb = new StringBuilder();
            final ServiceReference<?> ref = this.getServiceReference(servlet.serviceId);
            if ( ref != null )
            {
                int ranking = 0;
                final Object obj = ref.getProperty(Constants.SERVICE_RANKING);
                if ( obj instanceof Integer)
                {
                    ranking = (Integer)obj;
                }
                sb.append("${ranking} : ").append(String.valueOf(ranking)).append("\n");
            }
            sb.append("${async} : ").append(String.valueOf(servlet.asyncSupported)).append("\n");
            sb.append("${service.id} : ").append(String.valueOf(servlet.serviceId)).append("\n");
            if ( ref != null )
            {
                sb.append("${bundle} : ");
                sb.append("${#link:");
                sb.append(ref.getBundle().getBundleId());
                sb.append("}");
                sb.append(ref.getBundle().getSymbolicName());
                sb.append("${link#}\n");
            }

            final StringBuilder psb = new StringBuilder();
            for(final String p : servlet.patterns)
            {
                psb.append(p).append('\n');
            }
            odd = printRow(pw, odd, psb.toString(), servlet.name, sb.toString());
        }
        pw.println("</table>");
    }

    private void printResourceDetails(final PrintWriter pw, final ServletContextDTO dto)
    {
        if ( dto.resourceDTOs.length == 0 )
        {
            return;
        }
        pw.print("<p class=\"statline ui-state-highlight\">${Servlet Context} '");
        pw.print(escapeXml(dto.name));
        pw.println("' ${Registered Resource Services}</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Path}</th>");
        pw.println("<th class=\"header\">${Prefix}</th>");
        pw.println("<th class=\"header\">${Info}</th>");
        pw.println("</tr></thead>");

        boolean odd = true;
        for (final ResourceDTO rsrc : dto.resourceDTOs)
        {
            final StringBuilder sb = new StringBuilder();
            final ServiceReference<?> ref = this.getServiceReference(rsrc.serviceId);
            if ( ref != null )
            {
                int ranking = 0;
                final Object obj = ref.getProperty(Constants.SERVICE_RANKING);
                if ( obj instanceof Integer)
                {
                    ranking = (Integer)obj;
                }
                sb.append("${ranking} : ").append(String.valueOf(ranking)).append("\n");
            }
            sb.append("${service.id} : ").append(String.valueOf(rsrc.serviceId)).append("\n");
            if ( ref != null )
            {
                sb.append("${bundle} : ");
                sb.append("${#link:");
                sb.append(ref.getBundle().getBundleId());
                sb.append("}");
                sb.append(ref.getBundle().getSymbolicName());
                sb.append("${link#}\n");
            }

            final StringBuilder psb = new StringBuilder();
            for(final String p : rsrc.patterns)
            {
                psb.append(p).append('\n');
            }
            odd = printRow(pw, odd, psb.toString(), rsrc.prefix, sb.toString());
        }
        pw.println("</table>");
    }

    private void printErrorPageDetails(final PrintWriter pw, final ServletContextDTO dto)
    {
        if ( dto.errorPageDTOs.length == 0 )
        {
            return;
        }
        pw.print("<p class=\"statline ui-state-highlight\">${Servlet Context} '");
        pw.print(escapeXml(dto.name));
        pw.println("' ${Registered Resource Services}</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Path}</th>");
        pw.println("<th class=\"header\">${Name}</th>");
        pw.println("<th class=\"header\">${Info}</th>");
        pw.println("</tr></thead>");

        boolean odd = true;
        for (final ErrorPageDTO ep : dto.errorPageDTOs)
        {
            final StringBuilder sb = new StringBuilder();
            final ServiceReference<?> ref = this.getServiceReference(ep.serviceId);
            if ( ref != null )
            {
                int ranking = 0;
                final Object obj = ref.getProperty(Constants.SERVICE_RANKING);
                if ( obj instanceof Integer)
                {
                    ranking = (Integer)obj;
                }
                sb.append("${ranking} : ").append(String.valueOf(ranking)).append("\n");
            }
            sb.append("${async} : ").append(String.valueOf(ep.asyncSupported)).append("\n");
            sb.append("${service.id} : ").append(String.valueOf(ep.serviceId)).append("\n");
            if ( ref != null )
            {
                sb.append("${bundle} : ");
                sb.append("${#link:");
                sb.append(ref.getBundle().getBundleId());
                sb.append("}");
                sb.append(ref.getBundle().getSymbolicName());
                sb.append("${link#}\n");
            }

            final StringBuilder psb = new StringBuilder();
            for(final long p : ep.errorCodes)
            {
                psb.append(p).append('\n');
            }
            for(final String p : ep.exceptions)
            {
                psb.append(p).append('\n');
            }
            odd = printRow(pw, odd, psb.toString(), ep.name, sb.toString());
        }
        pw.println("</table>");
    }

    private void printListenerDetails(final PrintWriter pw, final ServletContextDTO dto)
    {
        if ( dto.listenerDTOs.length == 0 )
        {
            return;
        }
        pw.print("<p class=\"statline ui-state-highlight\">${Servlet Context} '");
        pw.print(escapeXml(dto.name));
        pw.println("' ${Registered Listeners}</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Type}</th>");
        pw.println("<th class=\"header\">${Info}</th>");
        pw.println("</tr></thead>");

        boolean odd = true;
        for (final ListenerDTO ep : dto.listenerDTOs)
        {
            final StringBuilder sb = new StringBuilder();
            final ServiceReference<?> ref = this.getServiceReference(ep.serviceId);
            if ( ref != null )
            {
                int ranking = 0;
                final Object obj = ref.getProperty(Constants.SERVICE_RANKING);
                if ( obj instanceof Integer)
                {
                    ranking = (Integer)obj;
                }
                sb.append("${ranking} : ").append(String.valueOf(ranking)).append("\n");
            }
            sb.append("${service.id} : ").append(String.valueOf(ep.serviceId)).append("\n");
            if ( ref != null )
            {
                sb.append("${bundle} : ");
                sb.append("${#link:");
                sb.append(ref.getBundle().getBundleId());
                sb.append("}");
                sb.append(ref.getBundle().getSymbolicName());
                sb.append("${link#}\n");
            }
            final StringBuilder tsb = new StringBuilder();
            for(final String t : ep.types)
            {
                tsb.append(t).append('\n');
            }
            odd = printRow(pw, odd, tsb.toString(), sb.toString());
        }
        pw.println("</table>");
    }

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration(final PrintWriter pw)
    {
        final RuntimeDTO dto = this.runtime.getRuntimeDTO();

        pw.println("HTTP Service Details");
        pw.println("====================");
        pw.println();
        pw.println("Runtime Properties");
        pw.println("------------------");

        for(final Map.Entry<String, Object> prop : dto.serviceDTO.properties.entrySet())
        {
            pw.print(prop.getKey());
            pw.print(" : ");
            pw.println(getValueAsString(prop.getValue()));
        }
        pw.println();
        for(final ServletContextDTO ctxDto : dto.servletContextDTOs )
        {
            pw.print("Servlet Context ");
            pw.println(ctxDto.name);
            pw.println("-----------------------------------------------");

            pw.print("Path : ");
            pw.println(getContextPath(ctxDto.contextPath));
            pw.print("service.id : ");
            pw.println(String.valueOf(ctxDto.serviceId));
            pw.println();
            if ( ctxDto.servletDTOs.length > 0 )
            {
                pw.println("Servlets");
                for (final ServletDTO servlet : ctxDto.servletDTOs)
                {
                    pw.print("Patterns : ");
                    pw.println(getValueAsString(servlet.patterns));
                    pw.print("Name : ");
                    pw.println(servlet.name);
                    final ServiceReference<?> ref = this.getServiceReference(servlet.serviceId);
                    if ( ref != null )
                    {
                        int ranking = 0;
                        final Object obj = ref.getProperty(Constants.SERVICE_RANKING);
                        if ( obj instanceof Integer)
                        {
                            ranking = (Integer)obj;
                        }
                        pw.print("Ranking : ");
                        pw.println(String.valueOf(ranking));
                    }
                    pw.print("async : ");
                    pw.println(String.valueOf(servlet.asyncSupported));
                    pw.print("service.id : ");
                    pw.println(String.valueOf(servlet.serviceId));
                    pw.println();
                    if ( ref != null )
                    {
                        pw.print("Bundle : ");
                        pw.print(ref.getBundle().getSymbolicName());
                        pw.print(" <");
                        pw.print(String.valueOf(ref.getBundle().getBundleId()));
                        pw.println(">");
                    }
                }
                pw.println();
            }

            if ( ctxDto.filterDTOs.length > 0 )
            {
                pw.println("Filters");
                for (final FilterDTO filter : ctxDto.filterDTOs)
                {
                    final List<String> patterns = new ArrayList<String>();
                    patterns.addAll(Arrays.asList(filter.patterns));
                    patterns.addAll(Arrays.asList(filter.regexs));
                    for(final String name : filter.servletNames)
                    {
                        patterns.add("Servlet : " + name);
                    }
                    Collections.sort(patterns);

                    pw.print("Patterns : ");
                    pw.println(patterns);
                    pw.print("Name : ");
                    pw.println(filter.name);
                    final ServiceReference<?> ref = this.getServiceReference(filter.serviceId);
                    if ( ref != null )
                    {
                        int ranking = 0;
                        final Object obj = ref.getProperty(Constants.SERVICE_RANKING);
                        if ( obj instanceof Integer)
                        {
                            ranking = (Integer)obj;
                        }
                        pw.print("Ranking : ");
                        pw.println(String.valueOf(ranking));
                    }
                    pw.print("async : ");
                    pw.println(String.valueOf(filter.asyncSupported));
                    pw.print("dispatcher : ");
                    pw.println(getValueAsString(filter.dispatcher));
                    pw.print("service.id : ");
                    pw.println(String.valueOf(filter.serviceId));
                    if ( ref != null )
                    {
                        pw.print("Bundle : ");
                        pw.print(ref.getBundle().getSymbolicName());
                        pw.print(" <");
                        pw.print(String.valueOf(ref.getBundle().getBundleId()));
                        pw.println(">");
                    }
                    pw.println();
                }
                pw.println();
            }
            if ( ctxDto.resourceDTOs.length > 0 )
            {
                pw.println("Resources");
                for (final ResourceDTO rsrc : ctxDto.resourceDTOs)
                {
                    pw.print("Patterns : ");
                    pw.println(getValueAsString(rsrc.patterns));
                    pw.print("Prefix : ");
                    pw.println(rsrc.prefix);
                    final ServiceReference<?> ref = this.getServiceReference(rsrc.serviceId);
                    if ( ref != null )
                    {
                        int ranking = 0;
                        final Object obj = ref.getProperty(Constants.SERVICE_RANKING);
                        if ( obj instanceof Integer)
                        {
                            ranking = (Integer)obj;
                        }
                        pw.print("Ranking : ");
                        pw.println(String.valueOf(ranking));
                    }
                    pw.print("service.id : ");
                    pw.println(String.valueOf(rsrc.serviceId));
                    pw.println();
                    if ( ref != null )
                    {
                        pw.print("Bundle : ");
                        pw.print(ref.getBundle().getSymbolicName());
                        pw.print(" <");
                        pw.print(String.valueOf(ref.getBundle().getBundleId()));
                        pw.println(">");
                    }
                }
                pw.println();

            }
            if ( ctxDto.errorPageDTOs.length > 0 )
            {
                pw.println("Error Pages");
                for (final ErrorPageDTO ep : ctxDto.errorPageDTOs)
                {
                    final List<String> patterns = new ArrayList<String>();
                    for(final long p : ep.errorCodes)
                    {
                        patterns.add(String.valueOf(p));
                    }
                    for(final String p : ep.exceptions)
                    {
                        patterns.add(p);
                    }
                    pw.print("Patterns : ");
                    pw.println(patterns);
                    pw.print("Name : ");
                    pw.println(ep.name);
                    final ServiceReference<?> ref = this.getServiceReference(ep.serviceId);
                    if ( ref != null )
                    {
                        int ranking = 0;
                        final Object obj = ref.getProperty(Constants.SERVICE_RANKING);
                        if ( obj instanceof Integer)
                        {
                            ranking = (Integer)obj;
                        }
                        pw.print("Ranking : ");
                        pw.println(String.valueOf(ranking));
                    }
                    pw.print("async : ");
                    pw.println(String.valueOf(ep.asyncSupported));
                    pw.print("service.id : ");
                    pw.println(String.valueOf(ep.serviceId));
                    if ( ref != null )
                    {
                        pw.print("Bundle : ");
                        pw.print(ref.getBundle().getSymbolicName());
                        pw.print(" <");
                        pw.print(String.valueOf(ref.getBundle().getBundleId()));
                        pw.println(">");
                    }
                    pw.println();
                }
                pw.println();
            }

            if ( ctxDto.listenerDTOs.length > 0 )
            {
                pw.println("Listeners");
                for (final ListenerDTO ep : ctxDto.listenerDTOs)
                {
                    pw.print("Types : ");
                    pw.println(getValueAsString(ep.types));
                    final ServiceReference<?> ref = this.getServiceReference(ep.serviceId);
                    if ( ref != null )
                    {
                        int ranking = 0;
                        final Object obj = ref.getProperty(Constants.SERVICE_RANKING);
                        if ( obj instanceof Integer)
                        {
                            ranking = (Integer)obj;
                        }
                        pw.print("Ranking : ");
                        pw.println(String.valueOf(ranking));
                    }
                    pw.print("service.id : ");
                    pw.println(String.valueOf(ep.serviceId));
                    if ( ref != null )
                    {
                        pw.print("Bundle : ");
                        pw.print(ref.getBundle().getSymbolicName());
                        pw.print(" <");
                        pw.print(String.valueOf(ref.getBundle().getBundleId()));
                        pw.println(">");
                    }
                    pw.println();
                }
                pw.println();
            }
            pw.println();
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
}
