/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.inventory.impl;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.impl.helper.ConfigurationWriter;
import org.apache.felix.inventory.impl.helper.HtmlConfigurationWriter;
import org.apache.felix.inventory.impl.helper.JSONConfigurationWriter;
import org.apache.felix.inventory.impl.helper.PlainTextConfigurationWriter;
import org.apache.felix.inventory.impl.helper.ZipConfigurationWriter;

/**
 * The web console plugin for a inventory printer.
 */
public abstract class AbstractWebConsolePlugin extends HttpServlet
{

    private static final long serialVersionUID = 1L;

    /** The inventory printer manager. */
    protected final InventoryPrinterManagerImpl inventoryPrinterManager;

    /**
     * Constructor
     *
     * @param inventoryPrinterManager The manager
     */
    AbstractWebConsolePlugin(final InventoryPrinterManagerImpl inventoryPrinterManager)
    {
        this.inventoryPrinterManager = inventoryPrinterManager;
    }

    protected abstract InventoryPrinterHandler getInventoryPrinterHandler();

    private void printConfigurationInventory(final ConfigurationWriter pw, final Format format,
        final InventoryPrinterHandler handler) throws IOException
    {
        if (handler == null)
        {
            final InventoryPrinterHandler[] adapters = this.inventoryPrinterManager.getHandlers(format);
            for (int i = 0; i < adapters.length; i++)
            {
                pw.printInventory(format, adapters[i]);
            }
        }
        else
        {
            if (format == null || handler.supports(format))
            {
                pw.printInventory(format, handler);
            }
        }
    }

    /**
     * Sets response headers to force the client to not cache the response
     * sent back. This method must be called before the response is committed
     * otherwise it will have no effect.
     * <p>
     * This method sets the <code>Cache-Control</code>, <code>Expires</code>,
     * and <code>Pragma</code> headers.
     *
     * @param response The response for which to set the cache prevention
     */
    private final void setNoCache(final HttpServletResponse response)
    {
        response.setHeader("Cache-Control", "no-cache"); //$NON-NLS-1$ //$NON-NLS-2$
        response.addHeader("Cache-Control", "no-store"); //$NON-NLS-1$ //$NON-NLS-2$
        response.addHeader("Cache-Control", "must-revalidate"); //$NON-NLS-1$ //$NON-NLS-2$
        response.addHeader("Cache-Control", "max-age=0"); //$NON-NLS-1$ //$NON-NLS-2$
        response.setHeader("Expires", "Thu, 01 Jan 1970 01:00:00 GMT"); //$NON-NLS-1$ //$NON-NLS-2$
        response.setHeader("Pragma", "no-cache"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    protected InventoryPrinterHandler getInventoryPrinterHandler(final String label)
    {
        return null; // all by default
    }

    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException
    {
        this.setNoCache(response);

        // full request?
        final InventoryPrinterHandler handler;
        final String pathInfo = request.getPathInfo();
        final int lastSlash = pathInfo.lastIndexOf('/');
        if (lastSlash > 0)
        {
            final int lastDot = pathInfo.lastIndexOf('.');
            final String label = (lastDot < lastSlash ? pathInfo.substring(lastSlash + 1) : pathInfo.substring(lastSlash + 1, lastDot));
            handler = this.getInventoryPrinterHandler(label); // usually all;
        }
        else
        {
            handler = this.getInventoryPrinterHandler();
            if (handler == null)
            {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }

        if (request.getPathInfo().endsWith(".txt")) { //$NON-NLS-2$
            response.setContentType("text/plain; charset=utf-8"); //$NON-NLS-2$
            final ConfigurationWriter pw = new PlainTextConfigurationWriter(response.getWriter());
            printConfigurationInventory(pw, Format.TEXT, handler);
            pw.flush();
        }
        else if (request.getPathInfo().endsWith(".zip")) { //$NON-NLS-2$
            String type = getServletContext().getMimeType(request.getPathInfo());
            if (type == null)
            {
                type = "application/x-zip"; //$NON-NLS-2$
            }
            response.setContentType(type);

            final ZipConfigurationWriter pw = ZipConfigurationWriter.create(response.getOutputStream());
            printConfigurationInventory(pw, null, handler);
            pw.finish();
        }
        else if (request.getPathInfo().endsWith(".nfo"))
        {
            if (handler == null)
            {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setContentType("text/html; charset=utf-8");

            final HtmlConfigurationWriter pw = new HtmlConfigurationWriter(response.getWriter());
            pw.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"");
            pw.println("  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
            pw.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
            pw.println("<head><title>dummy</title></head><body><div>");

            if (handler.supports(Format.HTML))
            {
                handler.print(pw, Format.HTML, false);
            }
            else if (handler.supports(Format.TEXT))
            {
                pw.enableFilter(true);
                handler.print(pw, Format.TEXT, false);
                pw.enableFilter(false);
            }
            else
            {
                pw.enableFilter(true);
                handler.print(pw, Format.JSON, false);
                pw.enableFilter(false);
            }
            pw.println("</div></body></html>");
            return;
        }
        else if (request.getPathInfo().endsWith(".json"))
        {
            if (handler == null)
            {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setContentType("application/json"); //$NON-NLS-1$
            response.setCharacterEncoding("UTF-8"); //$NON-NLS-1$

            final JSONConfigurationWriter jcw = new JSONConfigurationWriter(response.getWriter());
            final Format format;
            if (handler.supports(Format.JSON))
            {
                format = Format.JSON;
            }
            else
            {
                format = Format.TEXT;
                jcw.startJSONWrapper();
            }
            printConfigurationInventory(jcw, format, handler);
            jcw.endJSONWrapper();
            jcw.flush();
        }
        else
        {
            if (handler == null)
            {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            final HtmlConfigurationWriter pw = new HtmlConfigurationWriter(response.getWriter());
            pw.println("<script type=\"text/javascript\">");
            pw.println("// <![CDATA[");
            pw.println("function pad(value) { if ( value < 10 ) { return '0' + value;} return '' + value;}");
            pw.println("function downloadDump(ext, full) {");
            pw.println("  if (full) {");
            pw.println("    var now = new Date();");
            pw.println("    var name = \"configuration-status-\" + now.getUTCFullYear() + pad(now.getUTCMonth() + 1) + pad(now.getUTCDate()) + \"-\" + pad(now.getUTCHours()) + pad(now.getUTCMinutes()) + pad(now.getUTCSeconds()) + \".\";");
            pw.println("    location.href = location.href + \"/\" + name + ext;");
            pw.println("  } else {");
            pw.println("    location.href = location.href + '.' + ext;");
            pw.println("  }");
            pw.println("}");

            pw.println("$(document).ready(function() {");
            pw.println("    $('.downloadTxt').click(function() { downloadDump('txt', false)});");
            pw.println("    $('.downloadJson').click(function() { downloadDump('json', false)});");
            pw.println("    $('.downloadZip').click(function() { downloadDump('zip', false)});");
            pw.println("    $('.downloadFullZip').click(function() { downloadDump('zip', true)});");
            pw.println("    $('.downloadFullTxt').click(function() { downloadDump('txt', true)});");
            pw.println("});");
            pw.println("// ]]>");
            pw.println("</script>");
            pw.println("<br/><p class=\"statline\">");

            pw.print("Date: ");
            pw.println(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.US).format(new Date()));

            pw.print("<button type=\"button\" class=\"downloadFullZip\" style=\"float: right; margin-right: 30px; margin-top: 5px;\">Download Full Zip</button>");
            pw.print("<button type=\"button\" class=\"downloadFullTxt\" style=\"float: right; margin-right: 30px; margin-top: 5px;\">Download Full Text</button>");

            if (handler.supports(Format.JSON))
            {
                pw.print("<button type=\"button\" class=\"downloadJson\" style=\"float: right; margin-right: 30px; margin-top: 5px;\">Download As JSON</button>");
            }
            pw.print("<button type=\"button\" class=\"downloadZip\" style=\"float: right; margin-right: 30px; margin-top: 5px;\">Download As Zip</button>");
            if (handler.supports(Format.TEXT))
            {
                pw.print("<button type=\"button\" class=\"downloadTxt\" style=\"float: right; margin-right: 30px; margin-top: 5px;\">Download As Text</button>");
            }

            pw.println("<br/>&nbsp;</p>"); // status line
            pw.print("<div>");

            final boolean filter;
            final Format format;
            if (handler.supports(Format.HTML))
            {
                filter = false;
                format = Format.HTML;
            }
            else if (handler.supports(Format.TEXT))
            {
                // prefer TEXT of JSON if available
                filter = true;
                format = Format.TEXT;
            }
            else if (handler.supports(Format.JSON))
            {
                filter = true;
                format = Format.JSON;
            }
            else
            {
                // fallback to TEXT (if unknown format)
                filter = true;
                format = Format.TEXT;
            }

            pw.enableFilter(filter);
            handler.print(pw, format, false);
            pw.enableFilter(false);

            pw.print("</div>");
        }
    }

}