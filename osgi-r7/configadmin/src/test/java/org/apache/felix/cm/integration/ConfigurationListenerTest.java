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
package org.apache.felix.cm.integration;


import java.io.IOException;
import java.util.Hashtable;

import org.apache.felix.cm.integration.helper.SynchronousTestListener;
import org.apache.felix.cm.integration.helper.TestListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.cm.SynchronousConfigurationListener;


@RunWith(JUnit4TestRunner.class)
public class ConfigurationListenerTest extends ConfigurationTestBase
{

    static
    {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }


    @Test
    public void test_async_listener() throws IOException
    {
        final String pid = "test_listener";
        final TestListener testListener = new TestListener();
        final ServiceRegistration listener = this.bundleContext.registerService( ConfigurationListener.class.getName(),
            testListener, null );
        int eventCount = 0;

        Configuration config = configure( pid, null, false );
        try
        {
            delay();
            testListener.assertNoEvent();

            config.update( new Hashtable<String, Object>()
            {
                {
                    put( "x", "x" );
                }
            } );
            delay();
            testListener.assertEvent( ConfigurationEvent.CM_UPDATED, pid, null, true, ++eventCount );

            config.update( new Hashtable<String, Object>()
            {
                {
                    put( "x", "x" );
                }
            } );
            delay();
            testListener.assertEvent( ConfigurationEvent.CM_UPDATED, pid, null, true, ++eventCount );

            config.setBundleLocation( "new_Location" );
            delay();
            testListener.assertEvent( ConfigurationEvent.CM_LOCATION_CHANGED, pid, null, true, ++eventCount );

            config.update();
            testListener.assertNoEvent();

            config.delete();
            config = null;
            delay();
            testListener.assertEvent( ConfigurationEvent.CM_DELETED, pid, null, true, ++eventCount );
        }
        finally
        {
            if ( config != null )
            {
                try
                {
                    config.delete();
                }
                catch ( IOException ioe )
                {
                    // ignore
                }
            }

            listener.unregister();
        }
    }


    @Test
    public void test_sync_listener() throws IOException
    {
        final String pid = "test_listener";
        Configuration config = configure( pid, null, false );

        // Synchronous listener expecting synchronous events being
        // registered as a SynchronousConfigurationListener
        final TestListener testListener = new SynchronousTestListener();
        final ServiceRegistration listener = this.bundleContext.registerService(
            SynchronousConfigurationListener.class.getName(), testListener, null );

        // Synchronous listener expecting asynchronous events being
        // registered as a regular ConfigurationListener
        final TestListener testListenerAsync = new SynchronousTestListener();
        final ServiceRegistration listenerAsync = this.bundleContext.registerService(
            ConfigurationListener.class.getName(), testListenerAsync, null );

        int eventCount = 0;
        int eventCountAsync = 0;

        try
        {
            delay();
            testListener.assertNoEvent();
            testListenerAsync.assertNoEvent();

            config.update( new Hashtable<String, Object>()
            {
                {
                    put( "x", "x" );
                }
            } );
            delay();
            testListener.assertEvent( ConfigurationEvent.CM_UPDATED, pid, null, false, ++eventCount );
            testListenerAsync.assertEvent( ConfigurationEvent.CM_UPDATED, pid, null, true, ++eventCountAsync );

            config.update( new Hashtable<String, Object>()
            {
                {
                    put( "x", "x" );
                }
            } );
            delay();
            testListener.assertEvent( ConfigurationEvent.CM_UPDATED, pid, null, false, ++eventCount );
            testListenerAsync.assertEvent( ConfigurationEvent.CM_UPDATED, pid, null, true, ++eventCountAsync );

            config.setBundleLocation( "new_Location" );
            delay();
            testListener.assertEvent( ConfigurationEvent.CM_LOCATION_CHANGED, pid, null, false, ++eventCount );
            testListenerAsync.assertEvent( ConfigurationEvent.CM_LOCATION_CHANGED, pid, null, true, ++eventCountAsync );

            config.update();
            testListener.assertNoEvent();
            testListenerAsync.assertNoEvent();

            config.delete();
            config = null;
            delay();
            testListener.assertEvent( ConfigurationEvent.CM_DELETED, pid, null, false, ++eventCount );
            testListenerAsync.assertEvent( ConfigurationEvent.CM_DELETED, pid, null, true, ++eventCountAsync );
        }
        finally
        {
            if ( config != null )
            {
                try
                {
                    config.delete();
                }
                catch ( IOException ioe )
                {
                    // ignore
                }
            }

            listener.unregister();
            listenerAsync.unregister();
        }
    }
}
