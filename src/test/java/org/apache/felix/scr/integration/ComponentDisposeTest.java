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


import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.felix.scr.integration.components.SimpleComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;


@RunWith(JUnit4TestRunner.class)
public class ComponentDisposeTest extends ComponentTestBase
{
    static
    {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }


    @Test
    public void test_SimpleComponent_factory_configuration() throws Exception
    {
        final String factoryPid = "FactoryConfigurationComponent";

        deleteFactoryConfigurations( factoryPid );
        delay();

        getConfigurationsDisabledThenEnable(factoryPid, 0, ComponentConfigurationDTO.ACTIVE);//there should be none

        // create two factory configurations expecting two components
        final String pid0 = createFactoryConfiguration( factoryPid, "?" );
        final String pid1 = createFactoryConfiguration( factoryPid, "?" );
        delay();

        Collection<ComponentConfigurationDTO> ccs = findComponentConfigurationsByName(factoryPid, ComponentConfigurationDTO.ACTIVE);
		Assert.assertEquals(2, ccs.size());
        // expect two components, only first is active, second is disabled
        TestCase.assertEquals( 2, SimpleComponent.INSTANCES.size() );
        for (ComponentConfigurationDTO cc: ccs)
        {
        	TestCase.assertTrue(SimpleComponent.INSTANCES.containsKey(cc.id));
        }

        // dispose an instance
        final SimpleComponent anInstance = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( anInstance );
        TestCase.assertNotNull( anInstance.m_activateContext );
        anInstance.m_activateContext.getComponentInstance().dispose();
        delay();

        // expect one component
        ComponentConfigurationDTO cc = findComponentConfigurationByName(factoryPid, ComponentConfigurationDTO.ACTIVE);

        TestCase.assertEquals( 1, SimpleComponent.INSTANCES.size() );
    	TestCase.assertTrue(SimpleComponent.INSTANCES.containsKey(cc.id));

        final SimpleComponent instance = SimpleComponent.INSTANCES.values().iterator().next();

    }


}
