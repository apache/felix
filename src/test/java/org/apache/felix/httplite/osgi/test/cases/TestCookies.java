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
package org.apache.felix.httplite.osgi.test.cases;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.httplite.osgi.test.AbstractHttpliteTestCase;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;


/**
 * Test that servlet container handles cookies correctly.
 *
 */
public class TestCookies extends AbstractHttpliteTestCase
{
    private static final int MODE_NONE_SET = 1;
    private static final int MODE_SIMPLE = 2;
    private static final int MODE_MULTI = 0;


    //TODO: Test cookie expiration

    /**
     * Test that no cookies are sent by default.
     * 
     * @throws ServletException
     * @throws NamespaceException
     * @throws IOException
     */
    public void testNoCookiesSentByDefault() throws ServletException, NamespaceException, IOException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );
        CookieServlet servlet = new CookieServlet( MODE_NONE_SET );
        httpService.registerServlet( "/test", servlet, null, null );

        //Test that no cookies are currently set.
        HttpURLConnection client = getConnection( DEFAULT_BASE_URL + "/test", "GET" );

        client.connect();
        assertTrue( client.getResponseCode() == 200 );

    }


    /**
     * Test creating and sending one cookie.
     * 
     * @throws ServletException
     * @throws NamespaceException
     * @throws IOException
     */
    public void testSimpleCookie() throws ServletException, NamespaceException, IOException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );
        CookieServlet servlet = new CookieServlet( MODE_SIMPLE );
        httpService.registerServlet( "/test", servlet, null, null );

        //Test that no cookies are currently set.
        HttpURLConnection client = getConnection( DEFAULT_BASE_URL + "/test", "GET" );

        //Set the cookie in the response
        client.connect();
        assertTrue( client.getResponseCode() == 200 );
        assertNotNull( client.getHeaderFields().get( "Set-Cookie" ) );
        List l = ( List ) client.getHeaderFields().get( "Set-Cookie" );
        assertTrue( l.contains( "testcookie=testvalue" ) );

        for ( int i = 0; i < 10; ++i )
        {
            //Confirm the cookie in the request
            client = getConnection( DEFAULT_BASE_URL + "/test", "GET" );
            client.addRequestProperty( "Cookie", "testcookie=testvalue" );
            client.connect();
            assertTrue( client.getResponseCode() == 200 );
        }

    }


    /**
     * Test sending and recieving multiple cookies.
     * 
     * @throws ServletException
     * @throws NamespaceException
     * @throws IOException
     */
    public void testMultipleCookies() throws ServletException, NamespaceException, IOException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );
        CookieServlet servlet = new CookieServlet( MODE_MULTI );
        httpService.registerServlet( "/test", servlet, null, null );

        //Test that no cookies are currently set.
        HttpURLConnection client = getConnection( DEFAULT_BASE_URL + "/test", "GET" );

        //Set the cookie in the response
        client.connect();
        assertTrue( client.getResponseCode() == 200 );
        assertNotNull( client.getHeaderFields().get( "Set-Cookie" ) );
        List l = ( List ) client.getHeaderFields().get( "Set-Cookie" );
        assertTrue( l.contains( "testcookie=testvalue;testcookie2=testvalue2" ) );

        for ( int i = 0; i < 10; ++i )
        {
            //Confirm the cookie in the request
            client = getConnection( DEFAULT_BASE_URL + "/test", "GET" );
            client.addRequestProperty( "Cookie", "testcookie=testvalue;testcookie2=testvalue2" );
            client.connect();
            assertTrue( client.getResponseCode() == 200 );
        }

    }

    /**
     * Servlet to test cookie support.
     *
     */
    private class CookieServlet extends HttpServlet implements Servlet
    {

        private final int m_mode;
        private int requestCount = 0;
        private Cookie[] m_cookies;


        public CookieServlet( int mode )
        {
            this.m_mode = mode;
        }


        public Cookie[] getCookies()
        {
            return m_cookies;
        }


        protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
        {
            m_cookies = req.getCookies();

            switch ( m_mode )
            {
                case MODE_NONE_SET:
                    Cookie[] cookies = req.getCookies();

                    assertNull( cookies );

                    break;
                case MODE_SIMPLE:
                    requestCount++;
                    if ( requestCount == 1 )
                    {
                        assertNull( req.getCookies() );
                        Cookie c = new Cookie( "testcookie", "testvalue" );
                        resp.addCookie( c );

                    }
                    else if ( requestCount > 1 )
                    {
                        Cookie[] c = req.getCookies();

                        assertNotNull( c );
                        assertTrue( c.length == 1 );
                        assertTrue( c[0].getName().equals( "testcookie" ) );
                        assertTrue( c[0].getValue().equals( "testvalue" ) );
                    }

                    break;
                case MODE_MULTI:
                    requestCount++;
                    if ( requestCount == 1 )
                    {
                        assertNull( req.getCookies() );
                        Cookie c = new Cookie( "testcookie", "testvalue" );
                        resp.addCookie( c );
                        c = new Cookie( "testcookie2", "testvalue2" );
                        resp.addCookie( c );
                    }
                    else if ( requestCount > 1 )
                    {
                        Cookie[] c = req.getCookies();

                        assertNotNull( c );
                        assertTrue( c.length == 2 );
                        assertTrue( c[0].getName().equals( "testcookie" ) );
                        assertTrue( c[0].getValue().equals( "testvalue" ) );
                        assertTrue( c[1].getName().equals( "testcookie2" ) );
                        assertTrue( c[1].getValue().equals( "testvalue2" ) );
                    }

                    break;
                default:
                    throw new ServletException( "Invalid test mode." );
            }
        }
    }
}
