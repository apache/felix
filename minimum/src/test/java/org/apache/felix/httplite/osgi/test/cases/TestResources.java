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

import org.apache.felix.httplite.osgi.test.AbstractHttpliteTestCase;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;


/**
 * Tests for resources.
 *
 */
public class TestResources extends AbstractHttpliteTestCase
{

    //TODO: test GET binary file

    /**
     * Test that a resource can be retrieved from client.
     * @throws NamespaceException
     * @throws IOException 
     */
    public void testCanGetResource() throws NamespaceException, IOException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );

        httpService.registerResources( "/", "/webroot/", null );

        HttpURLConnection client = getConnection( DEFAULT_BASE_URL + "/index.html", "GET" );
        client.connect();

        assertTrue( client.getResponseCode() == 200 );
        String response = readInputAsString( client.getInputStream() );
        assertNotNull( response );
        assertTrue( response.indexOf( "boo" ) > -1 );
    }


    /**
     * Test that non-existent resource returns 404.
     * @throws IOException
     * @throws NamespaceException
     */
    public void testCannotGetInvalidResource() throws IOException, NamespaceException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );

        httpService.registerResources( "/", "/webroot/", null );

        HttpURLConnection client = getConnection( DEFAULT_BASE_URL + "/index2.html", "GET" );
        client.connect();

        assertTrue( client.getResponseCode() == 404 );
    }
}
