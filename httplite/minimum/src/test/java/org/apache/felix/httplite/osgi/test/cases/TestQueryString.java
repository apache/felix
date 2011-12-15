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
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.felix.httplite.osgi.test.AbstractHttpliteTestCase;
import org.apache.felix.httplite.osgi.test.BasicTestingServlet;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;


/**
 * Tests for query string handling by the container.
 *
 */
public class TestQueryString extends AbstractHttpliteTestCase
{

    //TODO: test unicode keys and values
    //TODO: test invalid query string in request

    /**
     * @throws ServletException
     * @throws NamespaceException
     * @throws IOException
     */
    public void testQueryString() throws ServletException, NamespaceException, IOException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );

        BasicTestingServlet testServlet = new BasicTestingServlet();
        httpService.registerServlet( "/test", testServlet, null, null );

        StringBuffer qs = new StringBuffer( "?" );
        int parameterCount = 16;
        for ( int i = 0; i < parameterCount; ++i )
        {
            qs.append( "k" + i );
            qs.append( "=" );
            qs.append( "v" + i );
            if ( i != ( parameterCount - 1 ) )
            {
                qs.append( '&' );
            }
        }

        HttpURLConnection client = getConnection( DEFAULT_BASE_URL + "/test" + qs.toString(), "GET" );

        client.connect();
        assertTrue( client.getResponseCode() == 200 );

        Map qsm = testServlet.getQueryStringMap();

        assertTrue( qsm.size() == parameterCount );

        for ( int i = 0; i < parameterCount; ++i )
        {
            assertTrue( qsm.containsKey( "k" + i ) );
            assertTrue( qsm.get( "k" + i ).equals( "v" + i ) );
        }
    }
}
