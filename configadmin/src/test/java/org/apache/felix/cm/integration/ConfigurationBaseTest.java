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
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import junit.framework.TestCase;

import org.apache.felix.cm.integration.helper.ManagedServiceFactoryTestActivator;
import org.apache.felix.cm.integration.helper.ManagedServiceTestActivator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;


@RunWith(JUnit4TestRunner.class)
public class ConfigurationBaseTest extends ConfigurationTestBase
{

    static
    {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }


    @Test
    public void test_configuration_getFacotryPid_after_config_admin_stop() throws BundleException
    {
        final String pid = "test_configuration_after_config_admin_stop";
        final Configuration config = configure( pid, null, true );

        final Bundle cfgAdminBundle = configAdminTracker.getServiceReference().getBundle();
        cfgAdminBundle.stop();
        try
        {
            config.getFactoryPid();
        }
        finally
        {
            try
            {
                cfgAdminBundle.start();
            }
            catch ( BundleException be )
            {
                // tooo bad
            }
        }
    }


    @Test
    public void test_configuration_equals_after_config_admin_stop() throws BundleException
    {
        final String pid = "test_configuration_after_config_admin_stop";
        final Configuration config = configure( pid, null, true );

        final Bundle cfgAdminBundle = configAdminTracker.getServiceReference().getBundle();
        cfgAdminBundle.stop();
        try
        {
            config.equals( config );
        }
        finally
        {
            try
            {
                cfgAdminBundle.start();
            }
            catch ( BundleException be )
            {
                // tooo bad
            }
        }
    }


    @Test
    public void test_configuration_hashCode_after_config_admin_stop() throws BundleException
    {
        final String pid = "test_configuration_after_config_admin_stop";
        final Configuration config = configure( pid, null, true );

        final Bundle cfgAdminBundle = configAdminTracker.getServiceReference().getBundle();
        cfgAdminBundle.stop();
        try
        {
            config.hashCode();
        }
        finally
        {
            try
            {
                cfgAdminBundle.start();
            }
            catch ( BundleException be )
            {
                // tooo bad
            }
        }
    }


    @Test
    public void test_configuration_toString_after_config_admin_stop() throws BundleException
    {
        final String pid = "test_configuration_after_config_admin_stop";
        final Configuration config = configure( pid, null, true );

        final Bundle cfgAdminBundle = configAdminTracker.getServiceReference().getBundle();
        cfgAdminBundle.stop();
        try
        {
            config.toString();
        }
        finally
        {
            try
            {
                cfgAdminBundle.start();
            }
            catch ( BundleException be )
            {
                // tooo bad
            }
        }
    }


    public void test_configuration_getPid_after_config_admin_stop() throws BundleException
    {
        final String pid = "test_configuration_after_config_admin_stop";
        final Configuration config = configure( pid, null, true );

        final Bundle cfgAdminBundle = configAdminTracker.getServiceReference().getBundle();
        cfgAdminBundle.stop();
        try
        {
            config.getPid();
        }
        finally
        {
            try
            {
                cfgAdminBundle.start();
            }
            catch ( BundleException be )
            {
                // tooo bad
            }
        }
    }


    @Test
    public void test_configuration_getProperties_after_config_admin_stop() throws BundleException
    {
        final String pid = "test_configuration_after_config_admin_stop";
        final Configuration config = configure( pid, null, true );

        final Bundle cfgAdminBundle = configAdminTracker.getServiceReference().getBundle();
        cfgAdminBundle.stop();
        try
        {
            config.getProperties();
        }
        finally
        {
            try
            {
                cfgAdminBundle.start();
            }
            catch ( BundleException be )
            {
                // tooo bad
            }
        }
    }


    @Test
    public void test_configuration_delete_after_config_admin_stop() throws BundleException
    {
        final String pid = "test_configuration_after_config_admin_stop";
        final Configuration config = configure( pid, null, true );

        final Bundle cfgAdminBundle = configAdminTracker.getServiceReference().getBundle();
        cfgAdminBundle.stop();
        try
        {
            config.delete();
            TestCase.fail( "Expected IllegalStateException for config.delete" );
        }
        catch ( IllegalStateException ise )
        {
            // expected
        }
        catch ( Exception e )
        {
            TestCase.fail( "Expected IllegalStateException for config.delete" );
        }
        finally
        {
            try
            {
                cfgAdminBundle.start();
            }
            catch ( BundleException be )
            {
                // tooo bad
            }
        }
    }


    @Test
    public void test_configuration_getBundleLocation_after_config_admin_stop() throws BundleException
    {
        final String pid = "test_configuration_after_config_admin_stop";
        final Configuration config = configure( pid, null, true );

        final Bundle cfgAdminBundle = configAdminTracker.getServiceReference().getBundle();
        cfgAdminBundle.stop();
        try
        {
            config.getBundleLocation();
            TestCase.fail( "Expected IllegalStateException for config.getBundleLocation" );
        }
        catch ( IllegalStateException ise )
        {
            // expected
        }
        catch ( Exception e )
        {
            TestCase.fail( "Expected IllegalStateException for config.getBundleLocation" );
        }
        finally
        {
            try
            {
                cfgAdminBundle.start();
            }
            catch ( BundleException be )
            {
                // tooo bad
            }
        }
    }


    @Test
    public void test_configuration_setBundleLocation_after_config_admin_stop() throws BundleException
    {
        final String pid = "test_configuration_after_config_admin_stop";
        final Configuration config = configure( pid, null, true );

        final Bundle cfgAdminBundle = configAdminTracker.getServiceReference().getBundle();
        cfgAdminBundle.stop();
        try
        {
            config.setBundleLocation( "?*" );
            TestCase.fail( "Expected IllegalStateException for config.setBundleLocation" );
        }
        catch ( IllegalStateException ise )
        {
            // expected
        }
        catch ( Exception e )
        {
            TestCase.fail( "Expected IllegalStateException for config.setBundleLocation" );
        }
        finally
        {
            try
            {
                cfgAdminBundle.start();
            }
            catch ( BundleException be )
            {
                // tooo bad
            }
        }
    }


    @Test
    public void test_configuration_update_after_config_admin_stop() throws BundleException
    {
        final String pid = "test_configuration_after_config_admin_stop";
        final Configuration config = configure( pid, null, true );

        final Bundle cfgAdminBundle = configAdminTracker.getServiceReference().getBundle();
        cfgAdminBundle.stop();
        try
        {
            config.update();
            TestCase.fail( "Expected IllegalStateException for config.update" );
        }
        catch ( IllegalStateException ise )
        {
            // expected
        }
        catch ( Exception e )
        {
            TestCase.fail( "Expected IllegalStateException for config.update" );
        }
        finally
        {
            try
            {
                cfgAdminBundle.start();
            }
            catch ( BundleException be )
            {
                // tooo bad
            }
        }
    }


    @SuppressWarnings("serial")
    @Test
    public void test_configuration_update_with_Dictionary_after_config_admin_stop() throws BundleException
    {
        final String pid = "test_configuration_after_config_admin_stop";
        final Configuration config = configure( pid, null, true );

        final Bundle cfgAdminBundle = configAdminTracker.getServiceReference().getBundle();
        cfgAdminBundle.stop();
        try
        {
            config.update( new Hashtable<String, Object>()
            {
                {
                    put( "sample", "sample" );
                }
            } );
            TestCase.fail( "Expected IllegalStateException for config.update" );
        }
        catch ( IllegalStateException ise )
        {
            // expected
        }
        catch ( Exception e )
        {
            TestCase.fail( "Expected IllegalStateException for config.update" );
        }
        finally
        {
            try
            {
                cfgAdminBundle.start();
            }
            catch ( BundleException be )
            {
                // tooo bad
            }
        }
    }


    @Test
    public void test_configuration_admin_createFactoryConfiguration_1_after_config_admin_stop() throws BundleException
    {
        final ConfigurationAdmin ca = getConfigurationAdmin();
        TestCase.assertNotNull( "ConfigurationAdmin service is required", ca );

        final Bundle cfgAdminBundle = configAdminTracker.getServiceReference().getBundle();
        cfgAdminBundle.stop();
        try
        {
            ca.createFactoryConfiguration( "sample" );
            TestCase.fail( "Expected IllegalStateException for ConfigurationAdmin.createFactoryConfiguration" );
        }
        catch ( IllegalStateException ise )
        {
            // expected
        }
        catch ( Exception e )
        {
            TestCase.fail( "Expected IllegalStateException for ConfigurationAdmin.createFactoryConfiguration, got: " + e );
        }
        finally
        {
            try
            {
                cfgAdminBundle.start();
            }
            catch ( BundleException be )
            {
                // tooo bad
            }
        }
    }


    @Test
    public void test_configuration_admin_createFactoryConfiguration_2_after_config_admin_stop() throws BundleException
    {
        final ConfigurationAdmin ca = getConfigurationAdmin();
        TestCase.assertNotNull( "ConfigurationAdmin service is required", ca );

        final Bundle cfgAdminBundle = configAdminTracker.getServiceReference().getBundle();
        cfgAdminBundle.stop();
        try
        {
            ca.createFactoryConfiguration( "sample", "location" );
            TestCase.fail( "Expected IllegalStateException for ConfigurationAdmin.createFactoryConfiguration" );
        }
        catch ( IllegalStateException ise )
        {
            // expected
        }
        catch ( Exception e )
        {
            TestCase.fail( "Expected IllegalStateException for ConfigurationAdmin.createFactoryConfiguration, got: " + e );
        }
        finally
        {
            try
            {
                cfgAdminBundle.start();
            }
            catch ( BundleException be )
            {
                // tooo bad
            }
        }
    }


    @Test
    public void test_configuration_admin_getConfiguration_1_after_config_admin_stop() throws BundleException
    {
        final ConfigurationAdmin ca = getConfigurationAdmin();
        TestCase.assertNotNull( "ConfigurationAdmin service is required", ca );

        final Bundle cfgAdminBundle = configAdminTracker.getServiceReference().getBundle();
        cfgAdminBundle.stop();
        try
        {
            ca.getConfiguration( "sample" );
            TestCase.fail( "Expected IllegalStateException for ConfigurationAdmin.getConfiguration" );
        }
        catch ( IllegalStateException ise )
        {
            // expected
        }
        catch ( Exception e )
        {
            TestCase.fail( "Expected IllegalStateException for ConfigurationAdmin.getConfiguration, got: " + e );
        }
        finally
        {
            try
            {
                cfgAdminBundle.start();
            }
            catch ( BundleException be )
            {
                // tooo bad
            }
        }
    }


    @Test
    public void test_configuration_admin_getConfiguration_2_after_config_admin_stop() throws BundleException
    {
        final ConfigurationAdmin ca = getConfigurationAdmin();
        TestCase.assertNotNull( "ConfigurationAdmin service is required", ca );

        final Bundle cfgAdminBundle = configAdminTracker.getServiceReference().getBundle();
        cfgAdminBundle.stop();
        try
        {
            ca.getConfiguration( "sample", "location" );
            TestCase.fail( "Expected IllegalStateException for ConfigurationAdmin.getConfiguration" );
        }
        catch ( IllegalStateException ise )
        {
            // expected
        }
        catch ( Exception e )
        {
            TestCase.fail( "Expected IllegalStateException for ConfigurationAdmin.getConfiguration, got: " + e );
        }
        finally
        {
            try
            {
                cfgAdminBundle.start();
            }
            catch ( BundleException be )
            {
                // tooo bad
            }
        }
    }


    @Test
    public void test_configuration_admin_listConfigurations_after_config_admin_stop() throws BundleException
    {
        final ConfigurationAdmin ca = getConfigurationAdmin();
        TestCase.assertNotNull( "ConfigurationAdmin service is required", ca );

        final Bundle cfgAdminBundle = configAdminTracker.getServiceReference().getBundle();
        cfgAdminBundle.stop();
        try
        {
            ca.listConfigurations( "(service.pid=sample)" );
            TestCase.fail( "Expected IllegalStateException for ConfigurationAdmin.listConfigurations" );
        }
        catch ( IllegalStateException ise )
        {
            // expected
        }
        catch ( Exception e )
        {
            TestCase.fail( "Expected IllegalStateException for ConfigurationAdmin.listConfigurations, got: " + e );
        }
        finally
        {
            try
            {
                cfgAdminBundle.start();
            }
            catch ( BundleException be )
            {
                // tooo bad
            }
        }
    }


    @Test
    public void test_configuration_change_counter() throws IOException
    {
        // 1. create config with pid and locationA
        // 2. update config with properties
        final String pid = "test_configuration_change_counter";
        final Configuration config = configure( pid, null, false );

        TestCase.assertEquals("Expect first version to be 1", 1, config.getChangeCount());

        config.update(new Hashtable(){{put("x", "x");}});
        TestCase.assertEquals("Expect second version to be 2", 2, config.getChangeCount());

        // delete
        config.delete();
    }


    @Test
    public void test_basic_configuration_configure_then_start() throws BundleException, IOException
    {
        // 1. create config with pid and locationA
        // 2. update config with properties
        final String pid = "test_basic_configuration_configure_then_start";
        final Configuration config = configure( pid, null, true );

        // 3. register ManagedService ms1 with pid from said locationA
        bundle = installBundle( pid, ManagedServiceTestActivator.class );
        bundle.start();
        delay();

        // ==> configuration supplied to the service ms1
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( tester.props );
        TestCase.assertEquals( pid, tester.props.get( Constants.SERVICE_PID ) );
        TestCase.assertNull( tester.props.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( tester.props.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, tester.props.get( PROP_NAME ) );
        TestCase.assertEquals( 1, tester.numManagedServiceUpdatedCalls );

        // delete
        config.delete();
        delay();

        // ==> update with null
        TestCase.assertNull( tester.props );
        TestCase.assertEquals( 2, tester.numManagedServiceUpdatedCalls );
    }


    @Test
    public void test_basic_configuration_strange_pid() throws BundleException, IOException
    {
        // 1. create config with pid and locationA
        // 2. update config with properties
        final String pid = "pid with blanks and stuff %\"'";
        theConfig.put( pid, pid );
        final Configuration config = configure( pid, null, true );
        theConfig.remove( pid );

        // 3. register ManagedService ms1 with pid from said locationA
        bundle = installBundle( pid, ManagedServiceTestActivator.class );
        bundle.start();
        delay();

        // ==> configuration supplied to the service ms1
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( tester.props );
        TestCase.assertEquals( pid, tester.props.get( Constants.SERVICE_PID ) );
        TestCase.assertNull( tester.props.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( tester.props.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, tester.props.get( PROP_NAME ) );
        TestCase.assertEquals( pid, tester.props.get( pid ) );
        TestCase.assertEquals( 1, tester.numManagedServiceUpdatedCalls );

        // delete
        config.delete();
        delay();

        // ==> update with null
        TestCase.assertNull( tester.props );
        TestCase.assertEquals( 2, tester.numManagedServiceUpdatedCalls );
    }


    @Test
    public void test_basic_configuration_start_then_configure() throws BundleException, IOException
    {
        final String pid = "test_basic_configuration_start_then_configure";

        // 1. register ManagedService ms1 with pid from said locationA
        bundle = installBundle( pid, ManagedServiceTestActivator.class );
        bundle.start();
        delay();

        // 1. create config with pid and locationA
        // 2. update config with properties
        final Configuration config = configure( pid, null, true );
        delay();

        // ==> configuration supplied to the service ms1
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( tester.props );
        TestCase.assertEquals( pid, tester.props.get( Constants.SERVICE_PID ) );
        TestCase.assertNull( tester.props.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( tester.props.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, tester.props.get( PROP_NAME ) );
        TestCase.assertEquals( 2, tester.numManagedServiceUpdatedCalls );

        // delete
        config.delete();
        delay();

        // ==> update with null
        TestCase.assertNull( tester.props );
        TestCase.assertEquals( 3, tester.numManagedServiceUpdatedCalls );
    }


    @Test
    public void test_basic_configuration_factory_start_then_configure() throws BundleException, IOException
    {
        final String factoryPid = "test_basic_configuration_factory_configure_then_start";
        bundle = installBundle( factoryPid, ManagedServiceFactoryTestActivator.class );
        bundle.start();
        delay();

        final Configuration config = createFactoryConfiguration( factoryPid, null, true );
        final String pid = config.getPid();
        delay();

        // ==> configuration supplied to the service ms1
        final ManagedServiceFactoryTestActivator tester = ManagedServiceFactoryTestActivator.INSTANCE;
        Dictionary<?, ?> props = tester.configs.get( pid );
        TestCase.assertNotNull( props );
        TestCase.assertEquals( pid, props.get( Constants.SERVICE_PID ) );
        TestCase.assertEquals( factoryPid, props.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( props.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, props.get( PROP_NAME ) );
        TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 0, tester.numManagedServiceFactoryDeleteCalls );

        // delete
        config.delete();
        delay();

        // ==> update with null
        TestCase.assertNull( tester.configs.get( pid ) );
        TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryDeleteCalls );
    }


    @Test
    public void test_basic_configuration_factory_configure_then_start() throws BundleException, IOException
    {
        // 1. create config with pid and locationA
        // 2. update config with properties
        final String factoryPid = "test_basic_configuration_factory_start_then_configure";
        final Configuration config = createFactoryConfiguration( factoryPid, null, true );
        final String pid = config.getPid();
        delay();

        // 3. register ManagedService ms1 with pid from said locationA
        bundle = installBundle( factoryPid, ManagedServiceFactoryTestActivator.class );
        bundle.start();
        delay();

        // ==> configuration supplied to the service ms1
        final ManagedServiceFactoryTestActivator tester = ManagedServiceFactoryTestActivator.INSTANCE;
        Dictionary<?, ?> props = tester.configs.get( pid );
        TestCase.assertNotNull( props );
        TestCase.assertEquals( pid, props.get( Constants.SERVICE_PID ) );
        TestCase.assertEquals( factoryPid, props.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( props.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, props.get( PROP_NAME ) );
        TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 0, tester.numManagedServiceFactoryDeleteCalls );

        // delete
        config.delete();
        delay();

        // ==> update with null
        TestCase.assertNull( tester.configs.get( pid ) );
        TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryDeleteCalls );
    }


    @Test
    public void test_start_bundle_configure_stop_start_bundle() throws BundleException
    {
        String pid = "test_start_bundle_configure_stop_start_bundle";

        // start the bundle and assert this
        bundle = installBundle( pid );
        bundle.start();
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // give cm time for distribution
        delay();

        // assert activater has no configuration
        TestCase.assertNull( "Expect no Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect update call", 1, tester.numManagedServiceUpdatedCalls );

        // configure after ManagedServiceRegistration --> configure via update
        configure( pid );
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a second update call", 2, tester.numManagedServiceUpdatedCalls );

        // stop the bundle now
        bundle.stop();

        // assert INSTANCE is null
        TestCase.assertNull( ManagedServiceTestActivator.INSTANCE );

        delay();

        // start the bundle again (and check)
        bundle.start();
        final ManagedServiceTestActivator tester2 = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started the second time!!", tester2 );
        TestCase.assertNotSame( "Instances must not be the same", tester, tester2 );

        // give cm time for distribution
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester2.props );
        TestCase.assertEquals( "Expect a second update call", 1, tester2.numManagedServiceUpdatedCalls );

        // cleanup
        bundle.uninstall();
        bundle = null;

        // remove the configuration for good
        deleteConfig( pid );
    }


    @Test
    public void test_configure_start_bundle_stop_start_bundle() throws BundleException
    {
        String pid = "test_configure_start_bundle_stop_start_bundle";
        configure( pid );

        // start the bundle and assert this
        bundle = installBundle( pid );
        bundle.start();
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // give cm time for distribution
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect no update call", 1, tester.numManagedServiceUpdatedCalls );

        // stop the bundle now
        bundle.stop();

        // assert INSTANCE is null
        TestCase.assertNull( ManagedServiceTestActivator.INSTANCE );

        delay();

        // start the bundle again (and check)
        bundle.start();
        final ManagedServiceTestActivator tester2 = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started the second time!!", tester2 );
        TestCase.assertNotSame( "Instances must not be the same", tester, tester2 );

        // give cm time for distribution
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester2.props );
        TestCase.assertEquals( "Expect a second update call", 1, tester2.numManagedServiceUpdatedCalls );

        // cleanup
        bundle.uninstall();
        bundle = null;

        // remove the configuration for good
        deleteConfig( pid );
    }


    @Test
    public void test_listConfiguration() throws BundleException, IOException
    {
        // 1. create a new Conf1 with pid1 and null location.
        // 2. Conf1#update(props) is called.
        final String pid = "test_listConfiguration";
        final Configuration config = configure( pid, null, true );

        // 3. bundleA will locationA registers ManagedServiceA with pid1.
        bundle = installBundle( pid );
        bundle.start();
        delay();

        // ==> ManagedServiceA is called back.
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( tester );
        TestCase.assertNotNull( tester.props );
        TestCase.assertEquals( 1, tester.numManagedServiceUpdatedCalls );

        // 4. bundleA is stopped but *NOT uninstalled*.
        bundle.stop();
        delay();

        // 5. test bundle calls cm.listConfigurations(null).
        final Configuration listed = getConfiguration( pid );

        // ==> Conf1 is included in the returned list and
        // it has locationA.
        // (In debug mode, dynamicBundleLocation==locationA
        // and staticBundleLocation==null)
        TestCase.assertNotNull( listed );
        TestCase.assertEquals( bundle.getLocation(), listed.getBundleLocation() );

        // 6. test bundle calls cm.getConfiguration(pid1)
        final Configuration get = getConfigurationAdmin().getConfiguration( pid );
        TestCase.assertEquals( bundle.getLocation(), get.getBundleLocation() );

        final Bundle cmBundle = getCmBundle();
        cmBundle.stop();
        delay();
        cmBundle.start();
        delay();

        // 5. test bundle calls cm.listConfigurations(null).
        final Configuration listed2 = getConfiguration( pid );

        // ==> Conf1 is included in the returned list and
        // it has locationA.
        // (In debug mode, dynamicBundleLocation==locationA
        // and staticBundleLocation==null)
        TestCase.assertNotNull( listed2 );
        TestCase.assertEquals( bundle.getLocation(), listed2.getBundleLocation() );

        // 6. test bundle calls cm.getConfiguration(pid1)
        final Configuration get2 = getConfigurationAdmin().getConfiguration( pid );
        TestCase.assertEquals( bundle.getLocation(), get2.getBundleLocation() );
    }


    @Test
    public void test_ManagedService_change_pid() throws BundleException, IOException
    {
        final String pid0 = "test_ManagedService_change_pid_0";
        final String pid1 = "test_ManagedService_change_pid_1";

        final Configuration config0 = configure( pid0, null, true );
        final Configuration config1 = configure( pid1, null, true );
        delay();

        // register ManagedService ms1 with pid from said locationA
        bundle = installBundle( pid0, ManagedServiceTestActivator.class );
        bundle.start();
        delay();

        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( tester.props );
        TestCase.assertEquals( pid0, tester.props.get( Constants.SERVICE_PID ) );
        TestCase.assertNull( tester.props.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( tester.props.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, tester.props.get( PROP_NAME ) );
        TestCase.assertEquals( 1, tester.numManagedServiceUpdatedCalls );

        // change ManagedService PID
        tester.changePid( pid1 );
        delay();

        TestCase.assertNotNull( tester.props );
        TestCase.assertEquals( pid1, tester.props.get( Constants.SERVICE_PID ) );
        TestCase.assertNull( tester.props.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( tester.props.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, tester.props.get( PROP_NAME ) );
        TestCase.assertEquals( 2, tester.numManagedServiceUpdatedCalls );

        // delete
        config0.delete();
        config1.delete();
        delay();

        // ==> update with null
        TestCase.assertNull( tester.props );
        TestCase.assertEquals( 3, tester.numManagedServiceUpdatedCalls );
    }


    @Test
    public void test_ManagedService_change_pid_overlap() throws BundleException, IOException
    {
        final String pid0 = "test_ManagedService_change_pid_0";
        final String pid1 = "test_ManagedService_change_pid_1";
        final String pid2 = "test_ManagedService_change_pid_2";

        final Configuration config0 = configure( pid0, null, true );
        final Configuration config1 = configure( pid1, null, true );
        final Configuration config2 = configure( pid2, null, true );
        delay();

        // register ManagedService ms1 with pid from said locationA
        bundle = installBundle( pid0 + "," + pid1, ManagedServiceTestActivator.class );
        bundle.start();
        delay();

        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( tester.props );

        TestCase.assertEquals( pid0, tester.configs.get( pid0 ).get( Constants.SERVICE_PID ) );
        TestCase.assertNull( tester.configs.get( pid0 ).get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( tester.configs.get( pid0 ).get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, tester.configs.get( pid0 ).get( PROP_NAME ) );

        TestCase.assertEquals( pid1, tester.configs.get( pid1 ).get( Constants.SERVICE_PID ) );
        TestCase.assertNull( tester.configs.get( pid1 ).get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( tester.configs.get( pid1 ).get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, tester.configs.get( pid1 ).get( PROP_NAME ) );

        // two pids, two calls
        TestCase.assertEquals( 2, tester.numManagedServiceUpdatedCalls );

        // change ManagedService PID
        tester.changePid( pid1 + "," + pid2 );
        delay();

        TestCase.assertNotNull( tester.props );

        // config pid0 is not "removed"
        TestCase.assertEquals( pid0, tester.configs.get( pid0 ).get( Constants.SERVICE_PID ) );
        TestCase.assertNull( tester.configs.get( pid0 ).get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( tester.configs.get( pid0 ).get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, tester.configs.get( pid0 ).get( PROP_NAME ) );

        // config pid1 is retained
        TestCase.assertEquals( pid1, tester.configs.get( pid1 ).get( Constants.SERVICE_PID ) );
        TestCase.assertNull( tester.configs.get( pid1 ).get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( tester.configs.get( pid1 ).get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, tester.configs.get( pid1 ).get( PROP_NAME ) );

        // config pid2 is added
        TestCase.assertEquals( pid2, tester.configs.get( pid2 ).get( Constants.SERVICE_PID ) );
        TestCase.assertNull( tester.configs.get( pid2 ).get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( tester.configs.get( pid2 ).get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, tester.configs.get( pid2 ).get( PROP_NAME ) );

        // one "additional" pid, one additional call
        TestCase.assertEquals( 3, tester.numManagedServiceUpdatedCalls );

        // delete
        config0.delete(); // ignored by MS
        config1.delete();
        config2.delete();
        delay();

        // ==> update with null
        TestCase.assertNull( tester.props );

        // two pids removed, two calls
        TestCase.assertEquals( 5, tester.numManagedServiceUpdatedCalls );
    }


    @Test
    public void test_ManagedServiceFactory_change_pid() throws BundleException, IOException
    {

        final String factoryPid0 = "test_ManagedServiceFactory_change_pid_0";
        final String factoryPid1 = "test_ManagedServiceFactory_change_pid_1";

        final Configuration config0 = createFactoryConfiguration( factoryPid0, null, true );
        final String pid0 = config0.getPid();
        final Configuration config1 = createFactoryConfiguration( factoryPid1, null, true );
        final String pid1 = config1.getPid();
        delay();

        bundle = installBundle( factoryPid0, ManagedServiceFactoryTestActivator.class );
        bundle.start();
        delay();

        // pid0 properties provided on registration
        final ManagedServiceFactoryTestActivator tester = ManagedServiceFactoryTestActivator.INSTANCE;
        Dictionary<?, ?> props0 = tester.configs.get( pid0 );
        TestCase.assertNotNull( props0 );
        TestCase.assertEquals( pid0, props0.get( Constants.SERVICE_PID ) );
        TestCase.assertEquals( factoryPid0, props0.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( props0.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, props0.get( PROP_NAME ) );
        TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 0, tester.numManagedServiceFactoryDeleteCalls );

        // change ManagedService PID
        tester.changePid( factoryPid1 );
        delay();

        // pid1 properties must have been added
        Dictionary<?, ?> props1 = tester.configs.get( pid1 );
        TestCase.assertNotNull( props1 );
        TestCase.assertEquals( pid1, props1.get( Constants.SERVICE_PID ) );
        TestCase.assertEquals( factoryPid1, props1.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( props1.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, props1.get( PROP_NAME ) );
        TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
        TestCase.assertEquals( 2, tester.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 0, tester.numManagedServiceFactoryDeleteCalls );

        // pid0 properties must still exist !
        Dictionary<?, ?> props01 = tester.configs.get( pid0 );
        TestCase.assertNotNull( props01 );
        TestCase.assertEquals( pid0, props01.get( Constants.SERVICE_PID ) );
        TestCase.assertEquals( factoryPid0, props01.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( props01.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, props01.get( PROP_NAME ) );


        // delete
        config0.delete();
        config1.delete();
        delay();

        // only pid1 properties removed because pid0 is not registered any longer
        TestCase.assertNotNull( tester.configs.get( pid0 ) );
        TestCase.assertNull( tester.configs.get( pid1 ) );
        TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
        TestCase.assertEquals( 2, tester.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryDeleteCalls );
    }


    @Test
    public void test_ManagedServiceFactory_change_pid_overlap() throws BundleException, IOException
    {

        final String factoryPid0 = "test_ManagedServiceFactory_change_pid_0";
        final String factoryPid1 = "test_ManagedServiceFactory_change_pid_1";
        final String factoryPid2 = "test_ManagedServiceFactory_change_pid_2";

        final Configuration config0 = createFactoryConfiguration( factoryPid0, null, true );
        final String pid0 = config0.getPid();
        final Configuration config1 = createFactoryConfiguration( factoryPid1, null, true );
        final String pid1 = config1.getPid();
        final Configuration config2 = createFactoryConfiguration( factoryPid2, null, true );
        final String pid2 = config2.getPid();
        delay();

        bundle = installBundle( factoryPid0 + "," + factoryPid1, ManagedServiceFactoryTestActivator.class );
        bundle.start();
        delay();

        // pid0 properties provided on registration
        final ManagedServiceFactoryTestActivator tester = ManagedServiceFactoryTestActivator.INSTANCE;
        Dictionary<?, ?> props0 = tester.configs.get( pid0 );
        TestCase.assertNotNull( props0 );
        TestCase.assertEquals( pid0, props0.get( Constants.SERVICE_PID ) );
        TestCase.assertEquals( factoryPid0, props0.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( props0.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, props0.get( PROP_NAME ) );

        Dictionary<?, ?> props1 = tester.configs.get( pid1 );
        TestCase.assertNotNull( props1 );
        TestCase.assertEquals( pid1, props1.get( Constants.SERVICE_PID ) );
        TestCase.assertEquals( factoryPid1, props1.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( props1.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, props1.get( PROP_NAME ) );

        TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
        TestCase.assertEquals( 2, tester.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 0, tester.numManagedServiceFactoryDeleteCalls );

        // change ManagedService PID
        tester.changePid( factoryPid1 + "," + factoryPid2 );
        delay();

        // pid2 properties must have been added
        Dictionary<?, ?> props2 = tester.configs.get( pid2 );
        TestCase.assertNotNull( props2 );
        TestCase.assertEquals( pid2, props2.get( Constants.SERVICE_PID ) );
        TestCase.assertEquals( factoryPid2, props2.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( props2.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, props2.get( PROP_NAME ) );

        // pid0 properties must still exist !
        Dictionary<?, ?> props01 = tester.configs.get( pid0 );
        TestCase.assertNotNull( props01 );
        TestCase.assertEquals( pid0, props01.get( Constants.SERVICE_PID ) );
        TestCase.assertEquals( factoryPid0, props01.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( props01.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, props01.get( PROP_NAME ) );

        // pid1 properties must still exist !
        Dictionary<?, ?> props11 = tester.configs.get( pid1 );
        TestCase.assertNotNull( props11 );
        TestCase.assertEquals( pid1, props11.get( Constants.SERVICE_PID ) );
        TestCase.assertEquals( factoryPid1, props11.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( props11.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, props11.get( PROP_NAME ) );

        TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
        TestCase.assertEquals( 3, tester.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 0, tester.numManagedServiceFactoryDeleteCalls );

        // delete
        config0.delete();
        config1.delete();
        config2.delete();
        delay();

        // only pid1 and pid2 properties removed because pid0 is not registered any longer
        TestCase.assertNotNull( tester.configs.get( pid0 ) );
        TestCase.assertNull( tester.configs.get( pid1 ) );
        TestCase.assertNull( tester.configs.get( pid2 ) );

        TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
        TestCase.assertEquals( 3, tester.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 2, tester.numManagedServiceFactoryDeleteCalls );
    }


    @Test
    public void test_factory_configuration_collision() throws IOException, InvalidSyntaxException, BundleException {
        final String factoryPid = "test_factory_configuration_collision";

        final Configuration cf = getConfigurationAdmin().createFactoryConfiguration( factoryPid, null );
        TestCase.assertNotNull( cf );
        final String pid = cf.getPid();

        // check factory configuration setup
        TestCase.assertNotNull( "Configuration must have PID", pid );
        TestCase.assertEquals( "Factory configuration must have requested factory PID", factoryPid, cf.getFactoryPid() );

        try
        {
            bundle = installBundle( factoryPid, ManagedServiceFactoryTestActivator.class );
            bundle.start();
            delay();

            final ManagedServiceFactoryTestActivator tester = ManagedServiceFactoryTestActivator.INSTANCE;
            TestCase.assertEquals( "MSF must not be updated with new configuration", 0, tester.numManagedServiceFactoryUpdatedCalls );

            // assert getConfiguration returns the same configurtion
            final Configuration c1 = getConfigurationAdmin().getConfiguration( pid, null );
            TestCase.assertEquals( "getConfiguration must retrieve required PID", pid, c1.getPid() );
            TestCase.assertEquals( "getConfiguration must retrieve new factory configuration", factoryPid, c1.getFactoryPid() );
            TestCase.assertNull( "Configuration must not have properties", c1.getProperties() );

            TestCase.assertEquals( "MSF must not be updated with new configuration", 0, tester.numManagedServiceFactoryUpdatedCalls );

            // restart config admin and verify getConfiguration persisted
            // the new factory configuration as such
            final Bundle cmBundle = getCmBundle();
            TestCase.assertNotNull( "Config Admin Bundle missing", cmBundle );
            cmBundle.stop();
            delay();
            cmBundle.start();
            delay();

            TestCase.assertEquals( "MSF must not be updated with new configuration even after CM restart", 0, tester.numManagedServiceFactoryUpdatedCalls );

            final Configuration c2 = getConfigurationAdmin().getConfiguration( pid, null );
            TestCase.assertEquals( "getConfiguration must retrieve required PID", pid, c2.getPid() );
            TestCase.assertEquals( "getConfiguration must retrieve new factory configuration from persistence", factoryPid, c2.getFactoryPid() );
            TestCase.assertNull( "Configuration must not have properties", c2.getProperties() );

            c2.update( theConfig );
            delay();

            TestCase.assertEquals( 1, tester.numManagedServiceFactoryUpdatedCalls );
            TestCase.assertEquals( theConfig.get( PROP_NAME ), tester.configs.get( cf.getPid() ).get( PROP_NAME ) );

            final Configuration[] cfs = getConfigurationAdmin().listConfigurations( "(" + ConfigurationAdmin.SERVICE_FACTORYPID + "="
                + factoryPid + ")" );
            TestCase.assertNotNull( "Expect at least one configuration", cfs );
            TestCase.assertEquals( "Expect exactly one configuration", 1, cfs.length );
            TestCase.assertEquals( cf.getPid(), cfs[0].getPid() );
            TestCase.assertEquals( cf.getFactoryPid(), cfs[0].getFactoryPid() );
        }
        finally
        {
            // make sure no configuration survives ...
            getConfigurationAdmin().getConfiguration( pid, null ).delete();
        }
    }

   @Test
    public void test_collection_property_order() throws IOException, BundleException
    {
        final String pid = "test_collection_property_order";
        final String[] value = new String[]
            { "a", "b", "c" };
        final Bundle cmBundle = getCmBundle();
        try
        {
            final Vector v = new Vector( Arrays.asList( value ) );
            getConfigurationAdmin().getConfiguration( pid ).update( new Hashtable()
            {
                {
                    put( "v", v );
                }
            } );
            assertOrder( value, getConfigurationAdmin().getConfiguration( pid ).getProperties().get( "v" ) );

            cmBundle.stop();
            cmBundle.start();

            assertOrder( value, getConfigurationAdmin().getConfiguration( pid ).getProperties().get( "v" ) );
            getConfigurationAdmin().getConfiguration( pid, null ).delete();

            final List l = Arrays.asList( value );
            getConfigurationAdmin().getConfiguration( pid ).update( new Hashtable()
            {
                {
                    put( "v", l );
                }
            } );
            assertOrder( value, getConfigurationAdmin().getConfiguration( pid ).getProperties().get( "v" ) );

            cmBundle.stop();
            cmBundle.start();

            assertOrder( value, getConfigurationAdmin().getConfiguration( pid ).getProperties().get( "v" ) );
            getConfigurationAdmin().getConfiguration( pid, null ).delete();
        }
        finally
        {
            // make sure no configuration survives ...
            getConfigurationAdmin().getConfiguration( pid, null ).delete();
        }
    }


    private void assertOrder( final String[] expected, final Object actual )
    {
        TestCase.assertTrue( "Actual value must be a collection", actual instanceof Collection );
        TestCase.assertEquals( "Collection must have " + expected.length + " entries", expected.length,
            ( ( Collection ) actual ).size() );

        final Iterator actualI = ( ( Collection ) actual ).iterator();
        for ( int i = 0; i < expected.length; i++ )
        {
            String string = expected[i];
            TestCase.assertEquals( i + "th element must be " + string, string, actualI.next() );
        }
    }
}
