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
package org.apache.felix.sigil.gogo.junit;


import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Hashtable;

import junit.framework.Assert;

import org.apache.felix.sigil.common.junit.server.JUnitService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.osgi.service.command.CommandProcessor;
import org.osgi.util.tracker.ServiceTracker;


public class Activator implements BundleActivator
{

    public void start( final BundleContext ctx ) throws Exception
    {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put( CommandProcessor.COMMAND_SCOPE, "sigil" );
        props.put( CommandProcessor.COMMAND_FUNCTION, new String[]
            { "runTests", "listTests" } );

        ServiceTracker tracker = new ServiceTracker( ctx, JUnitService.class.getName(), null );
        tracker.open();

        ctx.registerService( SigilJunitRunner.class.getName(), new SigilJunitRunner( tracker ), props );

        props.put( CommandProcessor.COMMAND_FUNCTION, new String[]
            { "newTest", "newTestSuite" } );
        ctx.registerService( SigilTestAdapter.class.getName(), new SigilTestAdapter(), props );

        props.put( CommandProcessor.COMMAND_SCOPE, "junit" );
        props.put( CommandProcessor.COMMAND_FUNCTION, getAssertMethods() );
        ctx.registerService( Assert.class.getName(), new Assert()
        {
        }, props );
    }


    /**
     * @return
     */
    private String[] getAssertMethods()
    {
        ArrayList<String> list = new ArrayList<String>();
        for ( Method m : Assert.class.getDeclaredMethods() )
        {
            if ( Modifier.isPublic( m.getModifiers() ) ) {
                list.add( m.getName() );
            }
        }
        return list.toArray( new String[list.size()] );
    }


    public void stop( BundleContext ctx ) throws Exception
    {
    }

}
