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

import java.util.Arrays;
import java.util.Hashtable;

import org.apache.felix.scr.integration.components.SimpleService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

import junit.framework.TestCase;

/**
 * Tests of nonstandard ComponentFactory behavior
 */

@RunWith(JUnit4TestRunner.class)
public class Felix5356Test extends ComponentTestBase
{

    private static final String PROP_NAME_FACTORY = ComponentTestBase.PROP_NAME + ".factory";

    static
    {
        descriptorFile = "/integration_test_simple_factory_components.xml";
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }

    @Test
    public void test_factory_component_instance_with_factory_configuration_after_enable() throws Exception
    {
        do_test_factory_component_with_factory_configuration(false);
    }

    @Test
    public void test_factory_component_instance_with_factory_configuration_before_enable() throws Exception
    {
        do_test_factory_component_with_factory_configuration(true);
    }

    private void do_test_factory_component_with_factory_configuration(boolean createConfigBeforeEnable) throws Exception {
        final String componentname = "factory.component.referred";
        final String componentfactory = "factory.component.factory.referred";

        String factoryConfigPid = null;
        if ( createConfigBeforeEnable ) {
            // create the factory configuration before enabling
            factoryConfigPid = createFactoryConfiguration( componentname, "?" );
            delay();
        }

        getConfigurationsDisabledThenEnable( componentname, 0, -1 );

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( "service.pid", "myFactoryInstance" );
        final ComponentFactory factory = getComponentFactory( componentfactory );

        final ComponentInstance instance = factory.newInstance( props );
        TestCase.assertNotNull( instance );

        TestCase.assertNotNull( instance.getInstance() );
        TestCase.assertTrue( instance.getInstance() instanceof SimpleService );

        // check registered components
        checkConfigurationCount( componentname, 1, ComponentConfigurationDTO.ACTIVE );
        // check registered service
        findServices(SimpleService.class.getName(), null, 1);

        if ( !createConfigBeforeEnable ) {
            factoryConfigPid = createFactoryConfiguration( componentname, "?" );
            delay();

            // check registered components, again should only be 1
            checkConfigurationCount( componentname, 1, ComponentConfigurationDTO.ACTIVE );
            // check registered service
            findServices(SimpleService.class.getName(), null, 1);
            TestCase.assertNotNull(instance.getInstance());
        }

        // delete the factory config
        getConfigurationAdmin().getConfiguration( factoryConfigPid ).delete();
        delay();

        // check registered components, again should only be 1
        checkConfigurationCount( componentname, 1, ComponentConfigurationDTO.ACTIVE );

        instance.dispose();

        // check registered components
        checkConfigurationCount( componentname, 0, ComponentConfigurationDTO.ACTIVE );
    }

    private void findServices(String clazz, String filter, int expected) throws InvalidSyntaxException {
        ServiceReference<?>[] services = bundleContext.getServiceReferences(clazz, filter);
        if (services == null) {
            services = new ServiceReference<?>[0];
        }
        TestCase.assertEquals("Wrong number of services found for clazz \"" + clazz + "\" and filter \"" + filter + "\" found: " + Arrays.toString(services), expected, services.length);
    }

}
