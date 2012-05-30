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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;


@RunWith(JUnit4TestRunner.class)
public class FELIX2813_ConfigurationAdminStartupTest extends ConfigurationTestBase implements ServiceListener,
    ConfigurationListener
{

    private Object lock = new Object();
    private boolean eventSeen;


    @Test
    public void testAddConfigurationWhenConfigurationAdminStarts() throws InvalidSyntaxException, BundleException
    {

        List<Bundle> bundles = new ArrayList<Bundle>();
        ServiceReference[] refs = configAdminTracker.getServiceReferences();
        if ( refs != null )
        {
            for ( ServiceReference ref : refs )
            {
                bundles.add( ref.getBundle() );
                ref.getBundle().stop();
            }
        }

        bundleContext.registerService( ConfigurationListener.class.getName(), this, null );
        bundleContext.addServiceListener( this, "(" + Constants.OBJECTCLASS + "=" + ConfigurationAdmin.class.getName()
            + ")" );

        // ensure we do not have a false positive below
        eventSeen = false;

        for ( Bundle bundle : bundles )
        {
            bundle.start();
        }

        /*
         * Look at the console output for the following exception:
         *
         * *ERROR* Unexpected problem executing task
         * java.lang.NullPointerException: reference and pid must not be null
         *     at org.osgi.service.cm.ConfigurationEvent.<init>(ConfigurationEvent.java:120)
         *     at org.apache.felix.cm.impl.ConfigurationManager$FireConfigurationEvent.run(ConfigurationManager.java:1818)
         *     at org.apache.felix.cm.impl.UpdateThread.run(UpdateThread.java:104)
         *     at java.lang.Thread.run(Thread.java:680)
         *
         * It is in fact the service reference that is still null, because the service registration
         * has not been 'set' yet.
         *
         * This following code will ensure the situation did not occurr and the
         * event has effectively been sent. The eventSeen flag is set by the
         * configurationEvent method when the event for the test PID has been
         * received. If the flag is not set, we wait at most 2 seconds for the
         * event to arrive. If the event does not arrive by then, the test is
         * assumed to have failed. This will rather generate false negatives
         * (on slow machines) than false positives.
         */
        synchronized ( lock )
        {
            if ( !eventSeen )
            {
                try
                {
                    lock.wait( 2000 );
                }
                catch ( InterruptedException ie )
                {
                    // don't care ...
                }
            }

            if ( !eventSeen )
            {
                TestCase.fail( "ConfigurationEvent not received within 2 seconds since bundle start" );
            }
        }

    }


    public void serviceChanged( ServiceEvent event )
    {
        if ( event.getType() == ServiceEvent.REGISTERED )
        {
            ServiceReference ref = event.getServiceReference();
            ConfigurationAdmin ca = ( ConfigurationAdmin ) bundleContext.getService( ref );
            try
            {
                org.osgi.service.cm.Configuration config = ca.getConfiguration( "test" );
                Hashtable<String, Object> props = new Hashtable<String, Object>();
                props.put( "abc", "123" );
                config.update( props );
            }
            catch ( IOException e )
            {
            }
        }
    }


    public void configurationEvent( ConfigurationEvent event )
    {
        if ( "test".equals( event.getPid() ) )
        {
            synchronized ( lock )
            {
                eventSeen = true;
                lock.notifyAll();
            }
        }
    }
}
