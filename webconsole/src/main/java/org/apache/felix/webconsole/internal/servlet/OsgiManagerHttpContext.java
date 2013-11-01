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
package org.apache.felix.webconsole.internal.servlet;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.WebConsoleSecurityProvider;
import org.apache.felix.webconsole.WebConsoleSecurityProvider2;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;


final class OsgiManagerHttpContext implements HttpContext
{

    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";

    private final HttpContext base;

    private final ServiceTracker tracker;

    private final String username;

    private final Password password;

    private final String realm;


    OsgiManagerHttpContext( HttpService httpService, final ServiceTracker tracker, final String username,
        final String password, final String realm )
    {
        this.tracker = tracker;
        this.username = username;
        this.password = new Password(password);
        this.realm = realm;
        this.base = httpService.createDefaultHttpContext();
    }


    public String getMimeType( String name )
    {
        return this.base.getMimeType( name );
    }


    public URL getResource( String name )
    {
        URL url = this.base.getResource( name );
        if ( url == null && name.endsWith( "/" ) )
        {
            return this.base.getResource( name.substring( 0, name.length() - 1 ) );
        }
        return url;
    }


    /**
     * Checks the <code>Authorization</code> header of the request for Basic
     * authentication user name and password. If contained, the credentials are
     * compared to the user name and password set for the OSGi Console.
     * <p>
     * If no user name is set, the <code>Authorization</code> header is
     * ignored and the client is assumed to be authenticated.
     *
     * @param request The HTTP request used to get the
     *            <code>Authorization</code> header.
     * @param response The HTTP response used to send the authentication request
     *            if authentication is required but not satisfied.
     * @return <code>true</code> if authentication is required and not
     *         satisfied by the request.
     */
    public boolean handleSecurity( HttpServletRequest request, HttpServletResponse response )
    {
        Object provider = tracker.getService();

        // check whether the security provider can fully handle the request
        if ( provider instanceof WebConsoleSecurityProvider2 )
        {
            return ( ( WebConsoleSecurityProvider2 ) provider ).authenticate( request, response );
        }

        // Return immediately if the header is missing
        String authHeader = request.getHeader( HEADER_AUTHORIZATION );
        if ( authHeader != null && authHeader.length() > 0 )
        {

            // Get the authType (Basic, Digest) and authInfo (user/password)
            // from
            // the header
            authHeader = authHeader.trim();
            int blank = authHeader.indexOf( ' ' );
            if ( blank > 0 )
            {
                String authType = authHeader.substring( 0, blank );
                String authInfo = authHeader.substring( blank ).trim();

                // Check whether authorization type matches
                if ( authType.equalsIgnoreCase( AUTHENTICATION_SCHEME_BASIC ) )
                {
                    try
                    {
                        byte[][] userPass = base64Decode( authInfo );
                        final String username = toString( userPass[0] );

                        // authenticate
                        if ( authenticate( provider, username, userPass[1] ) )
                        {
                            // as per the spec, set attributes
                            request.setAttribute( HttpContext.AUTHENTICATION_TYPE, HttpServletRequest.BASIC_AUTH );
                            request.setAttribute( HttpContext.REMOTE_USER, username );

                            // set web console user attribute
                            request.setAttribute( WebConsoleSecurityProvider2.USER_ATTRIBUTE, username );

                            // succeed
                            return true;
                        }
                    }
                    catch ( Exception e )
                    {
                        // Ignore
                    }
                }
            }
        }

        // request authentication
        try
        {
            response.setHeader( HEADER_WWW_AUTHENTICATE, AUTHENTICATION_SCHEME_BASIC + " realm=\"" + this.realm + "\"" );
            response.setStatus( HttpServletResponse.SC_UNAUTHORIZED );
            response.setContentLength( 0 );
            response.flushBuffer();
        }
        catch ( IOException ioe )
        {
            // failed sending the response ... cannot do anything about it
        }

        // inform HttpService that authentication failed
        return false;
    }


    public boolean authorize( final HttpServletRequest request, String role )
    {
        Object user = request.getAttribute( WebConsoleSecurityProvider2.USER_ATTRIBUTE );
        if ( user != null )
        {
            WebConsoleSecurityProvider provider = ( WebConsoleSecurityProvider ) tracker.getService();
            if ( provider != null )
            {
                return provider.authorize( user, role );
            }

            // no provider, grant access (backwards compatibility)
            return true;
        }

        // missing user in the request, deny access
        return false;
    }


    private static byte[][] base64Decode( String srcString )
    {
        byte[] transformed = Base64.decodeBase64( srcString );
        for ( int i = 0; i < transformed.length; i++ )
        {
            if ( transformed[i] == ':' )
            {
                byte[] user = new byte[i];
                byte[] pass = new byte[transformed.length - i - 1];
                System.arraycopy( transformed, 0, user, 0, user.length );
                System.arraycopy( transformed, i + 1, pass, 0, pass.length );
                return new byte[][]
                    { user, pass };
            }
        }

        return new byte[][]
            { transformed, new byte[0] };
    }


    private static String toString( final byte[] src )
    {
        try
        {
            return new String( src, "ISO-8859-1" );
        }
        catch ( UnsupportedEncodingException uee )
        {
            return new String( src );
        }
    }


    private boolean authenticate( Object provider, String username, byte[] password )
    {
        if ( provider != null )
        {
            return ( ( WebConsoleSecurityProvider ) provider ).authenticate( username, toString( password ) ) != null;
        }
        if ( this.username.equals( username ) && this.password.matches( password ) )
        {
            return true;
        }
        return false;
    }

}