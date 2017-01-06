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
import junit.framework.TestCase;

import org.apache.felix.cm.integration.helper.ManagedServiceFactoryTestActivator;
import org.apache.felix.cm.integration.helper.ManagedServiceFactoryTestActivator2;
import org.apache.felix.cm.integration.helper.ManagedServiceTestActivator;
import org.apache.felix.cm.integration.helper.ManagedServiceTestActivator2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;


@RunWith(JUnit4TestRunner.class)
public class ConfigurationBindingTest extends ConfigurationTestBase
{

    static
    {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }


    private ConfigListener configListener;
    private ServiceRegistration configListenerReg;


    @Override
    public void setUp()
    {
        super.setUp();

        configListener = new ConfigListener();
        configListenerReg = bundleContext.registerService( ConfigurationListener.class.getName(), configListener, null );
    }


    @Override
    public void tearDown() throws BundleException
    {
        if ( configListenerReg != null )
        {
            configListenerReg.unregister();
            configListenerReg = null;
        }
        configListener = null;

        super.tearDown();
    }


     @Test
    public void test_configuration_unbound_on_uninstall() throws BundleException
    {
        String pid = "test_configuration_unbound_on_uninstall";
        configure( pid );

        delay(); // for the event to be distributed
        configListener.assertEvents( ConfigurationEvent.CM_UPDATED, 1 );

        // ensure configuration is unbound
        final Configuration beforeInstall = getConfiguration( pid );
        TestCase.assertNull( beforeInstall.getBundleLocation() );

        bundle = installBundle( pid );

        // ensure no configuration bound before start
        final Configuration beforeStart = getConfiguration( pid );
        TestCase.assertNull( beforeInstall.getBundleLocation() );
        TestCase.assertNull( beforeStart.getBundleLocation() );
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 0 );

        bundle.start();
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // give cm time for distribution
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numManagedServiceUpdatedCalls );
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 1 );

        // ensure a freshly retrieved object also has the location
        final Configuration beforeStop = getConfiguration( pid );
        TestCase.assertEquals( beforeStop.getBundleLocation(), bundle.getLocation() );

        // check whether bundle context is set on first configuration
        TestCase.assertEquals( beforeInstall.getBundleLocation(), bundle.getLocation() );
        TestCase.assertEquals( beforeStart.getBundleLocation(), bundle.getLocation() );

        bundle.stop();

        delay();

        // ensure configuration still bound
        TestCase.assertEquals( beforeInstall.getBundleLocation(), bundle.getLocation() );
        TestCase.assertEquals( beforeStart.getBundleLocation(), bundle.getLocation() );
        TestCase.assertEquals( beforeStop.getBundleLocation(), bundle.getLocation() );

        // ensure a freshly retrieved object also has the location
        final Configuration beforeUninstall = getConfiguration( pid );
        TestCase.assertEquals( beforeUninstall.getBundleLocation(), bundle.getLocation() );

        bundle.uninstall();
        bundle = null;

        delay();

        // ensure configuration is not bound any more
        TestCase.assertNull( beforeInstall.getBundleLocation() );
        TestCase.assertNull( beforeStart.getBundleLocation() );
        TestCase.assertNull( beforeStop.getBundleLocation() );
        TestCase.assertNull( beforeUninstall.getBundleLocation() );
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 1 );

        // ensure a freshly retrieved object also does not have the location
        final Configuration atEnd = getConfiguration( pid );
        TestCase.assertNull( atEnd.getBundleLocation() );

        // remove the configuration for good
        deleteConfig( pid );
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_DELETED, 1 );
    }


     @Test
    public void test_configuration_unbound_on_uninstall_with_cm_restart() throws BundleException
    {
        final String pid = "test_configuration_unbound_on_uninstall_with_cm_restart";
        configure( pid );
        final Bundle cmBundle = getCmBundle();

        // ensure configuration is unbound
        final Configuration beforeInstall = getConfiguration( pid );
        TestCase.assertNull( beforeInstall.getBundleLocation() );

        bundle = installBundle( pid );

        // ensure no configuration bound before start
        final Configuration beforeStart = getConfiguration( pid );
        TestCase.assertNull( beforeInstall.getBundleLocation() );
        TestCase.assertNull( beforeStart.getBundleLocation() );

        bundle.start();
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "IOActivator not started !!", tester );

        // give cm time for distribution
        delay();

        // assert activater has configuration
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numManagedServiceUpdatedCalls );

        // ensure a freshly retrieved object also has the location
        final Configuration beforeStop = getConfiguration( pid );
        TestCase.assertEquals( beforeStop.getBundleLocation(), bundle.getLocation() );

        // check whether bundle context is set on first configuration
        TestCase.assertEquals( beforeInstall.getBundleLocation(), bundle.getLocation() );
        TestCase.assertEquals( beforeStart.getBundleLocation(), bundle.getLocation() );

        bundle.stop();

        // ensure configuration still bound
        TestCase.assertEquals( beforeInstall.getBundleLocation(), bundle.getLocation() );
        TestCase.assertEquals( beforeStart.getBundleLocation(), bundle.getLocation() );
        TestCase.assertEquals( beforeStop.getBundleLocation(), bundle.getLocation() );

        // ensure a freshly retrieved object also has the location
        final Configuration beforeUninstall = getConfiguration( pid );
        TestCase.assertEquals( beforeUninstall.getBundleLocation(), bundle.getLocation() );

        // stop cm bundle now before uninstalling configured bundle
        cmBundle.stop();
        delay();

        // assert configuration admin service is gone
        TestCase.assertNull( configAdminTracker.getService() );

        // uninstall bundle while configuration admin is stopped
        bundle.uninstall();
        bundle = null;

        // start cm bundle again after uninstallation
        cmBundle.start();
        delay();

        // ensure a freshly retrieved object also does not have the location
        // FELIX-1484: this test fails due to bundle location not verified
        // at first configuration access
        final Configuration atEnd = getConfiguration( pid );
        TestCase.assertNull( atEnd.getBundleLocation() );

        // remove the configuration for good
        deleteConfig( pid );
    }


     @Test
    public void test_not_updated_new_configuration_not_bound_after_bundle_uninstall() throws IOException,
        BundleException
    {
        final String pid = "test_not_updated_new_configuration_not_bound_after_bundle_uninstall";

        // create a configuration but do not update with properties
        final Configuration newConfig = configure( pid, null, false );
        TestCase.assertNull( newConfig.getProperties() );
        TestCase.assertNull( newConfig.getBundleLocation() );

        // start and settle bundle
        bundle = installBundle( pid );
        bundle.start();
        delay();

        // ensure no properties provided to bundle
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );
        TestCase.assertNull( "Expect no properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numManagedServiceUpdatedCalls );

        // assert configuration is still unset but bound
        TestCase.assertNull( newConfig.getProperties() );
        TestCase.assertEquals( bundle.getLocation(), newConfig.getBundleLocation() );

        // uninstall bundle, should unbind configuration
        bundle.uninstall();
        bundle = null;

        delay();

        // assert configuration is still unset and unbound
        TestCase.assertNull( newConfig.getProperties() );
        TestCase.assertNull( newConfig.getBundleLocation() );

        // remove the configuration for good
        deleteConfig( pid );
    }


     @Test
    public void test_create_with_location_unbind_before_service_supply() throws BundleException, IOException
    {

        final String pid = "test_create_with_location_unbind_before_service_supply";
        final String dummyLocation = "http://some/dummy/location";

        // 1. create and statically bind the configuration
        final Configuration config = configure( pid, dummyLocation, false );
        TestCase.assertEquals( pid, config.getPid() );
        TestCase.assertEquals( dummyLocation, config.getBundleLocation() );

        // 2. update configuration
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( PROP_NAME, PROP_NAME );
        config.update( props );
        TestCase.assertEquals( PROP_NAME, config.getProperties().get( PROP_NAME ) );
        TestCase.assertEquals( pid, config.getPid() );
        TestCase.assertEquals( dummyLocation, config.getBundleLocation() );

        // 3. (statically) set location to null
        config.setBundleLocation( null );
        TestCase.assertNull( config.getBundleLocation() );

        // 4. install bundle with service
        bundle = installBundle( pid );
        bundle.start();
        delay();

        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // assert activater has configuration (two calls, one per pid)
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numManagedServiceUpdatedCalls );

        TestCase.assertEquals( bundle.getLocation(), config.getBundleLocation() );

        bundle.uninstall();
        bundle = null;

        delay();

        // statically bound configurations must remain bound after bundle
        // uninstall
        TestCase.assertNull( config.getBundleLocation() );

        // remove the configuration for good
        deleteConfig( pid );
    }


     @Test
    public void test_statically_bound() throws BundleException
    {
        final String pid = "test_statically_bound";

        // install the bundle (we need the location)
        bundle = installBundle( pid );
        final String location = bundle.getLocation();

        // create and statically bind the configuration
        configure( pid );
        final Configuration config = getConfiguration( pid );
        TestCase.assertEquals( pid, config.getPid() );
        TestCase.assertNull( config.getBundleLocation() );
        config.setBundleLocation( location );
        TestCase.assertEquals( location, config.getBundleLocation() );

        // ensure configuration is settled before starting the bundle
        delay();

        // expect single config update and location change
        configListener.assertEvents( ConfigurationEvent.CM_UPDATED, 1 );
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 1 );

        bundle.start();

        // give cm time for distribution
        delay();

        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // assert activater has configuration (two calls, one per pid)
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numManagedServiceUpdatedCalls );

        TestCase.assertEquals( location, config.getBundleLocation() );

        // config already statically bound, no change event
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 0 );

        bundle.uninstall();
        bundle = null;

        delay();

        // statically bound configurations must remain bound after bundle
        // uninstall
        TestCase.assertEquals( location, config.getBundleLocation() );

        // configuration statically bound, no change event
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 0 );

        // remove the configuration for good
        deleteConfig( pid );

        delay();
        configListener.assertEvents( ConfigurationEvent.CM_DELETED, 1 );
        configListener.assertEvents( ConfigurationEvent.CM_UPDATED, 0 );
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 0 );
    }


     @Test
    public void test_static_binding_and_unbinding() throws BundleException
    {
        final String pid = "test_static_binding_and_unbinding";
        final String location = bundleContext.getBundle().getLocation();

        // create and statically bind the configuration
        configure( pid );
        final Configuration config = getConfiguration( pid );
        TestCase.assertEquals( pid, config.getPid() );
        TestCase.assertNull( config.getBundleLocation() );

        // first configuration updated event
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_UPDATED, 1 );

        // bind the configuration
        config.setBundleLocation( location );
        TestCase.assertEquals( location, config.getBundleLocation() );
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 1 );

        // restart CM bundle
        final Bundle cmBundle = getCmBundle();
        cmBundle.stop();
        delay();
        cmBundle.start();

        // assert configuration still bound
        final Configuration configAfterRestart = getConfiguration( pid );
        TestCase.assertEquals( pid, configAfterRestart.getPid() );
        TestCase.assertEquals( location, configAfterRestart.getBundleLocation() );

        // unbind the configuration
        configAfterRestart.setBundleLocation( null );
        TestCase.assertNull( configAfterRestart.getBundleLocation() );
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 1 );

        // restart CM bundle
        cmBundle.stop();
        delay();
        cmBundle.start();

        // assert configuration unbound
        final Configuration configUnboundAfterRestart = getConfiguration( pid );
        TestCase.assertEquals( pid, configUnboundAfterRestart.getPid() );
        TestCase.assertNull( configUnboundAfterRestart.getBundleLocation() );

        configListener.assertEvents( ConfigurationEvent.CM_DELETED, 0 );
        configListener.assertEvents( ConfigurationEvent.CM_UPDATED, 0 );
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 0 );
    }


     @Test
    public void test_dynamic_binding_and_unbinding() throws BundleException
    {
        final String pid = "test_dynamic_binding_and_unbinding";

        // create and statically bind the configuration
        configure( pid );
        final Configuration config = getConfiguration( pid );
        TestCase.assertEquals( pid, config.getPid() );
        TestCase.assertNull( config.getBundleLocation() );

        // dynamically bind the configuration
        bundle = installBundle( pid );
        final String location = bundle.getLocation();
        bundle.start();
        delay();
        TestCase.assertEquals( location, config.getBundleLocation() );

        // restart CM bundle
        final Bundle cmBundle = getCmBundle();
        cmBundle.stop();
        delay();
        cmBundle.start();

        // assert configuration still bound
        final Configuration configAfterRestart = getConfiguration( pid );
        TestCase.assertEquals( pid, configAfterRestart.getPid() );
        TestCase.assertEquals( location, configAfterRestart.getBundleLocation() );

        // stop bundle (configuration remains bound !!)
        bundle.stop();
        delay();
        TestCase.assertEquals( location, configAfterRestart.getBundleLocation() );

        // restart CM bundle
        cmBundle.stop();
        delay();
        cmBundle.start();

        // assert configuration still bound
        final Configuration configBoundAfterRestart = getConfiguration( pid );
        TestCase.assertEquals( pid, configBoundAfterRestart.getPid() );
        TestCase.assertEquals( location, configBoundAfterRestart.getBundleLocation() );
    }


     @Test
    public void test_static_binding() throws BundleException
    {
        final String pid = "test_static_binding";

        // install a bundle to get a location for binding
        bundle = installBundle( pid );
        final String location = bundle.getLocation();

        // create and statically bind the configuration
        configure( pid );
        final Configuration config = getConfiguration( pid );
        TestCase.assertEquals( pid, config.getPid() );
        TestCase.assertNull( config.getBundleLocation() );
        config.setBundleLocation( location );
        TestCase.assertEquals( location, config.getBundleLocation() );

        // ensure configuration is settled before starting the bundle
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_UPDATED, 1 );

        // start the bundle
        bundle.start();
        delay();
        TestCase.assertEquals( location, config.getBundleLocation() );
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 1 );

        // assert the configuration is supplied
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );
        TestCase.assertNotNull( "Expect Properties after Service Registration", tester.props );
        TestCase.assertEquals( "Expect a single update call", 1, tester.numManagedServiceUpdatedCalls );

        // remove the static binding and assert bound (again)
        config.setBundleLocation( null );
        delay();
        TestCase.assertEquals( location, config.getBundleLocation() );
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 2 );

        // uninstall bundle and assert configuration unbound
        bundle.uninstall();
        bundle = null;
        delay();
        TestCase.assertNull( config.getBundleLocation() );
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 1 );
    }


     @Test
    public void test_two_bundles_one_pid() throws BundleException, IOException
    {
        // 1. Bundle registers service with pid1
        final String pid = "test_two_bundles_one_pid";
        final Bundle bundleA = installBundle( pid, ManagedServiceTestActivator.class );
        final String locationA = bundleA.getLocation();
        bundleA.start();
        delay();

        // call back with null
        final ManagedServiceTestActivator tester = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNull( tester.props );
        TestCase.assertEquals( 1, tester.numManagedServiceUpdatedCalls );

        // 2. create new Conf with pid1 and locationA.
        final Configuration config = configure( pid, locationA, false );
        delay();

        // ==> No call back.
        TestCase.assertNull( tester.props );
        TestCase.assertEquals( 1, tester.numManagedServiceUpdatedCalls );

        // 3. Configuration#update(prop) is called.
        config.update( theConfig );
        delay();

        // ==> call back with the prop.
        TestCase.assertNotNull( tester.props );
        TestCase.assertEquals( 2, tester.numManagedServiceUpdatedCalls );

        // 4. Stop BundleA
        bundleA.stop();
        delay();

        // 5. Start BundleA
        bundleA.start();
        delay();

        // ==> call back with the prop.
        final ManagedServiceTestActivator tester2 = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( tester2.props );
        TestCase.assertEquals( 1, tester2.numManagedServiceUpdatedCalls );

        // 6. Configuration#deleted() is called.
        config.delete();
        delay();

        // ==> call back with null.
        TestCase.assertNull( tester2.props );
        TestCase.assertEquals( 2, tester2.numManagedServiceUpdatedCalls );

        // 7. uninstall Bundle A for cleanup.
        bundleA.uninstall();
        delay();

        // Test 2

        // 8. BundleA registers ManagedService with pid1.
        final Bundle bundleA2 = installBundle( pid, ManagedServiceTestActivator.class );
        final String locationA2 = bundleA.getLocation();
        bundleA2.start();
        delay();

        // call back with null
        final ManagedServiceTestActivator tester21 = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNull( tester21.props );
        TestCase.assertEquals( 1, tester21.numManagedServiceUpdatedCalls );

        // 9. create new Conf with pid1 and locationB.
        final String locationB = "test:locationB/" + pid;
        final Configuration configB = configure( pid, locationB, false );
        delay();

        // ==> No call back.
        TestCase.assertNull( tester21.props );
        TestCase.assertEquals( 1, tester21.numManagedServiceUpdatedCalls );

        // 10. Configuration#update(prop) is called.
        configB.update( theConfig );
        delay();

        // ==> No call back because the Conf is not bound to locationA.
        TestCase.assertNull( tester21.props );
        TestCase.assertEquals( 1, tester21.numManagedServiceUpdatedCalls );
    }


    @Test
    public void test_switch_static_binding() throws BundleException
    {
        // 1. create config with pid and locationA
        // 2. update config with properties
        final String pid = "test_switch_static_binding";
        final String locationA = "test:location/A/" + pid;
        final Configuration config = configure( pid, locationA, true );

        // 3. register ManagedService ms1 with pid from said locationA
        final Bundle bundleA = installBundle( pid, ManagedServiceTestActivator.class, locationA );
        bundleA.start();
        delay();

        // ==> configuration supplied to the service ms1
        final ManagedServiceTestActivator testerA1 = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( testerA1.props );
        TestCase.assertEquals( 1, testerA1.numManagedServiceUpdatedCalls );

        // 4. register ManagedService ms2 with pid from locationB
        final String locationB = "test:location/B/" + pid;
        final Bundle bundleB = installBundle( pid, ManagedServiceTestActivator2.class, locationB );
        bundleB.start();
        delay();

        // ==> invisible configuration supplied as null to service ms2
        final ManagedServiceTestActivator2 testerB1 = ManagedServiceTestActivator2.INSTANCE;
        TestCase.assertNull( testerB1.props );
        TestCase.assertEquals( 1, testerB1.numManagedServiceUpdatedCalls );

        // 5. Call Configuration.setBundleLocation( "locationB" )
        config.setBundleLocation( locationB );
        delay();

        // ==> configuration is bound to locationB
        TestCase.assertEquals( locationB, config.getBundleLocation() );

        // ==> configuration removed from service ms1
        TestCase.assertNull( testerA1.props );
        TestCase.assertEquals( 2, testerA1.numManagedServiceUpdatedCalls );

        // ==> configuration supplied to the service ms2
        TestCase.assertNotNull( testerB1.props );
        TestCase.assertEquals( 2, testerB1.numManagedServiceUpdatedCalls );
    }


    @Test
    public void test_switch_dynamic_binding() throws BundleException, IOException
    {
        // 1. create config with pid with null location
        // 2. update config with properties
        final String pid = "test_switch_dynamic_binding";
        final String locationA = "test:location/A/" + pid;
        final Configuration config = configure( pid, null, true );

        // 3. register ManagedService ms1 with pid from locationA
        final Bundle bundleA = installBundle( pid, ManagedServiceTestActivator.class, locationA );
        bundleA.start();
        delay();

        // ==> configuration supplied to the service ms1
        final ManagedServiceTestActivator testerA1 = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( testerA1.props );
        TestCase.assertEquals( 1, testerA1.numManagedServiceUpdatedCalls );

        // ==> configuration is dynamically bound to locationA
        TestCase.assertEquals( locationA, config.getBundleLocation() );

        // 4. register ManagedService ms2 with pid from locationB
        final String locationB = "test:location/B/" + pid;
        final Bundle bundleB = installBundle( pid, ManagedServiceTestActivator2.class, locationB );
        bundleB.start();
        delay();

        // ==> invisible configuration supplied as null to service ms2
        final ManagedServiceTestActivator2 testerB1 = ManagedServiceTestActivator2.INSTANCE;
        TestCase.assertNull( testerB1.props );
        TestCase.assertEquals( 1, testerB1.numManagedServiceUpdatedCalls );

        // 5. Call Configuration.setBundleLocation( "locationB" )
        config.setBundleLocation( locationB );
        delay();

        // ==> configuration is bound to locationB
        TestCase.assertEquals( locationB, config.getBundleLocation() );

        // ==> configuration removed from service ms1
        TestCase.assertNull( testerA1.props );
        TestCase.assertEquals( 2, testerA1.numManagedServiceUpdatedCalls );

        // ==> configuration supplied to the service ms2
        TestCase.assertNotNull( testerB1.props );
        TestCase.assertEquals( 2, testerB1.numManagedServiceUpdatedCalls );

        // 6. Update configuration now
        config.update();
        delay();

        // ==> configuration supplied to the service ms2
        TestCase.assertNotNull( testerB1.props );
        TestCase.assertEquals( 3, testerB1.numManagedServiceUpdatedCalls );
    }


    @Test
    public void test_switch_static_binding_factory() throws BundleException, IOException
    {
        // 1. create config with pid and locationA
        // 2. update config with properties
        final String factoryPid = "test_switch_static_binding_factory";
        final String locationA = "test:location/A/" + factoryPid;
        final Configuration config = createFactoryConfiguration( factoryPid, locationA, true );
        final String pid = config.getPid();

        // 3. register ManagedService ms1 with pid from said locationA
        final Bundle bundleA = installBundle( factoryPid, ManagedServiceFactoryTestActivator.class, locationA );
        bundleA.start();
        delay();

        // ==> configuration supplied to the service ms1
        final ManagedServiceFactoryTestActivator testerA1 = ManagedServiceFactoryTestActivator.INSTANCE;
        TestCase.assertNotNull( testerA1.configs.get( pid ) );
        TestCase.assertEquals( 1, testerA1.numManagedServiceFactoryUpdatedCalls );

        // 4. register ManagedService ms2 with pid from locationB
        final String locationB = "test:location/B/" + factoryPid;
        final Bundle bundleB = installBundle( factoryPid, ManagedServiceFactoryTestActivator2.class, locationB );
        bundleB.start();
        delay();

        // ==> configuration not supplied to service ms2
        final ManagedServiceFactoryTestActivator2 testerB1 = ManagedServiceFactoryTestActivator2.INSTANCE;
        TestCase.assertNull( testerB1.configs.get( pid ));
        TestCase.assertEquals( 0, testerB1.numManagedServiceFactoryUpdatedCalls );

        // 5. Call Configuration.setBundleLocation( "locationB" )
        config.setBundleLocation( locationB );
        delay();

        // ==> configuration is bound to locationB
        TestCase.assertEquals( locationB, config.getBundleLocation() );

        // ==> configuration removed from service ms1
        TestCase.assertNull( testerA1.configs.get( pid ));
        TestCase.assertEquals( 1, testerA1.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 1, testerA1.numManagedServiceFactoryDeleteCalls );

        // ==> configuration supplied to the service ms2
        TestCase.assertNotNull( testerB1.configs.get( pid ) );
        TestCase.assertEquals( 1, testerB1.numManagedServiceFactoryUpdatedCalls );

        // 6. Update configuration now
        config.update();
        delay();

        // ==> configuration supplied to the service ms2
        TestCase.assertNotNull( testerB1.configs.get( pid ) );
        TestCase.assertEquals( 2, testerB1.numManagedServiceFactoryUpdatedCalls );
   }


    @Test
    public void test_switch_dynamic_binding_factory() throws BundleException, IOException
    {
        // 1. create config with pid and locationA
        // 2. update config with properties
        final String factoryPid = "test_switch_static_binding_factory";
        final String locationA = "test:location/A/" + factoryPid;
        final Configuration config = createFactoryConfiguration( factoryPid, null, true );
        final String pid = config.getPid();

        TestCase.assertNull( config.getBundleLocation() );

        // 3. register ManagedService ms1 with pid from said locationA
        final Bundle bundleA = installBundle( factoryPid, ManagedServiceFactoryTestActivator.class, locationA );
        bundleA.start();
        delay();

        // ==> configuration supplied to the service ms1
        final ManagedServiceFactoryTestActivator testerA1 = ManagedServiceFactoryTestActivator.INSTANCE;
        TestCase.assertNotNull( testerA1.configs.get( pid ) );
        TestCase.assertEquals( 1, testerA1.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( locationA, config.getBundleLocation() );

        // 4. register ManagedService ms2 with pid from locationB
        final String locationB = "test:location/B/" + factoryPid;
        final Bundle bundleB = installBundle( factoryPid, ManagedServiceFactoryTestActivator2.class, locationB );
        bundleB.start();
        delay();

        // ==> configuration not supplied to service ms2
        final ManagedServiceFactoryTestActivator2 testerB1 = ManagedServiceFactoryTestActivator2.INSTANCE;
        TestCase.assertNull( testerB1.configs.get( pid ));
        TestCase.assertEquals( 0, testerB1.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( locationA, config.getBundleLocation() );

        // 5. Call Configuration.setBundleLocation( "locationB" )
        config.setBundleLocation( locationB );
        delay();

        // ==> configuration is bound to locationB
        TestCase.assertEquals( locationB, config.getBundleLocation() );

        // ==> configuration removed from service ms1
        TestCase.assertNull( testerA1.configs.get( pid ));
        TestCase.assertEquals( 1, testerA1.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 1, testerA1.numManagedServiceFactoryDeleteCalls );

        // ==> configuration supplied to the service ms2
        TestCase.assertNotNull( testerB1.configs.get( pid ) );
        TestCase.assertEquals( 1, testerB1.numManagedServiceFactoryUpdatedCalls );

        // 6. Update configuration now
        config.update();
        delay();

        // ==> configuration supplied to the service ms2
        TestCase.assertNotNull( testerB1.configs.get( pid ) );
        TestCase.assertEquals( 2, testerB1.numManagedServiceFactoryUpdatedCalls );
    }


    @Test
    public void test_switch_dynamic_binding_after_uninstall() throws BundleException, IOException
    {
        // 1. create config with pid with null location
        // 2. update config with properties
        final String pid = "test_switch_dynamic_binding";
        final String locationA = "test:location/A/" + pid;
        final Configuration config = configure( pid, null, true );

        TestCase.assertNull( config.getBundleLocation() );

        // 3. register ManagedService ms1 with pid from locationA
        final Bundle bundleA = installBundle( pid, ManagedServiceTestActivator.class, locationA );
        bundleA.start();
        delay();

        // ==> configuration supplied to the service ms1
        final ManagedServiceTestActivator testerA1 = ManagedServiceTestActivator.INSTANCE;
        TestCase.assertNotNull( testerA1.props );
        TestCase.assertEquals( 1, testerA1.numManagedServiceUpdatedCalls );

        // ==> configuration is dynamically bound to locationA
        TestCase.assertEquals( locationA, config.getBundleLocation() );

        // 4. register ManagedService ms2 with pid from locationB
        final String locationB = "test:location/B/" + pid;
        final Bundle bundleB = installBundle( pid, ManagedServiceTestActivator2.class, locationB );
        bundleB.start();
        delay();

        // ==> invisible configuration supplied as null to service ms2
        final ManagedServiceTestActivator2 testerB1 = ManagedServiceTestActivator2.INSTANCE;
        TestCase.assertNull( testerB1.props );
        TestCase.assertEquals( 1, testerB1.numManagedServiceUpdatedCalls );

        // 5. Uninstall bundle A
        bundleA.uninstall();
        delay();

        // ==> configuration is bound to locationB
        TestCase.assertEquals( locationB, config.getBundleLocation() );

        // ==> configuration supplied to the service ms2
        TestCase.assertNotNull( testerB1.props );
        TestCase.assertEquals( 2, testerB1.numManagedServiceUpdatedCalls );

        // 6. Update configuration now
        config.update();
        delay();

        // ==> configuration supplied to the service ms2
        TestCase.assertNotNull( testerB1.props );
        TestCase.assertEquals( 3, testerB1.numManagedServiceUpdatedCalls );
    }


    @Test
    public void test_switch_dynamic_binding_factory_after_uninstall() throws BundleException, IOException
    {
        // 1. create config with pid and locationA
        // 2. update config with properties
        final String factoryPid = "test_switch_static_binding_factory";
        final String locationA = "test:location/A/" + factoryPid;
        final Configuration config = createFactoryConfiguration( factoryPid, null, true );
        final String pid = config.getPid();

        TestCase.assertNull( config.getBundleLocation() );

        // 3. register ManagedService ms1 with pid from said locationA
        final Bundle bundleA = installBundle( factoryPid, ManagedServiceFactoryTestActivator.class, locationA );
        bundleA.start();
        delay();

        // ==> configuration supplied to the service ms1
        final ManagedServiceFactoryTestActivator testerA1 = ManagedServiceFactoryTestActivator.INSTANCE;
        TestCase.assertNotNull( testerA1.configs.get( pid ) );
        TestCase.assertEquals( 1, testerA1.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( locationA, config.getBundleLocation() );

        // 4. register ManagedService ms2 with pid from locationB
        final String locationB = "test:location/B/" + factoryPid;
        final Bundle bundleB = installBundle( factoryPid, ManagedServiceFactoryTestActivator2.class, locationB );
        bundleB.start();
        delay();

        // ==> configuration not supplied to service ms2
        final ManagedServiceFactoryTestActivator2 testerB1 = ManagedServiceFactoryTestActivator2.INSTANCE;
        TestCase.assertNull( testerB1.configs.get( pid ));
        TestCase.assertEquals( 0, testerB1.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( locationA, config.getBundleLocation() );

        // 5. Uninstall bundle A
        bundleA.uninstall();
        delay();

        // ==> configuration is bound to locationB
        TestCase.assertEquals( locationB, config.getBundleLocation() );

        // ==> configuration supplied to the service ms2
        TestCase.assertNotNull( testerB1.configs.get( pid ) );
        TestCase.assertEquals( 1, testerB1.numManagedServiceFactoryUpdatedCalls );

        // 6. Update configuration now
        config.update();
        delay();

        // ==> configuration supplied to the service ms2
        TestCase.assertNotNull( testerB1.configs.get( pid ) );
        TestCase.assertEquals( 2, testerB1.numManagedServiceFactoryUpdatedCalls );
    }


    @Test
    public void test_location_changed_events() throws BundleException, IOException
    {
        String pid = "test_location_changed_events";
        configure( pid );
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_UPDATED, 1 );

        // ensure configuration is unbound
        final Configuration config = getConfiguration( pid );
        TestCase.assertNull( config.getBundleLocation() );

        bundle = installBundle( pid );
        bundle.start();
        delay();

        // ensure no configuration bound before start
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 1 );

        // uninstall the bundle, dynamic location changed
        bundle.uninstall();
        bundle = null;
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 1 );

        // change the location
        config.setBundleLocation( "some_location_1" );
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 1 );

        // change the location
        config.setBundleLocation( "some_location_2" );
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 1 );

        // remove configuration, delete event
        config.delete();
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_DELETED, 1 );

        // no more events
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_DELETED, 0 );
        configListener.assertEvents( ConfigurationEvent.CM_UPDATED, 0 );
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 0 );
    }

    /**
     * Tests configuration dynamic binding. See FELIX-3360.
     */
    @SuppressWarnings({ "serial", "javadoc" })
    @Test
    public void test_dynamic_binding_getConfiguration_pid() throws BundleException, IOException {
        String ignoredPid = "test_dynamic_binding_getConfiguration_pid_ignored";
        String pid1 = "test_dynamic_binding_getConfiguration_pid_1";
        String pid2 = "test_dynamic_binding_getConfiguration_pid_2";

        // ensure configuration is unbound
        configure( pid1 );
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_UPDATED, 1 );

        bundle = installBundle( ignoredPid );
        bundle.start();
        delay();

        // ensure config1 unbound
        Configuration config1 = getConfiguration( pid1 );
        TestCase.assertNull( config1.getBundleLocation() );

        ServiceReference<ConfigurationAdmin> sr = bundle.getBundleContext().getServiceReference( ConfigurationAdmin.class );
        ConfigurationAdmin bundleCa = bundle.getBundleContext().getService( sr );

        // ensure dynamic binding
        Configuration bundleConfig1 = bundleCa.getConfiguration( pid1 );
        TestCase.assertEquals( bundle.getLocation(), bundleConfig1.getBundleLocation() );
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 1 );

        // create config2; ensure dynamic binding
        Configuration bundleConfig2 = bundleCa.getConfiguration( pid2 );
        TestCase.assertNull(bundleConfig2.getProperties());
        TestCase.assertEquals( bundle.getLocation(), bundleConfig2.getBundleLocation() );
        bundleConfig2.update( new Hashtable<String, String>()
        {
            {
                put( "key", "value" );
            }
        } );

        // uninstall the bundle, 2 dynamic locations changed
        bundle.uninstall();
        bundle = null;
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 2 );

        bundleConfig1 = getConfiguration( pid1 );
        TestCase.assertNull( bundleConfig1.getBundleLocation() );

        bundleConfig2 = getConfiguration( pid2 );
        TestCase.assertNull(bundleConfig2.getBundleLocation());

        bundleConfig1.delete();
        bundleConfig2.delete();
    }

    /**
     * Tests factory configuration dynamic binding. See FELIX-3360.
     */
    @SuppressWarnings({ "javadoc", "serial" })
    @Test
    public void test_dynamic_binding_createFactoryConfiguration_pid() throws BundleException, IOException {
        String ignoredPid = "test_dynamic_binding_createFactoryConfiguration_pid_ignored";
        String pid1 = null;
        String pid2 = null;
        String factoryPid1 = "test_dynamic_binding_createFactoryConfiguration_pid_1";
        String factoryPid2 = "test_dynamic_binding_createFactoryConfiguration_pid_2";

        // ensure configuration is unbound
        pid1 = createFactoryConfiguration( factoryPid1 ).getPid();
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_UPDATED, 1 );

        bundle = installBundle( ignoredPid );
        bundle.start();
        delay();

        // ensure config1 unbound
        Configuration config1 = getConfiguration( pid1 );
        TestCase.assertNull( config1.getBundleLocation() );

        ServiceReference<ConfigurationAdmin> sr = bundle.getBundleContext().getServiceReference( ConfigurationAdmin.class );
        ConfigurationAdmin bundleCa = bundle.getBundleContext().getService( sr );

        // ensure dynamic binding
        Configuration bundleConfig1 = bundleCa.getConfiguration( pid1 );
        TestCase.assertEquals( bundle.getLocation(), bundleConfig1.getBundleLocation() );
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 1 );

        // create config2; ensure dynamic binding
        Configuration bundleConfig2 = bundleCa.createFactoryConfiguration( factoryPid2 );
        pid2 = bundleConfig2.getPid();
        TestCase.assertNull(bundleConfig2.getProperties());
        TestCase.assertEquals( bundle.getLocation(), bundleConfig2.getBundleLocation() );
        bundleConfig2.update( new Hashtable<String, String>()
        {
            {
                put( "key", "value" );
            }
        } );

        // uninstall the bundle, 2 dynamic locations changed
        bundle.uninstall();
        bundle = null;
        delay();
        configListener.assertEvents( ConfigurationEvent.CM_LOCATION_CHANGED, 2 );

        bundleConfig1 = getConfiguration( pid1 );
        TestCase.assertNull( bundleConfig1.getBundleLocation() );

        bundleConfig2 = getConfiguration( pid2 );
        TestCase.assertNull(bundleConfig2.getBundleLocation());

        bundleConfig1.delete();
        bundleConfig2.delete();
    }

    private static class ConfigListener implements ConfigurationListener {

        private int[] events = new int[3];

        public void configurationEvent( ConfigurationEvent event )
        {
            events[event.getType()-1]++;
        }


        void assertEvents( final int type, final int numEvents )
        {
            TestCase.assertEquals( "Events of type " + type, numEvents, events[type - 1] );
            events[type - 1] = 0;
        }


        void reset()
        {
            for ( int i = 0; i < events.length; i++ )
            {
                events[i] = 0;
            }
        }
    }
}
