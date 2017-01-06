/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.felix.scr.integration.components.circular.A;
import org.apache.felix.scr.integration.components.circular.B;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @version $Rev$ $Date$
 */
@RunWith(JUnit4TestRunner.class)
public class CircularReferenceTest extends ComponentTestBase
{

    static
    {
        // uncomment to enable debugging of this test class
//        paxRunnerVmOption = DEBUG_VM_OPTION;

        descriptorFile = "/integration_test_circular.xml";
        COMPONENT_PACKAGE = COMPONENT_PACKAGE + ".circular";
   }


    /**
     * A and B have mandatory dependencies on each other.  Neither should start.
     */
    @Test
    public void test_A11_B11()
    {
        String componentNameA = "1.A.1.1.dynamic";
        final ComponentConfigurationDTO componentA = findComponentConfigurationByName( componentNameA, ComponentConfigurationDTO.UNSATISFIED_REFERENCE );

        String componentNameB = "1.B.1.1.dynamic";
        final ComponentConfigurationDTO componentB = findComponentConfigurationByName( componentNameB, ComponentConfigurationDTO.UNSATISFIED_REFERENCE );
    }

    /**
     * A > 1.1 > B > 0..n > A Both should start (A first), but B should not have an A reference.
     * @throws InvalidSyntaxException 
     */
    @Test
    public void test_A11_B0n_immediate_A_first() throws InvalidSyntaxException
    {
        String componentNameA = "2.A.1.1.dynamic";
        final ComponentConfigurationDTO componentA = findComponentConfigurationByName( componentNameA, ComponentConfigurationDTO.SATISFIED | ComponentConfigurationDTO.ACTIVE );
        A a = getServiceFromConfiguration(componentA, A.class);
        assertEquals( 1, a.getBs().size());

        delay();
        String componentNameB = "2.B.0.n.dynamic";
        final ComponentConfigurationDTO componentB = findComponentConfigurationByName( componentNameB, ComponentConfigurationDTO.ACTIVE );
        B b = getServiceFromConfiguration(componentB, B.class);
        assertEquals( 1, b.getAs().size() );
    }
    /**
     * A > 1.1 > B > 0..n > A Both should start (B first), and B should have an A reference.
     * @throws InvalidSyntaxException 
     */
    @Test
    public void test_A11_B0n_immediate_B_first() throws InvalidSyntaxException
    {
        String componentNameA = "3.A.1.1.dynamic";
        final ComponentConfigurationDTO componentA = findComponentConfigurationByName( componentNameA, ComponentConfigurationDTO.ACTIVE );
        A a = getServiceFromConfiguration(componentA, A.class);
        assertEquals( 1, a.getBs().size());

        delay();
        String componentNameB = "3.B.0.n.dynamic";
        final ComponentConfigurationDTO componentB = findComponentConfigurationByName( componentNameB, ComponentConfigurationDTO.ACTIVE );
        B b = getServiceFromConfiguration(componentB, B.class);
        assertEquals( 1, b.getAs().size() );
    }
    /**
     * A > 1.1 > B > 0..n > A Both should start, but B should not have an A reference.
     */
    @Test
    public void test_A11_B0n_delayed_A_first() throws InvalidSyntaxException
    {
        String componentNameA = "4.A.1.1.dynamic";
        final ComponentConfigurationDTO componentA = findComponentConfigurationByName( componentNameA, ComponentConfigurationDTO.SATISFIED );

        String componentNameB = "4.B.0.n.dynamic";
        final ComponentConfigurationDTO componentB = findComponentConfigurationByName( componentNameB, ComponentConfigurationDTO.SATISFIED );

        delay();

        A a = getServiceFromConfiguration(componentA, A.class);
        assertEquals( 1, a.getBs().size());
        delay(); //async binding of a to b after circular ref detected
        B b = getServiceFromConfiguration(componentB, B.class);
        assertEquals( 1, b.getAs().size() );
    }
    /**
     * A > 1.1 > B > 0..n > A Both should start, but B should not have an A reference.
     */
    @Test
    public void test_A11_B0n_delayed_B_first() throws Exception
    {
        String componentNameA = "4.A.1.1.dynamic";
        ComponentConfigurationDTO componentA = findComponentConfigurationByName( componentNameA, ComponentConfigurationDTO.SATISFIED );

        String componentNameB = "4.B.0.n.dynamic";
        final ComponentConfigurationDTO componentB = findComponentConfigurationByName( componentNameB, ComponentConfigurationDTO.SATISFIED);

        ServiceReference[] serviceReferencesB = bundleContext.getServiceReferences( B.class.getName(), "(service.pid=" + componentNameB + ")" );
        TestCase.assertEquals( 1, serviceReferencesB.length );
        ServiceReference serviceReferenceB = serviceReferencesB[0];
        Object serviceB = bundleContext.getService( serviceReferenceB );
        assertNotNull( serviceB );

        ServiceReference[] serviceReferencesA = bundleContext.getServiceReferences( A.class.getName(), "(service.pid=" + componentNameA + ")" );
        TestCase.assertEquals( 1, serviceReferencesA.length );
        ServiceReference serviceReferenceA = serviceReferencesA[0];
        Object serviceA = bundleContext.getService( serviceReferenceA );
        assertNotNull( serviceA );

        delay();
        A a = getServiceFromConfiguration(componentA, A.class);
        assertEquals( 1, a.getBs().size());
        B b = getServiceFromConfiguration(componentB, B.class);
        assertEquals( 1, b.getAs().size() );


        //disabling (removing the A service registration) and re-enabling will
        //result in a service event to B, so B will bind A.
        disableAndCheck(componentA);
        delay();
        enableAndCheck(componentA.description);
        delay();

        //new component.id, refetch configuration.
        componentA = findComponentConfigurationByName( componentNameA, ComponentConfigurationDTO.ACTIVE );
        a = getServiceFromConfiguration(componentA, A.class);
        assertEquals( 1, a.getBs().size());
        b = getServiceFromConfiguration(componentB, B.class);
        assertEquals( 1, b.getAs().size() );

    }
    /**
     * A > 1.1 > B > 0..1 > A Both should start (A first), but B should not have an A reference.
     * @throws InvalidSyntaxException 
     */
    @Test
    public void test_A11_B01_immediate_A_first() throws InvalidSyntaxException
    {
        String componentNameA = "5.A.1.1.dynamic";
        final ComponentConfigurationDTO componentA = findComponentConfigurationByName( componentNameA, ComponentConfigurationDTO.SATISFIED | ComponentConfigurationDTO.ACTIVE );
        A a = getServiceFromConfiguration(componentA, A.class);
        assertEquals( 1, a.getBs().size());

        String componentNameB = "5.B.0.1.dynamic";
        final ComponentConfigurationDTO componentB = findComponentConfigurationByName( componentNameB, ComponentConfigurationDTO.ACTIVE );
        B b = getServiceFromConfiguration(componentB, B.class);
        assertEquals( 1, b.getAs().size() );
    }
    /**
     * A > 1.1 > B > 0..1 > A Both should start (B first), and B should have an A reference.
     * @throws InvalidSyntaxException 
     */
    @Test
    public void test_A11_B01_immediate_B_first() throws InvalidSyntaxException
    {
        String componentNameA = "6.A.1.1.dynamic";
        final ComponentConfigurationDTO componentA = findComponentConfigurationByName( componentNameA, ComponentConfigurationDTO.ACTIVE );
        A a = getServiceFromConfiguration(componentA, A.class);
        assertEquals( 1, a.getBs().size());

        String componentNameB = "6.B.0.1.dynamic";
        final ComponentConfigurationDTO componentB = findComponentConfigurationByName( componentNameB, ComponentConfigurationDTO.ACTIVE );
        B b = getServiceFromConfiguration(componentB, B.class);
        assertEquals( 1, b.getAs().size() );
    }
    /**
     * A > 1.1 > B > 0..1 > A Both should start, but B should not have an A reference.
     */
    @Test
    public void test_A11_B01_delayed_A_first() throws InvalidSyntaxException
    {
        String componentNameA = "7.A.1.1.dynamic";
        final ComponentConfigurationDTO componentA = findComponentConfigurationByName( componentNameA, ComponentConfigurationDTO.SATISFIED );

        String componentNameB = "7.B.0.1.dynamic";
        final ComponentConfigurationDTO componentB = findComponentConfigurationByName( componentNameB, ComponentConfigurationDTO.SATISFIED );

        ServiceReference[] serviceReferences = bundleContext.getServiceReferences( A.class.getName(), "(service.pid=" + componentNameA + ")" );
        TestCase.assertEquals( 1, serviceReferences.length );
        ServiceReference serviceReference = serviceReferences[0];
        Object service = bundleContext.getService( serviceReference );
        assertNotNull( service );

        delay();
        A a = getServiceFromConfiguration(componentA, A.class);
        assertEquals( 1, a.getBs().size());
        B b = getServiceFromConfiguration(componentB, B.class);
        assertEquals( 1, b.getAs().size() );
    }
    /**
     * A > 1.1 > B > 0..1 > A Both should start, but B should not have an A reference.
     */
    @Test
    public void test_A11_B01_delayed_B_first() throws Exception
    {
        String componentNameA = "7.A.1.1.dynamic";
        ComponentConfigurationDTO componentA = findComponentConfigurationByName( componentNameA, ComponentConfigurationDTO.SATISFIED );

        String componentNameB = "7.B.0.1.dynamic";
        final ComponentConfigurationDTO componentB = findComponentConfigurationByName( componentNameB, ComponentConfigurationDTO.SATISFIED );

        ServiceReference[] serviceReferencesB = bundleContext.getServiceReferences( B.class.getName(), "(service.pid=" + componentNameB + ")" );
        TestCase.assertEquals( 1, serviceReferencesB.length );
        ServiceReference serviceReferenceB = serviceReferencesB[0];
        Object serviceB = bundleContext.getService( serviceReferenceB );
        assertNotNull( serviceB );

        ServiceReference[] serviceReferencesA = bundleContext.getServiceReferences( A.class.getName(), "(service.pid=" + componentNameA + ")" );
        TestCase.assertEquals( 1, serviceReferencesA.length );
        ServiceReference serviceReferenceA = serviceReferencesA[0];
        Object serviceA = bundleContext.getService( serviceReferenceA );
        assertNotNull( serviceA );


        delay();
        A a = getServiceFromConfiguration(componentA, A.class);
        assertEquals( 1, a.getBs().size());
        B b = getServiceFromConfiguration(componentB, B.class);
        assertEquals( 1, b.getAs().size() );


        //disabling (removing the A service registration) and re-enabling will
        //result in a service event to B, so B will bind A.
        disableAndCheck(componentA);
        delay();
        enableAndCheck(componentA.description);
        delay();

        //new component.id, refetch configuration.
        componentA = findComponentConfigurationByName( componentNameA, ComponentConfigurationDTO.ACTIVE );
        a = getServiceFromConfiguration(componentA, A.class);
        assertEquals( 1, a.getBs().size());
        b = getServiceFromConfiguration(componentB, B.class);
        assertEquals( 1, b.getAs().size() );

    }
    /**
     * A > 1.1 > B > 0..n > A Both should start (B first) and both should have references
     * @throws InvalidSyntaxException
     */
    @Test
    public void test_A11_immediate_B0n_delayed_B_first() throws InvalidSyntaxException
    {
    	String componentNameB = "8.B.0.n.dynamic";
        final ComponentConfigurationDTO componentB = findComponentConfigurationByName( componentNameB, ComponentConfigurationDTO.SATISFIED | ComponentConfigurationDTO.ACTIVE );
        ServiceReference[] serviceReferencesB = bundleContext.getServiceReferences( B.class.getName(), "(service.pid=" + componentNameB + ")" );
        TestCase.assertEquals( 1, serviceReferencesB.length );
        ServiceReference<B> serviceReferenceB = serviceReferencesB[0];
        B b = bundleContext.getService( serviceReferenceB );

        String componentNameA = "8.A.1.1.static";
        ComponentConfigurationDTO componentA = findComponentConfigurationByName( componentNameA, ComponentConfigurationDTO.SATISFIED | ComponentConfigurationDTO.ACTIVE );
        ServiceReference[] serviceReferencesA = bundleContext.getServiceReferences( A.class.getName(), "(service.pid=" + componentNameA + ")" );
        TestCase.assertEquals( 1, serviceReferencesA.length );
        ServiceReference<A> serviceReferenceA = serviceReferencesA[0];
        A a = bundleContext.getService( serviceReferenceA );
        assertNotNull( a );
        assertEquals( 1, a.getBs().size());
        delay();
        assertEquals( 1, b.getAs().size() );
        assertNotNull( b.getAs().get(0) );

    }
}
