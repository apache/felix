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
import org.apache.felix.scr.integration.components.ActivatorComponent;
import org.apache.felix.scr.integration.components.SimpleService;
import org.apache.felix.scr.integration.components.SimpleServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.ServiceReference;


@RunWith(JUnit4TestRunner.class)
=======
import org.apache.felix.scr.integration.components.ActivatorComponent;
import org.apache.felix.scr.integration.components.SimpleServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

import junit.framework.TestCase;


@RunWith(PaxExam.class)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
public class ComponentActivationTest extends ComponentTestBase
{

    static
    {
        // use different components
        descriptorFile = "/integration_test_activation_components.xml";

        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }


    @Test
<<<<<<< HEAD
    public void test_activator_not_declared()
    {
        final String componentname = "ActivatorComponent.no.decl";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );

        component.disable();

        delay();
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
    }


//    @Test  I think this test is wrong.  Failure to activate does not mean that the state changes from Registered.
    public void test_activate_missing()
    {
        final String componentname = "ActivatorComponent.activate.missing";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        // activate must fail
        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );

        component.disable();

        delay();
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
=======
    public void test_activator_not_declared() throws Exception
    {
        final String componentname = "ActivatorComponent.no.decl";

        ComponentConfigurationDTO cc = getDisabledConfigurationAndEnable(componentname, ComponentConfigurationDTO.ACTIVE);

        disableAndCheck( cc );
    }


    @Test //Changed to expect SATISFIED rather than unsatisfied
    public void test_activate_missing() throws Exception
    {
        final String componentname = "ActivatorComponent.activate.missing";

        // activate must fail, so state remains SATISFIED
        ComponentConfigurationDTO cc = getDisabledConfigurationAndEnable(componentname, ComponentConfigurationDTO.SATISFIED);

        disableAndCheck( cc );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    @Test
<<<<<<< HEAD
    public void test_deactivate_missing()
    {
        final String componentname = "ActivatorComponent.deactivate.missing";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );

        component.disable();

        delay();
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
=======
    public void test_deactivate_missing() throws Exception
    {
        final String componentname = "ActivatorComponent.deactivate.missing";

        ComponentConfigurationDTO cc = getDisabledConfigurationAndEnable(componentname, ComponentConfigurationDTO.ACTIVE);

        disableAndCheck( cc );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    @Test
<<<<<<< HEAD
    public void test_activator_declared()
    {
        final String componentname = "ActivatorComponent.decl";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );

        component.disable();

        delay();
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
    }


//    @Test  Failure to activate does not mean the state should change to unsatisfied.
    public void test_activate_fail()
    {
        final String componentname = "ActivatorComponent.activate.fail";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        // activate has failed
        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );

        component.disable();

        delay();
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
=======
    public void test_activator_declared() throws Exception
    {
        final String componentname = "ActivatorComponent.decl";

        ComponentConfigurationDTO cc = getDisabledConfigurationAndEnable(componentname, ComponentConfigurationDTO.ACTIVE);

        disableAndCheck( cc );
    }


    @Test // Failure to activate does not mean the state should change to unsatisfied.
    public void test_activate_fail() throws Exception
    {
        final String componentname = "ActivatorComponent.activate.fail";

        // activate must fail, so state remains SATISFIED
        ComponentConfigurationDTO cc = getDisabledConfigurationAndEnable(componentname, ComponentConfigurationDTO.FAILED_ACTIVATION);

        disableAndCheck( cc );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    @Test
<<<<<<< HEAD
    public void test_deactivate_fail()
    {
        final String componentname = "ActivatorComponent.deactivate.fail";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );

        component.disable();

        delay();
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
=======
    public void test_deactivate_fail() throws Exception
    {
        final String componentname = "ActivatorComponent.deactivate.fail";

        ComponentConfigurationDTO cc = getDisabledConfigurationAndEnable(componentname, ComponentConfigurationDTO.ACTIVE);

        disableAndCheck( cc );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    @Test
<<<<<<< HEAD
    public void test_activate_register_service()
    {
        final String componentname = "ActivatorComponent.activate.with.bind";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );

        ActivatorComponent ac = (ActivatorComponent) component.getComponentInstance().getInstance();
        TestCase.assertNotNull( ac.getSimpleService() );

        component.disable();

        delay();
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
=======
    public void test_activate_register_service() throws Exception
    {
        final String componentname = "ActivatorComponent.activate.with.bind";

        ComponentConfigurationDTO cc = getDisabledConfigurationAndEnable(componentname, ComponentConfigurationDTO.ACTIVE);

        ActivatorComponent ac = ActivatorComponent.getInstance();
        TestCase.assertNotNull( ac.getSimpleService() );

        disableAndCheck( cc );

        TestCase.assertNull( ac.getSimpleService() );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    @Test
<<<<<<< HEAD
    public void test_activate_register_service_delayed()
    {
        final String componentname = "ActivatorComponent.activate.delayed.with.bind";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_REGISTERED, component.getState() );

        ServiceReference<ActivatorComponent> ref = bundleContext.getServiceReference( ActivatorComponent.class );
        ActivatorComponent ac = bundleContext.getService( ref );
        TestCase.assertNotNull( ac.getSimpleService() );

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );

        component.disable();

        delay();
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
    }
    
    @Test
    public void test_activate_service_factory_register_service()
    {
        final String componentname = "ActivatorComponent.activate.service.factory.with.bind";

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_REGISTERED, component.getState() );

        ServiceReference<ActivatorComponent> ref = bundleContext.getServiceReference( ActivatorComponent.class );
        ActivatorComponent ac = bundleContext.getService( ref );
        TestCase.assertNotNull( ac.getSimpleService() );

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );

        component.disable();

        delay();
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
    }
    
    @Test
    public void test_activate_register_service_single_static_dependency()
=======
    public void test_activate_register_service_delayed() throws Exception
    {
        final String componentname = "ActivatorComponent.activate.delayed.with.bind";

        ComponentConfigurationDTO cc = getDisabledConfigurationAndEnable(componentname, ComponentConfigurationDTO.SATISFIED);

        getServiceFromConfiguration(cc, ActivatorComponent.class);

        findComponentConfigurationByName(componentname, ComponentConfigurationDTO.ACTIVE);

        disableAndCheck( cc );
    }

    @Test
    public void test_activate_service_factory_register_service() throws Exception
    {
        final String componentname = "ActivatorComponent.activate.service.factory.with.bind";

        ComponentConfigurationDTO cc = getDisabledConfigurationAndEnable(componentname, ComponentConfigurationDTO.SATISFIED);

        getServiceFromConfiguration(cc, ActivatorComponent.class);

        findComponentConfigurationByName(componentname, ComponentConfigurationDTO.ACTIVE);

        disableAndCheck( cc );
    }

    @Test
    public void test_activate_register_service_single_static_dependency() throws Exception
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        final String componentname = "ActivatorComponent.bind.single.static";

        testRequiredDependency( componentname );
    }

    @Test
<<<<<<< HEAD
    public void test_activate_register_service_multiple_static_reluctant_dependency()
=======
    public void test_activate_register_service_multiple_static_reluctant_dependency() throws Exception
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        final String componentname = "ActivatorComponent.bind.multiple.static.reluctant";

        testRequiredDependency( componentname );
    }

    @Test
<<<<<<< HEAD
    public void test_activate_register_service_multiple_static_greedy_dependency()
=======
    public void test_activate_register_service_multiple_static_greedy_dependency() throws Exception
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        final String componentname = "ActivatorComponent.bind.multiple.static.greedy";

        testRequiredDependency( componentname );
    }

    @Test
<<<<<<< HEAD
    public void test_activate_register_service_single_dynamic_dependency()
=======
    public void test_activate_register_service_single_dynamic_dependency() throws Exception
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        final String componentname = "ActivatorComponent.bind.single.dynamic";

        testRequiredDependency( componentname );
    }

    @Test
<<<<<<< HEAD
    public void test_activate_register_service_multiple_dynamic_dependency()
=======
    public void test_activate_register_service_multiple_dynamic_dependency() throws Exception
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        final String componentname = "ActivatorComponent.bind.multiple.dynamic";

        testRequiredDependency( componentname );
    }


<<<<<<< HEAD
    private void testRequiredDependency(final String componentname)
    {
        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
        
        SimpleServiceImpl ss = SimpleServiceImpl.create( bundleContext, "foo" );
        
        TestCase.assertEquals( Component.STATE_REGISTERED, component.getState() );

        ServiceReference<ActivatorComponent> ref = bundleContext.getServiceReference( ActivatorComponent.class );
        
        ss.drop();
        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
        
=======
    private void testRequiredDependency(final String componentname) throws Exception
    {
        ComponentConfigurationDTO cc = getDisabledConfigurationAndEnable(componentname, ComponentConfigurationDTO.UNSATISFIED_REFERENCE);

        SimpleServiceImpl ss = SimpleServiceImpl.create( bundleContext, "foo" );

        findComponentConfigurationByName(componentname, ComponentConfigurationDTO.SATISFIED);

        ServiceReference<ActivatorComponent> ref = bundleContext.getServiceReference( ActivatorComponent.class );

        ss.drop();
        findComponentConfigurationByName(componentname, ComponentConfigurationDTO.UNSATISFIED_REFERENCE);


>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        TestCase.assertNull(bundleContext.getServiceReference( ActivatorComponent.class ));
        ss = SimpleServiceImpl.create( bundleContext, "foo" );
        ref = bundleContext.getServiceReference( ActivatorComponent.class );
        ActivatorComponent ac = bundleContext.getService( ref );
        TestCase.assertNotNull( ac.getSimpleService() );

<<<<<<< HEAD
        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );

        component.disable();

        delay();
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
=======
        findComponentConfigurationByName(componentname, ComponentConfigurationDTO.ACTIVE);


        disableAndCheck( cc );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

}
