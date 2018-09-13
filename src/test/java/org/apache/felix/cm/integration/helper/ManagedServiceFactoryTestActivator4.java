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
package org.apache.felix.cm.integration.helper;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.Dictionary;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ManagedServiceFactory;


public class ManagedServiceFactoryTestActivator4 extends BaseTestActivator
{
    public static ManagedServiceFactoryTestActivator4 INSTANCE;


    public void start( BundleContext context ) throws Exception
    {
        context.registerService( ManagedServiceFactory.class.getName(), this, getServiceProperties( context ) );
        INSTANCE = this;
    }


    public void stop( BundleContext arg0 ) throws Exception
    {
        INSTANCE = null;
    }
    
    public void updated( String pid, Dictionary props )
    {
    	// Opening a socket is a secure action which Config Admin doesn't have permission to do
    	
    	int port = 0;
    	ServerSocket ss = null; 
    	try {
    		ss = new ServerSocket( 0 );
    		port = ss.getLocalPort();
    	} catch ( Exception e ) {
    		e.printStackTrace();
    	} finally {
    		if (ss != null) {
    			try {
					ss.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    	}
    	
		props.put( "port", port );
    	
        super.updated( pid, props );
    }
}
