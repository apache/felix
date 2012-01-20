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
package org.apache.felix.httplite.servlet;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * This class represents an HTTP response and handles sending properly
 * formatted responses to HTTP requests.
**/
public class HttpServletResponseImpl implements HttpServletResponse
{
    private static final int COPY_BUFFER_SIZE = 1024 * 4;

    private final SimpleDateFormat m_dateFormat;
    private final OutputStream m_out;
    private int m_bufferSize = COPY_BUFFER_SIZE;
    private ByteArrayOutputStream m_buffer;
    private final Map m_headers = new HashMap();
    private String m_characterEncoding = "UTF-8";
    //TODO: Make locale static and perhaps global to the service.
    private Locale m_locale = new Locale(System.getProperty("user.language"), System.getProperty( "user.country" ));
    private boolean m_getOutputStreamCalled = false;
    private boolean m_getWriterCalled = false;
    private ServletOutputStreamImpl m_servletOutputStream;
    private PrintWriter m_printWriter;
    private List m_cookies = null;

    private int m_statusCode = HttpURLConnection.HTTP_OK;
    private String m_customStatusMessage = null;
    private boolean m_headersWritten = false;

    /**
     * Constructs an HTTP response for the specified server and request.
     * @param outputStream The output stream for the client.
    **/
    public HttpServletResponseImpl(OutputStream outputStream)
    {
        m_out = outputStream;
        m_dateFormat = new SimpleDateFormat(HttpConstants.HTTP_DATE_FORMAT);
        m_dateFormat.setTimeZone(TimeZone.getTimeZone(HttpConstants.HTTP_TIMEZONE));
    }

    /**
     * Write HTTP headers to output stream.
     * 
     * @param close if true, should save state, only allow headers to be written once.  Does not close the stream.
     * @throws IOException on I/O error
     */
    void writeHeaders(boolean close) throws IOException
    {
        if (m_headersWritten)
        {
            throw new IllegalStateException("Headers have already been written.");
        }

        if (!m_headers.containsKey(HttpConstants.HEADER_CONTENT_LENGTH)
            && m_buffer != null)
        {
            setContentLength(m_buffer.size());
        }

        m_out.write(buildResponse(m_statusCode, m_headers, m_customStatusMessage, null));
        
        if (m_cookies != null)
        {
            m_out.write( "Set-Cookie: ".getBytes() );
            for (Iterator i = m_cookies.iterator(); i.hasNext();)
            {
                Cookie cookie = (Cookie) i.next();
                m_out.write( cookieToHeader(cookie) );
                
                if (i.hasNext()) {
                    m_out.write( ';' );
                }
            }
        }
        m_out.write(HttpConstants.HEADER_DELEMITER.getBytes());
        m_out.flush();

        if (close)
        {
            m_headersWritten = true;
        }
        else
        {
            m_headers.clear();
        }
    }

    /**
     * Copy the contents of the input to the output stream, then close the input stream.
     * @param inputStream input stream
     * @param close if connection should be closed 
     * @throws IOException on I/O error
     */
    public void writeToOutputStream(final InputStream inputStream, final boolean close)
        throws IOException
    {
        InputStream bufferedInput = new BufferedInputStream(inputStream);

        if (!m_headers.containsKey(HttpConstants.HEADER_CONTENT_LENGTH))
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            copy(bufferedInput, baos);
            byte[] outputBuffer = baos.toByteArray();

            setContentLength(outputBuffer.length);
            bufferedInput = new ByteArrayInputStream(outputBuffer);
        }

        if (!m_headersWritten)
        {
            writeHeaders(close);
        }

        try
        {
            copy(bufferedInput, m_out);

            m_out.flush();
        }
        finally
        {
            if (bufferedInput != null)
            {
                bufferedInput.close();
            }
        }
    }

    /**
     * Copy an input stream to an output stream.
     * 
     * @param input InputStream
     * @param output OutputStream
     * @throws IOException on I/O error.
     */
    public static void copy(final InputStream input, final OutputStream output)
        throws IOException
    {

        byte[] buf = new byte[COPY_BUFFER_SIZE];
        for (int len = input.read(buf); len >= 0; len = input.read(buf))
        {
            output.write(buf, 0, len);
        }
    }

    /**
     * Static utility method to send a continue response.
     * @throws java.io.IOException If any I/O error occurs.
    **/
    public void sendContinueResponse() throws IOException
    {
        m_out.write(buildResponse(HttpConstants.HTTP_RESPONSE_CONTINUE));
        m_out.flush();
    }

    /**
     * Static utility method to send a missing host response.
     * @throws java.io.IOException If any I/O error occurs.
    **/
    public void sendMissingHostResponse() throws IOException
    {
        m_out.write(buildResponse(HttpURLConnection.HTTP_BAD_REQUEST));
        m_out.flush();
    }

    /**
     * Static utility method to send a not implemented response.
     * @throws java.io.IOException If any I/O error occurs.
    **/
    public void sendNotImplementedResponse() throws IOException
    {
        m_out.write(buildResponse(HttpURLConnection.HTTP_NOT_IMPLEMENTED));
        m_out.flush();
    }

    /**
     * Static utility method to send a moved permanently response.
     * @param hostname The hostname of the new location.
     * @param port The port of the new location.
     * @param newURI The path of the new location.
     * @throws java.io.IOException If any I/O error occurs.
    **/
    public void sendMovedPermanently(final String hostname, final int port,
        final String newURI) throws IOException
    {
        StringBuffer sb = new StringBuffer();
        sb.append(HttpConstants.HEADER_LOCATION);
        sb.append(HttpConstants.HEADER_VALUE_DELIMITER);
        sb.append(HttpConstants.HTTP_SCHEME);
        sb.append("://");
        sb.append(hostname);
        if (port != 80)
        {
            sb.append(':');
            sb.append(Integer.toString(port));
        }
        sb.append(newURI);
        sb.append(HttpConstants.HEADER_DELEMITER);

        m_out.write(buildResponse(301, null, sb.toString(), null));
        m_out.flush();
    }

    /**
     * Static utility method to send a Not Found (404) response.
     * @throws java.io.IOException If any I/O error occurs.
    **/
    public void sendNotFoundResponse() throws IOException
    {
        m_out.write(buildResponse(HttpURLConnection.HTTP_NOT_FOUND));
        m_out.flush();
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#flushBuffer()
     */
    synchronized public void flushBuffer() throws IOException
    {
        if (m_getOutputStreamCalled)
        {
            m_servletOutputStream.flush();
        }
        else if (m_getWriterCalled)
        {
            m_printWriter.flush();
        }

        if (!m_headersWritten)
        {
            writeHeaders(true);
        }

        if (m_buffer != null)
        {
            byte[] content = m_buffer.toByteArray();
            copy(new ByteArrayInputStream(content), m_out);
            m_out.flush();
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#getBufferSize()
     */
    public int getBufferSize()
    {
        if (m_buffer != null)
        {
            return m_buffer.size();
        }

        return m_bufferSize;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#getCharacterEncoding()
     */
    public String getCharacterEncoding()
    {
        return m_characterEncoding;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#getContentType()
     */
    public String getContentType()
    {
        Object contentType = m_headers.get(HttpConstants.HEADER_CONTENT_TYPE);

        if (contentType != null)
        {
            return contentType.toString();
        }

        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#getLocale()
     */
    public Locale getLocale()
    {
        return m_locale;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#getOutputStream()
     */
    public ServletOutputStream getOutputStream() throws IOException
    {
        m_getOutputStreamCalled = true;

        if (m_getWriterCalled)
            throw new IllegalStateException(
                "getWriter method has already been called for this response object.");

        if (m_servletOutputStream == null)
        {
            m_buffer = new ByteArrayOutputStream(m_bufferSize);
            m_servletOutputStream = new ServletOutputStreamImpl(m_buffer);
        }
        return m_servletOutputStream;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#getWriter()
     */
    public PrintWriter getWriter() throws IOException
    {
        m_getWriterCalled = true;

        if (m_getOutputStreamCalled)
            throw new IllegalStateException(
                "getOutputStream method has already been called for this response object.");

        if (m_printWriter == null)
        {
            m_buffer = new ByteArrayOutputStream(m_bufferSize);
            m_printWriter = new PrintWriter(m_buffer);
        }

        return m_printWriter;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#isCommitted()
     */
    public boolean isCommitted()
    {
        return m_headersWritten;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#reset()
     */
    public void reset()
    {
        if (isCommitted())
        {
            throw new IllegalStateException("Response has already been committed.");
        }
        m_buffer.reset();
        m_printWriter = null;
        m_servletOutputStream = null;
        m_getOutputStreamCalled = false;
        m_getWriterCalled = false;
        m_headers.clear();
        m_statusCode = HttpURLConnection.HTTP_OK;
        m_customStatusMessage = null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#resetBuffer()
     */
    public void resetBuffer()
    {
        if (isCommitted())
        {
            throw new IllegalStateException("Response has already been committed.");
        }

        m_buffer.reset();
        m_printWriter = null;
        m_servletOutputStream = null;
        m_getOutputStreamCalled = false;
        m_getWriterCalled = false;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#setBufferSize(int)
     */
    public void setBufferSize(final int arg0)
    {
        if (isCommitted())
        {
            throw new IllegalStateException("Response has already been committed.");
        }

        m_bufferSize = arg0;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#setCharacterEncoding(java.lang.String)
     */
    public void setCharacterEncoding(final String arg0)
    {
        m_characterEncoding = arg0;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#setContentLength(int)
     */
    public void setContentLength(final int arg0)
    {
        m_headers.put(HttpConstants.HEADER_CONTENT_LENGTH, Integer.toString(arg0));
    }

    /**
     * Can be 'close' or 'Keep-Alive'.
     * @param type
     */
    public void setConnectionType(final String type)
    {
        setHeader(HttpConstants.HEADER_CONNECTION, type);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#setContentType(java.lang.String)
     */
    public void setContentType(final String arg0)
    {
        setHeader(HttpConstants.HEADER_CONTENT_TYPE, arg0);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#setLocale(java.util.Locale)
     */
    public void setLocale(final Locale arg0)
    {
        m_locale = arg0;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#addCookie(javax.servlet.http.Cookie)
     */
    public void addCookie(final Cookie cookie)
    {
        if (m_cookies == null)
        {
            m_cookies = new ArrayList();
        }
        
        if (!m_cookies.contains( cookie ))
        {
            m_cookies.add( cookie );
        }        
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#containsHeader(java.lang.String)
     */
    public boolean containsHeader(final String name)
    {
        return m_headers.get(name) != null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#encodeURL(java.lang.String)
     */
    public String encodeURL(final String url)
    {
        // TODO Re-examing if/when sessions are supported.
        return url;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#encodeRedirectURL(java.lang.String)
     */
    public String encodeRedirectURL(final String url)
    {
        return encodeUrl(url);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#encodeUrl(java.lang.String)
     */
    public String encodeUrl(final String url)
    {      
        //Deprecated method used for Java 1.3 compatibility.
        return URLEncoder.encode(url);       
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#encodeRedirectUrl(java.lang.String)
     */
    public String encodeRedirectUrl(String url)
    {
        return encodeURL(url);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#sendError(int, java.lang.String)
     */

    public void sendError(final int sc, final String msg) throws IOException
    {
        if (m_headersWritten)
            throw new IllegalStateException(
                "Response has already been committed, unable to send error.");

        m_out.write(buildResponse(sc, msg));
        m_out.flush();
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#sendError(int)
     */
    public void sendError(final int sc) throws IOException
    {
        sendError(sc, null);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#sendRedirect(java.lang.String)
     */
    public void sendRedirect(final String location) throws IOException
    {
        if (m_headersWritten)
        {
            throw new IllegalStateException("Response has already been committed.");
        }

        Map map = new HashMap();
        map.put("Location", location);
        m_out.write(buildResponse(307, map, null, null));
        m_out.flush();
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#setDateHeader(java.lang.String, long)
     */
    public void setDateHeader(final String name, final long date)
    {
        setHeader(name, m_dateFormat.format(new Date(date)));
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#addDateHeader(java.lang.String, long)
     */
    public void addDateHeader(final String name, final long date)
    {
        addHeader(name, m_dateFormat.format(new Date(date)));
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#setHeader(java.lang.String, java.lang.String)
     */

    public void setHeader(final String name, final String value)
    {
        if (value != null)
        {
            m_headers.put(name, value);
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#addHeader(java.lang.String, java.lang.String)
     */

    public void addHeader(final String name, final String value)
    {
        if (value != null && m_headers.containsKey(name))
        {
            Object pvalue = m_headers.get(name);

            if (pvalue instanceof List)
            {
                ((List) pvalue).add(value);
            }
            else
            {
                List vlist = new ArrayList();
                vlist.add(pvalue);
                vlist.add(value);
            }
        }
        else
        {
            setHeader(name, value);
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#setIntHeader(java.lang.String, int)
     */
    public void setIntHeader(final String name, final int value)
    {
        setHeader(name, Integer.toString(value));
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#addIntHeader(java.lang.String, int)
     */
    public void addIntHeader(final String name, final int value)
    {
        addHeader(name, Integer.toString(value));
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#setStatus(int)
     */
    public void setStatus(final int sc)
    {
        m_statusCode = sc;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServletResponse#setStatus(int, java.lang.String)
     */
    public void setStatus(int sc, String sm)
    {
        m_statusCode = sc;
        m_customStatusMessage = sm;
    }

    /**
     * @param code HTTP code
     * @return byte array of response
     */
    public static byte[] buildResponse(int code)
    {
        return buildResponse(code, null, null, HttpConstants.DEFAULT_HTML_HEADER);
    }

    /**
     * @param code HTTP code
     * @param userMessage user message
     * @return byte array of response
     */
    public static byte[] buildResponse(int code, String userMessage)
    {
        return buildResponse(code, null, userMessage, HttpConstants.DEFAULT_HTML_HEADER);
    }

    /**
     * Build a response given input parameters.
     * 
     * @param code HTTP code
     * @param headers Map of HTTP headers
     * @param userMessage user message
     * @param htmlStartTag custom HTML document start
     * @return byte array of response.
     */
    public static byte[] buildResponse(int code, Map headers, String userMessage,
        String htmlStartTag)
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append(HttpConstants.HTTP11_VERSION);
        buffer.append(' ');
        buffer.append(code);
        buffer.append(' ');
        
        if (code > 399)
        {
	        buffer.append("HTTP Error ");
	        buffer.append(code);
        }
        buffer.append(HttpConstants.HEADER_DELEMITER);
        if (code == 100)
        {
            buffer.append(HttpConstants.HEADER_DELEMITER);
        }
        else if (headers != null)
        {
            if (headers.containsKey(HttpConstants.HEADER_CONTENT_TYPE))
            {
                appendHeader(buffer, HttpConstants.HEADER_CONTENT_TYPE,
                    headers.get(HttpConstants.HEADER_CONTENT_TYPE).toString());
            }

            for (Iterator i = headers.entrySet().iterator(); i.hasNext();)
            {
                Map.Entry entry = (Map.Entry) i.next();

                if (entry.getValue() == null)
                {
                    throw new IllegalStateException(
                        "Header map contains value with null value: " + entry.getKey());
                }

                appendHeader(buffer, entry.getKey().toString(),
                    entry.getValue().toString());
            }
        }

        //Only append error HTML messages if the return code is in the error range.
        if (code > 399)
        {
        	//TODO: Consider disabling the HTML generation, optionally, so clients have full control of the response content.
            if (htmlStartTag == null)
            {
                htmlStartTag = HttpConstants.DEFAULT_HTML_HEADER;
            }
            buffer.append(htmlStartTag);
            buffer.append("<h1>");
            buffer.append(code);
            buffer.append(' ');
            buffer.append("HTTP Error ");
            buffer.append(code);

            if (userMessage != null)
            {
                buffer.append("</h1><p>");
                buffer.append(userMessage);
                buffer.append("</p>");
            }
            else
            {
                buffer.append("</h1>");
            }

            buffer.append("<h3>" + HttpConstants.SERVER_INFO + "</h3></html>");
        }

        return buffer.toString().getBytes();
    }
    
    /**
     * Convert a cookie into the HTTP header in response.
     * 
     * @param cookie Cookie
     * @return String as byte array of cookie as header
     */
    private byte[] cookieToHeader( Cookie cookie )
    {
        if (cookie == null || cookie.getName() == null || cookie.getValue() == null) 
        {
            throw new IllegalArgumentException( "Invalid cookie" );
        }
        
        StringBuffer sb = new StringBuffer();
                
        sb.append( cookie.getName() );
        sb.append( '=' );
        sb.append( cookie.getValue() );
        
        //TODO: Implement all Cookie fields
            
        return sb.toString().getBytes();
    }

    /**
     * Append name and value as an HTTP header to a StringBuffer
     * 
     * @param sb StringBuffer
     * @param name Name 
     * @param value Value
     */
    private static void appendHeader(StringBuffer sb, String name, String value)
    {
        sb.append(name);
        sb.append(HttpConstants.HEADER_VALUE_DELIMITER);
        sb.append(value);
        sb.append(HttpConstants.HEADER_DELEMITER);
    }
}