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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
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
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.runtime.dto.FailedServletContextDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
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
    private static final String ATTR_TEST = "test";
    private static final String ATTR_MSG = "msg";
    private static final String ATTR_SUBMIT = "resolve";


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
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        final String test = request.getParameter(ATTR_TEST);
        String msg = null;
        if (test != null && test.length() > 0) {

            final RequestInfoDTO dto = this.runtime.calculateRequestInfoDTO(test);

            final StringBuilder sb = new StringBuilder();
            if ( dto.servletDTO != null )
            {
                sb.append("Servlet: ");
                sb.append(getValueAsString(dto.servletDTO.patterns));
                sb.append(" (");
                sb.append("service.id=");
                sb.append(String.valueOf(dto.servletDTO.serviceId));
                sb.append("), Filters: [");
                boolean first = true;
                for(final FilterDTO f : dto.filterDTOs)
                {
                    if ( first )
                    {
                        first = false;
                    }
                    else
                    {
                        sb.append(", ");
                    }
                    sb.append(f.serviceId);
                }
                sb.append("]");
            }
            else if ( dto.resourceDTO != null )
            {
                sb.append("Resource: ");
                sb.append(getValueAsString(dto.resourceDTO.patterns));
                sb.append(" (");
                sb.append("service.id=");
                sb.append(String.valueOf(dto.resourceDTO.serviceId));
                sb.append("), Filters: [");
                boolean first = true;
                for(final FilterDTO f : dto.filterDTOs)
                {
                    if ( first )
                    {
                        first = false;
                    }
                    else
                    {
                        sb.append(", ");
                    }
                    sb.append(f.serviceId);
                }
                sb.append("]");
            }
            else
            {
                sb.append("<404>");
            }
            msg = sb.toString();
        }

        // finally redirect
        final String path = request.getContextPath() + request.getServletPath()
                + request.getPathInfo();
        final String redirectTo;
        if (msg == null) {
            redirectTo = path;
        } else {
            redirectTo = path + '?' + ATTR_MSG + '=' + encodeParam(msg) + '&'
                    + ATTR_TEST + '=' + encodeParam(test);
        }
        response.sendRedirect(redirectTo);
    }

    private String encodeParam(final String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // should never happen
            return value;
        }
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException
    {
        final RuntimeDTO dto = this.runtime.getRuntimeDTO();

        final PrintWriter pw = resp.getWriter();

        String path = req.getContextPath() + req.getServletPath();
        if ( req.getPathInfo() != null ) {
            path = path + req.getPathInfo();
        }
        printForm(pw, req.getParameter(ATTR_TEST), req.getParameter(ATTR_MSG), path);

        printRuntimeDetails(pw, dto.serviceDTO);

        for(final ServletContextDTO ctxDto : dto.servletContextDTOs )
        {
            printContextDetails(pw, ctxDto);
        }
        for(final FailedServletContextDTO ctxDto : dto.failedServletContextDTOs )
        {
            printFailedContextDetails(pw, ctxDto);
        }
        printFailedServletDetails(pw, dto);
        printFailedFilterDetails(pw, dto);
        printFailedResourceDetails(pw, dto);
        printFailedErrorPageDetails(pw, dto);
        printFailedListenerDetails(pw, dto);

        pw.println("<br/>");
    }

    private void printForm(final PrintWriter pw, final String value, final String msg, final String path)
    {
        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");

        separatorHtml(pw);

        titleHtml(
                pw,
                "Test Servlet Resolution",
                "To test the servlet resolution, enter a relative URL into "
                        + "the field and click 'Resolve'.");

        pw.println("<tr class='content'>");
        pw.println("<td class='content'>Test</td>");
        pw.print("<td class='content' colspan='2'>");
        pw.print("<form method='POST' action='");
        pw.print(path);
        pw.print("'>");
        pw.print("<input type='text' name='" + ATTR_TEST + "' value='");
        if (value != null) {
            pw.print(escapeXml(value));
        }
        pw.println("' class='input' size='50'>");
        pw.println("&nbsp;&nbsp;<input type='submit' name='" + ATTR_SUBMIT
                + "' value='Resolve' class='submit'>");
        pw.print("</form>");
        pw.print("</td>");
        pw.println("</tr>");

        if (msg != null) {
            pw.println("<tr class='content'>");
            pw.println("<td class='content'>&nbsp;</td>");
            pw.print("<td class='content' colspan='2'>");
            pw.print(escapeXml(msg));
            pw.println("</td>");
            pw.println("</tr>");
        }
        pw.println("</table>");
    }

    private void titleHtml(PrintWriter pw, String title, String description)
    {
        pw.println("<tr class='content'>");
        pw.println("<th colspan='3'class='content container'>" + title
                + "</th>");
        pw.println("</tr>");

        if (description != null) {
            pw.println("<tr class='content'>");
            pw.println("<td colspan='3'class='content'>" + description
                    + "</th>");
            pw.println("</tr>");
        }
    }

    private void separatorHtml(PrintWriter pw)
    {
        pw.println("<tr class='content'>");
        pw.println("<td class='content' colspan='3'>&nbsp;</td>");
        pw.println("</tr>");
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
        pw.println("<br/>");
    }

    private boolean printRow(final PrintWriter pw, final boolean odd, final String...columns)
    {
        pw.print("<tr class=\"");
        if ( odd ) pw.print("odd"); else pw.print("even");
        pw.println(" ui-state-default\">");

        for(final String val : columns)
        {
            pw.print("<td>");
            if ( val != null )
            {
                String text = escapeXml(val).replace("\n", "<br/>");
                int pos;
                while ( (pos = text.indexOf("${#link:")) != -1)
                {
                    final int endPos = text.indexOf("}", pos);
                    final int bundleId = Integer.valueOf(text.substring(pos + 8, endPos));
                    final int tokenEndPos = text.indexOf("${link#}", pos);

                    text = text.substring(0, pos) + "<a href=\"${appRoot}/bundles/" + String.valueOf(bundleId) + "\">" +
                           text.substring(endPos + 1, tokenEndPos) + "</a>" + text.substring(tokenEndPos + 8);
                }
                pw.print(text);
            }
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

    private boolean printServiceRankingRow(final PrintWriter pw, final long serviceId, final boolean odd)
    {
        int ranking = 0;
        final ServiceReference<?> ref = this.getServiceReference(serviceId);
        if ( ref != null )
        {
            final Object obj = ref.getProperty(Constants.SERVICE_RANKING);
            if ( obj instanceof Integer)
            {
                ranking = (Integer)obj;
            }
        }
        return printRow(pw, odd, "${ranking}", String.valueOf(ranking));
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
        odd = printServiceRankingRow(pw, dto.serviceId, odd);
        pw.println("</table>");

        printServletDetails(pw, dto);
        printFilterDetails(pw, dto);
        printResourceDetails(pw, dto);
        printErrorPageDetails(pw, dto);
        printListenerDetails(pw, dto);

        pw.println("<br/>");
    }

    private void printFailedContextDetails(final PrintWriter pw, final FailedServletContextDTO dto)
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
        odd = printRow(pw, odd, "${reason}", getErrorText(dto.failureReason));
        odd = printRow(pw, odd, "${service.id}", String.valueOf(dto.serviceId));
        pw.println("</table>");
    }

    private void appendServiceRanking(final StringBuilder sb, final ServiceReference<?> ref)
    {
        int ranking = 0;
        if ( ref != null )
        {
            final Object obj = ref.getProperty(Constants.SERVICE_RANKING);
            if ( obj instanceof Integer)
            {
                ranking = (Integer)obj;
            }
        }
        sb.append("${ranking} : ").append(String.valueOf(ranking)).append("\n");
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
            final ServiceReference<?> ref = this.getServiceReference(filter.serviceId);
            final StringBuilder sb = new StringBuilder();
            sb.append("${service.id} : ").append(String.valueOf(filter.serviceId)).append("\n");
            appendServiceRanking(sb, ref);
            sb.append("${async} : ").append(String.valueOf(filter.asyncSupported)).append("\n");
            sb.append("${dispatcher} : ").append(getValueAsString(filter.dispatcher)).append("\n");
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

    private String getErrorText(final int reason)
    {
        switch ( reason )
        {
        case DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT : return "Exception on init";
        case DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING : return "No match";
        case DTOConstants.FAILURE_REASON_SERVICE_IN_USE : return "In use";
        case DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE : return "Not gettable";
        case DTOConstants.FAILURE_REASON_SERVLET_CONTEXT_FAILURE : return "Context failure";
        case DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE : return "Shadowed";
        case DTOConstants.FAILURE_REASON_VALIDATION_FAILED : return "Invalid";
        default: return "unknown";
        }
    }
    private void printFailedFilterDetails(final PrintWriter pw, final RuntimeDTO dto)
    {
        if ( dto.failedFilterDTOs.length == 0 )
        {
            return;
        }
        pw.print("<p class=\"statline ui-state-highlight\">${Failed Filter Services}</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Pattern}</th>");
        pw.println("<th class=\"header\">${Filter}</th>");
        pw.println("<th class=\"header\">${Info}</th>");
        pw.println("</tr></thead>");

        boolean odd = true;
        for (final FailedFilterDTO filter : dto.failedFilterDTOs)
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("${reason} : ").append(getErrorText(filter.failureReason)).append("\n");
            final ServiceReference<?> ref = this.getServiceReference(filter.serviceId);
            sb.append("${service.id} : ").append(String.valueOf(filter.serviceId)).append("\n");
            appendServiceRanking(sb, ref);
            sb.append("${async} : ").append(String.valueOf(filter.asyncSupported)).append("\n");
            sb.append("${dispatcher} : ").append(getValueAsString(filter.dispatcher)).append("\n");
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
            sb.append("${service.id} : ").append(String.valueOf(servlet.serviceId)).append("\n");
            appendServiceRanking(sb, ref);
            sb.append("${async} : ").append(String.valueOf(servlet.asyncSupported)).append("\n");
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

    private void printFailedServletDetails(final PrintWriter pw, final RuntimeDTO dto)
    {
        if ( dto.failedServletDTOs.length == 0 )
        {
            return;
        }
        pw.print("<p class=\"statline ui-state-highlight\">${Failed Servlet Services}</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Path}</th>");
        pw.println("<th class=\"header\">${Name}</th>");
        pw.println("<th class=\"header\">${Info}</th>");
        pw.println("</tr></thead>");

        boolean odd = true;
        for (final FailedServletDTO servlet : dto.failedServletDTOs)
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("${reason} : ").append(getErrorText(servlet.failureReason)).append("\n");
            final ServiceReference<?> ref = this.getServiceReference(servlet.serviceId);
            sb.append("${service.id} : ").append(String.valueOf(servlet.serviceId)).append("\n");
            appendServiceRanking(sb, ref);
            sb.append("${async} : ").append(String.valueOf(servlet.asyncSupported)).append("\n");
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
            sb.append("${service.id} : ").append(String.valueOf(rsrc.serviceId)).append("\n");
            appendServiceRanking(sb, ref);
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

    private void printFailedResourceDetails(final PrintWriter pw, final RuntimeDTO dto)
    {
        if ( dto.failedResourceDTOs.length == 0 )
        {
            return;
        }
        pw.print("<p class=\"statline ui-state-highlight\">${Failed Resource Services}</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Path}</th>");
        pw.println("<th class=\"header\">${Prefix}</th>");
        pw.println("<th class=\"header\">${Info}</th>");
        pw.println("</tr></thead>");

        boolean odd = true;
        for (final FailedResourceDTO rsrc : dto.failedResourceDTOs)
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("${reason} : ").append(getErrorText(rsrc.failureReason)).append("\n");
            final ServiceReference<?> ref = this.getServiceReference(rsrc.serviceId);
            sb.append("${service.id} : ").append(String.valueOf(rsrc.serviceId)).append("\n");
            appendServiceRanking(sb, ref);
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
        pw.println("' ${Registered Error Pages}</p>");

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
            sb.append("${service.id} : ").append(String.valueOf(ep.serviceId)).append("\n");
            appendServiceRanking(sb, ref);
            sb.append("${async} : ").append(String.valueOf(ep.asyncSupported)).append("\n");
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

    private void printFailedErrorPageDetails(final PrintWriter pw, final RuntimeDTO dto)
    {
        if ( dto.failedErrorPageDTOs.length == 0 )
        {
            return;
        }
        pw.print("<p class=\"statline ui-state-highlight\">${Registered Error Pages}</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Path}</th>");
        pw.println("<th class=\"header\">${Name}</th>");
        pw.println("<th class=\"header\">${Info}</th>");
        pw.println("</tr></thead>");

        boolean odd = true;
        for (final FailedErrorPageDTO ep : dto.failedErrorPageDTOs)
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("${reason} : ").append(getErrorText(ep.failureReason)).append("\n");
            final ServiceReference<?> ref = this.getServiceReference(ep.serviceId);
            sb.append("${service.id} : ").append(String.valueOf(ep.serviceId)).append("\n");
            appendServiceRanking(sb, ref);
            sb.append("${async} : ").append(String.valueOf(ep.asyncSupported)).append("\n");
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
            sb.append("${service.id} : ").append(String.valueOf(ep.serviceId)).append("\n");
            appendServiceRanking(sb, ref);
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

    private void printFailedListenerDetails(final PrintWriter pw, final RuntimeDTO dto)
    {
        if ( dto.failedListenerDTOs.length == 0 )
        {
            return;
        }
        pw.print("<p class=\"statline ui-state-highlight\">${Failed Listeners}</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Type}</th>");
        pw.println("<th class=\"header\">${Info}</th>");
        pw.println("</tr></thead>");

        boolean odd = true;
        for (final FailedListenerDTO ep : dto.failedListenerDTOs)
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("${reason} : ").append(getErrorText(ep.failureReason)).append("\n");
            final ServiceReference<?> ref = this.getServiceReference(ep.serviceId);
            sb.append("${service.id} : ").append(String.valueOf(ep.serviceId)).append("\n");
            appendServiceRanking(sb, ref);
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

    private void printServiceIdAndRanking(final PrintWriter pw, final ServiceReference<?> ref, final long serviceId)
    {
        pw.print("service.id : ");
        pw.println(String.valueOf(serviceId));
        int ranking = 0;
        if ( ref != null )
        {
            final Object obj = ref.getProperty(Constants.SERVICE_RANKING);
            if ( obj instanceof Integer)
            {
                ranking = (Integer)obj;
            }
        }
        pw.print("Ranking : ");
        pw.println(String.valueOf(ranking));
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
            printServiceIdAndRanking(pw, this.getServiceReference(ctxDto.serviceId), ctxDto.serviceId);
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
                    printServiceIdAndRanking(pw, ref, servlet.serviceId);
                    pw.print("async : ");
                    pw.println(String.valueOf(servlet.asyncSupported));
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
                    printServiceIdAndRanking(pw, ref, filter.serviceId);
                    pw.print("async : ");
                    pw.println(String.valueOf(filter.asyncSupported));
                    pw.print("dispatcher : ");
                    pw.println(getValueAsString(filter.dispatcher));
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
                    printServiceIdAndRanking(pw, ref, rsrc.serviceId);
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
                    printServiceIdAndRanking(pw, ref, ep.serviceId);
                    pw.print("async : ");
                    pw.println(String.valueOf(ep.asyncSupported));
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
                    printServiceIdAndRanking(pw, ref, ep.serviceId);
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

        if ( dto.failedServletContextDTOs.length > 0 )
        {
            for(final FailedServletContextDTO ctxDto : dto.failedServletContextDTOs )
            {
                pw.print("Failed Servlet Context ");
                pw.println(ctxDto.name);
                pw.println("-----------------------------------------------");

                pw.print("Reason : ");
                pw.println(getErrorText(ctxDto.failureReason));
                pw.print("Path : ");
                pw.println(getContextPath(ctxDto.contextPath));
                printServiceIdAndRanking(pw, this.getServiceReference(ctxDto.serviceId), ctxDto.serviceId);
                pw.println();
            }
        }
        if ( dto.failedServletDTOs.length > 0 )
        {
            pw.println("Failed Servlets");
            for (final FailedServletDTO servlet : dto.failedServletDTOs)
            {
                pw.print("Patterns : ");
                pw.println(getValueAsString(servlet.patterns));
                pw.print("Reason : ");
                pw.println(getErrorText(servlet.failureReason));
                pw.print("Name : ");
                pw.println(servlet.name);
                final ServiceReference<?> ref = this.getServiceReference(servlet.serviceId);
                printServiceIdAndRanking(pw, ref, servlet.serviceId);
                pw.print("async : ");
                pw.println(String.valueOf(servlet.asyncSupported));
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

        if ( dto.failedFilterDTOs.length > 0 )
        {
            pw.println("Failed Filters");
            for (final FailedFilterDTO filter : dto.failedFilterDTOs)
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
                pw.print("Reason : ");
                pw.println(getErrorText(filter.failureReason));
                pw.print("Name : ");
                pw.println(filter.name);
                final ServiceReference<?> ref = this.getServiceReference(filter.serviceId);
                printServiceIdAndRanking(pw, ref, filter.serviceId);
                pw.print("async : ");
                pw.println(String.valueOf(filter.asyncSupported));
                pw.print("dispatcher : ");
                pw.println(getValueAsString(filter.dispatcher));
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
        if ( dto.failedResourceDTOs.length > 0 )
        {
            pw.println("Failed Resources");
            for (final FailedResourceDTO rsrc : dto.failedResourceDTOs)
            {
                pw.print("Patterns : ");
                pw.println(getValueAsString(rsrc.patterns));
                pw.print("Reason : ");
                pw.println(getErrorText(rsrc.failureReason));
                pw.print("Prefix : ");
                pw.println(rsrc.prefix);
                final ServiceReference<?> ref = this.getServiceReference(rsrc.serviceId);
                printServiceIdAndRanking(pw, ref, rsrc.serviceId);
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
        if ( dto.failedErrorPageDTOs.length > 0 )
        {
            pw.println("Failed Error Pages");
            for (final FailedErrorPageDTO ep : dto.failedErrorPageDTOs)
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
                pw.print("Reason : ");
                pw.println(getErrorText(ep.failureReason));
                pw.print("Name : ");
                pw.println(ep.name);
                final ServiceReference<?> ref = this.getServiceReference(ep.serviceId);
                printServiceIdAndRanking(pw, ref, ep.serviceId);
                pw.print("async : ");
                pw.println(String.valueOf(ep.asyncSupported));
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

        if ( dto.failedListenerDTOs.length > 0 )
        {
            pw.println("Listeners");
            for (final FailedListenerDTO ep : dto.failedListenerDTOs)
            {
                pw.print("Types : ");
                pw.println(getValueAsString(ep.types));
                pw.print("Reason : ");
                pw.println(getErrorText(ep.failureReason));
                final ServiceReference<?> ref = this.getServiceReference(ep.serviceId);
                printServiceIdAndRanking(pw, ref, ep.serviceId);
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

    public void unregister()
    {
        if (this.serviceReg != null)
        {
            this.serviceReg.unregister();
            this.serviceReg = null;
        }
    }
}
