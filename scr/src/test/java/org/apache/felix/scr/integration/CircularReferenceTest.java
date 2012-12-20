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
import org.apache.felix.scr.Component;
import org.apache.felix.scr.integration.components.circular.A;
import org.apache.felix.scr.integration.components.circular.B;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @version $Rev:$ $Date:$
 */
@RunWith(JUnit4TestRunner.class)
public class CircularReferenceTest extends ComponentTestBase
{

    static
    {
        // uncomment to enable debugging of this test class
//        paxRunnerVmOption = DEBUG_VM_OPTION;

        descriptorFile = "/integration_test_circular.xml";
    }


    /**
     * A and B have mandatory dependencies on each other.  Neither should start.
     */
    @Test
    public void test_A11_B11()
    {
        String componentNameA = "1.A.1.1.dynamic";
        final Component componentA = findComponentByName( componentNameA );
        TestCase.assertNotNull( componentA );
        TestCase.assertEquals( Component.STATE_UNSATISFIED, componentA.getState() );

        String componentNameB = "1.B.1.1.dynamic";
        final Component componentB = findComponentByName( componentNameB );
        TestCase.assertNotNull( componentB );
        TestCase.assertEquals( Component.STATE_UNSATISFIED, componentB.getState() );


    }

    /**
     * A > 1.1 > B > 0..n > A Both should start (A first), but B should not have an A reference.
     */
    @Test
    public void test_A11_B0n_immediate_A_first()
    {
        String componentNameA = "2.A.1.1.dynamic";
        final Component componentA = findComponentByName( componentNameA );
        TestCase.assertNotNull( componentA );
        TestCase.assertEquals( Component.STATE_ACTIVE, componentA.getState() );
        A a = ( A ) componentA.getComponentInstance().getInstance();
        assertEquals( 1, a.getBs().size());

        String componentNameB = "2.B.0.n.dynamic";
        final Component componentB = findComponentByName( componentNameB );
        TestCase.assertNotNull( componentB );
        TestCase.assertEquals( Component.STATE_ACTIVE, componentB.getState() );
        B b = ( B ) componentB.getComponentInstance().getInstance();
        assertEquals( 1, b.getAs().size() );
    }
    /**
     * A > 1.1 > B > 0..n > A Both should start (B first), and B should have an A reference.
     */
    @Test
    public void test_A11_B0n_immediate_B_first()
    {
        String componentNameA = "3.A.1.1.dynamic";
        final Component componentA = findComponentByName( componentNameA );
        TestCase.assertNotNull( componentA );
        TestCase.assertEquals( Component.STATE_ACTIVE, componentA.getState() );
        A a = ( A ) componentA.getComponentInstance().getInstance();
        assertEquals( 1, a.getBs().size());

        String componentNameB = "3.B.0.n.dynamic";
        final Component componentB = findComponentByName( componentNameB );
        TestCase.assertNotNull( componentB );
        TestCase.assertEquals( Component.STATE_ACTIVE, componentB.getState() );
        B b = ( B ) componentB.getComponentInstance().getInstance();
        assertEquals( 1, b.getAs().size() );
    }
    /**
     * A > 1.1 > B > 0..n > A Both should start, but B should not have an A reference.
     */
    @Test
    public void test_A11_B0n_delayed_A_first() throws InvalidSyntaxException
    {
        String componentNameA = "4.A.1.1.dynamic";
        final Component componentA = findComponentByName( componentNameA );
        TestCase.assertNotNull( componentA );
        TestCase.assertEquals( Component.STATE_REGISTERED, componentA.getState() );

        String componentNameB = "4.B.0.n.dynamic";
        final Component componentB = findComponentByName( componentNameB );
        TestCase.assertNotNull( componentB );
        TestCase.assertEquals( Component.STATE_REGISTERED, componentB.getState() );

        ServiceReference[] serviceReferences = bundleContext.getServiceReferences( A.class.getName(), "(service.pid=" + componentNameA + ")" );
        TestCase.assertEquals( 1, serviceReferences.length );
        ServiceReference serviceReference = serviceReferences[0];
        Object service = bundleContext.getService( serviceReference );
        assertNotNull( service );

        delay();

        A a = ( A ) componentA.getComponentInstance().getInstance();
        assertEquals( 1, a.getBs().size() );
        B b = ( B ) componentB.getComponentInstance().getInstance();
        assertEquals( 1, b.getAs().size() );
    }
    /**
     * A > 1.1 > B > 0..n > A Both should start, but B should not have an A reference.
     */
    @Test
    public void test_A11_B0n_delayed_B_first() throws InvalidSyntaxException
    {
        String componentNameA = "4.A.1.1.dynamic";
        final Component componentA = findComponentByName( componentNameA );
        TestCase.assertNotNull( componentA );
        TestCase.assertEquals( Component.STATE_REGISTERED, componentA.getState() );

        String componentNameB = "4.B.0.n.dynamic";
        final Component componentB = findComponentByName( componentNameB );
        TestCase.assertNotNull( componentB );
        TestCase.assertEquals( Component.STATE_REGISTERED, componentB.getState() );

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
        A a = ( A ) componentA.getComponentInstance().getInstance();
        assertEquals( 1, a.getBs().size() );
        B b = ( B ) componentB.getComponentInstance().getInstance();
        assertEquals( 1, b.getAs().size() );


        //disabling (removing the A service registration) and re-enabling will
        //result in a service event to B, so B will bind A.
        componentA.disable();
        delay();
        componentA.enable();
        delay();
        ServiceReference[] serviceReferencesA1 = bundleContext.getServiceReferences( A.class.getName(), "(service.pid=" + componentNameA + ")" );
        TestCase.assertEquals( 1, serviceReferencesA1.length );
        ServiceReference serviceReferenceA1 = serviceReferencesA1[0];
        Object serviceA1 = bundleContext.getService( serviceReferenceA1 );
        assertNotNull( serviceA1 );

        A a1 = ( A ) componentA.getComponentInstance().getInstance();
        assertEquals( 1, a1.getBs().size() );
        B b1 = ( B ) componentB.getComponentInstance().getInstance();
        assertEquals( 1, b1.getAs().size() );

    }
    /**
     * A > 1.1 > B > 0..1 > A Both should start (A first), but B should not have an A reference.
     */
    @Test
    public void test_A11_B01_immediate_A_first()
    {
        String componentNameA = "5.A.1.1.dynamic";
        final Component componentA = findComponentByName( componentNameA );
        TestCase.assertNotNull( componentA );
        TestCase.assertEquals( Component.STATE_ACTIVE, componentA.getState() );
        A a = ( A ) componentA.getComponentInstance().getInstance();
        assertEquals( 1, a.getBs().size());

        String componentNameB = "5.B.0.1.dynamic";
        final Component componentB = findComponentByName( componentNameB );
        TestCase.assertNotNull( componentB );
        TestCase.assertEquals( Component.STATE_ACTIVE, componentB.getState() );
        B b = ( B ) componentB.getComponentInstance().getInstance();
        assertEquals( 1, b.getAs().size() );
    }
    /**
     * A > 1.1 > B > 0..1 > A Both should start (B first), and B should have an A reference.
     */
    @Test
    public void test_A11_B01_immediate_B_first()
    {
        String componentNameA = "6.A.1.1.dynamic";
        final Component componentA = findComponentByName( componentNameA );
        TestCase.assertNotNull( componentA );
        TestCase.assertEquals( Component.STATE_ACTIVE, componentA.getState() );
        A a = ( A ) componentA.getComponentInstance().getInstance();
        assertEquals( 1, a.getBs().size());

        String componentNameB = "6.B.0.1.dynamic";
        final Component componentB = findComponentByName( componentNameB );
        TestCase.assertNotNull( componentB );
        TestCase.assertEquals( Component.STATE_ACTIVE, componentB.getState() );
        B b = ( B ) componentB.getComponentInstance().getInstance();
        assertEquals( 1, b.getAs().size() );
    }
    /**
     * A > 1.1 > B > 0..1 > A Both should start, but B should not have an A reference.
     */
    @Test
    public void test_A11_B01_delayed_A_first() throws InvalidSyntaxException
    {
        String componentNameA = "7.A.1.1.dynamic";
        final Component componentA = findComponentByName( componentNameA );
        TestCase.assertNotNull( componentA );
        TestCase.assertEquals( Component.STATE_REGISTERED, componentA.getState() );

        String componentNameB = "7.B.0.1.dynamic";
        final Component componentB = findComponentByName( componentNameB );
        TestCase.assertNotNull( componentB );
        TestCase.assertEquals( Component.STATE_REGISTERED, componentB.getState() );

        ServiceReference[] serviceReferences = bundleContext.getServiceReferences( A.class.getName(), "(service.pid=" + componentNameA + ")" );
        TestCase.assertEquals( 1, serviceReferences.length );
        ServiceReference serviceReference = serviceReferences[0];
        Object service = bundleContext.getService( serviceReference );
        assertNotNull( service );

        delay();
        A a = ( A ) componentA.getComponentInstance().getInstance();
        assertEquals( 1, a.getBs().size() );
        B b = ( B ) componentB.getComponentInstance().getInstance();
        assertEquals( 1, b.getAs().size() );
    }
    /**
     * A > 1.1 > B > 0..1 > A Both should start, but B should not have an A reference.
     */
    @Test
    public void test_A11_B01_delayed_B_first() throws InvalidSyntaxException
    {
        String componentNameA = "7.A.1.1.dynamic";
        final Component componentA = findComponentByName( componentNameA );
        TestCase.assertNotNull( componentA );
        TestCase.assertEquals( Component.STATE_REGISTERED, componentA.getState() );

        String componentNameB = "7.B.0.1.dynamic";
        final Component componentB = findComponentByName( componentNameB );
        TestCase.assertNotNull( componentB );
        TestCase.assertEquals( Component.STATE_REGISTERED, componentB.getState() );

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
        A a = ( A ) componentA.getComponentInstance().getInstance();
        assertEquals( 1, a.getBs().size() );
        B b = ( B ) componentB.getComponentInstance().getInstance();
        assertEquals( 1, b.getAs().size() );


        //disabling (removing the A service registration) and re-enabling will
        //result in a service event to B, so B will bind A.
        componentA.disable();
        delay();
        componentA.enable();
        delay();
        ServiceReference[] serviceReferencesA1 = bundleContext.getServiceReferences( A.class.getName(), "(service.pid=" + componentNameA + ")" );
        TestCase.assertEquals( 1, serviceReferencesA1.length );
        ServiceReference serviceReferenceA1 = serviceReferencesA1[0];
        Object serviceA1 = bundleContext.getService( serviceReferenceA1 );
        assertNotNull( serviceA1 );

        A a1 = ( A ) componentA.getComponentInstance().getInstance();
        assertEquals( 1, a1.getBs().size() );
        B b1 = ( B ) componentB.getComponentInstance().getInstance();
        assertEquals( 1, b1.getAs().size() );

    }
}
