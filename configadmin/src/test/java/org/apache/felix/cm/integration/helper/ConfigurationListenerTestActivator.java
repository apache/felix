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
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

public class ConfigurationListenerTestActivator extends BaseTestActivator implements ConfigurationListener
{
    public static ConfigurationListenerTestActivator INSTANCE;
    
    private BundleContext context;
    
    public int numListenerUpdatedCalls = 0;
    public int numListenerDeleteCalls = 0;
    public int numListenerLocationChangedCalls = 0;

    public void start( BundleContext context ) throws Exception
    {
        this.context = context;
        context.registerService( ConfigurationListener.class.getName(), this, null );
        INSTANCE = this;
    }


    public void stop( BundleContext arg0 ) throws Exception
    {
        INSTANCE = null;
    }
    
    @Override
    public void configurationEvent(ConfigurationEvent event) {
    
    	String pid = event.getPid();

    	switch( event.getType() ) {
    	    case ConfigurationEvent.CM_DELETED :
    		    numListenerDeleteCalls++;
    		    this.configs.remove( pid );
    		    return;
    	    case ConfigurationEvent.CM_LOCATION_CHANGED :
    	    	numListenerLocationChangedCalls++;
    	    	return;
    	    case ConfigurationEvent.CM_UPDATED :
    	    	numListenerUpdatedCalls++;
    	    	// Deliberate fall through
    	}

        try {
            if( !pid.equals(getServiceProperties( context ).get( Constants.SERVICE_PID ))) {
                return;
            }
        } catch ( Exception e1 ) {
            e1.printStackTrace();
            return;
        }
        
        
        Dictionary<String, Object> props;
        
        try {
            props = context.getService( event.getReference() )
                    .getConfiguration( pid ).getProperties();
        } catch ( IOException ioe ) {
            ioe.printStackTrace();
            return;
        }
        
        this.configs.put( pid, props );
        
        // Opening a socket is a secure action which Config Admin doesn't have permission to do
        
        int port = 0;
        ServerSocket ss = null; 
        try {
            ss = new ServerSocket( 0 );
            port = ss.getLocalPort();
        } catch ( Exception e ) {
            e.printStackTrace();
            return;
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
    }
}
