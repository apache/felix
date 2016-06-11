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
package org.apache.felix.scr.integration;

import org.apache.felix.scr.integration.components.SimpleComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.cm.ConfigurationPermission;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

import junit.framework.TestCase;

@RunWith(JUnit4TestRunner.class)
public class LocationTest extends ComponentTestBase
{

    private static final String COMPONENT_NAME = "SimpleComponent.configuration.require";
    private static final String REGION = "?foo";
    private boolean eventReceived;

    static
    {
        descriptorFile = "/integration_test_simple_components_location.xml";
        // uncomment to enable debugging of this test class
        //         paxRunnerVmOption = DEBUG_VM_OPTION;
    }

    /*
     * tests that ds does not override a dynamic (null) location binding.
     */
    @Test
    public void testLocationBinding() throws Exception
    {
        final String pid = COMPONENT_NAME;
        deleteConfig( pid );
        delay();
        checkConfigurationCount( pid, 0, -1 );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        Configuration config = configure( pid );
        delay();

        findComponentConfigurationByName( pid, ComponentConfigurationDTO.ACTIVE );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        //dynamic (null) bundle location not overridden by ds, so all bundles can use the config.
        Bundle b2 = installBundle( descriptorFile, COMPONENT_PACKAGE, "simplecomponent2", "0.0.11", null );
        b2.start();
        checkConfigurationCount( b2, pid, 1, -1 );

        bundle.stop();
        delay();

        checkConfigurationCount( b2, pid, 1, -1 );

        ConfigurationListener listener = new ConfigurationListener()
        {

            public void configurationEvent(ConfigurationEvent event)
            {
                if ( event.getType() == ConfigurationEvent.CM_LOCATION_CHANGED )
                {
                    eventReceived = true;
                }
            }

        };
        ServiceRegistration<ConfigurationListener> sr = bundleContext.registerService( ConfigurationListener.class,
            listener, null );
        config.setBundleLocation( null );
        delay();

        if ( eventReceived )
        {
            checkConfigurationCount( b2, pid, 1, ComponentConfigurationDTO.ACTIVE );
        }

        sr.unregister();

    }

    @Test
    public void testLocationChangeToRegionBinding() throws Exception
    {
        final String pid = COMPONENT_NAME;
        checkConfigurationCount( pid, 0, -1 );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        Configuration config = configure( pid );
        delay();

        findComponentConfigurationByName( pid, ComponentConfigurationDTO.ACTIVE );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        Bundle b2 = installBundle( descriptorFile, COMPONENT_PACKAGE, "simplecomponent2", "0.0.11", null );
        b2.start();
        checkConfigurationCount( b2, pid, 1, -1 );

        bundle.stop();
        delay();

        checkConfigurationCount( b2, pid, 1, -1 );

        ConfigurationListener listener = new ConfigurationListener()
        {

            public void configurationEvent(ConfigurationEvent event)
            {
                if ( event.getType() == ConfigurationEvent.CM_LOCATION_CHANGED )
                {
                    eventReceived = true;
                }
            }

        };
        ServiceRegistration<ConfigurationListener> sr = bundleContext.registerService( ConfigurationListener.class,
            listener, null );
        config.setBundleLocation( REGION );
        delay();

        if ( eventReceived )
        {
            checkConfigurationCount( b2, pid, 1, ComponentConfigurationDTO.ACTIVE );
        }

        sr.unregister();

    }

    @Test
    public void testRegionBinding() throws Exception
    {
        try
        {
            new ConfigurationPermission( REGION, ConfigurationPermission.TARGET );
        }
        catch ( IllegalArgumentException e )
        {
            return;//not an R5 CA
        }

        final String pid = COMPONENT_NAME;
        deleteConfig( pid );
        checkConfigurationCount( pid, 0, -1 );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        Configuration config = configure( pid, REGION );
        delay();

        findComponentConfigurationByName( pid, ComponentConfigurationDTO.ACTIVE );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        Bundle b2 = installBundle( descriptorFile, COMPONENT_PACKAGE, "simplecomponent2", "0.0.11", null );
        b2.start();
        checkConfigurationCount( b2, pid, 1, ComponentConfigurationDTO.ACTIVE );

    }

}
