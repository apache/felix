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
package org.apache.felix.status.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.status.PrinterMode;
import org.apache.felix.status.StatusPrinterHandler;
import org.apache.felix.status.StatusPrinterManager;

/**
 * The web console plugin for a status printer.
 */
public abstract class AbstractWebConsolePlugin extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** The status printer manager. */
    protected final StatusPrinterManager statusPrinterManager;

    /**
     * Constructor
     * @param statusPrinterManager The manager
     */
    AbstractWebConsolePlugin(final StatusPrinterManager statusPrinterManager) {
        this.statusPrinterManager = statusPrinterManager;
    }

    protected abstract StatusPrinterHandler getStatusPrinterHandler();

    private void printConfigurationStatus( final ConfigurationWriter pw, final PrinterMode mode, final StatusPrinterHandler handler )
    throws IOException {
        if ( handler == null ) {
            for(final StatusPrinterHandler sph : this.statusPrinterManager.getHandlers(mode)) {
                pw.title(sph.getTitle());
                sph.printConfiguration(mode, pw);
                pw.end();
            }
        } else {
            if ( handler.supports(mode) ) {
                pw.title(handler.getTitle());
                handler.printConfiguration(mode, pw);
                pw.end();
            }
        }
    }

    private void addAttachments( final ConfigurationWriter cf, final StatusPrinterHandler handler )
    throws IOException {
        if ( handler == null ) {
            for(final StatusPrinterHandler sph : this.statusPrinterManager.getHandlers(PrinterMode.ZIP_FILE)) {
                final URL[] attachments = sph.getAttachmentsForZip();
                if ( attachments != null ) {
                    cf.handleAttachments( sph.getTitle(), attachments );
                }
            }
        } else {
            if ( handler.supports(PrinterMode.ZIP_FILE) ) {
                final URL[] attachments = handler.getAttachmentsForZip();
                if ( attachments != null ) {
                    cf.handleAttachments( handler.getTitle(), attachments );
                }
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
    private final void setNoCache(final HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache"); //$NON-NLS-1$ //$NON-NLS-2$
        response.addHeader("Cache-Control", "no-store"); //$NON-NLS-1$ //$NON-NLS-2$
        response.addHeader("Cache-Control", "must-revalidate"); //$NON-NLS-1$ //$NON-NLS-2$
        response.addHeader("Cache-Control", "max-age=0"); //$NON-NLS-1$ //$NON-NLS-2$
        response.setHeader("Expires", "Thu, 01 Jan 1970 01:00:00 GMT"); //$NON-NLS-1$ //$NON-NLS-2$
        response.setHeader("Pragma", "no-cache"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response)
    throws ServletException, IOException {
        this.setNoCache( response );

        // full request?
        final StatusPrinterHandler handler;
        if ( request.getPathInfo().lastIndexOf('/') > 0 ) {
            handler = null; // all;
        } else {
            handler = this.getStatusPrinterHandler();
            if ( handler == null ) {
                response.sendError( HttpServletResponse.SC_NOT_FOUND );
                return;
            }
        }
        if ( request.getPathInfo().endsWith( ".txt" ) ) { //$NON-NLS-2$
            response.setContentType( "text/plain; charset=utf-8" ); //$NON-NLS-2$
            final ConfigurationWriter pw = new PlainTextConfigurationWriter( response.getWriter() );
            printConfigurationStatus( pw, PrinterMode.TEXT, handler );
            pw.flush();
        } else if ( request.getPathInfo().endsWith( ".zip" ) ) { //$NON-NLS-2$
            String type = getServletContext().getMimeType( request.getPathInfo() );
            if ( type == null ) {
                type = "application/x-zip"; //$NON-NLS-2$
            }
            response.setContentType( type );

            final ZipOutputStream zip = new ZipOutputStream( response.getOutputStream() );
            zip.setLevel( Deflater.BEST_SPEED );
            zip.setMethod( ZipOutputStream.DEFLATED );

            final Date now = new Date();
            // create time stamp entry
            final ZipEntry entry = new ZipEntry( "timestamp.txt" ); //$NON-NLS-2$
            entry.setTime(now.getTime());
            zip.putNextEntry( entry );
            final StringBuilder sb = new StringBuilder();
            sb.append("Date: ");
            synchronized ( StatusPrinterAdapter.DISPLAY_DATE_FORMAT )                             {
                sb.append(StatusPrinterAdapter.DISPLAY_DATE_FORMAT.format(now));
            }
            sb.append(" (");
            sb.append(String.valueOf(now.getTime()));
            sb.append(")\n");

            zip.write(sb.toString().getBytes("UTF-8"));
            zip.closeEntry();
            zip.flush();

            final ConfigurationWriter pw = new ZipConfigurationWriter( zip );
            printConfigurationStatus( pw, PrinterMode.ZIP_FILE, handler );
            pw.flush();

            addAttachments( pw, handler );
            zip.finish();
        } else if ( request.getPathInfo().endsWith( ".nfo" ) ) {
            if ( handler == null ) {
                response.sendError( HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setContentType( "text/html; charset=utf-8" );

            final HtmlConfigurationWriter pw = new HtmlConfigurationWriter( response.getWriter() );
            pw.println ( "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"" );
            pw.println ( "  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" );
            pw.println ( "<html xmlns=\"http://www.w3.org/1999/xhtml\">" );
            pw.println ( "<head><title>dummy</title></head><body><div>" );

            if ( handler.supports(PrinterMode.HTML_BODY) ) {
                handler.printConfiguration(PrinterMode.HTML_BODY, pw);
            } else {
                pw.enableFilter( true );
                handler.printConfiguration(PrinterMode.TEXT, pw);
                pw.enableFilter( false );
            }
            pw.println( "</div></body></html>" );
            return;
        } else {
            if ( handler == null ) {
                response.sendError( HttpServletResponse.SC_NOT_FOUND);
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
            pw.println("    $('.downloadZip').click(function() { downloadDump('zip', false)});");
            pw.println("    $('.downloadFullZip').click(function() { downloadDump('zip', true)});");
            pw.println("    $('.downloadFullTxt').click(function() { downloadDump('txt', true)});");
            pw.println("});");
            pw.println("// ]]>");
            pw.println("</script>");
            pw.println( "<br/><p class=\"statline\">");

            final Date currentTime = new Date();
            synchronized ( StatusPrinterAdapter.DISPLAY_DATE_FORMAT )                             {
                pw.print("Date: ");
                pw.println(StatusPrinterAdapter.DISPLAY_DATE_FORMAT.format(currentTime));
            }

            pw.print("<button type=\"button\" class=\"downloadFullZip\" style=\"float: right; margin-right: 30px; margin-top: 5px;\">Download Full Zip</button>");
            pw.print("<button type=\"button\" class=\"downloadFullTxt\" style=\"float: right; margin-right: 30px; margin-top: 5px;\">Download Full Text</button>");

            if ( handler.supports(PrinterMode.ZIP_FILE) ) {
                pw.print("<button type=\"button\" class=\"downloadZip\" style=\"float: right; margin-right: 30px; margin-top: 5px;\">Download As Zip</button>");
            }
            if ( handler.supports(PrinterMode.TEXT ) ) {
                pw.print("<button type=\"button\" class=\"downloadTxt\" style=\"float: right; margin-right: 30px; margin-top: 5px;\">Download As Text</button>");
            }

            pw.println("<br/>&nbsp;</p>"); // status line
            pw.print("<div>");
            if ( handler.supports(PrinterMode.HTML_BODY) ) {
                handler.printConfiguration(PrinterMode.HTML_BODY, pw);
            } else {
                pw.enableFilter( true );
                handler.printConfiguration(PrinterMode.TEXT, pw);
                pw.enableFilter( false );
            }
            pw.print("</div>");
        }
    }

    private abstract static class ConfigurationWriter extends PrintWriter {

        ConfigurationWriter( final Writer delegatee ) {
            super( delegatee );
        }

        public void title( final String title ) throws IOException {
            // dummy implementation
        }


        public void end() throws IOException {
            // dummy implementation
        }

        public void handleAttachments( final String title, final URL[] urls ) throws IOException {
            throw new UnsupportedOperationException( "handleAttachments not supported by this configuration writer: "
                + this );
        }
    }

    private static class HtmlConfigurationWriter extends ConfigurationWriter {

        // whether or not to filter "<" signs in the output
        private boolean doFilter;


        HtmlConfigurationWriter( final Writer delegatee ) {
            super( delegatee );
        }


        void enableFilter( final boolean doFilter ) {
            this.doFilter = doFilter;
        }

        // IE has an issue with white-space:pre in our case so, we write
        // <br/> instead of [CR]LF to get the line break. This also works
        // in other browsers.
        @Override
        public void println() {
            if ( doFilter ) {
                this.write('\n'); // write <br/>
            } else {
                super.println();
            }
        }

        // some VM implementation directly write in underlying stream, instead of
        // delegation to the write() method. So we need to override this, to make
        // sure, that everything is escaped correctly
        @Override
        public void print(final String str) {
            final char[] chars = str.toCharArray();
            write(chars, 0, chars.length);
        }


        private final char[] oneChar = new char[1];

        // always delegate to write(char[], int, int) otherwise in some VM
        // it cause endless cycle and StackOverflowError
        @Override
        public void write(final int character) {
            synchronized (oneChar) {
                oneChar[0] = (char) character;
                write(oneChar, 0, 1);
            }
        }

        // write the characters unmodified unless filtering is enabled in
        // which case the writeFiltered(String) method is called for filtering
        @Override
        public void write(char[] chars, int off, int len) {
            if (doFilter) {
                chars = this.escapeHtml(new String(chars, off, len)).toCharArray();
                off = 0;
                len = chars.length;
            }
            super.write(chars, off, len);
        }

        // write the string unmodified unless filtering is enabled in
        // which case the writeFiltered(String) method is called for filtering
        @Override
        public void write( final String string, final int off, final int len ) {
            write(string.toCharArray(), off, len);
        }

        /**
         * Escapes HTML special chars like: <>&\r\n and space
         *
         *
         * @param text the text to escape
         * @return the escaped text
         */
        private String escapeHtml(final String text) {
            final StringBuilder sb = new StringBuilder(text.length() * 4 / 3);
            char ch, oldch = '_';
            for (int i = 0; i < text.length(); i++) {
                switch (ch = text.charAt(i)) {
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
                        if (oldch != '\r' && oldch != '\n') // don't add twice <br>
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

    private static class PlainTextConfigurationWriter extends ConfigurationWriter {

        PlainTextConfigurationWriter( final Writer delegatee ) {
            super( delegatee );
        }

        @Override
        public void title( final String title ) throws IOException {
            print( "*** " );
            print( title );
            println( ":" );
        }


        @Override
        public void end() throws IOException {
            println();
        }
    }

    private static class ZipConfigurationWriter extends ConfigurationWriter {

        private final ZipOutputStream zip;

        private int counter;


        ZipConfigurationWriter( final ZipOutputStream zip ) {
            super( new OutputStreamWriter( zip ) );
            this.zip = zip;
        }

        @Override
        public void title( final String title ) throws IOException {
            String name = MessageFormat.format( "{0,number,000}-{1}.txt", new Object[]
                { new Integer( counter ), title } );

            counter++;

            final ZipEntry entry = new ZipEntry( name );
            zip.putNextEntry( entry );
        }

        private OutputStream startFile( final String title, final String name) throws IOException {
            final String path = MessageFormat.format( "{0,number,000}-{1}/{2}", new Object[]
                 { new Integer( counter ), title, name } );
            final ZipEntry entry = new ZipEntry( path );
            zip.putNextEntry( entry );

            return zip;
        }

        @Override
        public void handleAttachments( final String title, final URL[] attachments)
        throws IOException {
            for(int i = 0; i < attachments.length; i++) {
                final URL current = attachments[i];
                final String path = current.getPath();
                final String name;
                if ( path == null || path.length() == 0 ) {
                    // sanity code, we should have a path, but if not let's
                    // just create some random name
                    name = "file" + Double.doubleToLongBits( Math.random() );
                } else {
                    final int pos = path.lastIndexOf('/');
                    name = (pos == -1 ? path : path.substring(pos + 1));
                }
                final OutputStream os = this.startFile(title, name);
                final InputStream is = current.openStream();
                try {
                    byte[] buffer = new byte[4096];
                    int n = 0;
                    while (-1 != (n = is.read(buffer))) {
                        os.write(buffer, 0, n);
                    }
                } finally {
                    if ( is != null ) {
                        try { is.close(); } catch (final IOException ignore) {}
                    }
                }
                this.end();
            }

            // increase the filename counter
            counter++;
        }


        @Override
        public void end() throws IOException {
            flush();

            zip.closeEntry();
        }
    }
}