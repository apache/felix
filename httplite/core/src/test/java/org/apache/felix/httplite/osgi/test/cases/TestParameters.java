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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.felix.httplite.osgi.test.AbstractHttpliteTestCase;
import org.apache.felix.httplite.osgi.test.BasicTestingServlet;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;


/**
 * Test request parameter handling by container.
 *
 */
public class TestParameters extends AbstractHttpliteTestCase
{

    //TODO: test unicode parameters
    //TODO: test parameters with empty values
    //TODO: test parameter name collision

    /**
     * @throws ServletException
     * @throws NamespaceException
     * @throws IOException
     */
    public void testCorrectParameterCount() throws ServletException, NamespaceException, IOException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );

        BasicTestingServlet testServlet = new BasicTestingServlet();
        httpService.registerServlet( "/test", testServlet, null, null );

        HttpURLConnection client = getConnection( DEFAULT_BASE_URL + "/test", "GET" );

        int parameterCount = 16;
        for ( int i = 0; i < parameterCount; ++i )
        {
            client.addRequestProperty( "k" + i, "v" + i );
        }
        client.connect();
        assertTrue( client.getResponseCode() == 200 );
        
        int headerCount = 0;
        Enumeration e = testServlet.getHeaderNames();
        while (e.hasMoreElements()) 
        {
            headerCount++;
            System.out.println("header: " + e.nextElement().toString());
        }
        
        
        assertTrue( headerCount >= parameterCount );
    }


    /**
     * Test the parameter contents.
     * 
     * @throws IOException
     * @throws ServletException
     * @throws NamespaceException
     */
    public void testParameterContents() throws IOException, ServletException, NamespaceException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );

        BasicTestingServlet testServlet = new BasicTestingServlet();
        httpService.registerServlet( "/test", testServlet, null, null );

        HttpURLConnection client = getConnection( DEFAULT_BASE_URL + "/test", "GET" );

        int parameterCount = 16;
        for ( int i = 0; i < parameterCount; ++i )
        {
            client.addRequestProperty( "k" + i, "v" + i );
        }
        client.connect();
        assertTrue( client.getResponseCode() == 200 );
        Map rp = new HashMap();
        
        Enumeration e = testServlet.getHeaderNames();
        while (e.hasMoreElements()) 
        {
            String key = e.nextElement().toString();
            rp.put( key , testServlet.getHeader( key ) );
        }
        
        for ( int i = 0; i < parameterCount; ++i )
        {
             assertTrue( rp.get( "k" + i ).equals( "v" + i ) );
        }
    }
}
