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
package org.apache.felix.scr.impl.metadata;


import java.lang.reflect.Array;
import java.util.List;

import junit.framework.TestCase;

import org.apache.felix.scr.impl.MockLogger;
import org.osgi.service.component.ComponentException;


public class ComponentMetadataTest extends TestCase
{

    private MockLogger logger = new MockLogger();


    // test various combinations of component metadata with respect to
    //  -- immediate: true, false, unset
    //  -- factory: set, unset
    //  -- service: set, unset
    //  -- servicefactory: true, false, unset

    public void testImmediate()
    {
        // immediate is default true if no service element is defined
        final ComponentMetadata cm0 = createComponentMetadata( null, null );
        cm0.validate( logger );
        assertTrue( "Component without service must be immediate", cm0.isImmediate() );

        // immediate is explicit true
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.TRUE, null );
        cm1.validate( logger );
        assertTrue( "Component must be immediate", cm1.isImmediate() );

        // immediate is explicit true
        final ComponentMetadata cm2 = createComponentMetadata( Boolean.TRUE, null );
        cm2.setService( createServiceMetadata( null ) );
        cm2.validate( logger );
        assertTrue( "Component must be immediate", cm2.isImmediate() );

        // immediate is explicit true
        final ComponentMetadata cm3 = createComponentMetadata( Boolean.TRUE, null );
        cm3.setService( createServiceMetadata( Boolean.FALSE ) );
        cm3.validate( logger );
        assertTrue( "Component must be immediate", cm3.isImmediate() );

        // validation failure of immediate with service factory
        final ComponentMetadata cm4 = createComponentMetadata( Boolean.TRUE, null );
        cm4.setService( createServiceMetadata( Boolean.TRUE ) );
        try
        {
            cm4.validate( logger );
            fail( "Expect validation failure for immediate service factory" );
        }
        catch ( ComponentException ce )
        {
            // expect
        }
    }


    public void testDelayed()
    {
        // immediate is default false if service element is defined
        final ComponentMetadata cm0 = createComponentMetadata( null, null );
        cm0.setService( createServiceMetadata( null ) );
        cm0.validate( logger );
        assertFalse( "Component with service must be delayed", cm0.isImmediate() );

        // immediate is default false if service element is defined
        final ComponentMetadata cm1 = createComponentMetadata( null, null );
        cm1.setService( createServiceMetadata( Boolean.TRUE ) );
        cm1.validate( logger );
        assertFalse( "Component with service must be delayed", cm1.isImmediate() );

        // immediate is default false if service element is defined
        final ComponentMetadata cm2 = createComponentMetadata( null, null );
        cm2.setService( createServiceMetadata( Boolean.FALSE ) );
        cm2.validate( logger );
        assertFalse( "Component with service must be delayed", cm2.isImmediate() );

        // immediate is false if service element is defined
        final ComponentMetadata cm3 = createComponentMetadata( Boolean.FALSE, null );
        cm3.setService( createServiceMetadata( null ) );
        cm3.validate( logger );
        assertFalse( "Component with service must be delayed", cm3.isImmediate() );

        // immediate is false if service element is defined
        final ComponentMetadata cm4 = createComponentMetadata( Boolean.FALSE, null );
        cm4.setService( createServiceMetadata( Boolean.TRUE ) );
        cm4.validate( logger );
        assertFalse( "Component with service must be delayed", cm4.isImmediate() );

        // immediate is false if service element is defined
        final ComponentMetadata cm5 = createComponentMetadata( Boolean.FALSE, null );
        cm5.setService( createServiceMetadata( Boolean.FALSE ) );
        cm5.validate( logger );
        assertFalse( "Component with service must be delayed", cm5.isImmediate() );

        // explicit delayed fails when there is no service
        final ComponentMetadata cm6 = createComponentMetadata( Boolean.FALSE, null );
        try
        {
            cm6.validate( logger );
            fail( "Expect validation failure for delayed component without service" );
        }
        catch ( ComponentException ce )
        {
            // expect
        }
    }


    public void testFactory()
    {
        // immediate is default false if factory is defined
        final ComponentMetadata cm0 = createComponentMetadata( null, "factory" );
        cm0.validate( logger );
        assertFalse( "Component with factory must be delayed", cm0.isImmediate() );

        // immediate is false if factory is defined
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.FALSE, "factory" );
        cm1.validate( logger );
        assertFalse( "Component with factory must be delayed", cm1.isImmediate() );

        // immediate is default false if factory is defined
        final ComponentMetadata cm2 = createComponentMetadata( Boolean.TRUE, "factory" );
        try
        {
            cm2.validate( logger );
            fail( "Expect validation failure for immediate factory component" );
        }
        catch ( ComponentException ce )
        {
            // expect
        }

        // immediate is default false if factory is defined
        final ComponentMetadata cm10 = createComponentMetadata( null, "factory" );
        cm10.setService( createServiceMetadata( null ) );
        cm10.validate( logger );
        assertFalse( "Component with factory must be delayed", cm10.isImmediate() );

        // immediate is false if factory is defined
        final ComponentMetadata cm11 = createComponentMetadata( Boolean.FALSE, "factory" );
        cm11.setService( createServiceMetadata( null ) );
        cm11.validate( logger );
        assertFalse( "Component with factory must be delayed", cm11.isImmediate() );

        // immediate is default false if factory is defined
        final ComponentMetadata cm12 = createComponentMetadata( Boolean.TRUE, "factory" );
        cm12.setService( createServiceMetadata( null ) );
        try
        {
            cm12.validate( logger );
            fail( "Expect validation failure for immediate factory component" );
        }
        catch ( ComponentException ce )
        {
            // expect
        }

        // immediate is default false if factory is defined
        final ComponentMetadata cm20 = createComponentMetadata( null, "factory" );
        cm20.setService( createServiceMetadata( Boolean.FALSE ) );
        cm20.validate( logger );
        assertFalse( "Component with factory must be delayed", cm20.isImmediate() );

        // immediate is false if factory is defined
        final ComponentMetadata cm21 = createComponentMetadata( Boolean.FALSE, "factory" );
        cm21.setService( createServiceMetadata( Boolean.FALSE ) );
        cm21.validate( logger );
        assertFalse( "Component with factory must be delayed", cm21.isImmediate() );

        // immediate is default false if factory is defined
        final ComponentMetadata cm22 = createComponentMetadata( Boolean.TRUE, "factory" );
        cm22.setService( createServiceMetadata( Boolean.FALSE ) );
        try
        {
            cm22.validate( logger );
            fail( "Expect validation failure for immediate factory component" );
        }
        catch ( ComponentException ce )
        {
            // expect
        }

        // immediate is default false if factory is defined
        final ComponentMetadata cm30 = createComponentMetadata( null, "factory" );
        cm30.setService( createServiceMetadata( Boolean.TRUE ) );
        try
        {
            cm30.validate( logger );
            fail( "Expect validation failure for factory component with service factory" );
        }
        catch ( ComponentException ce )
        {
            // expect
        }

        // immediate is false if factory is defined
        final ComponentMetadata cm31 = createComponentMetadata( Boolean.FALSE, "factory" );
        cm31.setService( createServiceMetadata( Boolean.TRUE ) );
        try
        {
            cm31.validate( logger );
            fail( "Expect validation failure for factory component with service factory" );
        }
        catch ( ComponentException ce )
        {
            // expect
        }

        // immediate is default false if factory is defined
        final ComponentMetadata cm32 = createComponentMetadata( Boolean.TRUE, "factory" );
        cm32.setService( createServiceMetadata( Boolean.TRUE ) );
        try
        {
            cm32.validate( logger );
            fail( "Expect validation failure for immediate factory component with service factory" );
        }
        catch ( ComponentException ce )
        {
            // expect
        }

    }


    public void test_component_no_name_ds10()
    {
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.TRUE, null );
        cm1.setName( null );
        try
        {
            cm1.validate( logger );
            fail( "Expected validation failure for DS 1.0 component without name" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_component_no_name_ds11()
    {
        final ComponentMetadata cm1 = createComponentMetadata11( Boolean.TRUE, null );
        cm1.setName( null );
        cm1.validate( logger );
        assertEquals( "Expected name to equal implementation class name", cm1.getImplementationClassName(),
            cm1.getName() );
    }


    public void test_component_activate_ds10()
    {
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.TRUE, null );
        cm1.validate( logger );
        assertEquals( "Activate method name", "activate", cm1.getActivate() );
        assertFalse( "Activate method expected to not be declared", cm1.isActivateDeclared() );

        final ComponentMetadata cm2 = createComponentMetadata( Boolean.TRUE, null );
        cm2.setActivate( "someMethod" );
        failDS10Validation( cm2, "activate", logger );
    }


    public void test_component_activate_ds11()
    {
        final ComponentMetadata cm1 = createComponentMetadata11( Boolean.TRUE, null );
        cm1.validate( logger );
        assertEquals( "Activate method name", "activate", cm1.getActivate() );
        assertFalse( "Activate method expected to not be declared", cm1.isActivateDeclared() );

        final ComponentMetadata cm2 = createComponentMetadata11( Boolean.TRUE, null );
        cm2.setActivate( "someMethod" );
        cm2.validate( logger );
        assertEquals( "Activate method name", "someMethod", cm2.getActivate() );
        assertTrue( "Activate method expected to be declared", cm2.isActivateDeclared() );
    }


    public void test_component_deactivate_ds10()
    {
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.TRUE, null );
        cm1.validate( logger );
        assertEquals( "Deactivate method name", "deactivate", cm1.getDeactivate() );
        assertFalse( "Deactivate method expected to not be declared", cm1.isDeactivateDeclared() );

        final ComponentMetadata cm2 = createComponentMetadata( Boolean.TRUE, null );
        cm2.setDeactivate( "someMethod" );
        failDS10Validation( cm2, "deactivate", logger );
    }


    public void test_component_deactivate_ds11()
    {
        final ComponentMetadata cm1 = createComponentMetadata11( Boolean.TRUE, null );
        cm1.validate( logger );
        assertEquals( "Deactivate method name", "deactivate", cm1.getDeactivate() );
        assertFalse( "Deactivate method expected to not be declared", cm1.isDeactivateDeclared() );

        final ComponentMetadata cm2 = createComponentMetadata11( Boolean.TRUE, null );
        cm2.setDeactivate( "someMethod" );
        cm2.validate( logger );
        assertEquals( "Deactivate method name", "someMethod", cm2.getDeactivate() );
        assertTrue( "Deactivate method expected to be declared", cm2.isDeactivateDeclared() );
    }


    public void test_component_modified_ds10()
    {
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.TRUE, null );
        cm1.validate( logger );
        assertNull( "Modified method name", cm1.getModified() );

        final ComponentMetadata cm2 = createComponentMetadata( Boolean.TRUE, null );
        cm2.setModified( "someName" );
        failDS10Validation( cm2, "modified", logger );
    }


    public void test_component_modified_ds11()
    {
        final ComponentMetadata cm1 = createComponentMetadata11( Boolean.TRUE, null );
        cm1.validate( logger );
        assertNull( "Modified method name", cm1.getModified() );

        final ComponentMetadata cm2 = createComponentMetadata11( Boolean.TRUE, null );
        cm2.setModified( "someMethod" );
        cm2.validate( logger );
        assertEquals( "Modified method name", "someMethod", cm2.getModified() );
    }


    public void test_component_configuration_policy_ds10()
    {
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.TRUE, null );
        cm1.validate( logger );
        assertEquals( "Configuration policy", ComponentMetadata.CONFIGURATION_POLICY_OPTIONAL,
            cm1.getConfigurationPolicy() );

        final ComponentMetadata cm2 = createComponentMetadata( Boolean.TRUE, null );
        cm2.setConfigurationPolicy( ComponentMetadata.CONFIGURATION_POLICY_IGNORE );
        failDS10Validation( cm2, "configuration-policy", logger );

        final ComponentMetadata cm3 = createComponentMetadata( Boolean.TRUE, null );
        cm3.setConfigurationPolicy( ComponentMetadata.CONFIGURATION_POLICY_OPTIONAL );
        failDS10Validation( cm3, "configuration-policy", logger );

        final ComponentMetadata cm4 = createComponentMetadata( Boolean.TRUE, null );
        cm4.setConfigurationPolicy( ComponentMetadata.CONFIGURATION_POLICY_REQUIRE );
        failDS10Validation( cm4, "configuration-policy", logger );

        final ComponentMetadata cm5 = createComponentMetadata( Boolean.TRUE, null );
        cm5.setConfigurationPolicy( "undefined" );
        failDS10Validation( cm5, "configuration-policy", logger );
    }


    public void test_component_configuration_policy_ds11()
    {
        final ComponentMetadata cm1 = createComponentMetadata11( Boolean.TRUE, null );
        cm1.validate( logger );
        assertEquals( "Configuration policy", ComponentMetadata.CONFIGURATION_POLICY_OPTIONAL,
            cm1.getConfigurationPolicy() );

        final ComponentMetadata cm2 = createComponentMetadata11( Boolean.TRUE, null );
        cm2.setConfigurationPolicy( ComponentMetadata.CONFIGURATION_POLICY_IGNORE );
        cm2.validate( logger );
        assertEquals( "Configuration policy", ComponentMetadata.CONFIGURATION_POLICY_IGNORE,
            cm2.getConfigurationPolicy() );

        final ComponentMetadata cm3 = createComponentMetadata11( Boolean.TRUE, null );
        cm3.setConfigurationPolicy( ComponentMetadata.CONFIGURATION_POLICY_OPTIONAL );
        cm3.validate( logger );
        assertEquals( "Configuration policy", ComponentMetadata.CONFIGURATION_POLICY_OPTIONAL,
            cm3.getConfigurationPolicy() );

        final ComponentMetadata cm4 = createComponentMetadata11( Boolean.TRUE, null );
        cm4.setConfigurationPolicy( ComponentMetadata.CONFIGURATION_POLICY_REQUIRE );
        cm4.validate( logger );
        assertEquals( "Configuration policy", ComponentMetadata.CONFIGURATION_POLICY_REQUIRE,
            cm4.getConfigurationPolicy() );

        final ComponentMetadata cm5 = createComponentMetadata11( Boolean.TRUE, null );
        cm5.setConfigurationPolicy( "undefined" );
        try
        {
            cm5.validate( logger );
            fail( "Expected validation failure due to undefined configuration policy" );
        }
        catch ( ComponentException ce )
        {
            // expected due to undefned configuration policy
        }
    }


    public void test_reference_valid()
    {
        // two references, should validate
        final ComponentMetadata cm1 = createComponentMetadata( Boolean.TRUE, null );
        cm1.addDependency( createReferenceMetadata( "name1" ) );
        cm1.addDependency( createReferenceMetadata( "name2" ) );
        cm1.validate( logger );
    }


    public void test_reference_duplicate_name()
    {
        // two references with same name, must warn
        final ComponentMetadata cm2 = createComponentMetadata( Boolean.TRUE, null );
        cm2.addDependency( createReferenceMetadata( "name1" ) );
        cm2.addDependency( createReferenceMetadata( "name1" ) );
        try
        {
            cm2.validate( logger );
            fail( "Expect validation failure for duplicate reference name" );
        }
        catch ( ComponentException ee )
        {
             //expected
        }
    }


    public void test_reference_no_name_ds10()
    {
        // un-named reference, illegal for pre DS 1.1
        final ComponentMetadata cm3 = createComponentMetadata( Boolean.TRUE, null );
        cm3.addDependency( createReferenceMetadata( null ) );
        try
        {
            cm3.validate( logger );
            fail( "Expect validation failure for DS 1.0 reference without name" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_reference_no_name_ds11()
    {
        // un-named reference, illegal for DS 1.1
        final ComponentMetadata cm4 = createComponentMetadata11( Boolean.TRUE, null );
        final ReferenceMetadata rm4 = createReferenceMetadata( null );
        cm4.addDependency( rm4 );
        cm4.validate( logger );
        assertEquals( "Reference name defaults to interface", rm4.getInterface(), rm4.getName() );
    }


    public void test_reference_updated_ds10()
    {
        // updated method ignored for DS 1.0
        final ReferenceMetadata rm3 = createReferenceMetadata( "test" );
        rm3.setUpdated( "my_updated_method" );
        final ComponentMetadata cm3 = createComponentMetadata( Boolean.TRUE, null );
        cm3.addDependency( rm3 );

        // according to DS 1.2 must fail validation (FELIX-3648)
        failDS10Validation( cm3, "updated", logger );
    }


    public void test_reference_updated_ds11()
    {
        // updated method ignored for DS 1.1
        final ReferenceMetadata rm3 = createReferenceMetadata( "test" );
        rm3.setUpdated( "my_updated_method" );
        final ComponentMetadata cm3 = createComponentMetadata11( Boolean.TRUE, null );
        cm3.addDependency( rm3 );

        // according to DS 1.2 must fail validation (FELIX-3648)
        failDS10Validation( cm3, "updated", logger );
    }


    public void test_reference_updated_ds11_felix()
    {
        // updated method accepted for DS 1.1-felix
        final ReferenceMetadata rm3 = createReferenceMetadata( "test" );
        rm3.setUpdated( "my_updated_method" );
        final ComponentMetadata cm3 = createComponentMetadata( DSVersion.DS11Felix, Boolean.TRUE, null );
        cm3.addDependency( rm3 );

        // validates fine and logs no message
        cm3.validate( logger );

        assertEquals( "my_updated_method", rm3.getUpdated() );
    }


    public void test_reference_updated_ds12()
    {
        // updated method accepted for DS 1.2
        final ReferenceMetadata rm3 = createReferenceMetadata( "test" );
        rm3.setUpdated( "my_updated_method" );
        final ComponentMetadata cm3 = createComponentMetadata( DSVersion.DS12, Boolean.TRUE, null );
        cm3.addDependency( rm3 );

        // validates fine and logs no message
        cm3.validate( logger );

        assertEquals( "my_updated_method", rm3.getUpdated() );
    }


    public void test_duplicate_implementation_ds10()
    {
        final ComponentMetadata cm = createComponentMetadata( Boolean.TRUE, null );
        cm.setImplementationClassName( "second.implementation.class" );
        try
        {
            cm.validate( logger );
            fail( "Expect validation failure for duplicate implementation element" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_duplicate_implementation_ds11()
    {
        final ComponentMetadata cm = createComponentMetadata11( Boolean.TRUE, null );
        cm.setImplementationClassName( "second.implementation.class" );
        try
        {
            cm.validate( logger );
            fail( "Expect validation failure for duplicate implementation element" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_duplicate_service_ds10()
    {
        final ComponentMetadata cm = createComponentMetadata( Boolean.TRUE, null );
        cm.setService( createServiceMetadata( Boolean.TRUE ) );
        cm.setService( createServiceMetadata( Boolean.TRUE ) );
        try
        {
            cm.validate( logger );
            fail( "Expect validation failure for duplicate service element" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_duplicate_service_ds11()
    {
        final ComponentMetadata cm = createComponentMetadata11( Boolean.TRUE, null );
        cm.setService( createServiceMetadata( Boolean.TRUE ) );
        cm.setService( createServiceMetadata( Boolean.TRUE ) );
        try
        {
            cm.validate( logger );
            fail( "Expect validation failure for duplicate service element" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_property_no_name_ds10()
    {
        final ComponentMetadata cm = createComponentMetadata( null, null );
        cm.addProperty( createPropertyMetadata( null, null, "" ) );
        try
        {
            cm.validate( logger );
            fail( "Expect validation failure for missing property name" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_property_no_name_ds11()
    {
        final ComponentMetadata cm = createComponentMetadata11( null, null );
        cm.addProperty( createPropertyMetadata( null, null, "" ) );
        try
        {
            cm.validate( logger );
            fail( "Expect validation failure for missing property name" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_property_char_ds10() throws ComponentException
    {
        final ComponentMetadata cm = createComponentMetadata( null, null );
        PropertyMetadata prop = createPropertyMetadata( "x", "Char", Integer.toString( 'x' ) );
        cm.addProperty( prop );
        cm.validate( logger );
        assertTrue( prop.getValue() instanceof Character );
        assertEquals( new Character( 'x' ), prop.getValue() );
    }


    public void test_property_char_ds11()
    {
        final ComponentMetadata cm = createComponentMetadata11( null, null );
        cm.addProperty( createPropertyMetadata( "x", "Char", "x" ) );
        try
        {
            cm.validate( logger );
            fail( "Expect validation failure for illegal property type Char" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_property_non_character()
    {
        final ComponentMetadata cm = createComponentMetadata( null, null );

        assertProperty( "String", "Ein String", cm );
        assertProperty( "Double", new Double( 2.5 ), cm );
        assertProperty( "Float", new Float( 2.5 ), cm );
        assertProperty( "Long", new Long( 2 ), cm );
        assertProperty( "Integer", new Integer( 2 ), cm );
        assertProperty( "Short", new Short( ( short ) 2 ), cm );
        assertProperty( "Byte", new Byte( ( byte ) 2 ), cm );
        assertProperty( "Boolean", Boolean.TRUE, cm );

        assertPropertyFail( "Double", "x", cm );
        assertPropertyFail( "Float", "x", cm );
        assertPropertyFail( "Long", "x", cm );
        assertPropertyFail( "Integer", "x", cm );
        assertPropertyFail( "Short", "x", cm );
        assertPropertyFail( "Byte", "x", cm );
    }


    public void test_property_array_non_character()
    {
        final ComponentMetadata cm = createComponentMetadata( null, null );
        assertPropertyArray( "String", "Ein String", cm );
        assertPropertyArray( "Double", new Double( 2.5 ), cm );
        assertPropertyArray( "Float", new Float( 2.5 ), cm );
        assertPropertyArray( "Long", new Long( 2 ), cm );
        assertPropertyArray( "Integer", new Integer( 2 ), cm );
        assertPropertyArray( "Short", new Short( ( short ) 2 ), cm );
        assertPropertyArray( "Byte", new Byte( ( byte ) 2 ), cm );
        assertPropertyArray( "Boolean", Boolean.TRUE, cm );

        assertPropertyArrayFail( "Double", "x", cm );
        assertPropertyArrayFail( "Float", "x", cm );
        assertPropertyArrayFail( "Long", "x", cm );
        assertPropertyArrayFail( "Integer", "x", cm );
        assertPropertyArrayFail( "Short", "x", cm );
        assertPropertyArrayFail( "Byte", "x", cm );
    }


    public void test_property_character_ds10()
    {
        final ComponentMetadata cm = createComponentMetadata( null, null );
        try
        {
            createPropertyMetadata( "x", "Character", Integer.toString( 'x' ) ).validate( cm );
            fail( "Expect validation failure for illegal property type Character" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }

    public void test_configuration_pid_use_ds12()
    {
      ComponentMetadata cm = createComponentMetadata11( null, null );
        try
        {
          cm.setConfigurationPid( new String[] {"configurationPid"} );
          cm.validate( logger );
          fail( "Expect validation failure for illegal configuration-pid usage in ds 1.1 namespace" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }

        cm = createComponentMetadata12( null, null );
        try
        {
          cm.setConfigurationPid( new String[] {"configurationPid"} );
          cm.validate( logger );
        }
        catch ( ComponentException ce )
        {
          ce.printStackTrace();
          fail( "Expect correct validation for legal configuration-pid usage in ds 1.2 or later namespace" );
        }
    }

    public void test_get_configuration_pid_method()
    {
        doTest_get_configuration_pid_method(DSVersion.DS10);
        doTest_get_configuration_pid_method(DSVersion.DS11);
        doTest_get_configuration_pid_method(DSVersion.DS12);
    }

    private void doTest_get_configuration_pid_method(DSVersion specVersion)
    {
        // Make sure that getConfigurationPid returns the default component name (implementation class name).
        // We only do this kind of test if spec is greater than ds 1.0, because in ds 1.0, the component name is mandatory.
        if ( specVersion.isDS11() )
        {
            ComponentMetadata cm = new ComponentMetadata( specVersion );
            try
            {
                cm.setImplementationClassName("implementation.class");
                cm.setName( null );
                cm.validate( logger );
            }
            catch ( ComponentException ce )
            {
                fail( "Expect correct validation for unnamed component" );
            }
            List<String> pid = cm.getConfigurationPid();
            assertFalse( "Expect non-null configuration pid when component name is not specified", pid.isEmpty() );
            assertEquals( "Expect configuration-pid to be equals to component implementation",
                          "implementation.class", pid.get( 0 ) );
        }

        // Make sure that getConfigurationPid returns the name of the component, if specified
        ComponentMetadata cm = new ComponentMetadata( specVersion );
        try
        {
            cm.setImplementationClassName("implementation.class");
            cm.setName("my.component.name");
            cm.validate( logger );
        }
        catch ( ComponentException ce )
        {
            fail( "Expect correct validation for named component" );
        }
        List<String> pid = cm.getConfigurationPid();
        assertFalse( "Expect non-null configuration pid when component name is not specified", pid.isEmpty() );
        assertEquals( "Expect configuration-pid to be equals to component name",
                      "my.component.name", pid.get( 0 ) );
    }

    public void test_property_character_ds11() throws ComponentException
    {
        final ComponentMetadata cm = createComponentMetadata11( null, null );
        PropertyMetadata prop = createPropertyMetadata( "x", "Character", Integer.toString( 'x' ) );
        cm.addProperty( prop );
        cm.validate( logger );
        assertTrue( prop.getValue() instanceof Character );
        assertEquals( new Character( 'x' ), prop.getValue() );
    }


    //---------- Helper methods

    // method also used by XmlHandlerTest
    static void failDS10Validation( final ComponentMetadata metadata, final String expectedValidationReason,
        final MockLogger logger )
    {
        try
        {
            metadata.validate( logger );
            fail( "Expected validation failure for Component " + metadata.getName() + " containing '"
                + expectedValidationReason + "'" );
        }
        catch ( ComponentException ce )
        {
            assertTrue(
                "Expected validation reason to contain '" + expectedValidationReason + "': actual: " + ce.getMessage(),
                ce.getMessage().indexOf( expectedValidationReason ) >= 0 );
        }
    }


    // Creates Component Metadata for the given namespace
    private ComponentMetadata createComponentMetadata( DSVersion dsVersion, Boolean immediate, String factory )
    {
        ComponentMetadata meta = new ComponentMetadata( dsVersion );
        meta.setName( "place.holder" );
        meta.setImplementationClassName( "place.holder.implementation" );
        if ( immediate != null )
        {
            meta.setImmediate( immediate.booleanValue() );
        }
        if ( factory != null )
        {
            meta.setFactoryIdentifier( factory );
        }
        return meta;
    }


    // Creates DS 1.0 Component Metadata
    private ComponentMetadata createComponentMetadata( Boolean immediate, String factory )
    {
        return createComponentMetadata( DSVersion.DS10, immediate, factory );
    }


    // Creates DS 1.1 Component Metadata
    private ComponentMetadata createComponentMetadata11( Boolean immediate, String factory )
    {
        return createComponentMetadata( DSVersion.DS11, immediate, factory );
    }

    // Creates DS 1.2 Component Metadata
    private ComponentMetadata createComponentMetadata12( Boolean immediate, String factory )
    {
        return createComponentMetadata( DSVersion.DS12, immediate, factory );
    }

    private ServiceMetadata createServiceMetadata( Boolean serviceFactory )
    {
        ServiceMetadata meta = new ServiceMetadata();
        meta.addProvide( "place.holder.service" );
        if ( serviceFactory != null )
        {
            meta.setServiceFactory( serviceFactory.booleanValue() );
        }
        return meta;
    }


    private ReferenceMetadata createReferenceMetadata( String name )
    {
        ReferenceMetadata meta = new ReferenceMetadata();
        meta.setName( name );
        meta.setInterface( "place.holder" );
        return meta;
    }


    private PropertyMetadata createPropertyMetadata( String propertyName, String type, String value )
    {
        PropertyMetadata meta = new PropertyMetadata();
        if ( propertyName != null )
        {
            meta.setName( propertyName );
        }
        if ( type != null )
        {
            meta.setType( type );
        }
        if ( value != null )
        {
            meta.setValue( value );
        }
        return meta;
    }


    private void assertProperty( String type, Object value, ComponentMetadata cmeta )
    {
        PropertyMetadata meta = createPropertyMetadata( "dummy", type, String.valueOf( value ) );
        meta.validate( cmeta );
        assertSame( value.getClass(), meta.getValue().getClass() );
        assertEquals( value, meta.getValue() );
    }


    private void assertPropertyArray( String type, Object value, ComponentMetadata cmeta )
    {
        PropertyMetadata meta = createPropertyMetadata( "dummy", type, null );
        meta.setValues( String.valueOf( value ) );
        meta.validate( cmeta );

        Object propVal = meta.getValue();
        assertTrue( propVal.getClass().isArray() );
        assertPrimitiveType( value.getClass(), propVal.getClass().getComponentType() );
        assertEquals( 1, Array.getLength( propVal ) );
        assertEquals( value, Array.get( propVal, 0 ) );
    }


    private void assertPropertyFail( String type, String value, ComponentMetadata cmeta )
    {
        try
        {
            PropertyMetadata meta = createPropertyMetadata( "dummy", type, value );
            meta.validate( cmeta );
            fail( "Expected validation failure for " + type + "=" + value );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    private void assertPropertyArrayFail( String type, String value, ComponentMetadata cmeta )
    {
        try
        {
            PropertyMetadata meta = createPropertyMetadata( "dummy", type, null );
            meta.setValues( value );
            meta.validate( cmeta );
            fail( "Expected validation failure for " + type + "=" + value );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    private void assertPrimitiveType( final Class expectedBoxClass, final Class actualClass )
    {
        if ( expectedBoxClass == String.class )
        {
            assertEquals( expectedBoxClass, actualClass );
        }
        else if ( expectedBoxClass == Double.class )
        {
            assertEquals( Double.TYPE, actualClass );
        }
        else if ( expectedBoxClass == Float.class )
        {
            assertEquals( Float.TYPE, actualClass );
        }
        else if ( expectedBoxClass == Long.class )
        {
            assertEquals( Long.TYPE, actualClass );
        }
        else if ( expectedBoxClass == Integer.class )
        {
            assertEquals( Integer.TYPE, actualClass );
        }
        else if ( expectedBoxClass == Short.class )
        {
            assertEquals( Short.TYPE, actualClass );
        }
        else if ( expectedBoxClass == Byte.class )
        {
            assertEquals( Byte.TYPE, actualClass );
        }
        else if ( expectedBoxClass == Boolean.class )
        {
            assertEquals( Boolean.TYPE, actualClass );
        }
        else
        {
            fail( "Unexpected box class " + expectedBoxClass );
        }
    }
}
