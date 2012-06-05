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
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;


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
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }



    @Test
    public void test_component_factory_with_factory_configuration() throws InvalidSyntaxException, IOException
    {
        // this test is about non-standard behaviour of ComponentFactory services

        final String componentname = "factory.component";
        final String componentfactory = "factory.component.factory";

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

        final String factoryConfigPid = createFactoryConfiguration( componentname );
        delay();

        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        final Map<?, ?> instanceMap = ( Map<?, ?> ) getFieldValue( component, "m_configuredServices" );
        TestCase.assertNotNull( instanceMap );
        TestCase.assertEquals( 1, instanceMap.size() );

        final Object instanceManager = getFieldValue( SimpleComponent.INSTANCE.m_activateContext.getComponentInstance(), "m_componentManager" );
        TestCase.assertTrue( instanceMap.containsValue( instanceManager ) );


        // check registered components
        Component[] allFactoryComponents = findComponentsByName( componentname );
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

        // modify the configuration
        Configuration config = getConfigurationAdmin().getConfiguration( factoryConfigPid );
        Dictionary props = config.getProperties();
        props.put( PROP_NAME, PROP_NAME_FACTORY );
        config.update( props );
        delay();

        // ensure instance with new configuration
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertEquals( PROP_NAME_FACTORY, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        // check registered components
        allFactoryComponents = findComponentsByName( componentname );
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

        // disable the factory
        component.disable();
        delay();

        // factory is disabled and so is the instance
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
        TestCase.assertEquals( 1, instanceMap.size() );

        // enabled the factory
        component.enable();
        delay();

        // factory is enabled and so is the instance
        TestCase.assertEquals( Component.STATE_FACTORY, component.getState() );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        TestCase.assertEquals( 1, instanceMap.size() );

        // check registered components
        allFactoryComponents = findComponentsByName( componentname );
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

        // delete the configuration
        getConfigurationAdmin().getConfiguration( factoryConfigPid ).delete();
        delay();

        // factory is enabled but instance has been removed
        TestCase.assertEquals( Component.STATE_FACTORY, component.getState() );
        TestCase.assertNull( SimpleComponent.INSTANCE );
        TestCase.assertEquals( 0, instanceMap.size() );

        // check registered components
        allFactoryComponents = findComponentsByName( componentname );
        TestCase.assertNotNull( allFactoryComponents );
        TestCase.assertEquals( 1, allFactoryComponents.length );
        for ( int i = 0; i < allFactoryComponents.length; i++ )
        {
            final Component c = allFactoryComponents[i];
            if ( c.getId() == component.getId() )
            {
                TestCase.assertEquals( Component.STATE_FACTORY, c.getState() );
            }
            else
            {
                TestCase.fail( "Unexpected Component " + c );
            }
        }
    }

}
