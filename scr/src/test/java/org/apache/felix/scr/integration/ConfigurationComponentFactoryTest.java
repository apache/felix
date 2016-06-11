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

import java.util.Dictionary;

import org.apache.felix.scr.integration.components.SimpleComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.cm.Configuration;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

import junit.framework.TestCase;

/**
 * Tests of nonstandard ComponentFactory behavior
 */

@RunWith(JUnit4TestRunner.class)
public class ConfigurationComponentFactoryTest extends ComponentTestBase
{

    private static final String PROP_NAME_FACTORY = ComponentTestBase.PROP_NAME + ".factory";

    static
    {
        NONSTANDARD_COMPONENT_FACTORY_BEHAVIOR = true;
        descriptorFile = "/integration_test_simple_factory_components.xml";
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }

    @Test
    public void test_non_spec_component_factory_with_factory_configuration() throws Exception
    {
        // this test is about non-standard behaviour of ComponentFactory services

        final String componentname = "factory.component";
        final String componentfactory = "factory.component.factory";

        getConfigurationsDisabledThenEnable( componentname, 0, -1 );

        TestCase.assertNull( SimpleComponent.INSTANCE );

        final ComponentFactory factory = getComponentFactory( componentfactory );

        final String factoryConfigPid = createFactoryConfiguration( componentname, "?" );
        delay();

        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        // check registered components
        checkConfigurationCount( componentname, 1, ComponentConfigurationDTO.ACTIVE );

        // modify the configuration
        Configuration config = getConfigurationAdmin().getConfiguration( factoryConfigPid, "?" );
        Dictionary<String, Object> props = config.getProperties();
        props.put( PROP_NAME, PROP_NAME_FACTORY );
        config.update( props );
        delay();

        // ensure instance with new configuration
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertEquals( PROP_NAME_FACTORY, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        // check registered components
        checkConfigurationCount( componentname, 1, ComponentConfigurationDTO.ACTIVE );

        // disable the factory
        disableAndCheck( componentname );
        delay();

        // enabled the factory, factory configuration results in component instance
        getConfigurationsDisabledThenEnable( componentname, 1, ComponentConfigurationDTO.ACTIVE );

        // delete the configuration
        getConfigurationAdmin().getConfiguration( factoryConfigPid ).delete();
        delay();

        // factory is enabled but instance has been removed

        // check registered components
        checkConfigurationCount( componentname, 0, ComponentConfigurationDTO.ACTIVE );
    }

}
