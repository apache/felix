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
public class TestOSGiService extends AbstractHttpliteTestCase
{

    /**
     * HTTPService is available
     */
    public void testHTTPServiceAvailability()
    {
        assertNotNull( getHTTPService( registry.getBundleContext() ) );
    }


    /**
     * Can register a servlet with the HTTPService
     * 
     * @throws ServletException
     * @throws NamespaceException
     */
    public void testCanRegisterServlet() throws ServletException, NamespaceException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );

        httpService.registerServlet( "/", new BasicTestingServlet(), null, null );
    }


    /**
     * Test that HTTP Service does not allow same alias to be registered twice.
     * 
     * @throws ServletException
     * @throws NamespaceException
     */
    public void testCannotRegisterSameAliasTwice() throws ServletException, NamespaceException
    {

        HttpService httpService = getHTTPService( registry.getBundleContext() );

        boolean namespaceExceptionThrown = false;
        httpService.registerServlet( "/alias", new BasicTestingServlet(), null, null );

        try
        {
            httpService.registerServlet( "/alias", new BasicTestingServlet(), null, null );
        }
        catch ( NamespaceException e )
        {
            namespaceExceptionThrown = true;
        }

        assertTrue( namespaceExceptionThrown );
    }


    /**
     * Test invalid aliases throw NamespaceException.
     * 
     * @throws ServletException
     * @throws NamespaceException
     */
    public void testCannotRegisterInvalidAlias() throws ServletException, NamespaceException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );
        String[] badAliases =
            { "noslash" };

        for ( int i = 0; i < badAliases.length; ++i )
        {
            boolean namespaceExceptionThrown = false;
            try
            {
                httpService.registerServlet( badAliases[i], new BasicTestingServlet(), null, null );
            }
            catch ( NamespaceException e )
            {
                namespaceExceptionThrown = true;
            }

        }
    }


    /**
     * Test that an alias can be registered after it's been previously registered and then unregistered.
     * 
     * @throws ServletException
     * @throws NamespaceException
     */
    public void testCanReregisterAlias() throws ServletException, NamespaceException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );

        httpService.registerServlet( "/alias", new BasicTestingServlet(), null, null );

        httpService.unregister( "/alias" );

        httpService.registerServlet( "/alias", new BasicTestingServlet(), null, null );
    }


    /**
     * Test resources can be registered.
     * 
     * @throws NamespaceException
     */
    public void testCanRegisterResources() throws NamespaceException
    {
        HttpService httpService = getHTTPService( registry.getBundleContext() );

        httpService.registerResources( "/restest", "/webroot/", null );
    }

}
