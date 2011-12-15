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
import java.util.Random;

import javax.servlet.ServletException;

import org.apache.felix.httplite.osgi.test.AbstractHttpliteTestCase;
import org.apache.felix.httplite.osgi.test.BasicTestingServlet;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;


/**
 * Tests related to OSGi service registry.
 * 
 * @author kgilmer
 * 
 */
public class TestServletContainer extends AbstractHttpliteTestCase
{
    /**
     * Test calling GET enters TestServlet doGet() method.
     * 
     * @throws ServletException
     * @throws NamespaceException
     * @throws IOException 
     */
    public void testExecuteGET() throws ServletException, NamespaceException, IOException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );

        BasicTestingServlet testServlet = new BasicTestingServlet();
        httpService.registerServlet( "/test", testServlet, null, null );

        HttpURLConnection client = getConnection( DEFAULT_BASE_URL + "/test", "GET" );

        client.connect();

        assertTrue( client.getResponseCode() == 200 );
        assertTrue( testServlet.isGetCalled() );
        assertFalse( testServlet.isDeleteCalled() );
        assertFalse( testServlet.isPostCalled() );
        assertFalse( testServlet.isPutCalled() );
    }


    /**
     * Test can execute POST method.
     * 
     * @throws ServletException
     * @throws NamespaceException
     * @throws IOException
     */
    public void testExecutePOST() throws ServletException, NamespaceException, IOException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );

        BasicTestingServlet testServlet = new BasicTestingServlet();
        httpService.registerServlet( "/test", testServlet, null, null );

        HttpURLConnection client = getConnection( DEFAULT_BASE_URL + "/test", "POST" );
        client.connect();

        assertTrue( client.getResponseCode() == 200 );
        assertFalse( testServlet.isGetCalled() );
        assertFalse( testServlet.isDeleteCalled() );
        assertTrue( testServlet.isPostCalled() );
        assertFalse( testServlet.isPutCalled() );
    }


    /**
     * Test can execute PUT method.
     * 
     * @throws ServletException
     * @throws NamespaceException
     * @throws IOException
     */
    public void testExecutePUT() throws ServletException, NamespaceException, IOException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );

        BasicTestingServlet testServlet = new BasicTestingServlet();
        httpService.registerServlet( "/test", testServlet, null, null );

        HttpURLConnection client = getConnection( DEFAULT_BASE_URL + "/test", "PUT" );

        client.connect();

        assertTrue( client.getResponseCode() == 200 );
        assertFalse( testServlet.isGetCalled() );
        assertFalse( testServlet.isDeleteCalled() );
        assertFalse( testServlet.isPostCalled() );
        assertTrue( testServlet.isPutCalled() );
    }


    /**
     * Test can execute DELETE method.
     * 
     * @throws ServletException
     * @throws NamespaceException
     * @throws IOException
     */
    public void testExecuteDELETE() throws ServletException, NamespaceException, IOException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );

        BasicTestingServlet testServlet = new BasicTestingServlet();
        httpService.registerServlet( "/test", testServlet, null, null );

        HttpURLConnection client = getConnection( DEFAULT_BASE_URL + "/test", "DELETE" );

        client.connect();

        assertTrue( client.getResponseCode() == 200 );
        assertFalse( testServlet.isGetCalled() );
        assertTrue( testServlet.isDeleteCalled() );
        assertFalse( testServlet.isPostCalled() );
        assertFalse( testServlet.isPutCalled() );
    }


    /**
     * Test that container returns exact content as specified in servlet.
     * 
     * @throws ServletException
     * @throws NamespaceException
     * @throws IOException
     */
    public void testGETResponseStringContent() throws ServletException, NamespaceException, IOException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );

        String content = "test content";

        BasicTestingServlet testServlet = new BasicTestingServlet( content, false );
        httpService.registerServlet( "/test", testServlet, null, null );

        HttpURLConnection client = getConnection( DEFAULT_BASE_URL + "/test", "GET" );

        client.connect();

        assertTrue( client.getResponseCode() == 200 );

        String response = readInputAsString( client.getInputStream() );

        printBytes( content.getBytes() );
        printBytes( response.getBytes() );
        assertTrue( content.equals( response ) );

        httpService.unregister( "/test" );

        content = "test content";
        testServlet = new BasicTestingServlet( content, true );
        httpService.registerServlet( "/test", testServlet, null, null );

        client = getConnection( DEFAULT_BASE_URL + "/test", "GET" );

        client.connect();

        assertTrue( client.getResponseCode() == 200 );

        response = readInputAsString( client.getInputStream() );
        printBytes( content.getBytes() );
        printBytes( response.getBytes() );
        assertTrue( content.length() == response.length() );
        assertTrue( content.equals( response ) );
    }


    /**
     * Test that container returns exact content as specified in servlet.
     * 
     * @throws ServletException
     * @throws NamespaceException
     * @throws IOException
     */
    public void testGETResponseBinaryContent() throws ServletException, NamespaceException, IOException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );

        byte[] content = generateRandomBinaryContent();

        BasicTestingServlet testServlet = new BasicTestingServlet( content, false );
        httpService.registerServlet( "/test", testServlet, null, null );

        HttpURLConnection client = getConnection( DEFAULT_BASE_URL + "/test", "GET" );

        client.connect();

        assertTrue( client.getResponseCode() == 200 );

        byte[] response = readInputAsByteArray( client.getInputStream() );

        printBytes( content );
        printBytes( response );
        assertTrue( content.length ==  response.length );
        for (int i = 0; i < content.length; ++i) 
        {
            assertTrue( content[i] == response[i] );
        }

        httpService.unregister( "/test" );       
    }


    private byte[] generateRandomBinaryContent()
    {
        Random rnd = new Random();

        int l = rnd.nextInt( 40 ) + 20;

        byte[] buf = new byte[l];

        rnd.nextBytes( buf );

        return buf;
    }


    private static void printBytes( byte[] b )
    {
        for ( int i = 0; i < b.length; ++i )
        {
            System.out.print( b[i] );
            System.out.print( ", " );
        }
        System.out.println();
    }
}
