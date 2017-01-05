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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.felix.scr.impl.MockBundle;
import org.apache.felix.scr.impl.MockLogger;
import org.apache.felix.scr.impl.parser.KXml2SAXParser;
import org.apache.felix.scr.impl.parser.ParseException;
import org.apache.felix.scr.impl.xml.XmlHandler;
import org.osgi.service.component.ComponentException;
import org.xmlpull.v1.XmlPullParserException;


public class XmlHandlerTest extends TestCase
{
    private MockLogger logger;


    protected void setUp() throws Exception
    {
        super.setUp();

        logger = new MockLogger();
    }


    public void test_unclosed_elements() throws Exception
    {
        try
        {
            readMetadataFromString( "<component name=\"n\"><implementation class=\"n\" /><component name=\"x\">" );
            fail( "ParseException expected for unclosed elements" );
        }
        catch ( ParseException pe )
        {
            // exptected
        }
    }


    public void test_no_opening_element() throws Exception
    {
        try
        {
            readMetadataFromString( "</component>" );
            fail( "Exception expected for element without opening element" );
        }
        catch ( Exception p )
        {
            // exptected
        }
    }


    public void test_interleaved_elements() throws Exception
    {
        try
        {
            readMetadataFromString( "<component name=\"n\" ><implementation class=\"n\"></component></implementation>" );
            fail( "Exception expected for interleaved elements" );
        }
        catch ( Exception p )
        {
            // exptected
        }
    }


    public void test_namespace_1_0_0() throws Exception
    {
        final List metadataList = readMetadataFromString( "<scr:component xmlns:scr=\"http://www.osgi.org/xmlns/scr/v1.0.0\" name=\"n\" ><implementation class=\"n\"/></scr:component>" );
        assertEquals( "1 Descriptor expected", 1, metadataList.size() );
        final ComponentMetadata metadata = ( ComponentMetadata ) metadataList.get( 0 );
        assertEquals( "Expect NS 1.0.0", DSVersion.DS10, metadata.getDSVersion() );
    }


    public void test_namespace_1_1_0() throws Exception
    {
        final List metadataList = readMetadataFromString( "<scr:component xmlns:scr=\"http://www.osgi.org/xmlns/scr/v1.1.0\" name=\"n\" ><implementation class=\"n\"/></scr:component>" );
        assertEquals( "1 Descriptor expected", 1, metadataList.size() );
        final ComponentMetadata metadata = ( ComponentMetadata ) metadataList.get( 0 );
        assertEquals( "Expect NS 1.1.0", DSVersion.DS11, metadata.getDSVersion() );
    }


    public void test_namespace_1_1_0_felix() throws Exception
    {
        final List metadataList = readMetadataFromString( "<scr:component xmlns:scr=\"http://felix.apache.org/xmlns/scr/v1.1.0-felix\" name=\"n\" ><implementation class=\"n\"/></scr:component>" );
        assertEquals( "1 Descriptor expected", 1, metadataList.size() );
        final ComponentMetadata metadata = ( ComponentMetadata ) metadataList.get( 0 );
        assertEquals( "Expect NS 1.1.0-felix", DSVersion.DS11Felix, metadata.getDSVersion() );
    }


    public void test_namespace_1_2_0() throws Exception
    {
        final List metadataList = readMetadataFromString( "<scr:component xmlns:scr=\"http://www.osgi.org/xmlns/scr/v1.2.0\" name=\"n\" ><implementation class=\"n\"/></scr:component>" );
        assertEquals( "1 Descriptor expected", 1, metadataList.size() );
        final ComponentMetadata metadata = ( ComponentMetadata ) metadataList.get( 0 );
        assertEquals( "Expect NS 1.2.0", DSVersion.DS12, metadata.getDSVersion() );
    }


    public void test_namespace_1_2_0_felix() throws Exception
    {
        final List metadataList = readMetadataFromString( "<scr:component xmlns:scr=\"http://felix.apache.org/xmlns/scr/v1.2.0-felix\" name=\"n\" ><implementation class=\"n\"/></scr:component>" );
        assertEquals( "1 Descriptor expected", 1, metadataList.size() );
        final ComponentMetadata metadata = ( ComponentMetadata ) metadataList.get( 0 );
        assertEquals( "Expect NS 1.2.0-felix", DSVersion.DS12Felix, metadata.getDSVersion() );
    }


    public void test_namespace_unknown() throws Exception
    {
        final List metadataList = readMetadataFromString( "<components xmlns:scr=\"http://www.osgi.org/xmlns/scr/v1.1.0-felix\"><scr:component name=\"n\" ><implementation class=\"n\"/></scr:component></components>" );
        assertTrue( "No Descriptor expected", metadataList.isEmpty() );
    }


    public void test_no_namespace() throws Exception
    {
        final List metadataList = readMetadata( "/components_no_namespace.xml" );
        assertEquals( "1 Descriptor expected", 1, metadataList.size() );

        final ComponentMetadata metadata = ( ComponentMetadata ) metadataList.get( 0 );
        assertEquals( "Expect NS 1.0.0", DSVersion.DS10, metadata.getDSVersion() );
    }


    public void test_component_attributes_11() throws Exception
    {
        final List metadataList10 = readMetadata( "/components_activate_10.xml" );
        assertEquals( "Component Descriptors", 4, metadataList10.size() );
        ComponentMetadataTest.failDS10Validation( ( ComponentMetadata ) metadataList10.get( 0 ), "activate", logger );
        ComponentMetadataTest.failDS10Validation( ( ComponentMetadata ) metadataList10.get( 1 ), "deactivate", logger );
        ComponentMetadataTest.failDS10Validation( ( ComponentMetadata ) metadataList10.get( 2 ), "modified", logger );
        ComponentMetadataTest.failDS10Validation( ( ComponentMetadata ) metadataList10.get( 3 ),
            "configuration-policy", logger );

        final List metadataList11 = readMetadata( "/components_activate_11.xml" );
        assertEquals( "Component Descriptors", 1, metadataList11.size() );
        final ComponentMetadata cm11 = ( ComponentMetadata ) metadataList11.get( 0 );
        cm11.validate( logger );
        assertEquals( "DS Version 1.1", DSVersion.DS11, cm11.getDSVersion() );
        assertEquals( "Expected Activate Method set", "myactivate", cm11.getActivate() );
        assertTrue( "Activate method expected to be declared", cm11.isActivateDeclared() );
        assertEquals( "Expected Deactivate Method set", "mydeactivate", cm11.getDeactivate() );
        assertTrue( "Activate method expected to be declared", cm11.isDeactivateDeclared() );
        assertEquals( "Expected Modified Method set", "mymodified", cm11.getModified() );
        assertEquals( "Expected Configuration Policy set", ComponentMetadata.CONFIGURATION_POLICY_IGNORE,
            cm11.getConfigurationPolicy() );
    }


    public void test_component_no_name() throws Exception
    {
        final List metadataList10 = readMetadata( "/components_anonymous_10.xml" );
        assertEquals( "Component Descriptors", 1, metadataList10.size() );
        final ComponentMetadata cm10 = ( ComponentMetadata ) metadataList10.get( 0 );
        try
        {
            cm10.validate( logger );
            fail( "Expected validation failure for component without name" );
        }
        catch ( ComponentException ce )
        {
            // expected !!
        }

        final List metadataList11 = readMetadata( "/components_anonymous_11.xml" );
        assertEquals( "Component Descriptors", 1, metadataList11.size() );
        final ComponentMetadata cm11 = ( ComponentMetadata ) metadataList11.get( 0 );
        cm11.validate( logger );
        assertEquals( "Expected name equals class", cm11.getImplementationClassName(), cm11.getName() );
    }


    public void test_reference_no_name() throws Exception
    {
        final List metadataList10 = readMetadata( "/components_anonymous_10.xml" );
        assertEquals( "Component Descriptors", 1, metadataList10.size() );
        final ComponentMetadata cm10 = ( ComponentMetadata ) metadataList10.get( 0 );
        try
        {
            cm10.validate( logger );
            fail( "Expected validation failure for component without name" );
        }
        catch ( ComponentException ce )
        {
            // expected !!
        }

        final List metadataList11 = readMetadata( "/components_anonymous_11.xml" );
        assertEquals( "Component Descriptors", 1, metadataList11.size() );
        final ComponentMetadata cm11 = ( ComponentMetadata ) metadataList11.get( 0 );
        cm11.validate( logger );
        assertEquals( "Expected name equals class", cm11.getImplementationClassName(), cm11.getName() );
    }


    public void test_all_elements_10() throws Exception
    {
        final List metadataList10 = readMetadata( "/components_all_elements_10.xml" );
        assertEquals( "Component Descriptors", 1, metadataList10.size() );
        final ComponentMetadata cm10 = ( ComponentMetadata ) metadataList10.get( 0 );

        // dont validate this, we test the raw reading

        // ds namespace
        assertEquals( "DS Version 1.0", DSVersion.DS10, cm10.getDSVersion() );
        assertFalse( "DS Version 1.0", cm10.getDSVersion().isDS11() );

        // base component attributes
        assertEquals( "component name", true, cm10.isEnabled() );
        assertEquals( "component name", "components.all.name", cm10.getName() );
        assertEquals( "component name", "components.all.factory", cm10.getFactoryIdentifier() );
        assertEquals( "component name", true, cm10.isFactory() );
        assertEquals( "component name", true, cm10.isImmediate() );

        // ds 1.1 elements
        assertEquals( "activate method", "myactivate", cm10.getActivate() );
        assertEquals( "deactivate method", "mydeactivate", cm10.getDeactivate() );
        assertTrue( "Activate method expected to be declared", cm10.isActivateDeclared() );
        assertEquals( "modified method", "mymodified", cm10.getModified() );
        assertTrue( "Deactivate method expected to be declared", cm10.isDeactivateDeclared() );
        assertEquals( "configuration policy", "ignore", cm10.getConfigurationPolicy() );

        // from the implementation element
        assertEquals( "component name", "components.all.impl", cm10.getImplementationClassName() );

        // property setting
        final PropertyMetadata prop = getPropertyMetadata( cm10, "prop" );
        prop.validate( cm10 ); // property value requires validation
        assertNotNull( "prop exists", prop );
        assertEquals( "prop type", "Integer", prop.getType() );
        assertEquals( "prop value", 1234, ( ( Integer ) prop.getValue() ).intValue() );

        final PropertyMetadata file_property = getPropertyMetadata( cm10, "file.property" );
        file_property.validate( cm10 ); // property value requires validation
        assertNotNull( "file.property exists", file_property );
        assertEquals( "file.property type", "String", file_property.getType() );
        assertEquals( "file.property value", "Property from File", file_property.getValue() );

        // service setup
        final ServiceMetadata sm = cm10.getServiceMetadata();
        sm.validate( cm10 ); // service metadata requires validation to set scope properly
        assertNotNull( "service", sm );
        assertEquals( "servicefactory", ServiceMetadata.Scope.bundle, sm.getScope() );
        assertEquals( "1 interface", 1, sm.getProvides().length );
        assertEquals( "service interface", "components.all.service", sm.getProvides()[0] );

        // references - basic
        final ReferenceMetadata rm = getReference( cm10, "ref.name" );
        assertNotNull( "refeference ref.name", rm );
        assertEquals( "ref.name name", "ref.name", rm.getName() );
        assertEquals( "ref.name interface", "ref.service", rm.getInterface() );
        assertEquals( "ref.name cardinality", "0..n", rm.getCardinality() );
        assertEquals( "ref.name policy", "dynamic", rm.getPolicy() );
        assertEquals( "ref.name target", "ref.target", rm.getTarget() );
        assertEquals( "ref.name target prop name", "ref.name.target", rm.getTargetPropertyName() );
        assertEquals( "ref.name bind method", "ref_bind", rm.getBind() );
        assertEquals( "ref.name undbind method", "ref_unbind", rm.getUnbind() );

        // references - cardinality side properties (isOptional, isMultiple)
        final ReferenceMetadata rm01 = getReference( cm10, "ref.01" );
        assertNotNull( "refeference ref.01", rm01 );
        assertEquals( "ref.01 cardinality", "0..1", rm01.getCardinality() );
        final ReferenceMetadata rm11 = getReference( cm10, "ref.11" );
        assertNotNull( "refeference ref.11", rm11 );
        assertEquals( "ref.11 cardinality", "1..1", rm11.getCardinality() );
        final ReferenceMetadata rm0n = getReference( cm10, "ref.0n" );
        assertNotNull( "refeference ref.0n", rm0n );
        assertEquals( "ref.0n cardinality", "0..n", rm0n.getCardinality() );
        final ReferenceMetadata rm1n = getReference( cm10, "ref.1n" );
        assertNotNull( "refeference ref.1n", rm1n );
        assertEquals( "ref.1n cardinality", "1..n", rm1n.getCardinality() );
    }


    public void test_duplicate_implementation_class_10() throws Exception
    {
        final List metadataList10 = readMetadata( "/components_duplicate_implementation_10.xml" );
        assertEquals( "Component Descriptors", 1, metadataList10.size() );
        final ComponentMetadata cm10 = ( ComponentMetadata ) metadataList10.get( 0 );
        try
        {
            cm10.validate( logger );
            fail( "Expect validation failure for duplicate implementation element" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_duplicate_implementation_class_11() throws Exception
    {
        final List metadataList11 = readMetadata( "/components_duplicate_implementation_11.xml" );
        assertEquals( "Component Descriptors", 1, metadataList11.size() );
        final ComponentMetadata cm11 = ( ComponentMetadata ) metadataList11.get( 0 );
        try
        {
            cm11.validate( logger );
            fail( "Expect validation failure for duplicate implementation element" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_duplicate_service_10() throws Exception
    {
        final List metadataList10 = readMetadata( "/components_duplicate_service_10.xml" );
        assertEquals( "Component Descriptors", 1, metadataList10.size() );
        final ComponentMetadata cm10 = ( ComponentMetadata ) metadataList10.get( 0 );
        try
        {
            cm10.validate( logger );
            fail( "Expect validation failure for duplicate service element" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    public void test_duplicate_service_11() throws Exception
    {
        final List metadataList11 = readMetadata( "/components_duplicate_service_11.xml" );
        assertEquals( "Component Descriptors", 1, metadataList11.size() );
        final ComponentMetadata cm11 = ( ComponentMetadata ) metadataList11.get( 0 );
        try
        {
            cm11.validate( logger );
            fail( "Expect validation failure for duplicate service element" );
        }
        catch ( ComponentException ce )
        {
            // expected
        }
    }


    //---------- helper

    private List readMetadata( final Reader reader ) throws IOException, ComponentException, XmlPullParserException,
        Exception
    {

        try
        {
            final KXml2SAXParser parser = new KXml2SAXParser( reader );

            XmlHandler handler = new XmlHandler( new MockBundle(), logger, false, false );
            parser.parseXML( handler );

            return handler.getComponentMetadataList();
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch ( IOException ignore )
            {
            }
        }
    }


    private List readMetadata( String filename ) throws IOException, ComponentException, XmlPullParserException,
        Exception
    {
        BufferedReader in = new BufferedReader( new InputStreamReader( getClass().getResourceAsStream( filename ),
            "UTF-8" ) );
        return readMetadata( in );
    }


    private List readMetadataFromString( final String source ) throws IOException, ComponentException,
        XmlPullParserException, Exception
    {
        return readMetadata( new StringReader( source ) );
    }


    private ReferenceMetadata getReference( final ComponentMetadata cm, final String name )
    {
        List rmlist = cm.getDependencies();
        for ( Iterator rmi = rmlist.iterator(); rmi.hasNext(); )
        {
            ReferenceMetadata rm = ( ReferenceMetadata ) rmi.next();
            if ( name.equals( rm.getName() ) )
            {
                return rm;
            }
        }

        // none found
        return null;
    }


    private PropertyMetadata getPropertyMetadata( final ComponentMetadata cm, final String name )
    {
        List pmlist = cm.getPropertyMetaData();
        for ( Iterator pmi = pmlist.iterator(); pmi.hasNext(); )
        {
            PropertyMetadata pm = ( PropertyMetadata ) pmi.next();
            if ( name.equals( pm.getName() ) )
            {
                return pm;
            }
        }

        // none found
        return null;
    }

    public void test_properties_11() throws Exception
    {
        final List metadataList11 = readMetadata( "/components_properties_11.xml" );
        assertEquals( "Component Descriptors", 1, metadataList11.size() );
        final ComponentMetadata cm11 = ( ComponentMetadata ) metadataList11.get( 0 );

        // dont validate this, we test the raw reading

        // ds namespace
        assertEquals( "DS Version 1.1", DSVersion.DS11, cm11.getDSVersion() );
        assertTrue( "DS Version 1.1", cm11.getDSVersion().isDS11() );

        assertEquals( "component name", "DummyClass", cm11.getName() );
        assertEquals( "component name", "DummyClass", cm11.getImplementationClassName() );

        // property setting
        final PropertyMetadata prop = getPropertyMetadata( cm11, "char_array_property" );
        assertNotNull( "prop exists", prop );
        assertEquals( "prop type", "Character", prop.getType() );
        prop.validate( cm11 ); // property value conversion requires validation
        Object value = prop.getValue();
        assertTrue( "prop array", value instanceof char[] );
        char[] chars = ( char[] ) value;
        assertEquals( "prop number of values", 2, chars.length );
        assertEquals( "prop value 0", 'A', chars[0] );
        assertEquals( "prop value 1", 'B', chars[1] );
    }

}
