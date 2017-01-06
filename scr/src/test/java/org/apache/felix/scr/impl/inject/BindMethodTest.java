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
package org.apache.felix.scr.impl.inject;


import junit.framework.TestCase;

import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.MockBundle;
import org.apache.felix.scr.impl.helper.ComponentMethods;
import org.apache.felix.scr.impl.inject.BindMethod;
import org.apache.felix.scr.impl.inject.BindParameters;
import org.apache.felix.scr.impl.manager.ComponentContainer;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.manager.RefPair;
import org.apache.felix.scr.impl.manager.SingleComponentManager;
import org.apache.felix.scr.impl.manager.SingleRefPair;
import org.apache.felix.scr.impl.manager.components.FakeService;
import org.apache.felix.scr.impl.manager.components.T1;
import org.apache.felix.scr.impl.manager.components.T1MapSR;
import org.apache.felix.scr.impl.manager.components.T1a;
import org.apache.felix.scr.impl.manager.components.T3;
import org.apache.felix.scr.impl.manager.components2.T2;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.DSVersion;
import org.easymock.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;


public class BindMethodTest extends TestCase
{

    private ServiceReference m_serviceReference;
    private FakeService m_serviceInstance;
    private BundleContext m_context;


    @Override
    public void setUp()
    {
        m_serviceReference = EasyMock.createNiceMock( ServiceReference.class );
        m_serviceInstance = EasyMock.createNiceMock( FakeService.class );
        m_context = EasyMock.createNiceMock( BundleContext.class );

        EasyMock.expect( m_context.getService( m_serviceReference ) ).andReturn( m_serviceInstance )
                .anyTimes();

        EasyMock.expect( m_serviceReference.getPropertyKeys() ).andReturn( new String[]
            { Constants.SERVICE_ID } ).anyTimes();
        EasyMock.expect( m_serviceReference.getProperty( Constants.SERVICE_ID ) ).andReturn( "Fake Service" )
            .anyTimes();
        EasyMock.replay( new Object[]
            { m_serviceReference, m_context } );
    }


    public void test_Unexistent()
    {
        testMethod( "unexistent", new T1(), DSVersion.DS10, null );
        testMethod( "unexistent", new T1(), DSVersion.DS11, null );
        testMethod( "unexistent", new T2(), DSVersion.DS10, null );
        testMethod( "unexistent", new T2(), DSVersion.DS11, null );
        testMethod( "unexistent", new T3(), DSVersion.DS10, null );
        testMethod( "unexistent", new T3(), DSVersion.DS11, null );
    }


    public void test_privateT1()
    {
        testMethod( "privateT1", new T1(), DSVersion.DS10, null );
        testMethod( "privateT1", new T1(), DSVersion.DS11, null );
        testMethod( "privateT1", new T2(), DSVersion.DS10, null );
        testMethod( "privateT1", new T2(), DSVersion.DS11, null );
        testMethod( "privateT1", new T3(), DSVersion.DS10, null );
        testMethod( "privateT1", new T3(), DSVersion.DS11, null );
    }


    public void test_privateT1SR()
    {
        testMethod( "privateT1SR", new T1(), DSVersion.DS10, null );
        testMethod( "privateT1SR", new T1(), DSVersion.DS11, "privateT1SR" );
        testMethod( "privateT1SR", new T2(), DSVersion.DS10, null );
        testMethod( "privateT1SR", new T2(), DSVersion.DS11, null );
    }


    public void test_privateT1SI()
    {
        testMethod( "privateT1SI", new T1(), DSVersion.DS10, null );
        testMethod( "privateT1SI", new T1(), DSVersion.DS11, "privateT1SI" );
        testMethod( "privateT1SI", new T2(), DSVersion.DS10, null );
        testMethod( "privateT1SI", new T2(), DSVersion.DS11, null );
    }


    public void test_privateT1SIMap()
    {
        testMethod( "privateT1SIMap", new T1(), DSVersion.DS10, null );
        testMethod( "privateT1SIMap", new T1(), DSVersion.DS11, "privateT1SIMap" );
        testMethod( "privateT1SIMap", new T2(), DSVersion.DS10, null );
        testMethod( "privateT1SIMap", new T2(), DSVersion.DS11, null );
    }


    public void test_privateT1SSI()
    {
        testMethod( "privateT1SSI", new T1(), DSVersion.DS10, null );
        testMethod( "privateT1SSI", new T1(), DSVersion.DS11, "privateT1SSI" );
        testMethod( "privateT1SSI", new T2(), DSVersion.DS10, null );
        testMethod( "privateT1SSI", new T2(), DSVersion.DS11, null );
    }


    public void test_privateT1SSIMap()
    {
        testMethod( "privateT1SSIMap", new T1(), DSVersion.DS10, null );
        testMethod( "privateT1SSIMap", new T1(), DSVersion.DS11, "privateT1SSIMap" );
        testMethod( "privateT1SSIMap", new T2(), DSVersion.DS10, null );
        testMethod( "privateT1SSIMap", new T2(), DSVersion.DS11, null );
    }


    public void test_privateT2()
    {
        testMethod( "privateT2", new T1(), DSVersion.DS10, null );
        testMethod( "privateT2", new T1(), DSVersion.DS11, null );
        testMethod( "privateT2", new T2(), DSVersion.DS10, null );
        testMethod( "privateT2", new T2(), DSVersion.DS11, null );
    }


    public void test_privateT2SR()
    {
        testMethod( "privateT2SR", new T1(), DSVersion.DS10, null );
        testMethod( "privateT2SR", new T1(), DSVersion.DS11, null );
        testMethod( "privateT2SR", new T2(), DSVersion.DS10, null );
        testMethod( "privateT2SR", new T2(), DSVersion.DS11, "privateT2SR" );
    }


    public void test_privateT2SI()
    {
        testMethod( "privateT2SI", new T1(), DSVersion.DS10, null );
        testMethod( "privateT2SI", new T1(), DSVersion.DS11, null );
        testMethod( "privateT2SI", new T2(), DSVersion.DS10, null );
        testMethod( "privateT2SI", new T2(), DSVersion.DS11, "privateT2SI" );
    }


    public void test_privateT2SIMap()
    {
        testMethod( "privateT2SIMap", new T1(), DSVersion.DS10, null );
        testMethod( "privateT2SIMap", new T1(), DSVersion.DS11, null );
        testMethod( "privateT2SIMap", new T2(), DSVersion.DS10, null );
        testMethod( "privateT2SIMap", new T2(), DSVersion.DS11, "privateT2SIMap" );
    }


    public void test_privateT2SSI()
    {
        testMethod( "privateT2SSI", new T1(), DSVersion.DS10, null );
        testMethod( "privateT2SSI", new T1(), DSVersion.DS11, null );
        testMethod( "privateT2SSI", new T2(), DSVersion.DS10, null );
        testMethod( "privateT2SSI", new T2(), DSVersion.DS11, "privateT2SSI" );
    }


    public void test_privateT2SSIMap()
    {
        testMethod( "privateT2SSIMap", new T1(), DSVersion.DS10, null );
        testMethod( "privateT2SSIMap", new T1(), DSVersion.DS11, null );
        testMethod( "privateT2SSIMap", new T2(), DSVersion.DS10, null );
        testMethod( "privateT2SSIMap", new T2(), DSVersion.DS11, "privateT2SSIMap" );
    }


    public void test_packageT1()
    {
        testMethod( "packageT1", new T1(), DSVersion.DS10, null );
        testMethod( "packageT1", new T1(), DSVersion.DS11, null );
        testMethod( "packageT1", new T2(), DSVersion.DS10, null );
        testMethod( "packageT1", new T2(), DSVersion.DS11, null );
        testMethod( "packageT1", new T3(), DSVersion.DS10, null );
        testMethod( "packageT1", new T3(), DSVersion.DS11, null );
        testMethod( "packageT1", new T1a(), DSVersion.DS10, null );
        testMethod( "packageT1", new T1a(), DSVersion.DS11, null );
    }


    public void test_packageT1SR()
    {
        testMethod( "packageT1SR", new T1(), DSVersion.DS10, null );
        testMethod( "packageT1SR", new T1(), DSVersion.DS11, "packageT1SR" );
        testMethod( "packageT1SR", new T2(), DSVersion.DS10, null );
        testMethod( "packageT1SR", new T2(), DSVersion.DS11, null );
        testMethod( "packageT1SR", new T3(), DSVersion.DS10, null );
        testMethod( "packageT1SR", new T3(), DSVersion.DS11, null );
        testMethod( "packageT1SR", new T1a(), DSVersion.DS10, null );
        testMethod( "packageT1SR", new T1a(), DSVersion.DS11, "packageT1SR" );
    }


    public void test_packageT1SI()
    {
        testMethod( "packageT1SI", new T1(), DSVersion.DS10, null );
        testMethod( "packageT1SI", new T1(), DSVersion.DS11, "packageT1SI" );
        testMethod( "packageT1SI", new T2(), DSVersion.DS10, null );
        testMethod( "packageT1SI", new T2(), DSVersion.DS11, null );
        testMethod( "packageT1SI", new T3(), DSVersion.DS10, null );
        testMethod( "packageT1SI", new T3(), DSVersion.DS11, null );
        testMethod( "packageT1SI", new T1a(), DSVersion.DS10, null );
        testMethod( "packageT1SI", new T1a(), DSVersion.DS11, "packageT1SI" );
    }


    public void test_packageT1SIMap()
    {
        testMethod( "packageT1SIMap", new T1(), DSVersion.DS10, null );
        testMethod( "packageT1SIMap", new T1(), DSVersion.DS11, "packageT1SIMap" );
        testMethod( "packageT1SIMap", new T2(), DSVersion.DS10, null );
        testMethod( "packageT1SIMap", new T2(), DSVersion.DS11, null );
        testMethod( "packageT1SIMap", new T3(), DSVersion.DS10, null );
        testMethod( "packageT1SIMap", new T3(), DSVersion.DS11, null );
        testMethod( "packageT1SIMap", new T1a(), DSVersion.DS10, null );
        testMethod( "packageT1SIMap", new T1a(), DSVersion.DS11, "packageT1SIMap" );
    }


    public void test_packageT1SSI()
    {
        testMethod( "packageT1SSI", new T1(), DSVersion.DS10, null );
        testMethod( "packageT1SSI", new T1(), DSVersion.DS11, "packageT1SSI" );
        testMethod( "packageT1SSI", new T2(), DSVersion.DS10, null );
        testMethod( "packageT1SSI", new T2(), DSVersion.DS11, null );
        testMethod( "packageT1SSI", new T3(), DSVersion.DS10, null );
        testMethod( "packageT1SSI", new T3(), DSVersion.DS11, null );
        testMethod( "packageT1SSI", new T1a(), DSVersion.DS10, null );
        testMethod( "packageT1SSI", new T1a(), DSVersion.DS11, "packageT1SSI" );
    }


    public void test_packageT1SSIMap()
    {
        testMethod( "packageT1SSIMap", new T1(), DSVersion.DS10, null );
        testMethod( "packageT1SSIMap", new T1(), DSVersion.DS11, "packageT1SSIMap" );
        testMethod( "packageT1SSIMap", new T2(), DSVersion.DS10, null );
        testMethod( "packageT1SSIMap", new T2(), DSVersion.DS11, null );
        testMethod( "packageT1SSIMap", new T3(), DSVersion.DS10, null );
        testMethod( "packageT1SSIMap", new T3(), DSVersion.DS11, null );
        testMethod( "packageT1SSIMap", new T1a(), DSVersion.DS10, null );
        testMethod( "packageT1SSIMap", new T1a(), DSVersion.DS11, "packageT1SSIMap" );
    }


    public void test_packageT2()
    {
        testMethod( "packageT2", new T1(), DSVersion.DS10, null );
        testMethod( "packageT2", new T1(), DSVersion.DS11, null );
        testMethod( "packageT2", new T2(), DSVersion.DS10, null );
        testMethod( "packageT2", new T2(), DSVersion.DS11, null );
    }


    public void test_packageT2SR()
    {
        testMethod( "packageT2SR", new T1(), DSVersion.DS10, null );
        testMethod( "packageT2SR", new T1(), DSVersion.DS11, null );
        testMethod( "packageT2SR", new T2(), DSVersion.DS10, null );
        testMethod( "packageT2SR", new T2(), DSVersion.DS11, "packageT2SR" );
    }


    public void test_packageT2SI()
    {
        testMethod( "packageT2SI", new T1(), DSVersion.DS10, null );
        testMethod( "packageT2SI", new T1(), DSVersion.DS11, null );
        testMethod( "packageT2SI", new T2(), DSVersion.DS10, null );
        testMethod( "packageT2SI", new T2(), DSVersion.DS11, "packageT2SI" );
    }


    public void test_packageT2SIMap()
    {
        testMethod( "packageT2SIMap", new T1(), DSVersion.DS10, null );
        testMethod( "packageT2SIMap", new T1(), DSVersion.DS11, null );
        testMethod( "packageT2SIMap", new T2(), DSVersion.DS10, null );
        testMethod( "packageT2SIMap", new T2(), DSVersion.DS11, "packageT2SIMap" );
    }


    public void test_packageT2SSI()
    {
        testMethod( "packageT2SSI", new T1(), DSVersion.DS10, null );
        testMethod( "packageT2SSI", new T1(), DSVersion.DS11, null );
        testMethod( "packageT2SSI", new T2(), DSVersion.DS10, null );
        testMethod( "packageT2SSI", new T2(), DSVersion.DS11, "packageT2SSI" );
    }


    public void test_packageT2SSIMap()
    {
        testMethod( "packageT2SSIMap", new T1(), DSVersion.DS10, null );
        testMethod( "packageT2SSIMap", new T1(), DSVersion.DS11, null );
        testMethod( "packageT2SSIMap", new T2(), DSVersion.DS10, null );
        testMethod( "packageT2SSIMap", new T2(), DSVersion.DS11, "packageT2SSIMap" );
    }


    public void test_protectedT1()
    {
        testMethod( "protectedT1", new T1(), DSVersion.DS10, null );
        testMethod( "protectedT1", new T1(), DSVersion.DS11, null );
        testMethod( "protectedT1", new T2(), DSVersion.DS10, null );
        testMethod( "protectedT1", new T2(), DSVersion.DS11, null );
    }


    public void test_protectedT1SR()
    {
        testMethod( "protectedT1SR", new T1(), DSVersion.DS10, "protectedT1SR" );
        testMethod( "protectedT1SR", new T1(), DSVersion.DS11, "protectedT1SR" );
        testMethod( "protectedT1SR", new T2(), DSVersion.DS10, "protectedT1SR" );
        testMethod( "protectedT1SR", new T2(), DSVersion.DS11, "protectedT1SR" );
    }


    public void test_protectedT1SI()
    {
        testMethod( "protectedT1SI", new T1(), DSVersion.DS10, "protectedT1SI" );
        testMethod( "protectedT1SI", new T1(), DSVersion.DS11, "protectedT1SI" );
        testMethod( "protectedT1SI", new T2(), DSVersion.DS10, "protectedT1SI" );
        testMethod( "protectedT1SI", new T2(), DSVersion.DS11, "protectedT1SI" );
    }


    public void test_protectedT1SSI()
    {
        testMethod( "protectedT1SSI", new T1(), DSVersion.DS10, "protectedT1SSI" );
        testMethod( "protectedT1SSI", new T1(), DSVersion.DS11, "protectedT1SSI" );
        testMethod( "protectedT1SSI", new T2(), DSVersion.DS10, "protectedT1SSI" );
        testMethod( "protectedT1SSI", new T2(), DSVersion.DS11, "protectedT1SSI" );
    }


    public void test_publicT1()
    {
        testMethod( "publicT1", new T1(), DSVersion.DS10, null );
        testMethod( "publicT1", new T1(), DSVersion.DS11, null );
        testMethod( "publicT1", new T2(), DSVersion.DS10, null );
        testMethod( "publicT1", new T2(), DSVersion.DS11, null );
    }


    public void test_publicT1SR()
    {
        testMethod( "publicT1SR", new T1(), DSVersion.DS10, "publicT1SR" );
        testMethod( "publicT1SR", new T1(), DSVersion.DS11, "publicT1SR" );
        testMethod( "publicT1SR", new T2(), DSVersion.DS10, "publicT1SR" );
        testMethod( "publicT1SR", new T2(), DSVersion.DS11, "publicT1SR" );
    }


    public void test_publicT1SI()
    {
        testMethod( "publicT1SI", new T1(), DSVersion.DS10, "publicT1SI" );
        testMethod( "publicT1SI", new T1(), DSVersion.DS11, "publicT1SI" );
        testMethod( "publicT1SI", new T2(), DSVersion.DS10, "publicT1SI" );
        testMethod( "publicT1SI", new T2(), DSVersion.DS11, "publicT1SI" );
    }


    public void test_publicT1SIMap()
    {
        testMethod( "publicT1SIMap", new T1(), DSVersion.DS10, null );
        testMethod( "publicT1SIMap", new T1(), DSVersion.DS11, "publicT1SIMap" );
        testMethod( "publicT1SIMap", new T2(), DSVersion.DS10, null );
        testMethod( "publicT1SIMap", new T2(), DSVersion.DS11, "publicT1SIMap" );
    }


    public void test_publicT1SSI()
    {
        testMethod( "publicT1SSI", new T1(), DSVersion.DS10, "publicT1SSI" );
        testMethod( "publicT1SSI", new T1(), DSVersion.DS11, "publicT1SSI" );
        testMethod( "publicT1SSI", new T2(), DSVersion.DS10, "publicT1SSI" );
        testMethod( "publicT1SSI", new T2(), DSVersion.DS11, "publicT1SSI" );
    }


    public void test_publicT1SSIMap()
    {
        testMethod( "publicT1SSIMap", new T1(), DSVersion.DS10, null );
        testMethod( "publicT1SSIMap", new T1(), DSVersion.DS11, "publicT1SSIMap" );
        testMethod( "publicT1SSIMap", new T2(), DSVersion.DS10, null );
        testMethod( "publicT1SSIMap", new T2(), DSVersion.DS11, "publicT1SSIMap" );
    }


    public void test_suitable()
    {
        // T1 should use its own public implementation
        testMethod( "suitable", new T1(), DSVersion.DS10, "suitableT1" );
        testMethod( "suitable", new T1(), DSVersion.DS11, "suitableT1" );

        // T2's private implementation is only visible for DS 1.1
        testMethod( "suitable", new T2(), DSVersion.DS10, null );
        testMethod( "suitable", new T2(), DSVersion.DS11, "suitableT2" );

        // T3 extends T2 and cannot see T2's private method
        testMethod( "suitable", new T3(), DSVersion.DS10, null );
        testMethod( "suitable", new T3(), DSVersion.DS11, null );

        // T1a extends T1 and uses T1's public method
        testMethod( "suitable", new T1a(), DSVersion.DS10, "suitableT1" );
        testMethod( "suitable", new T1a(), DSVersion.DS11, "suitableT1" );
    }

    public void test_13()
    {
        //single map param
        testMethod( "packageT1Map", new T1(), DSVersion.DS12, null);
        testMethod( "packageT1Map", new T1(), DSVersion.DS13, "packageT1Map");

        //map, sr
        testMethod( "packageT1MapSR", new T1MapSR(), DSVersion.DS12, null);
        testMethod( "packageT1MapSR", new T1MapSR(), DSVersion.DS13, "packageT1MapSR");
    }


    private void testMethod( final String methodName, final T1 component, final DSVersion dsVersion,
        final String expectCallPerformed )
    {
        ComponentContainer container = newContainer();
        SingleComponentManager icm = new SingleComponentManager( container, new ComponentMethodsImpl() );
        BindMethod bm = new BindMethod( methodName, component.getClass(),
                FakeService.class.getName(), dsVersion, false );
        RefPair refPair = new SingleRefPair( m_serviceReference );
        ComponentContextImpl<T1> cc = new ComponentContextImpl(icm, new MockBundle(), null);
        assertTrue( bm.getServiceObject( cc, refPair, m_context, icm ) );
        BindParameters bp = new BindParameters(cc, refPair);
        bm.invoke( component, bp, null, icm );
        assertEquals( expectCallPerformed, component.callPerformed );
    }

    private ComponentContainer newContainer()
    {
        final ComponentMetadata metadata = newMetadata();
        ComponentContainer container = new ComponentContainer() {

            public BundleComponentActivator getActivator()
            {
                return null;
            }

            public ComponentMetadata getComponentMetadata()
            {
                return metadata;
            }

            public void disposed(SingleComponentManager component)
            {
            }

            public boolean isEnabled()
            {
                return false;
            }

        };
        return container;
    }

	private ComponentMetadata newMetadata() {
		ComponentMetadata metadata = new ComponentMetadata( DSVersion.DS11 );
        metadata.setName("foo");
        metadata.setImplementationClassName(Object.class.getName());
        metadata.validate(null);
		return metadata;
	}

}
