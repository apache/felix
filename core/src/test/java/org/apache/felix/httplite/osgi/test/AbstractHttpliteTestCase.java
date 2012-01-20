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
package org.apache.felix.httplite.osgi.test;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;


/**
 * Base class with commong HTTP testing methods.  Depends on Apache Commons-IO.
 *
 */
public abstract class AbstractHttpliteTestCase extends AbstractPojoSRTestCase
{

    protected static final int DEFAULT_PORT = 8080;
    protected static final String DEFAULT_BASE_URL = "http://localhost:" + DEFAULT_PORT;

    protected static String readInputAsString( InputStream in ) throws IOException
    {
        return IOUtils.toString( in );
    }


    /**
     * Create an array of bytesfrom the complete contents of an InputStream.
     * 
     * @param in
     *            InputStream to turn into a byte array
     * @return byte array (byte[]) w/ contents of input stream, or null if inputstream is null.
     * @throws IOException
     *             on I/O error
     */
    protected static byte[] readInputAsByteArray( InputStream in ) throws IOException
    {
        return IOUtils.toByteArray( in );
    }


    /**
     * Create a HttpURLConnection for specified url.
     * 
     * @param urlStr
     * @param method
     * @return
     * @throws IOException
     */
    protected static HttpURLConnection getConnection( String urlStr, String method ) throws IOException
    {
        URL url = new URL( urlStr );
        HttpURLConnection connection = ( HttpURLConnection ) url.openConnection();
        connection.setRequestMethod( method );
        connection.setConnectTimeout( 1000 );

        return connection;

    }


    /**
     * @param context
     * @return instance of HTTP service
     */
    protected HttpService getHTTPService( BundleContext context )
    {
        ServiceReference sr = registry.getServiceReference( HttpService.class.getName() );

        assertNotNull( sr );

        Object svc = registry.getService( sr );

        assertNotNull( svc );
        assertTrue( svc instanceof HttpService );

        return ( HttpService ) svc;
    }
}
