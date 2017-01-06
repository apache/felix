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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;


@RunWith(JUnit4TestRunner.class)
public class TargetedPidTest extends ConfigurationTestBase
{

    static
    {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }


    @Test
    public void test_targetet_pid_no_replace() throws BundleException
    {
        String basePid = "test_targeted";
        String[] pids = null;
        try
        {

            // start the bundle and assert this
            bundle = installBundle( basePid );
            bundle.start();
            final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
            TestCase.assertNotNull( "Activator not started !!", tester );

            // give cm time for distribution
            delay();

            // assert activater has configuration
            int callCount = 0;
            TestCase.assertNull( "Expect no Properties after Service Registration", tester.props );
            TestCase.assertEquals( "Expect calls", ++callCount, tester.numManagedServiceUpdatedCalls );

            pids = new String[]
                {
                    basePid,
                    String.format( "%s|%s", basePid, bundle.getSymbolicName() ),
                    String.format( "%s|%s|%s", basePid, bundle.getSymbolicName(),
                        bundle.getHeaders().get( Constants.BUNDLE_VERSION ) ),
                    String.format( "%s|%s|%s|%s", basePid, bundle.getSymbolicName(),
                        bundle.getHeaders().get( Constants.BUNDLE_VERSION ), bundle.getLocation() ) };

            for (String pid : pids) {
                configure( pid );
                delay();
                TestCase.assertNotNull( "Expect Properties after update " + pid, tester.props );
                TestCase.assertEquals( "Expect PID", pid, tester.props.get( Constants.SERVICE_PID ) );
                TestCase.assertEquals( "Expect calls", ++callCount, tester.numManagedServiceUpdatedCalls );
                deleteConfig( pid );
                delay();
                TestCase.assertNull( "Expect no Properties after delete " + pid, tester.props );
                TestCase.assertEquals( "Expect calls", ++callCount, tester.numManagedServiceUpdatedCalls );
            }

            // cleanup
            bundle.uninstall();
            bundle = null;

        }
        finally
        {
            // remove the configuration for good
            if ( pids != null )
            {
                for ( String p : pids )
                {
                    deleteConfig( p );
                }
            }
        }
    }

    @Test
    public void test_targetet_pid_replace() throws BundleException
    {
        String basePid = "test_targeted";
        String[] pids = null;
        try
        {

            // start the bundle and assert this
            bundle = installBundle( basePid );
            bundle.start();
            final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
            TestCase.assertNotNull( "Activator not started !!", tester );

            // give cm time for distribution
            delay();

            // assert activater has configuration
            int callCount = 0;
            TestCase.assertNull( "Expect no Properties after Service Registration", tester.props );
            TestCase.assertEquals( "Expect calls", ++callCount, tester.numManagedServiceUpdatedCalls );

            pids = new String[]
                {
                basePid,
                String.format( "%s|%s", basePid, bundle.getSymbolicName() ),
                String.format( "%s|%s|%s", basePid, bundle.getSymbolicName(),
                    bundle.getHeaders().get( Constants.BUNDLE_VERSION ) ),
                    String.format( "%s|%s|%s|%s", basePid, bundle.getSymbolicName(),
                        bundle.getHeaders().get( Constants.BUNDLE_VERSION ), bundle.getLocation() ) };

            for (String pid : pids) {
                configure( pid );
                delay();
                TestCase.assertNotNull( "Expect Properties after update " + pid, tester.props );
                TestCase.assertEquals( "Expect PID", pid, tester.props.get( Constants.SERVICE_PID ) );
                TestCase.assertEquals( "Expect calls", ++callCount, tester.numManagedServiceUpdatedCalls );
            }

            // cleanup
            bundle.uninstall();
            bundle = null;

        }
        finally
        {
            // remove the configuration for good
            if ( pids != null )
            {
                for ( String p : pids )
                {
                    deleteConfig( p );
                }
            }
        }
    }

    @Test
    public void test_targetet_pid_delete_fallback() throws BundleException
    {
        String basePid = "test_targeted";
        String[] pids = null;
        try
        {

            // start the bundle and assert this
            bundle = installBundle( basePid );

            pids = new String[]
                {
                    String.format( "%s|%s|%s|%s", basePid, bundle.getSymbolicName(),
                        bundle.getHeaders().get( Constants.BUNDLE_VERSION ), bundle.getLocation() ),
                    String.format( "%s|%s|%s", basePid, bundle.getSymbolicName(),
                        bundle.getHeaders().get( Constants.BUNDLE_VERSION ) ),
                    String.format( "%s|%s", basePid, bundle.getSymbolicName() ), basePid };

            for (String pid : pids) {
                configure( pid );
            }


            bundle.start();
            final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
            TestCase.assertNotNull( "Activator not started !!", tester );

            // give cm time for distribution
            delay();

            // assert activater has configuration
            int callCount = 0;
            for (String pid : pids) {
                TestCase.assertNotNull( "Expect Properties after update " + pid, tester.props );
                TestCase.assertEquals( "Expect PID", pid, tester.props.get( Constants.SERVICE_PID ) );
                TestCase.assertEquals( "Expect calls", ++callCount, tester.numManagedServiceUpdatedCalls );

                deleteConfig( pid );
                delay();
            }

            // final delete
            TestCase.assertNull( "Expect Properties after delete", tester.props );
            TestCase.assertEquals( "Expect calls", ++callCount, tester.numManagedServiceUpdatedCalls );

            // cleanup
            bundle.uninstall();
            bundle = null;

        }
        finally
        {
            // remove the configuration for good
            if ( pids != null )
            {
                for ( String p : pids )
                {
                    deleteConfig( p );
                }
            }
        }
    }

    @Test
    public void test_pid_with_pipe() throws BundleException
    {
        final String pid0 = "test_targeted";
        final String pid1 = String.format( "%s|%s", pid0, ManagedServiceTestActivator.class.getName() );
        try
        {

            // start the bundle and assert this
            bundle = installBundle( pid1 );

            configure( pid0 );
            configure( pid1 );

            bundle.start();
            final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
            TestCase.assertNotNull( "Activator not started !!", tester );

            // give cm time for distribution
            delay();

            // assert activater has configuration
            int callCount = 0;
            TestCase.assertNotNull( "Expect Properties after update", tester.props );
            TestCase.assertEquals( "Expect PID", pid1, tester.props.get( Constants.SERVICE_PID ) );
            TestCase.assertEquals( "Expect calls", ++callCount, tester.numManagedServiceUpdatedCalls );

            // delete pid1 - don't expect pid0 is assigned
            deleteConfig( pid1 );
            delay();

            // final delete
            TestCase.assertNull( "Expect no Properties after delete", tester.props );
            TestCase.assertEquals( "Expect calls", ++callCount, tester.numManagedServiceUpdatedCalls );

            // cleanup
            bundle.uninstall();
            bundle = null;

        }
        finally
        {
            // remove the configuration for good
            deleteConfig( pid0 );
            deleteConfig( pid1 );
        }
    }

}
