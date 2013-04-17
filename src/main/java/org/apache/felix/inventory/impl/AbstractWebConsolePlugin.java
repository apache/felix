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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.inventory.Format;

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
            if (handler.supports(format))
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

    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
        IOException
    {
        this.setNoCache(response);

        // full request?
        final InventoryPrinterHandler handler;
        if (request.getPathInfo().lastIndexOf('/') > 0)
        {
            handler = null; // all;
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

            final ZipOutputStream zip = new ZipOutputStream(response.getOutputStream());
            zip.setLevel(Deflater.BEST_SPEED);
            zip.setMethod(ZipOutputStream.DEFLATED);

            final Date now = new Date();
            // create time stamp entry
            final ZipEntry entry = new ZipEntry("timestamp.txt"); //$NON-NLS-2$
            entry.setTime(now.getTime());
            zip.putNextEntry(entry);
            final StringBuffer sb = new StringBuffer();
            sb.append("Date: ");
            synchronized (InventoryPrinterAdapter.DISPLAY_DATE_FORMAT)
            {
                sb.append(InventoryPrinterAdapter.DISPLAY_DATE_FORMAT.format(now));
            }
            sb.append(" (");
            sb.append(String.valueOf(now.getTime()));
            sb.append(")\n");

            zip.write(sb.toString().getBytes("UTF-8"));
            zip.closeEntry();

            final ZipConfigurationWriter pw = new ZipConfigurationWriter(zip);
            printConfigurationInventory(pw, Format.TEXT, handler);
            printConfigurationInventory(pw, Format.JSON, handler);

            zip.finish();
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

            final Date currentTime = new Date();
            synchronized (InventoryPrinterAdapter.DISPLAY_DATE_FORMAT)
            {
                pw.print("Date: ");
                pw.println(InventoryPrinterAdapter.DISPLAY_DATE_FORMAT.format(currentTime));
            }

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
            if (handler.supports(Format.HTML))
            {
                handler.print(pw, Format.HTML, false);
            }
            else
            {
                pw.enableFilter(true);
                handler.print(pw, Format.TEXT, false);
                pw.enableFilter(false);
            }
            pw.print("</div>");
        }
    }

    /**
     * Base class for all configuration writers.
     */
    private abstract static class ConfigurationWriter extends PrintWriter
    {

        ConfigurationWriter(final Writer delegatee)
        {
            super(delegatee);
        }

        protected void title(final String title) throws IOException
        {
            // dummy implementation
        }

        protected void end() throws IOException
        {
            // dummy implementation
        }

        public void printInventory(final Format format, final InventoryPrinterHandler handler) throws IOException
        {
            this.title(handler.getTitle());
            handler.print(this, format, false);
            this.end();
        }
    }

    /**
     * The JSON configuration writer
     */
    private static class JSONConfigurationWriter extends ConfigurationWriter
    {

        private boolean wrapJSON;

        private boolean startLine;

        private boolean needComma;

        JSONConfigurationWriter(final Writer delegatee)
        {
            super(delegatee);
            this.wrapJSON = false;
        }

        public void startJSONWrapper()
        {
            println("{");
            println("  \"value\": [");

            this.wrapJSON = true;
            this.startLine = true;
            this.needComma = false;
        }

        public void endJSONWrapper()
        {
            if (this.wrapJSON)
            {
                // properly terminate the current line
                this.println();

                this.wrapJSON = false;
                this.startLine = false;

                super.println();
                super.println("  ]");
                super.println("}");
            }
        }

        // IE has an issue with white-space:pre in our case so, we write
        // <br/> instead of [CR]LF to get the line break. This also works
        // in other browsers.
        public void println()
        {
            if (wrapJSON)
            {
                if (!this.startLine)
                {
                    super.write('"');
                    this.startLine = true;
                    this.needComma = true;
                }
            }
            else
            {
                super.println();
            }
        }

        // some VM implementation directly write in underlying stream, instead
        // of
        // delegation to the write() method. So we need to override this, to
        // make
        // sure, that everything is escaped correctly
        public void print(final String str)
        {
            final char[] chars = str.toCharArray();
            write(chars, 0, chars.length);
        }

        private final char[] oneChar = new char[1];

        // always delegate to write(char[], int, int) otherwise in some VM
        // it cause endless cycle and StackOverflowError
        public void write(final int character)
        {
            synchronized (oneChar)
            {
                oneChar[0] = (char) character;
                write(oneChar, 0, 1);
            }
        }

        // write the characters unmodified unless filtering is enabled in
        // which case the writeFiltered(String) method is called for filtering
        public void write(char[] chars, int off, int len)
        {
            if (this.wrapJSON)
            {
                if (this.startLine)
                {
                    this.startLine();
                    this.startLine = false;
                }

                String v = new String(chars, off, len);
                StringTokenizer st = new StringTokenizer(v, "\r\n\"", true);
                while (st.hasMoreTokens())
                {
                    String t = st.nextToken();
                    if (t.length() == 1)
                    {
                        char c = t.charAt(0);
                        if (c == '\r')
                        {
                            // ignore
                        }
                        else if (c == '\n')
                        {
                            this.println();
                            this.startLine();
                        }
                        else if (c == '"')
                        {
                            super.write('\\');
                            super.write(c);
                        }
                        else
                        {
                            super.write(c);
                        }
                    }
                    else
                    {
                        super.write(t.toCharArray(), 0, t.length());
                    }
                }
            }
            else
            {
                super.write(chars, off, len);
            }
        }

        // write the string unmodified unless filtering is enabled in
        // which case the writeFiltered(String) method is called for filtering
        public void write(final String string, final int off, final int len)
        {
            write(string.toCharArray(), off, len);
        }

        private void startLine()
        {
            if (this.needComma)
            {
                super.write(',');
                super.println();
                this.needComma = false;
            }

            super.write("    \"".toCharArray(), 0, 5);
            this.startLine = false;
        }
    }

    /**
     * The HTML configuration writer outputs the status as an HTML snippet.
     */
    private static class HtmlConfigurationWriter extends ConfigurationWriter
    {

        // whether or not to filter "<" signs in the output
        private boolean doFilter;

        HtmlConfigurationWriter(final Writer delegatee)
        {
            super(delegatee);
        }

        void enableFilter(final boolean doFilter)
        {
            this.doFilter = doFilter;
        }

        // IE has an issue with white-space:pre in our case so, we write
        // <br/> instead of [CR]LF to get the line break. This also works
        // in other browsers.
        public void println()
        {
            if (doFilter)
            {
                this.write('\n'); // write <br/>
            }
            else
            {
                super.println();
            }
        }

        // some VM implementation directly write in underlying stream, instead
        // of
        // delegation to the write() method. So we need to override this, to
        // make
        // sure, that everything is escaped correctly
        public void print(final String str)
        {
            final char[] chars = str.toCharArray();
            write(chars, 0, chars.length);
        }

        private final char[] oneChar = new char[1];

        // always delegate to write(char[], int, int) otherwise in some VM
        // it cause endless cycle and StackOverflowError
        public void write(final int character)
        {
            synchronized (oneChar)
            {
                oneChar[0] = (char) character;
                write(oneChar, 0, 1);
            }
        }

        // write the characters unmodified unless filtering is enabled in
        // which case the writeFiltered(String) method is called for filtering
        public void write(char[] chars, int off, int len)
        {
            if (doFilter)
            {
                chars = this.escapeHtml(new String(chars, off, len)).toCharArray();
                off = 0;
                len = chars.length;
            }
            super.write(chars, off, len);
        }

        // write the string unmodified unless filtering is enabled in
        // which case the writeFiltered(String) method is called for filtering
        public void write(final String string, final int off, final int len)
        {
            write(string.toCharArray(), off, len);
        }

        /**
         * Escapes HTML special chars like: <>&\r\n and space
         *
         *
         * @param text the text to escape
         * @return the escaped text
         */
        private String escapeHtml(final String text)
        {
            final StringBuffer sb = new StringBuffer(text.length() * 4 / 3);
            char ch, oldch = '_';
            for (int i = 0; i < text.length(); i++)
            {
                switch (ch = text.charAt(i))
                {
                    case '<':
                        sb.append("&lt;"); //$NON-NLS-1$
                        break;
                    case '>':
                        sb.append("&gt;"); //$NON-NLS-1$
                        break;
                    case '&':
                        sb.append("&amp;"); //$NON-NLS-1$
                        break;
                    case ' ':
                        sb.append("&nbsp;"); //$NON-NLS-1$
                        break;
                    case '\r':
                    case '\n':
                        if (oldch != '\r' && oldch != '\n') // don't add twice
                                                            // <br>
                            sb.append("<br/>\n"); //$NON-NLS-1$
                        break;
                    default:
                        sb.append(ch);
                }
                oldch = ch;
            }

            return sb.toString();
        }
    }

    /**
     * The plain text configuration writer outputs the status as plain text.
     */
    private static class PlainTextConfigurationWriter extends ConfigurationWriter
    {

        PlainTextConfigurationWriter(final Writer delegatee)
        {
            super(delegatee);
        }

        protected void title(final String title) throws IOException
        {
            print("*** ");
            print(title);
            println(":");
        }

        protected void end() throws IOException
        {
            println();
        }
    }

    /**
     * The ZIP configuration writer creates a zip with
     * - txt output of a inventory printers (if supported)
     * - json output of a inventory printers (if supported)
     * - attachments from a inventory printer (if supported)
     */
    private static class ZipConfigurationWriter extends ConfigurationWriter
    {

        private final ZipOutputStream zip;

        ZipConfigurationWriter(final ZipOutputStream zip)
        {
            super(new OutputStreamWriter(zip));
            this.zip = zip;
        }

        public void printInventory(final Format format, final InventoryPrinterHandler handler) throws IOException
        {
            if (format == Format.TEXT)
            {
                final ZipEntry entry = new ZipEntry(handler.getName().concat(".txt"));
                zip.putNextEntry(entry);
                handler.print(this, format, false);
                flush();
                zip.closeEntry();

                handler.addAttachments(handler.getName().concat("/"), this.zip);
            }
            else if (format == Format.JSON)
            {
                final String name = "json/".concat(handler.getName()).concat(".json");

                final ZipEntry entry = new ZipEntry(name);
                zip.putNextEntry(entry);
                handler.print(this, Format.JSON, true);
                flush();

                zip.closeEntry();
                if (!handler.supports(Format.TEXT))
                {
                    handler.addAttachments(handler.getName().concat("/"), this.zip);
                }
            }
        }
    }
}