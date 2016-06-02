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


import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.manager.ComponentContainer;
import org.apache.felix.scr.impl.helper.ComponentMethods;
import org.apache.felix.scr.impl.manager.SingleComponentManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.DSVersion;
import org.apache.felix.scr.impl.metadata.instances.AcceptMethod;
import org.apache.felix.scr.impl.metadata.instances.BaseObject;
import org.apache.felix.scr.impl.metadata.instances.Level1Object;
import org.apache.felix.scr.impl.metadata.instances.Level3Object;
import org.apache.felix.scr.impl.metadata.instances2.Level2Object;
import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;


public class ActivateMethodTest extends TestCase
{

    private static final Class ACCEPT_METHOD_CLASS = AcceptMethod.class;

    private ComponentContext m_ctx;

    BaseObject base = new BaseObject();

    Level1Object level1 = new Level1Object();

    Level2Object level2 = new Level2Object();

    Level3Object level3 = new Level3Object();

    protected void setUp() throws Exception
    {
        super.setUp();
        Bundle bundle = ( Bundle ) EasyMock.createNiceMock( Bundle.class );
        BundleContext context = ( BundleContext ) EasyMock.createNiceMock( BundleContext.class );
        EasyMock.expect( context.getBundle() ).andReturn( bundle ).anyTimes();

        m_ctx = (ComponentContext) EasyMock.createNiceMock(ComponentContext.class);
        EasyMock.expect( m_ctx.getProperties() ).andReturn( new Hashtable() ).anyTimes();
        EasyMock.expect( m_ctx.getBundleContext() ).andReturn( context ).anyTimes();
        EasyMock.replay( new Object[]
            { m_ctx, context } );

    }


    public void test_private_no_arg() throws Exception
    {
        checkMethod( base, "activate_no_arg" );

        // activate_no_arg is private to BaseObject and must not be
        // accessible from extensions
        ensureMethodNotFoundMethod( level1, "activate_no_arg" );
        ensureMethodNotFoundMethod( level2, "activate_no_arg" );
        ensureMethodNotFoundMethod( level3, "activate_no_arg" );
    }


    public void test_protected_activate_comp() throws Exception
    {
        // activate_comp is protected in BaseObject and must be accessible
        // in all instances
        checkMethod( base, "activate_comp" );
        checkMethod( level1, "activate_comp" );
        checkMethod( level2, "activate_comp" );
        checkMethod( level3, "activate_comp" );
    }


    public void test_private_activate_level1_bundle() throws Exception
    {
        // activate_level1_bundle is private in Level1Object and must be
        // accessible in Level1Object only
        ensureMethodNotFoundMethod( base, "activate_level1_bundle" );
        checkMethod( level1, "activate_level1_bundle" );
        ensureMethodNotFoundMethod( level2, "activate_level1_bundle" );
        ensureMethodNotFoundMethod( level3, "activate_level1_bundle" );
    }


    public void test_protected_activate_level1_map() throws Exception
    {
        // activate_level1_map is protected in Level1Object and must be
        // accessible in Level1Object and extensions but not in BaseObject
        ensureMethodNotFoundMethod( base, "activate_level1_map" );
        checkMethod( level1, "activate_level1_map" );
        checkMethod( level2, "activate_level1_map" );
        checkMethod( level3, "activate_level1_map" );
    }


    public void test_private_activate_comp_map() throws Exception
    {
        // private_activate_comp_map is private in Level2Object and must be
        // accessible in Level2Object only
        ensureMethodNotFoundMethod( base, "activate_comp_map" );
        ensureMethodNotFoundMethod( level1, "activate_comp_map" );
        checkMethod( level2, "activate_comp_map" );
        checkMethod( level3, "activate_comp_map" );
    }


    public void test_public_activate_collision() throws Exception
    {
        // activate_collision is private in Level2Object and must be
        // accessible in Level2Object only.
        // also the method is available taking no arguments and a single
        // map argument which takes precedence and which we expect
        ensureMethodNotFoundMethod( base, "activate_collision" );
        ensureMethodNotFoundMethod( level1, "activate_collision" );
        checkMethod( level2, "activate_collision" );
        checkMethod( level3, "activate_collision" );
    }


    public void test_package_activate_comp_bundle() throws Exception
    {
        // activate_comp_bundle is package private and thus visible in
        // base and level1 but not in level 2 (different package) and
        // level 3 (inheritance through different package)

        checkMethod( base, "activate_comp_bundle" );
        checkMethod( level1, "activate_comp_bundle" );
        ensureMethodNotFoundMethod( level2, "activate_comp_bundle" );
        ensureMethodNotFoundMethod( level3, "activate_comp_bundle" );
    }


    public void test_getPackage() throws Exception
    {
        Class dpc = getClass().getClassLoader().loadClass( "DefaultPackageClass" );
        assertEquals( "", BaseMethod.getPackageName( dpc ) );

        assertEquals( "org.apache.felix.scr.impl.metadata.instances", BaseMethod.getPackageName( base.getClass() ) );
    }


    public void test_accept() throws Exception
    {
        // public visible unless returning non-void
        assertMethod( true, "public_void", false, false );
        assertMethod( false, "public_string", false, false );

        // protected visible unless returning non-void
        assertMethod( true, "protected_void", false, false );
        assertMethod( false, "protected_string", false, false );

        // private not visible
        assertMethod( false, "private_void", false, false );
        assertMethod( false, "private_string", false, false );

        // private visible unless returning non-void
        assertMethod( true, "private_void", true, false );
        assertMethod( false, "private_string", true, false );
        assertMethod( true, "private_void", true, true );
        assertMethod( false, "private_string", true, true );

        // private not visible, accept package is ignored
        assertMethod( false, "private_void", false, true );
        assertMethod( false, "private_string", false, true );

        // package not visible
        assertMethod( false, "package_void", false, false );
        assertMethod( false, "package_string", false, false );

        // package visible unless returning non-void
        assertMethod( true, "package_void", false, true );
        assertMethod( false, "package_string", false, true );
        assertMethod( true, "package_void", true, true );
        assertMethod( false, "package_string", true, true );

        // package not visible, accept private is ignored
        assertMethod( false, "package_void", true, false );
        assertMethod( false, "package_string", true, false );
    }


    public void test_suitable_method_selection() throws Exception
    {
        // this would be the protected BaseObject.activate_suitable
        checkMethod( base, "activate_suitable" );
        checkMethod( level1, "activate_suitable" );

        // this would be the private Level2Object.activate_suitable
        checkMethod( level2, "activate_suitable" );

        // this must fail to find a method, since Level2Object's activate_suitable
        // is private and terminates the search for Level3Object
        ensureMethodNotFoundMethod( level3, "activate_suitable" );
    }

    public void test_unsuitable_method_selection() throws Exception
    {
        //check that finding an unsuitable method does not prevent finding
        // a lower precedence suitable method.

        checkMethod( level2, "activate_comp_unsuitable" );

        checkMethod( level3, "activate_comp_unsuitable" );
    }

    public void test_precedence() throws Exception
    {
        //All tested methods are only in base.  They differ in arguments and visibility.
        //R4.2 compendium  112.5.8
        //private method, arg ComponentContext
        checkMethod( base, "activate_precedence_1", "activate_precedence_1_comp" );
        //package method, arg BundleContext
        checkMethod( level1, "activate_precedence_1", "activate_precedence_1_bundleContext" );
        //protected method, arg Map
        checkMethod( level2, "activate_precedence_1", "activate_precedence_1_map" );

        //private method, arg Map
        checkMethod( base, "activate_precedence_2", "activate_precedence_2_map" );
        //package method, args ComponentContext and Map
        checkMethod( level1, "activate_precedence_2", "activate_precedence_2_comp_bundleContext" );
        //protected method, no args
        checkMethod( level2, "activate_precedence_2", "activate_precedence_2_empty" );
    }

    //---------- internal

    /**
     * Checks whether a method with the given name can be found for the
     * activate/deactivate method parameter list and whether the method returns
     * its name when called.
     *
     * @param obj
     * @param methodName
     */
    private void checkMethod( BaseObject obj, String methodName )
    {
        checkMethod( obj, methodName, methodName, DSVersion.DS11 );
    }

    /**
     * Checks whether a method with the given name can be found for the
     * activate/deactivate method parameter list and whether the method returns
     * the expected description when called.
     *
     * @param obj
     * @param methodName
     * @param methodDesc
     */
    private void checkMethod( BaseObject obj, String methodName, String methodDesc )
    {
        checkMethod(obj, methodName, methodDesc, DSVersion.DS11);
    }


    /**
     * Checks whether a method with the given name can be found for the
     * activate/deactivate method parameter list and whether the method returns
     * the expected description when called.
     *
     * @param obj
     * @param methodName
     * @param methodDesc
     * @param version DSVersion tested
     */
    private void checkMethod( BaseObject obj, String methodName, String methodDesc, DSVersion version )
    {
        ComponentContainer<?> container = newContainer();
        SingleComponentManager<?> icm = new SingleComponentManager( container, new ComponentMethodsImpl() );
        ActivateMethod am = new ActivateMethod( methodName, methodName != null, obj.getClass(), version, false, false );
        am.invoke( obj, new ActivatorParameter( m_ctx, -1 ), null, icm );
        Method m = am.getMethod();
        assertNotNull( m );
        assertEquals( methodName, m.getName() );
        assertEquals( methodDesc, obj.getCalledMethod() );
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


    /**
     * Ensures no method with the given name accepting any of the
     * activate/deactive method parameters can be found.
     *
     * @param obj
     * @param methodName
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private void ensureMethodNotFoundMethod( BaseObject obj, String methodName )
    {
        ensureMethodNotFoundMethod(obj, methodName, DSVersion.DS11);
    }


    /**
     * Ensures no method with the given name accepting any of the
     * activate/deactive method parameters can be found.
     *
     * @param obj
     * @param methodName
     * @param version DS version tested
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private void ensureMethodNotFoundMethod( BaseObject obj, String methodName, DSVersion version )
    {
        ComponentContainer container = newContainer();
        SingleComponentManager icm = new SingleComponentManager( container, new ComponentMethodsImpl() );
        ActivateMethod am = new ActivateMethod( methodName, methodName != null, obj.getClass(), version, false, false );
        am.invoke( obj, new ActivatorParameter( m_ctx, -1 ), null, icm );
        Method m = am.getMethod();
        assertNull( m );
        assertNull( obj.getCalledMethod() );
    }


    private void assertMethod( boolean expected, String methodName, boolean acceptPrivate, boolean acceptPackage )
        throws NoSuchMethodException
    {
        Method method = ACCEPT_METHOD_CLASS.getDeclaredMethod( methodName, null );
        boolean accepted = BaseMethod.accept( method, acceptPrivate, acceptPackage, false );
        assertEquals( expected, accepted );
    }
    
    private static @interface Ann{}
    private static class Sort
    {
        public void a(Ann ann) {};
        public void a(int c) {};
        public void a(Integer c) {};
        public void a(BundleContext c) {};
        public void a(Map m) {};
        public void a() {};
        public void a(ComponentContext cc) {};
        public void a(ComponentContext cc, BundleContext c) {};
        public void b() {};
        
    }
    public void testMethodSorting() throws Exception
    {
        ActivateMethod am = new ActivateMethod( "a", true, Sort.class, DSVersion.DS11, false, false );
        List<Method> ms = am.getSortedMethods(Sort.class);
        assertEquals(8, ms.size());
        assertEquals(1, ms.get(0).getParameterTypes().length);
        assertEquals(ComponentContext.class, ms.get(0).getParameterTypes()[0]);
        assertEquals(1, ms.get(1).getParameterTypes().length);
        assertEquals(BundleContext.class, ms.get(1).getParameterTypes()[0]);
        assertEquals(1, ms.get(2).getParameterTypes().length);
        assertEquals(Ann.class, ms.get(2).getParameterTypes()[0]);
        assertEquals(1, ms.get(3).getParameterTypes().length);
        assertEquals(Map.class, ms.get(3).getParameterTypes()[0]);
        assertEquals(1, ms.get(4).getParameterTypes().length);
        assertEquals(int.class, ms.get(4).getParameterTypes()[0]);
        assertEquals(1, ms.get(5).getParameterTypes().length);
        assertEquals(Integer.class, ms.get(5).getParameterTypes()[0]);
        assertEquals(2, ms.get(6).getParameterTypes().length);
        assertEquals(0, ms.get(7).getParameterTypes().length);
    }
    
    public void test_13_annos() throws Exception
    {
        checkMethod(base, "activate_13_2_annotations", "activate_13_2_annotations", DSVersion.DS13 );
    }
    
}
