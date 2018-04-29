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


<<<<<<< HEAD
import junit.framework.TestCase;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.integration.components.SimpleComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;


@RunWith(JUnit4TestRunner.class)
=======
import org.apache.felix.scr.integration.components.SimpleComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

import junit.framework.TestCase;


@RunWith(PaxExam.class)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
public class ComponentConfigurationPidTest extends ComponentTestBase
{
    static
    {
        // uncomment to enable debugging of this test class
        //  paxRunnerVmOption = DEBUG_VM_OPTION;
        descriptorFile = "/integration_test_simple_components_configuration_pid.xml";
    }

    @Test
<<<<<<< HEAD
    public void test_configurationpid_use_other_pid()
    {
        final String pid = "ConfigurationPid.otherPid";
        final String name = "ConfigurationPid.componentName";
        final Component component = findComponentByName( name );

        deleteConfig( pid );
        delay();

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
=======
    public void test_configurationpid_use_other_pid() throws Exception
    {
        final String pid = "ConfigurationPid.otherPid";
        final String name = "ConfigurationPid.componentName";
        deleteConfig( pid );
        delay();
        TestCase.assertNull( SimpleComponent.INSTANCE );

        getConfigurationsDisabledThenEnable(name, 0, ComponentConfigurationDTO.UNSATISFIED_REFERENCE);

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        TestCase.assertNull( SimpleComponent.INSTANCE );

        configure( pid );
        delay();

<<<<<<< HEAD
        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
=======
        findComponentConfigurationByName( name, ComponentConfigurationDTO.ACTIVE);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        deleteConfig( pid );
        delay();

<<<<<<< HEAD
        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.disable();
        delay();

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
    }
    
    @Test
    public void test_configurationpid_must_not_use_name_as_pid()
    {
        final String name = "ConfigurationPid.componentName";
        final String pid = name;
        final Component component = findComponentByName( name );

        deleteConfig( pid );
        delay();

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
=======
        ComponentDescriptionDTO cd = checkConfigurationCount(name, 0, -1);
        TestCase.assertNull( SimpleComponent.INSTANCE );

        disableAndCheck( cd );
        TestCase.assertNull( SimpleComponent.INSTANCE );
    }

    @Test
    public void test_configurationpid_must_not_use_name_as_pid() throws Exception
    {
        final String name = "ConfigurationPid.componentName";
        final String pid = name;
        deleteConfig( pid );
        delay();

        getConfigurationsDisabledThenEnable(name, 0, ComponentConfigurationDTO.UNSATISFIED_REFERENCE);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        TestCase.assertNull( SimpleComponent.INSTANCE );

        configure( pid );
        delay();

<<<<<<< HEAD
        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );

        deleteConfig( pid );
        delay();
=======
        ComponentDescriptionDTO cd = checkConfigurationCount(name, 0, -1);
        TestCase.assertNull( SimpleComponent.INSTANCE );

        deleteConfig( pid );
        delay();

        disableAndCheck( cd );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }
}
