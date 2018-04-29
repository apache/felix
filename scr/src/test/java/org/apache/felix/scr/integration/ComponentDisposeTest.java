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
import java.lang.reflect.Field;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.integration.components.SimpleComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.component.ComponentContext;


@RunWith(JUnit4TestRunner.class)
=======
import java.util.Collection;

import org.apache.felix.scr.integration.components.SimpleComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

import junit.framework.Assert;
import junit.framework.TestCase;


@RunWith(PaxExam.class)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
public class ComponentDisposeTest extends ComponentTestBase
{
    static
    {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }


    @Test
<<<<<<< HEAD
    public void test_SimpleComponent_factory_configuration()
=======
    public void test_SimpleComponent_factory_configuration() throws Exception
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        final String factoryPid = "FactoryConfigurationComponent";

        deleteFactoryConfigurations( factoryPid );
        delay();

<<<<<<< HEAD
        // one single component exists without configuration
        final Component[] noConfigurations = findComponentsByName( factoryPid );
        TestCase.assertNotNull( noConfigurations );
        TestCase.assertEquals( 1, noConfigurations.length );
        TestCase.assertEquals( Component.STATE_DISABLED, noConfigurations[0].getState() );
        TestCase.assertTrue( SimpleComponent.INSTANCES.isEmpty() );

        // enable the component, configuration required, hence unsatisfied
        noConfigurations[0].enable();
        delay();

        final Component[] enabledNoConfigs = findComponentsByName( factoryPid );
        TestCase.assertNotNull( enabledNoConfigs );
        TestCase.assertEquals( 1, enabledNoConfigs.length );
        TestCase.assertEquals( Component.STATE_UNSATISFIED, enabledNoConfigs[0].getState() );
        TestCase.assertTrue( SimpleComponent.INSTANCES.isEmpty() );

        // create two factory configurations expecting two components
        final String pid0 = createFactoryConfiguration( factoryPid );
        final String pid1 = createFactoryConfiguration( factoryPid );
        delay();

        // expect two components, only first is active, second is disabled
        final Component[] twoConfigs = findComponentsByName( factoryPid );
        TestCase.assertNotNull( twoConfigs );
        TestCase.assertEquals( 2, twoConfigs.length );

        // find the active and inactive configs, fail if none
        int activeConfig;
        int inactiveConfig;
        if ( twoConfigs[0].getState() == Component.STATE_ACTIVE )
        {
            // [0] is active, [1] expected disabled
            activeConfig = 0;
            inactiveConfig = 1;
        }
        else if ( twoConfigs[1].getState() == Component.STATE_ACTIVE )
        {
            // [1] is active, [0] expected disabled
            activeConfig = 1;
            inactiveConfig = 0;
        }
        else
        {
            TestCase.fail( "One of two components expected active" );
            return; // eases the compiler...
        }

        TestCase.assertEquals( Component.STATE_ACTIVE, twoConfigs[activeConfig].getState() );
        TestCase.assertEquals( Component.STATE_DISABLED, twoConfigs[inactiveConfig].getState() );
        TestCase.assertEquals( 1, SimpleComponent.INSTANCES.size() );
        TestCase.assertTrue( SimpleComponent.INSTANCES.containsKey( twoConfigs[activeConfig].getId() ) );
        TestCase.assertFalse( SimpleComponent.INSTANCES.containsKey( twoConfigs[inactiveConfig].getId() ) );

        // enable second component
        twoConfigs[inactiveConfig].enable();
        delay();

        // ensure both components active
        TestCase.assertEquals( Component.STATE_ACTIVE, twoConfigs[0].getState() );
        TestCase.assertEquals( Component.STATE_ACTIVE, twoConfigs[1].getState() );
        TestCase.assertEquals( 2, SimpleComponent.INSTANCES.size() );
        TestCase.assertTrue( SimpleComponent.INSTANCES.containsKey( twoConfigs[0].getId() ) );
        TestCase.assertTrue( SimpleComponent.INSTANCES.containsKey( twoConfigs[1].getId() ) );

=======
        getConfigurationsDisabledThenEnable(factoryPid, 0, ComponentConfigurationDTO.ACTIVE);//there should be none

        // create two factory configurations expecting two components
        createFactoryConfiguration( factoryPid, "?" );
        createFactoryConfiguration( factoryPid, "?" );
        delay();

        Collection<ComponentConfigurationDTO> ccs = findComponentConfigurationsByName(factoryPid, ComponentConfigurationDTO.ACTIVE);
        Assert.assertEquals(2, ccs.size());
        // expect two components, only first is active, second is disabled
        TestCase.assertEquals( 2, SimpleComponent.INSTANCES.size() );
        for (ComponentConfigurationDTO cc: ccs)
        {
            TestCase.assertTrue(SimpleComponent.INSTANCES.containsKey(cc.id));
        }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        // dispose an instance
        final SimpleComponent anInstance = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( anInstance );
        TestCase.assertNotNull( anInstance.m_activateContext );
        anInstance.m_activateContext.getComponentInstance().dispose();
        delay();

        // expect one component
<<<<<<< HEAD
        final Component[] oneConfig = findComponentsByName( factoryPid );
        TestCase.assertNotNull( oneConfig );
        TestCase.assertEquals( 1, oneConfig.length );
        TestCase.assertEquals( Component.STATE_ACTIVE, oneConfig[0].getState() );
        TestCase.assertEquals( 1, SimpleComponent.INSTANCES.size() );
        TestCase.assertTrue( SimpleComponent.INSTANCES.containsKey( oneConfig[0].getId() ) );

        final SimpleComponent instance = SimpleComponent.INSTANCES.values().iterator().next();

        final Object holder = getComponentHolder( instance.m_activateContext );
        TestCase.assertNotNull( holder );

        Map<?, ?> m_components = ( Map<?, ?> ) getFieldValue( holder, "m_components" );
        TestCase.assertNotNull( m_components );
        TestCase.assertEquals( 1, m_components.size() );
    }


    private static Object getComponentHolder( ComponentContext ctx )
    {
        try
        {
            final Class<?> ccImpl = getType( ctx, "ComponentContextImpl" );
            final Field m_componentManager = getField( ccImpl, "m_componentManager" );
            final Object acm = m_componentManager.get( ctx );

            final Class<?> cmImpl = getType( acm, "SingleComponentManager" );
            final Field m_componentHolder = getField( cmImpl, "m_componentHolder" );
            return m_componentHolder.get( acm );
        }
        catch ( Throwable t )
        {
            TestCase.fail( "Cannot get ComponentHolder for " + ctx + ": " + t );
            return null; // keep the compiler happy
        }
=======
        ComponentConfigurationDTO cc = findComponentConfigurationByName(factoryPid, ComponentConfigurationDTO.ACTIVE);

        TestCase.assertEquals( 1, SimpleComponent.INSTANCES.size() );
        TestCase.assertTrue(SimpleComponent.INSTANCES.containsKey(cc.id));

        SimpleComponent.INSTANCES.values().iterator().next();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }
}
