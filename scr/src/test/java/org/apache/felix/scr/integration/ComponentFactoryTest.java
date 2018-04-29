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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.integration.components.SimpleComponent;
import org.apache.felix.scr.integration.components.SimpleServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;


@RunWith(JUnit4TestRunner.class)
public class ComponentFactoryTest extends ComponentTestBase
{

    private static final String PROP_NAME_FACTORY = ComponentTestBase.PROP_NAME + ".factory";

=======
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;

import org.apache.felix.scr.integration.components.SimpleComponent;
import org.apache.felix.scr.integration.components.SimpleService;
import org.apache.felix.scr.integration.components.SimpleServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.log.LogService;

import junit.framework.TestCase;

@RunWith(PaxExam.class)
public class ComponentFactoryTest extends ComponentTestBase
{

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    static
    {
        descriptorFile = "/integration_test_simple_factory_components.xml";
        // uncomment to enable debugging of this test class
<<<<<<< HEAD
//        paxRunnerVmOption = DEBUG_VM_OPTION;
    }

    @Test
    public void test_component_factory() throws InvalidSyntaxException
=======
        //        paxRunnerVmOption = DEBUG_VM_OPTION;
    }

    @Test
    public void test_component_factory() throws Exception
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        final String componentname = "factory.component";
        final String componentfactory = "factory.component.factory";

<<<<<<< HEAD
        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_FACTORY, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        final ServiceReference[] refs = bundleContext.getServiceReferences( ComponentFactory.class.getName(), "("
            + ComponentConstants.COMPONENT_FACTORY + "=" + componentfactory + ")" );
        TestCase.assertNotNull( refs );
        TestCase.assertEquals( 1, refs.length );
        final ComponentFactory factory = ( ComponentFactory ) bundleContext.getService( refs[0] );
        TestCase.assertNotNull( factory );

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( PROP_NAME_FACTORY, PROP_NAME_FACTORY );
        final ComponentInstance instance = factory.newInstance( props );
        TestCase.assertNotNull( instance );

        TestCase.assertNotNull( instance.getInstance() );
        TestCase.assertEquals( SimpleComponent.INSTANCE, instance.getInstance() );
        TestCase.assertEquals( PROP_NAME_FACTORY, SimpleComponent.INSTANCE.getProperty( PROP_NAME_FACTORY ) );

        final Map<?, ?> instanceMap = ( Map<?, ?> ) getFieldValue( component, "m_componentInstances" );
        TestCase.assertNotNull( instanceMap );
        TestCase.assertEquals( 1, instanceMap.size() );

        final Object instanceManager = getComponentManagerFromComponentInstance( instance );
        TestCase.assertTrue( instanceMap.containsValue( instanceManager ) );

        // check registered components
        final Component[] allFactoryComponents = findComponentsByName( componentname );
        TestCase.assertNotNull( allFactoryComponents );
        TestCase.assertEquals( 2, allFactoryComponents.length );
        for ( int i = 0; i < allFactoryComponents.length; i++ )
        {
            final Component c = allFactoryComponents[i];
            if ( c.getId() == component.getId() )
            {
                TestCase.assertEquals( Component.STATE_FACTORY, c.getState() );
            }
            else if ( c.getId() == SimpleComponent.INSTANCE.m_id )
            {
                TestCase.assertEquals( Component.STATE_ACTIVE, c.getState() );
            }
            else
            {
                TestCase.fail( "Unexpected Component " + c );
            }
        }
=======
        getConfigurationsDisabledThenEnable( componentname, 0, -1 );

        TestCase.assertNull( SimpleComponent.INSTANCE );

        final ComponentInstance instance = createFactoryComponentInstance( componentfactory );

        // check registered components
        checkConfigurationCount( componentname, 1, ComponentConfigurationDTO.ACTIVE );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        instance.dispose();
        TestCase.assertNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( instance.getInstance() ); // SCR 112.12.6.2

<<<<<<< HEAD
        TestCase.assertEquals( 0, instanceMap.size() );
        TestCase.assertFalse( instanceMap.containsValue( instanceManager ) );
    }


    @Test
    public void test_component_factory_disable_factory() throws InvalidSyntaxException
=======
        checkConfigurationCount( componentname, 0, ComponentConfigurationDTO.ACTIVE );

    }

    @Test
    public void test_component_factory_disable_factory() throws Exception
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        // tests components remain alive after factory has been disabled

        final String componentname = "factory.component";
        final String componentfactory = "factory.component.factory";

<<<<<<< HEAD
        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_FACTORY, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        final ServiceReference[] refs = bundleContext.getServiceReferences( ComponentFactory.class.getName(), "("
            + ComponentConstants.COMPONENT_FACTORY + "=" + componentfactory + ")" );
        TestCase.assertNotNull( refs );
        TestCase.assertEquals( 1, refs.length );
        final ComponentFactory factory = ( ComponentFactory ) bundleContext.getService( refs[0] );
        TestCase.assertNotNull( factory );

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( PROP_NAME_FACTORY, PROP_NAME_FACTORY );
        final ComponentInstance instance = factory.newInstance( props );
        TestCase.assertNotNull( instance );

        TestCase.assertNotNull( instance.getInstance() );
        TestCase.assertEquals( SimpleComponent.INSTANCE, instance.getInstance() );
        TestCase.assertEquals( PROP_NAME_FACTORY, SimpleComponent.INSTANCE.getProperty( PROP_NAME_FACTORY ) );

        final Map<?, ?> instanceMap = ( Map<?, ?> ) getFieldValue( component, "m_componentInstances" );
        TestCase.assertNotNull( instanceMap );
        TestCase.assertEquals( 1, instanceMap.size() );

        final Object instanceManager = getComponentManagerFromComponentInstance( instance );
        TestCase.assertTrue( instanceMap.containsValue( instanceManager ) );

        // disable the factory
        component.disable();
        delay();

        // factory is disabled but the instance is still alive
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertEquals( 1, instanceMap.size() );
        TestCase.assertTrue( instanceMap.containsValue( instanceManager ) );
=======
        getConfigurationsDisabledThenEnable( componentname, 0, -1 );

        TestCase.assertNull( SimpleComponent.INSTANCE );

        final ComponentInstance instance = createFactoryComponentInstance( componentfactory );

        checkConfigurationCount( componentname, 1, ComponentConfigurationDTO.ACTIVE );

        // disable the factory
        disableAndCheck( componentname );
        delay();

        // factory is disabled but the instance is still alive
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        instance.dispose();
        TestCase.assertNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( instance.getInstance() ); // SCR 112.12.6.2

<<<<<<< HEAD
        TestCase.assertEquals( 0, instanceMap.size() );
        TestCase.assertFalse( instanceMap.containsValue( instanceManager ) );
    }


    @Test
    public void test_component_factory_newInstance_failure() throws InvalidSyntaxException
=======
    }

    @Test
    public void test_component_factory_newInstance_failure() throws Exception
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        final String componentname = "factory.component";
        final String componentfactory = "factory.component.factory";

<<<<<<< HEAD
        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_FACTORY, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        final ServiceReference[] refs = bundleContext.getServiceReferences( ComponentFactory.class.getName(), "("
            + ComponentConstants.COMPONENT_FACTORY + "=" + componentfactory + ")" );
        TestCase.assertNotNull( refs );
        TestCase.assertEquals( 1, refs.length );
        final ComponentFactory factory = ( ComponentFactory ) bundleContext.getService( refs[0] );
        TestCase.assertNotNull( factory );

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( PROP_NAME_FACTORY, PROP_NAME_FACTORY );
        props.put( SimpleComponent.PROP_ACTIVATE_FAILURE, "Requested Failure" );
        try
        {
            final ComponentInstance instance = factory.newInstance( props );
            TestCase.assertNotNull( instance );
=======
        getConfigurationsDisabledThenEnable( componentname, 0, -1 );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        try
        {
            Hashtable<String, String> props = new Hashtable<String, String>();
            props.put( PROP_NAME_FACTORY, PROP_NAME_FACTORY );
            props.put( SimpleComponent.PROP_ACTIVATE_FAILURE, "Requested Failure" );
            createFactoryComponentInstance( componentfactory, props );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            TestCase.fail( "Expected newInstance method to fail with ComponentException" );
        }
        catch ( ComponentException ce )
        {
            // this is expected !
        }

<<<<<<< HEAD
        final Map<?, ?> instanceMap = ( Map<?, ?> ) getFieldValue( component, "m_componentInstances" );
        TestCase.assertNotNull( instanceMap );
        TestCase.assertTrue( instanceMap.isEmpty() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
    }


    @Test
    public void test_component_factory_require_configuration() throws InvalidSyntaxException
=======
        checkConfigurationCount( componentname, 0, ComponentConfigurationDTO.ACTIVE );
    }

    @Test
    public void test_component_factory_require_configuration() throws Exception
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        final String componentname = "factory.component.configuration";
        final String componentfactory = "factory.component.factory.configuration";

<<<<<<< HEAD
        // ensure there is no configuration for the component
        deleteConfig( componentname );
        delay();

        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
=======
        testConfiguredFactory( componentname, componentfactory, false, false );

    }

    @Test
    public void test_component_factory_require_configuration_obsolete() throws Exception
    {
        final String componentname = "factory.component.configuration.obsolete";

        TestCase.assertNull( SimpleComponent.INSTANCE );

        createFactoryConfiguration( componentname, "?" );
        delay();
        getConfigurationsDisabledThenEnable( componentname, 1, ComponentConfigurationDTO.ACTIVE );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );
    }

    @Test
    public void test_component_factory_optional_configuration() throws Exception
    {
        final String componentname = "factory.component.configuration.optional";
        final String componentfactory = "factory.component.factory.configuration.optional";

        testConfiguredFactory( componentname, componentfactory, true, false );

    }

    @Test
    public void test_component_factory_optional_configuration_13() throws Exception
    {
        final String componentname = "factory.component.configuration.optional.13";
        final String componentfactory = "factory.component.factory.configuration.optional.13";

        testConfiguredFactory( componentname, componentfactory, true, true );
    }

    @Test
    public void test_component_factory_optional_configuration_nomodify() throws Exception
    {
        final String componentname = "factory.component.configuration.optional.nomodify";
        final String componentfactory = "factory.component.factory.configuration.optional.nomodify";

        testConfiguredFactory( componentname, componentfactory, true, false );

    }

    private ComponentInstance testConfiguredFactory(final String componentname, final String componentfactory,
            boolean optional, boolean expectComponent)
                    throws InvocationTargetException, InterruptedException, InvalidSyntaxException
    {
        // ensure there is no configuration for the component
        deleteConfig( componentname );
        delay();

        getConfigurationsDisabledThenEnable( componentname, 0, -1 );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        TestCase.assertNull( SimpleComponent.INSTANCE );

        // At this point, since we don't have created the configuration, then the ComponentFactory
        // should not be available.
<<<<<<< HEAD
        
        ServiceReference[] refs = bundleContext.getServiceReferences( ComponentFactory.class.getName(), "("
            + ComponentConstants.COMPONENT_FACTORY + "=" + componentfactory + ")" );
        TestCase.assertNull( refs );
        
        // supply configuration now and ensure active
        configure( componentname );
        delay();        

        TestCase.assertEquals( Component.STATE_FACTORY, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        // get the component factory service
        refs = bundleContext.getServiceReferences( ComponentFactory.class.getName(), "("
            + ComponentConstants.COMPONENT_FACTORY + "=" + componentfactory + ")" );
        TestCase.assertNotNull( refs );
        TestCase.assertEquals( 1, refs.length );
        final ComponentFactory factory = ( ComponentFactory ) bundleContext.getService( refs[0] );
        TestCase.assertNotNull( factory );

        // create an instance
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( PROP_NAME_FACTORY, PROP_NAME_FACTORY );
        final ComponentInstance instance = factory.newInstance( props );
        TestCase.assertNotNull( instance );
=======

        checkFactory( componentfactory, optional );

        // supply configuration now and ensure active
        configure( componentname );
        delay();

        checkConfigurationCount( componentname, 0, ComponentConfigurationDTO.ACTIVE );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        // get the component factory service
        final ComponentInstance instance = createFactoryComponentInstance( componentfactory );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        final Object instanceObject = instance.getInstance();
        TestCase.assertNotNull( instanceObject );
        TestCase.assertEquals( SimpleComponent.INSTANCE, instanceObject );
        TestCase.assertEquals( PROP_NAME_FACTORY, SimpleComponent.INSTANCE.getProperty( PROP_NAME_FACTORY ) );
<<<<<<< HEAD
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );                        

        final Map<?, ?> instanceMap = ( Map<?, ?> ) getFieldValue( component, "m_componentInstances" );
        TestCase.assertNotNull( instanceMap );
        TestCase.assertEquals( 1, instanceMap.size() );

        final Object instanceManager = getComponentManagerFromComponentInstance( instance );
        TestCase.assertTrue( instanceMap.containsValue( instanceManager ) );

        // delete config, ensure factory is not active anymore and component instance not changed
        deleteConfig( componentname );
        delay();
        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );

        TestCase.assertNotNull( instance.getInstance() );
        TestCase.assertEquals( SimpleComponent.INSTANCE, instance.getInstance() );
        TestCase.assertEquals( instanceObject, instance.getInstance() );
        TestCase.assertEquals( PROP_NAME_FACTORY, SimpleComponent.INSTANCE.getProperty( PROP_NAME_FACTORY ) );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        instance.dispose();
        TestCase.assertNull( SimpleComponent.INSTANCE ); // component is deactivated
        TestCase.assertNull( instance.getInstance() ); // SCR 112.12.6.2

        // with removal of the factory, the created instance should also be removed
        TestCase.assertEquals( 0, instanceMap.size() );
        TestCase.assertFalse( instanceMap.containsValue( instanceManager ) );
    }

    @Test
    public void test_component_factory_reference() throws InvalidSyntaxException
=======
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        checkConfigurationCount( componentname, 1, ComponentConfigurationDTO.ACTIVE );

        // delete config, ensure factory is not active anymore and component instance gone
        //(configuration required >> dispose of instance.  Also for pre-1.3 components, removing config unconditionally
        //deactivates component.
        deleteConfig( componentname );
        delay();

        checkFactory( componentfactory, optional );

        if ( expectComponent )
        {
            TestCase.assertNotNull( instance.getInstance() );
            TestCase.assertNotNull( SimpleComponent.INSTANCE );

            // with removal of the factory, the created instance should also be removed
            checkConfigurationCount( componentname, 1, ComponentConfigurationDTO.ACTIVE );
        }
        else
        {
            TestCase.assertNull( instance.getInstance() );
            TestCase.assertNull( SimpleComponent.INSTANCE );

            // with removal of the factory, the created instance should also be removed
            checkConfigurationCount( componentname, 0, ComponentConfigurationDTO.ACTIVE );
        }
        return instance;
    }

    @Test
    public void test_component_factory_reference() throws Exception
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        final String componentname = "factory.component.reference";
        final String componentfactory = "factory.component.factory.reference";

        SimpleServiceImpl.create( bundleContext, "ignored" ).setFilterProperty( "ignored" );

<<<<<<< HEAD
        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        // missing reference -> unsatisfied
        TestCase.assertEquals( Component.STATE_UNSATISFIED, component.getState() );
=======
        getConfigurationsDisabledThenEnable( componentname, 0, -1 );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        TestCase.assertNull( SimpleComponent.INSTANCE );

        // register a service : filterprop=match
        SimpleServiceImpl match = SimpleServiceImpl.create( bundleContext, "required" ).setFilterProperty( "required" );
        delay();

<<<<<<< HEAD
        TestCase.assertEquals( Component.STATE_FACTORY, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        final ServiceReference[] refs = bundleContext.getServiceReferences( ComponentFactory.class.getName(), "("
            + ComponentConstants.COMPONENT_FACTORY + "=" + componentfactory + ")" );
        TestCase.assertNotNull( refs );
        TestCase.assertEquals( 1, refs.length );
        final ComponentFactory factory = ( ComponentFactory ) bundleContext.getService( refs[0] );
        TestCase.assertNotNull( factory );

        // non-overwrite filterprop
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( PROP_NAME_FACTORY, PROP_NAME_FACTORY );
        final ComponentInstance instance = factory.newInstance( props );
        TestCase.assertNotNull( instance );

        TestCase.assertNotNull( instance.getInstance() );
        TestCase.assertEquals( SimpleComponent.INSTANCE, instance.getInstance() );
        TestCase.assertEquals( PROP_NAME_FACTORY, SimpleComponent.INSTANCE.getProperty( PROP_NAME_FACTORY ) );
        TestCase.assertEquals( 1, SimpleComponent.INSTANCE.m_multiRef.size() );
        TestCase.assertTrue( SimpleComponent.INSTANCE.m_multiRef.contains( match ) );

        final Map<?, ?> instanceMap = ( Map<?, ?> ) getFieldValue( component, "m_componentInstances" );
        TestCase.assertNotNull( instanceMap );
        TestCase.assertEquals( 1, instanceMap.size() );

        final Object instanceManager = getComponentManagerFromComponentInstance( instance );
        TestCase.assertTrue( instanceMap.containsValue( instanceManager ) );

        // check registered components
        final Component[] allFactoryComponents = findComponentsByName( componentname );
        TestCase.assertNotNull( allFactoryComponents );
        TestCase.assertEquals( 2, allFactoryComponents.length );
        for ( int i = 0; i < allFactoryComponents.length; i++ )
        {
            final Component c = allFactoryComponents[i];
            if ( c.getId() == component.getId() )
            {
                TestCase.assertEquals( Component.STATE_FACTORY, c.getState() );
            }
            else if ( c.getId() == SimpleComponent.INSTANCE.m_id )
            {
                TestCase.assertEquals( Component.STATE_ACTIVE, c.getState() );
            }
            else
            {
                TestCase.fail( "Unexpected Component " + c );
            }
        }
=======
        TestCase.assertNull( SimpleComponent.INSTANCE );

        final ComponentInstance instance = createFactoryComponentInstance( componentfactory );
        TestCase.assertEquals( 1, SimpleComponent.INSTANCE.m_multiRef.size() );
        TestCase.assertTrue( SimpleComponent.INSTANCE.m_multiRef.contains( match ) );

        // check registered components
        checkConfigurationCount( componentname, 1, ComponentConfigurationDTO.ACTIVE );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        instance.dispose();
        TestCase.assertNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( instance.getInstance() ); // SCR 112.12.6.2
<<<<<<< HEAD

        TestCase.assertEquals( 0, instanceMap.size() );
        TestCase.assertFalse( instanceMap.containsValue( instanceManager ) );
=======
        checkConfigurationCount( componentname, 0, ComponentConfigurationDTO.ACTIVE );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        // overwritten filterprop
        Hashtable<String, String> propsNonMatch = new Hashtable<String, String>();
        propsNonMatch.put( PROP_NAME_FACTORY, PROP_NAME_FACTORY );
        propsNonMatch.put( "ref.target", "(filterprop=nomatch)" );
<<<<<<< HEAD
=======
        ComponentFactory factory = getComponentFactory( componentfactory );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        try
        {
            factory.newInstance( propsNonMatch );
            TestCase.fail( "Missing reference must fail instance creation" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }

        final SimpleServiceImpl noMatch = SimpleServiceImpl.create( bundleContext, "nomatch" ).setFilterProperty(
<<<<<<< HEAD
            "nomatch" );
=======
                "nomatch" );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        delay();

        final ComponentInstance instanceNonMatch = factory.newInstance( propsNonMatch );

        TestCase.assertNotNull( instanceNonMatch );

        TestCase.assertNotNull( instanceNonMatch.getInstance() );
        TestCase.assertEquals( SimpleComponent.INSTANCE, instanceNonMatch.getInstance() );
        TestCase.assertEquals( PROP_NAME_FACTORY, SimpleComponent.INSTANCE.getProperty( PROP_NAME_FACTORY ) );

        TestCase.assertEquals( 1, SimpleComponent.INSTANCE.m_multiRef.size() );
        TestCase.assertTrue( SimpleComponent.INSTANCE.m_multiRef.contains( noMatch ) );

        // check registered components
<<<<<<< HEAD
        final Component[] allFactoryComponents2 = findComponentsByName( componentname );
        TestCase.assertNotNull( allFactoryComponents2 );
        TestCase.assertEquals( 2, allFactoryComponents2.length );
        for ( int i = 0; i < allFactoryComponents2.length; i++ )
        {
            final Component c = allFactoryComponents2[i];
            if ( c.getId() == component.getId() )
            {
                TestCase.assertEquals( Component.STATE_FACTORY, c.getState() );
            }
            else if ( c.getId() == SimpleComponent.INSTANCE.m_id )
            {
                TestCase.assertEquals( Component.STATE_ACTIVE, c.getState() );
            }
            else
            {
                TestCase.fail( "Unexpected Component " + c );
            }
        }
=======
        checkConfigurationCount( componentname, 1, ComponentConfigurationDTO.ACTIVE );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        match.getRegistration().unregister();
        delay();

        // check registered components (ComponentFactory aint no longer)
<<<<<<< HEAD
        final Component[] allFactoryComponents3 = findComponentsByName( componentname );
        TestCase.assertNotNull( allFactoryComponents3 );
        TestCase.assertEquals( 2, allFactoryComponents3.length );
        for ( int i = 0; i < allFactoryComponents3.length; i++ )
        {
            final Component c = allFactoryComponents3[i];
            if ( c.getId() == component.getId() )
            {
                TestCase.assertEquals( Component.STATE_UNSATISFIED, c.getState() );
            }
            else if ( c.getId() == SimpleComponent.INSTANCE.m_id )
            {
                TestCase.assertEquals( Component.STATE_ACTIVE, c.getState() );
            }
            else
            {
                TestCase.fail( "Unexpected Component " + c );
            }
        }
=======
        checkConfigurationCount( componentname, 1, ComponentConfigurationDTO.ACTIVE );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        //it has already been deactivated.... this should cause an exception?
        noMatch.getRegistration().unregister();
        delay();

        // check registered components (ComponentFactory aint no longer)
<<<<<<< HEAD
        final Component[] allFactoryComponents4 = findComponentsByName( componentname );
        TestCase.assertNotNull( allFactoryComponents4 );
        TestCase.assertEquals( 1, allFactoryComponents4.length );
        for ( int i = 0; i < allFactoryComponents4.length; i++ )
        {
            final Component c = allFactoryComponents4[i];
            if ( c.getId() == component.getId() )
            {
                TestCase.assertEquals( Component.STATE_UNSATISFIED, c.getState() );
            }
            else
            {
                TestCase.fail( "Unexpected Component " + c );
            }
        }
=======
        checkConfigurationCount( componentname, 0, ComponentConfigurationDTO.ACTIVE );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        // deactivated due to unsatisfied reference
        TestCase.assertNull( instanceNonMatch.getInstance() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        //Check that calling dispose on a deactivated instance has no effect
        instanceNonMatch.dispose();
        TestCase.assertNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( instanceNonMatch.getInstance() ); // SCR 112.12.6.2
    }

    @Test
<<<<<<< HEAD
    public void test_component_factory_referredTo() throws InvalidSyntaxException
    {
        //set up the component that refers to the service the factory will create.
        final String referringComponentName = "ComponentReferringToFactoryObject";
        final Component referringComponent = findComponentByName( referringComponentName );
        TestCase.assertNotNull( referringComponent );
        referringComponent.enable();
        delay();

        //make sure it's unsatisfied (service is not yet available
        TestCase.assertEquals( Component.STATE_UNSATISFIED, referringComponent.getState() );

=======
    public void test_component_factory_referredTo() throws Exception
    {
        //set up the component that refers to the service the factory will create.
        final String referringComponentName = "ComponentReferringToFactoryObject";
        getConfigurationsDisabledThenEnable( referringComponentName, 1,
                ComponentConfigurationDTO.UNSATISFIED_REFERENCE );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        final String componentname = "factory.component.referred";
        final String componentfactory = "factory.component.factory.referred";

<<<<<<< HEAD
        final Component component = findComponentByName( componentname );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_FACTORY, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        final ServiceReference[] refs = bundleContext.getServiceReferences( ComponentFactory.class.getName(), "("
            + ComponentConstants.COMPONENT_FACTORY + "=" + componentfactory + ")" );
        TestCase.assertNotNull( refs );
        TestCase.assertEquals( 1, refs.length );
        final ComponentFactory factory = ( ComponentFactory ) bundleContext.getService( refs[0] );
        TestCase.assertNotNull( factory );

        // create the factory instance
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( "service.pid", "myFactoryInstance" );
=======
        getConfigurationsDisabledThenEnable( componentname, 0, -1 );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( "service.pid", "myFactoryInstance" );
        final ComponentFactory factory = getComponentFactory( componentfactory );

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        final ComponentInstance instance = factory.newInstance( props );
        TestCase.assertNotNull( instance );

        TestCase.assertNotNull( instance.getInstance() );
<<<<<<< HEAD

        //The referring service should now be active
        TestCase.assertEquals( Component.STATE_ACTIVE, referringComponent.getState() );
=======
        TestCase.assertTrue( instance.getInstance() instanceof SimpleService );
        //The referring service should now be active
        checkConfigurationCount( referringComponentName, 1, ComponentConfigurationDTO.ACTIVE );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        instance.dispose();
        TestCase.assertNull( instance.getInstance() ); // SCR 112.12.6.2

        //make sure it's unsatisfied (service is no longer available)
<<<<<<< HEAD
        TestCase.assertEquals( Component.STATE_UNSATISFIED, referringComponent.getState() );
    }

    @Test
    public void test_component_factory_with_target_filters() throws InvalidSyntaxException
    {
        final String componentfactory = "factory.component.reference.targetfilter";
        final Component component = findComponentByName( componentfactory );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        SimpleServiceImpl s1 = SimpleServiceImpl.create(bundleContext, "service1");
        SimpleServiceImpl s2 = SimpleServiceImpl.create(bundleContext, "service2");

        // supply configuration now and ensure active
        configure( componentfactory );
        delay();        

        TestCase.assertEquals( Component.STATE_FACTORY, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
        
        final ServiceReference[] refs = bundleContext.getServiceReferences( ComponentFactory.class.getName(), "("
            + ComponentConstants.COMPONENT_FACTORY + "=" + componentfactory + ")" );
        TestCase.assertNotNull( refs );
        TestCase.assertEquals( 1, refs.length );
        final ComponentFactory factory = ( ComponentFactory ) bundleContext.getService( refs[0] );
        TestCase.assertNotNull( factory );

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( PROP_NAME_FACTORY, PROP_NAME_FACTORY );
        props.put("ref.target", "(value=service2)");
        final ComponentInstance instance = factory.newInstance( props );
        TestCase.assertNotNull( instance );

        TestCase.assertNotNull( instance.getInstance() );
        TestCase.assertEquals( SimpleComponent.INSTANCE, instance.getInstance() );
        TestCase.assertEquals( PROP_NAME_FACTORY, SimpleComponent.INSTANCE.getProperty( PROP_NAME_FACTORY ) );

        log.log(LogService.LOG_WARNING, "Bound Services: " +  SimpleComponent.INSTANCE.m_multiRef);
=======
        checkConfigurationCount( referringComponentName, 1, ComponentConfigurationDTO.UNSATISFIED_REFERENCE );
    }

    @Test
    public void test_component_factory_with_target_filters() throws Exception
    {
        final String componentfactory = "factory.component.reference.targetfilter";
        getConfigurationsDisabledThenEnable( componentfactory, 0, -1 );

        SimpleServiceImpl s1 = SimpleServiceImpl.create( bundleContext, "service1" );
        SimpleServiceImpl s2 = SimpleServiceImpl.create( bundleContext, "service2" );

        // supply configuration now and ensure active
        configure( componentfactory );
        delay();

        TestCase.assertNull( SimpleComponent.INSTANCE );

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( PROP_NAME_FACTORY, PROP_NAME_FACTORY );
        props.put( "ref.target", "(value=service2)" );
        final ComponentInstance instance = createFactoryComponentInstance( componentfactory, props );

        log.log( LogService.LOG_WARNING, "Bound Services: " + SimpleComponent.INSTANCE.m_multiRef );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        TestCase.assertFalse( SimpleComponent.INSTANCE.m_multiRef.contains( s1 ) );
        TestCase.assertTrue( SimpleComponent.INSTANCE.m_multiRef.contains( s2 ) );

        instance.dispose();
        TestCase.assertNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( instance.getInstance() ); // SCR 112.12.6.2
<<<<<<< HEAD
        
        s2.drop();
        s1.drop();
    }
    
    @Test
    public void test_component_factory_set_bundle_location_null() throws Exception
    {
        final String componentfactory = "factory.component.reference.targetfilter";
        final Component component = findComponentByName( componentfactory );

        TestCase.assertNotNull( component );
        TestCase.assertFalse( component.isDefaultEnabled() );

        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        component.enable();
        delay();

        SimpleServiceImpl s1 = SimpleServiceImpl.create(bundleContext, "service1");

        ConfigurationAdmin ca = getConfigurationAdmin();
        org.osgi.service.cm.Configuration config = ca.getConfiguration( componentfactory, null );
        config.setBundleLocation( null );
        delay();
        if ( isAtLeastR5() )
        {
            //check that ConfigurationSupport got a Location changed event and set the bundle location
            TestCase.assertNotNull( config.getBundleLocation() );
        } 
        // supply configuration now and ensure active
        configure( componentfactory );
        delay();        

        TestCase.assertEquals( Component.STATE_FACTORY, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
        
        final ServiceReference[] refs = bundleContext.getServiceReferences( ComponentFactory.class.getName(), "("
            + ComponentConstants.COMPONENT_FACTORY + "=" + componentfactory + ")" );
        TestCase.assertNotNull( refs );
        TestCase.assertEquals( 1, refs.length );
        final ComponentFactory factory = ( ComponentFactory ) bundleContext.getService( refs[0] );
        TestCase.assertNotNull( factory );
        
        s1.drop();
=======

        s2.drop();
        s1.drop();
    }

    @Test
    public void test_component_factory_set_bundle_location() throws Exception
    {
        final String componentfactoryPid = "factory.component.configuration";
        final String componentFactoryName = "factory.component.factory.configuration";
        getConfigurationsDisabledThenEnable( componentfactoryPid, 0, -1 );
        checkFactory( componentFactoryName, false );

        org.osgi.service.cm.Configuration config = configure( componentfactoryPid );
        delay();
        assertNotNull( getComponentFactory( componentFactoryName ) );

        config.setBundleLocation( "foo" );
        delay();
        checkFactory( componentFactoryName, false );

        config.setBundleLocation( bundle.getLocation() );
        delay();
        assertNotNull( getComponentFactory( componentFactoryName ) );

        config.setBundleLocation( "foo" );
        delay();
        checkFactory( componentFactoryName, false );

        config.setBundleLocation( "?" );
        delay();
        assertNotNull( getComponentFactory( componentFactoryName ) );

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

}
