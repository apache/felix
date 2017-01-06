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


import junit.framework.TestCase;

import org.apache.felix.cm.integration.helper.ManagedServiceTestActivator;
import org.apache.felix.cm.integration.helper.ManagedServiceTestActivator2;
import org.apache.felix.cm.integration.helper.MultiManagedServiceTestActivator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.Configuration;


/**
 * The <code>MultiServicePIDTest</code> tests the case of multiple services
 * bound with the same PID
 */
@RunWith(JUnit4TestRunner.class)
public class MultiServicePIDTest extends ConfigurationTestBase
{
    static
    {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }

    @Test
    public void test_two_services_same_pid_in_same_bundle_configure_before_registration() throws BundleException
    {
        final String pid = "test.pid";

        configure( pid );

        final Configuration config = getConfiguration( pid );
        TestCase.assertEquals( pid, config.getPid() );
        TestCase.assertNull( config.getBundleLocation() );

        bundle = installBundle( pid, MultiManagedServiceTestActivator.class );
        bundle.start();

        // give cm time for distribution
        delay();

        final MultiManagedServiceTestActivator tester = MultiManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // assert activater has configuration (two calls, one per pid)
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect two update calls", 2, tester.numManagedServiceUpdatedCalls );

        TestCase.assertEquals( bundle.getLocation(), config.getBundleLocation() );

        bundle.uninstall();
        bundle = null;

        delay();

        TestCase.assertNull( config.getBundleLocation() );

        // remove the configuration for good
        deleteConfig( pid );
    }


    @Test
    public void test_two_services_same_pid_in_same_bundle_configure_after_registration() throws BundleException
    {
        final String pid = "test.pid";

        bundle = installBundle( pid, MultiManagedServiceTestActivator.class );
        bundle.start();

        // give cm time for distribution
        delay();

        final MultiManagedServiceTestActivator tester = MultiManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // no configuration yet
        TestCase.assertNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect two update calls", 2, tester.numManagedServiceUpdatedCalls );

        configure( pid );
        delay();

        final Configuration config = getConfiguration( pid );
        TestCase.assertEquals( pid, config.getPid() );
        TestCase.assertEquals( bundle.getLocation(), config.getBundleLocation() );

        // assert activater has configuration (two calls, one per pid)
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect another two single update call", 4, tester.numManagedServiceUpdatedCalls );

        bundle.uninstall();
        bundle = null;

        delay();

        TestCase.assertNull( config.getBundleLocation() );

        // remove the configuration for good
        deleteConfig( pid );
    }


    @Test
    public void test_two_services_same_pid_in_two_bundle_configure_before_registration() throws BundleException
    {
        Bundle bundle2 = null;
        try
        {
            final String pid = "test.pid";

            configure( pid );

            final Configuration config = getConfiguration( pid );
            TestCase.assertEquals( pid, config.getPid() );
            TestCase.assertNull( config.getBundleLocation() );

            bundle = installBundle( pid, ManagedServiceTestActivator.class );
            bundle.start();

            bundle2 = installBundle( pid, ManagedServiceTestActivator2.class );
            bundle2.start();

            // give cm time for distribution
            delay();

            final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
            TestCase.assertNotNull( "Activator not started !!", tester );

            final ManagedServiceTestActivator2 tester2 = ManagedServiceTestActivator2.INSTANCE;
            TestCase.assertNotNull( "Activator 2 not started !!", tester2 );

            // expect first activator to have received properties

            // assert first bundle has configuration (one calls, one per srv)
            TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
            TestCase.assertEquals( "Expect a single update call", 1, tester.numManagedServiceUpdatedCalls );

            // assert second bundle has no configuration (but called with null)
            TestCase.assertNull( tester2.props );
            TestCase.assertEquals( 1, tester2.numManagedServiceUpdatedCalls );

            // expect configuration bound to first bundle
            TestCase.assertEquals( bundle.getLocation(), config.getBundleLocation() );

            bundle.uninstall();
            bundle = null;

            delay();

            // after uninstallation, the configuration is redispatched
            // due to the dynamic binding being removed

            // expect configuration reassigned
            TestCase.assertEquals( bundle2.getLocation(), config.getBundleLocation() );

            // assert second bundle now has the configuration
            TestCase.assertNotNull( "Expect Properties after Configuration redispatch", tester2.props );
            TestCase.assertEquals( "Expect a single update call after Configuration redispatch", 2,
                tester2.numManagedServiceUpdatedCalls );

            // remove the configuration for good
            deleteConfig( pid );
        }
        finally
        {
            if ( bundle2 != null )
            {
                bundle2.uninstall();
            }
        }
    }


    @Test
    public void test_two_services_same_pid_in_two_bundle_configure_after_registration() throws BundleException
    {
        Bundle bundle2 = null;
        try
        {
            final String pid = "test.pid";

            bundle = installBundle( pid, ManagedServiceTestActivator.class );
            bundle.start();

            bundle2 = installBundle( pid, ManagedServiceTestActivator2.class );
            bundle2.start();

            final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
            TestCase.assertNotNull( "Activator not started !!", tester );

            final ManagedServiceTestActivator2 tester2 = ManagedServiceTestActivator2.INSTANCE;
            TestCase.assertNotNull( "Activator not started !!", tester2 );

            delay();

            // expect no configuration but a call in each service
            TestCase.assertNull( "Expect Properties after Service Registration", tester.props );
            TestCase.assertEquals( "Expect a single update call", 1, tester.numManagedServiceUpdatedCalls );
            TestCase.assertNull( "Expect Properties after Service Registration", tester2.props );
            TestCase.assertEquals( "Expect a single update call", 1, tester2.numManagedServiceUpdatedCalls );

            configure( pid );

            delay();

            final Configuration config = getConfiguration( pid );
            TestCase.assertEquals( pid, config.getPid() );

            TestCase.assertEquals(
                "Configuration must be bound to first bundle because the service has higher ranking",
                bundle.getLocation(), config.getBundleLocation() );

            // configuration assigned to the first bundle
            TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
            TestCase.assertEquals( "Expect a single update call", 2, tester.numManagedServiceUpdatedCalls );

            TestCase.assertNull( "Expect Properties after Service Registration", tester2.props );
            TestCase.assertEquals( "Expect a single update call", 1, tester2.numManagedServiceUpdatedCalls );

            bundle.uninstall();
            bundle = null;

            delay();

            // after uninstallation, the configuration is redispatched
            // due to the dynamic binding being removed

            // expect configuration reassigned
            TestCase.assertEquals( bundle2.getLocation(), config.getBundleLocation() );

            // assert second bundle now has the configuration
            TestCase.assertNotNull( "Expect Properties after Configuration redispatch", tester2.props );
            TestCase.assertEquals( "Expect a single update call after Configuration redispatch", 2,
                tester2.numManagedServiceUpdatedCalls );

            // remove the configuration for good
            deleteConfig( pid );
        }
        finally
        {
            if ( bundle2 != null )
            {
                bundle2.uninstall();
            }
        }
    }

}
