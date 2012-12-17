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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.felix.httplite.osgi.Logger;
import org.apache.felix.httplite.osgi.ServiceRegistration;
import org.apache.felix.httplite.osgi.ServiceRegistrationResolver;


/**
 * This class represents an HTTP request, which is parses from a given input
 * stream, and implements HttpServletRequest for servlet processing.
 **/
public class HttpServletRequestImpl implements HttpServletRequest
{
    /**
     * HTTP Method
     */
    private String m_method;
    /**
     * Host info of URI
     */
    private String m_uriHost;
    /**
     * URI of HTTP request
     */
    private String m_uri;
    /**
     * HTTP  version
     */
    private String m_version;
    /**
     * Headers in HTTP request
     */
    private final Map m_headers = new HashMap();
    private final Socket m_socket;
    private Cookie[] m_cookies;
    //TODO: Make locale static and perhaps global to the service.
    private final Locale m_locale = Locale.getDefault();
    private Map m_attributes;
    private final ServiceRegistrationResolver m_resolver;
    private String m_servletPath;
    /**
     * Map of the parameters of the request.
     */
    private Map m_parameters;

    /**
     * When the body is parsed this value will be set to a non-null value
     * regardless of the body content. As such it serves as a flag for parsing
     * the body.
     */
    private byte[] m_requestBody = null;
    //private final static String m_encoding = "UTF-8";
    private final Logger m_logger;
    private String m_queryString;
    /**
     * Used to enforce the servlet API getInputStream()/getReader() calls.
     */
    private boolean m_getInputStreamCalled = false;
    /**
     * Used to enforce the servlet API getInputStream()/getReader() calls.
     */
    private boolean m_getReaderCalled = false;


    /**
     * @param socket Socket assocated with request
     * @param serviceRegistrationResolver 
     * @param logger
     */
    public HttpServletRequestImpl( final Socket socket, final ServiceRegistrationResolver serviceRegistrationResolver,
        final Logger logger )
    {
        this.m_socket = socket;
        this.m_resolver = serviceRegistrationResolver;
        this.m_logger = logger;
    }


    /**
     * @return The socket this request is associated with.
     */
    protected Socket getSocket()
    {
        return m_socket;
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getInputStream()
     */
    public ServletInputStream getInputStream() throws IOException
    {

        if ( m_getReaderCalled )
        {
            throw new IllegalStateException( "getReader() has already been called." );
        }

        if ( m_requestBody == null )
        {
            parseBody( new BufferedInputStream( m_socket.getInputStream() ) );
        }

        m_getInputStreamCalled = true;

        return new ConcreteServletInputStream( new ByteArrayInputStream( m_requestBody ) );
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getReader()
     */
    public BufferedReader getReader() throws IOException
    {
        if ( m_getInputStreamCalled )
        {
            throw new IllegalStateException( "getInputStream() has already been called." );
        }
        if ( m_requestBody == null )
        {
            parseBody( new BufferedInputStream( m_socket.getInputStream() ) );
        }

        m_getReaderCalled = true;

        return new BufferedReader( new InputStreamReader( new ByteArrayInputStream( m_requestBody ) ) );
    }


    /**
     * This method parses the HTTP request line from the specified input stream
     * and stores the result.
     * 
     * @param is
     *            The input stream from which to read the HTTP request.
     * @throws java.io.IOException
     *             If any I/O error occurs.
     **/
    public void parseRequestLine( final ConcreteServletInputStream is ) throws IOException
    {
        String requestLine = is.readLine();
        if ( requestLine == null )
        {
            throw new IOException( "Unexpected end of file when reading request line." );
        }
        StringTokenizer st = new StringTokenizer( requestLine, " " );
        if ( st.countTokens() != 3 )
        {
            throw new IOException( "Malformed HTTP request: " + requestLine );
        }
        m_method = st.nextToken();
        m_uri = st.nextToken();
        m_version = st.nextToken();

        // If the URI has query string, parse it.
        int qsIdx = m_uri.indexOf( "?" );
        if ( qsIdx > 0 )
        {
            m_queryString = m_uri.substring( qsIdx + 1 );
            m_uri = m_uri.substring( 0, qsIdx );
        }

        // If path contains multiple successive path separators (a//b/c a/b////c, etc.), strip them.
        if ( m_uri.indexOf( "//" ) > -1 )
        {
            // separator

            m_uri = stripRedundantSeparators( m_uri );
        }
    }


    /**
     * Remove successive '/' characters.
     * 
     * @param in input string
     * @return stripped string
     */
    private String stripRedundantSeparators( String in )
    {
        StringBuffer sb = new StringBuffer();
        boolean lastIsSeparator = false;

        for ( int i = 0; i < in.length(); ++i )
        {
            char c = in.charAt( i );

            if ( lastIsSeparator && c == '/' )
            {
                continue;
            }

            sb.append( c );

            if ( c == '/' )
            {
                lastIsSeparator = true;
            }
            else
            {
                lastIsSeparator = false;
            }
        }

        return sb.toString();
    }


    /**
     * This method parses the HTTP header lines from the specified input stream
     * and stores the results.
     * 
     * The map m_headers is populated with two types of values, Strings if the
     * header occurs once or a List in the case that the same header is
     * specified multiple times.
     * 
     * @param is
     *            The input stream from which to read the HTTP header lines.
     * @throws java.io.IOException
     *             If any I/O error occurs.
     **/
    public void parseHeader( final ConcreteServletInputStream is ) throws IOException
    {
        for ( String s = is.readLine(); ( s != null ) && ( s.length() != 0 ); s = is.readLine() )
        {
            int idx = s.indexOf( ":" );
            if ( idx > 0 )
            {
                String header = s.substring( 0, idx ).trim();
                String value = s.substring( idx + 1 ).trim();

                String key = header.toLowerCase();

                if ( !m_headers.containsKey( key ) )
                {
                    m_headers.put( key, value );
                }
                else
                {
                    Object originalValue = m_headers.get( key );

                    if ( originalValue instanceof String )
                    {
                        List headerList = new ArrayList();
                        headerList.add( originalValue );
                        headerList.add( value );
                        m_headers.put( key, headerList );
                    }
                    else if ( originalValue instanceof List )
                    {
                        ( ( List ) originalValue ).add( value );
                    }
                    else
                    {
                        throw new RuntimeException( "Unexpected type in m_headers: "
                            + originalValue.getClass().getName() );
                    }
                }
            }
        }

        if ( m_headers.containsKey( "Host" ) )
        {
            m_uriHost = m_headers.get( "Host" ).toString();
        }
    }


    /**
     * This method parses the HTTP body from the specified input stream and
     * ignores the result.
     * 
     * @param is
     *            The input stream from which to read the HTTP body.
     * @throws java.io.IOException
     *             If any I/O error occurs.
     **/
    public void parseBody( final InputStream is ) throws IOException
    {
        int length = getContentLength();

        if ( length > 0 )
        {
            ByteArrayOutputStream baos = null;

            byte[] buf = new byte[length];
            int left = length;

            do
            {
                left = left - is.read( buf );
                if ( left > 0 )
                {
                    if ( baos == null )
                    {
                        baos = new ByteArrayOutputStream( length );
                    }
                    baos.write( buf );
                }
            }
            while ( left > 0 );

            if ( baos != null )
            {
                m_requestBody = baos.toByteArray();
            }
            else
            {
                m_requestBody = buf;
            }
        }
        else
        {
            // Set this to a non-null value so we know that the body has been
            // parsed.
            m_requestBody = new byte[0];
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getMethod()
     */

    public String getMethod()
    {
        return m_method;
    }


    /**
     * Returns the value of the specified header, if present.
     * 
     * @param header
     *            The header value to retrieve.
     * @return The value of the specified header or <tt>null</tt>.
     **/

    public String getHeader( final String header )
    {
        Object value = m_headers.get( header.toLowerCase() );

        if ( value == null )
        {
            return null;
        }

        return value.toString();
    }


    public Enumeration getHeaders( final String name )
    {
        Object v = m_headers.get( name );

        if ( v == null )
        {
            return HttpConstants.EMPTY_ENUMERATION;
        }

        if ( v instanceof String )
        {
            return Collections.enumeration( Arrays.asList( new String[]
                { ( String ) v } ) );
        }

        if ( v instanceof List )
        {
            return Collections.enumeration( ( List ) v );
        }

        throw new RuntimeException( "Unexpected type in m_headers: " + v.getClass().getName() );
    }


    public Enumeration getHeaderNames()
    {
        if ( m_headers.isEmpty() )
        {
            return HttpConstants.EMPTY_ENUMERATION;
        }

        return Collections.enumeration( m_headers.keySet() );
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getAttribute(java.lang.String)
     */
    public Object getAttribute( final String arg0 )
    {
        if ( m_attributes != null )
        {
            return m_attributes.get( arg0 );
        }

        return null;
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getAttributeNames()
     */

    public Enumeration getAttributeNames()
    {
        if ( m_attributes != null )
        {
            return Collections.enumeration( m_attributes.keySet() );
        }

        return HttpConstants.EMPTY_ENUMERATION;
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getCharacterEncoding()
     */
    public String getCharacterEncoding()
    {
        return getHeader( "Accept-Encoding" );
    }


    public int getContentLength()
    {
        int len = 0;

        try
        {
            len = Integer.parseInt( getHeader( "Content-Length" ) );
        }
        catch ( NumberFormatException e )
        {
            // Ignore this exception intentionally.
        }

        return len;
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getContentType()
     */
    public String getContentType()
    {
        return getHeader( "Content-Type" );
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getLocale()
     */
    public Locale getLocale()
    {
        return m_locale;
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getLocales()
     */
    public Enumeration getLocales()
    {
        return Collections.enumeration( Arrays.asList( new Object[]
            { m_locale } ) );
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
     */
    public String getParameter( final String arg0 )
    {
        if ( m_parameters == null )
        {
            try
            {
                m_parameters = parseParameters();
            }
            catch ( UnsupportedEncodingException e )
            {
                m_logger.log( Logger.LOG_ERROR, "Failed to parse request parameters.", e );
                return null;
            }
        }

        return ( String ) m_parameters.get( arg0 );
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getParameterMap()
     */
    public Map getParameterMap()
    {
        if ( m_parameters == null )
        {
            try
            {
                m_parameters = parseParameters();
            }
            catch ( UnsupportedEncodingException e )
            {
                m_logger.log( Logger.LOG_ERROR, "Failed to parse request parameters.", e );
                return null;
            }
        }

        return m_parameters;
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getParameterNames()
     */
    public Enumeration getParameterNames()
    {
        if ( m_parameters == null )
        {
            try
            {
                m_parameters = parseParameters();
            }
            catch ( UnsupportedEncodingException e )
            {
                m_logger.log( Logger.LOG_ERROR, "Failed to parse request parameters.", e );
                return null;
            }
        }

        return Collections.enumeration( m_parameters.keySet() );
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getParameterValues(java.lang.String)
     */
    public String[] getParameterValues( String arg0 )
    {
        if ( m_parameters == null )
        {
            try
            {
                m_parameters = parseParameters();
            }
            catch ( UnsupportedEncodingException e )
            {
                m_logger.log( Logger.LOG_ERROR, "Failed to parse request parameters.", e );
                return null;
            }
        }

        return ( String[] ) m_parameters.values().toArray( new String[m_parameters.size()] );
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getProtocol()
     */
    public String getProtocol()
    {
        return m_version;
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
     */
    public String getRealPath( final String arg0 )
    {
        throw new UnimplementedAPIException();
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletRequest#getRemoteAddr()
     */
    public String getRemoteAddr()
    {
        return getSocket().getInetAddress().getHostAddress();
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#getRemoteHost()
     */

    public String getRemoteHost()
    {
        return getSocket().getInetAddress().getHostName();
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
     */
    public RequestDispatcher getRequestDispatcher( String arg0 )
    {
        return null;
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#getScheme()
     */
    public String getScheme()
    {
        return HttpConstants.HTTP_SCHEME;
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#getServerName()
     */
    public String getServerName()
    {
        return HttpConstants.SERVER_INFO;
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#getServerPort()
     */
    public int getServerPort()
    {
        return getSocket().getLocalPort();
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#isSecure()
     */
    public boolean isSecure()
    {
        return false;
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#removeAttribute(java.lang.String)
     */
    public void removeAttribute( String arg0 )
    {
        if ( m_attributes != null )
        {
            m_attributes.remove( arg0 );
        }
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute( String arg0, Object arg1 )
    {
        if ( m_attributes == null )
        {
            m_attributes = new HashMap();
        }

        m_attributes.put( arg0, arg1 );
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#setCharacterEncoding(java.lang.String)
     */

    public void setCharacterEncoding( String arg0 ) throws UnsupportedEncodingException
    {
        throw new UnimplementedAPIException();
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getAuthType()
     */
    public String getAuthType()
    {
        return null;
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getCookies()
     */
    public Cookie[] getCookies()
    {
        if ( m_cookies == null )
        {

            String cookieHeader = getHeader( "Cookie" );

            if ( cookieHeader == null )
            {
                return null;
            }

            List cookieList = new ArrayList();

            for ( Iterator i = Arrays.asList( splitString( cookieHeader, ";" ) ).iterator(); i.hasNext(); )
            {
                String[] nvp = splitString( i.next().toString(), "=" );

                if ( nvp.length != 2 )
                {
                    //Ignore invalid cookie and and continue.
                    continue;
                }

                cookieList.add( new Cookie( nvp[0].trim(), nvp[1].trim() ) );
            }
            m_cookies = ( Cookie[] ) cookieList.toArray( new Cookie[cookieList.size()] );
        }

        return m_cookies;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.servlet.http.HttpServletRequest#getDateHeader(java.lang.String)
     */
    public long getDateHeader( final String name )
    {
        String headerValue = getHeader( name );

        if ( headerValue == null )
        {
            return -1;
        }

        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat(HttpConstants.HTTP_DATE_FORMAT);
            sdf.setTimeZone(TimeZone.getTimeZone(HttpConstants.HTTP_TIMEZONE));
            return sdf.parse( headerValue ).getTime();
        }
        catch ( ParseException e )
        {
            throw new IllegalArgumentException( "Unable to convert to date: " + headerValue );
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getIntHeader(java.lang.String)
     */
    public int getIntHeader( final String name )
    {
        String value = getHeader( name );

        if ( value == null )
        {
            return -1;
        }

        return Integer.parseInt( value );
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getPathInfo()
     */
    public String getPathInfo()
    {
        String alias = getAlias();

        if ( m_uri != null && alias.length() > 0 )
        {
            if ( m_uri.length() == alias.length() )
            {
                return null;
            }

            return m_uri.substring( alias.length() );
        }

        return null;
    }


    public String getPathTranslated()
    {
        // TODO: Always returning null may be incorrect.
        return null;
    }


    public String getContextPath()
    {
        return "";
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getQueryString()
     */
    public String getQueryString()
    {
        return m_queryString;
    }


    public String getRemoteUser()
    {
        return null;
    }


    public boolean isUserInRole( String role )
    {
        return false;
    }


    public Principal getUserPrincipal()
    {
        return null;
    }


    public String getRequestedSessionId()
    {
        return null;
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getRequestURI()
     */
    public String getRequestURI()
    {
        return m_uri;
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getRequestURL()
     */
    public StringBuffer getRequestURL()
    {
        StringBuffer sb = new StringBuffer();
        if ( m_uriHost != null )
        {
            sb.append( m_uriHost );
        }
        sb.append( m_uri );

        return sb;
    }


    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServletRequest#getServletPath()
     */
    public String getServletPath()
    {
        if ( m_servletPath == null )
        {
            ServiceRegistration element = m_resolver.getServiceRegistration( m_uri );

            if ( element == null )
            {
                throw new IllegalStateException( "Unable to get ServletElement for HttpRequest." );
            }

            m_servletPath = element.getAlias();
        }

        return m_servletPath;
    }


    /**
     * @return Alias associated with this request
     */
    private String getAlias()
    {
        ServiceRegistration element = m_resolver.getServiceRegistration( m_uri );

        if ( element == null )
        {
            throw new IllegalStateException( "Unable to get ServletElement for HttpRequest." );
        }

        return element.getAlias();
    }


    public HttpSession getSession( boolean create )
    {
        throw new UnimplementedAPIException();
    }


    public HttpSession getSession()
    {
        throw new UnimplementedAPIException();
    }


    public boolean isRequestedSessionIdValid()
    {
        throw new UnimplementedAPIException();
    }


    public boolean isRequestedSessionIdFromCookie()
    {
        throw new UnimplementedAPIException();
    }


    public boolean isRequestedSessionIdFromURL()
    {
        throw new UnimplementedAPIException();
    }


    public boolean isRequestedSessionIdFromUrl()
    {
        throw new UnimplementedAPIException();
    }


    /**
     * Parse the parameters in the request and return as a Map of <String,
     * String>.
     * 
     * @return
     * @throws UnsupportedEncodingException
     */
    private Map parseParameters() throws UnsupportedEncodingException
    {
        Map params = new HashMap();

        String queryString = getQueryString();

        if ( queryString != null && queryString.length() > 0 )
        {
            parseParameterString( queryString, params );
        }

        if ( m_requestBody != null && m_requestBody.length > 0 )
        {
            parseParameterString( new String( m_requestBody ), params );
        }

        return Collections.unmodifiableMap( params );
    }


    /**
     * 
     * @param queryString
     *            A String formatted like: 'home=Cosby&favorite+flavor=flies'
     * @param params
     *            Map of <String, String> of existing parameters to be added to.
     * 
     * @throws UnsupportedEncodingException
     *             if encoding type is unsupported
     */
    private void parseParameterString( final String queryString, final Map params ) throws UnsupportedEncodingException
    {
        String[] parameters = splitString( queryString, "&" );
        
        for ( Iterator i = Arrays.asList( parameters ).iterator(); i.hasNext(); )
        {
            String[] nva = splitString( i.next().toString(), "=" );

            if ( nva.length == 2 )
            {
                //Deprecated method decode() intentionally used for Java 1.3 compatibility.
                params.put( URLDecoder.decode( nva[0].trim() ), nva[1].trim() );
            }
        }
    }


    /**
     * @param method
     *            HTTP method
     * @return true if the psased HTTP method is supported by this server.
     */
    public static boolean isSupportedMethod( final String method )
    {
        if ( method.equals( HttpConstants.OPTIONS_REQUEST ) )
        {
            return false;
        }

        return true;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        if ( m_method != null && m_uri != null )
        {
            return m_method + " " + m_uri;
        }

        return super.toString();
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#getLocalAddr()
     */
    public String getLocalAddr()
    {
        return m_socket.getLocalAddress().getHostAddress();
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#getLocalName()
     */
    public String getLocalName()
    {
        return m_socket.getLocalAddress().getHostName();
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#getLocalPort()
     */
    public int getLocalPort()
    {
        return m_socket.getLocalPort();
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletRequest#getRemotePort()
     */
    public int getRemotePort()
    {
        return m_socket.getPort();
    }


    /**
     * Split a string into substrings based on delimiter character.
     * 
     * @param instr input string
     * @param separator separator char
     * @return array of substrings
     * @throws IllegalArgumentException if instr parameter is null.
     */
    private static String[] splitString( String instr, String separator )
    {
		if ( instr == null )
        {
            throw new IllegalArgumentException( "Input string must not be null." );
        }

	    StringTokenizer tokenizer = new StringTokenizer(instr, separator);
        int length = tokenizer.countTokens();
		String[] str_array = new String[length];
        for ( int i = 0; i < length; i++ )
        {
            str_array[i] = tokenizer.nextToken();
        }
        return str_array;
    }
}
