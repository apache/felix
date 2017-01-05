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


import junit.framework.TestCase;

import org.apache.felix.scr.integration.components.SimpleComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;


@RunWith(JUnit4TestRunner.class)
public class ComponentConfigurationPidTest extends ComponentTestBase
{
    static
    {
        // uncomment to enable debugging of this test class
        //  paxRunnerVmOption = DEBUG_VM_OPTION;
        descriptorFile = "/integration_test_simple_components_configuration_pid.xml";
    }

    @Test
    public void test_configurationpid_use_other_pid() throws Exception
    {
        final String pid = "ConfigurationPid.otherPid";
        final String name = "ConfigurationPid.componentName";
        deleteConfig( pid );
        delay();
        TestCase.assertNull( SimpleComponent.INSTANCE );

        getConfigurationsDisabledThenEnable(name, 0, ComponentConfigurationDTO.UNSATISFIED_REFERENCE);

        TestCase.assertNull( SimpleComponent.INSTANCE );

        configure( pid );
        delay();

        findComponentConfigurationByName( name, ComponentConfigurationDTO.ACTIVE);
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        deleteConfig( pid );
        delay();

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
        TestCase.assertNull( SimpleComponent.INSTANCE );

        configure( pid );
        delay();

        ComponentDescriptionDTO cd = checkConfigurationCount(name, 0, -1);
        TestCase.assertNull( SimpleComponent.INSTANCE );

        deleteConfig( pid );
        delay();
        
        disableAndCheck( cd );
    }
}
