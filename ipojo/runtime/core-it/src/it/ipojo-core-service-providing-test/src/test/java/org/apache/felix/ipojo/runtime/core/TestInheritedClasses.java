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
package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.runtime.core.services.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestInheritedClasses extends Common {

    private Factory pi1, pi11, pi12, pi2, pi21, pi3;

    @Before
    public void setUp() {
        pi1 = ipojoHelper.getFactory("PS-PI1");
        pi11 = ipojoHelper.getFactory("PS-PI1-1");
        pi12 = ipojoHelper.getFactory("PS-PI1-2");

        pi2 = ipojoHelper.getFactory("PS-PI2");
        pi21 = ipojoHelper.getFactory("PS-PI2-1");

        pi3 = ipojoHelper.getFactory("PS-PI3");
    }

    private boolean contains(String[] arr, String txt) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(txt)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testPI1Factory() {
        String[] specs = pi1.getComponentDescription().getprovidedServiceSpecification();
        assertEquals("Check provides count", specs.length, 4);
        assertTrue("Check Child", contains(specs, ChildInterface.class.getName()));
        assertTrue("Check Parent1", contains(specs, ParentInterface1.class.getName()));
        assertTrue("Check Parent2", contains(specs, ParentInterface2.class.getName()));
        assertTrue("Check ParentParent", contains(specs, ParentParentInterface.class.getName()));
    }

    @Test
    public void testPI11Factory() {
        String[] specs = pi11.getComponentDescription().getprovidedServiceSpecification();
        assertEquals("Check provides count", specs.length, 1);
        assertTrue("Check ParentParent", contains(specs, ParentParentInterface.class.getName()));
    }

    @Test
    public void testPI12Factory() {
        String[] specs = pi12.getComponentDescription().getprovidedServiceSpecification();
        assertEquals("Check provides count", specs.length, 2);
        assertTrue("Check Parent2", contains(specs, ParentInterface2.class.getName()));
        assertTrue("Check ParentParent", contains(specs, ParentParentInterface.class.getName()));
    }

    @Test
    public void testPI2Factory() {
        String[] specs = pi2.getComponentDescription().getprovidedServiceSpecification();
        assertEquals("Check provides count (" + specs.length + ")", specs.length, 4);
        assertTrue("Check Child", contains(specs, ChildInterface.class.getName()));
        assertTrue("Check Parent1", contains(specs, ParentInterface1.class.getName()));
        assertTrue("Check Parent2", contains(specs, ParentInterface2.class.getName()));
        assertTrue("Check ParentParent", contains(specs, ParentParentInterface.class.getName()));
    }

    @Test
    public void testPI21Factory() {
        String[] specs = pi21.getComponentDescription().getprovidedServiceSpecification();
        assertEquals("Check provides count", specs.length, 1);
        assertTrue("Check ParentParent", contains(specs, ParentParentInterface.class.getName()));
    }

    @Test
    public void testPI3Factory() {
        String[] specs = pi3.getComponentDescription().getprovidedServiceSpecification();
        assertEquals("Check provides count", specs.length, 5);
        assertTrue("Check Child", contains(specs, ChildInterface.class.getName()));
        assertTrue("Check Parent1", contains(specs, ParentInterface1.class.getName()));
        assertTrue("Check Parent2", contains(specs, ParentInterface2.class.getName()));
        assertTrue("Check ParentParent", contains(specs, ParentParentInterface.class.getName()));
        assertTrue("Check FS", contains(specs, FooService.class.getName()));
    }

    @Test
    public void testIP1() {
        ComponentInstance ci = ipojoHelper.createComponentInstance(pi1.getName(), "ci");

        ServiceReference ref1 = ipojoHelper.getServiceReferenceByName(ChildInterface.class.getName(), "ci");
        assertNotNull("Check Child", ref1);

        ServiceReference ref2 = ipojoHelper.getServiceReferenceByName(ParentInterface1.class.getName(), "ci");
        assertNotNull("Check Parent1", ref2);

        ServiceReference ref3 = ipojoHelper.getServiceReferenceByName(ParentInterface2.class.getName(), "ci");
        assertNotNull("Check Parent2", ref3);

        ServiceReference ref4 = ipojoHelper.getServiceReferenceByName(ParentParentInterface.class.getName(), "ci");
        assertNotNull("Check PP", ref4);

        ci.dispose();
    }

    @Test
    public void testIP11() {
        ComponentInstance ci = ipojoHelper.createComponentInstance(pi11.getName(), "ci");

        ServiceReference ref4 = ipojoHelper.getServiceReferenceByName(ParentParentInterface.class.getName(), "ci");
        assertNotNull("Check PP", ref4);

        ci.dispose();
    }

    @Test
    public void testIP12() {
        ComponentInstance ci = ipojoHelper.createComponentInstance(pi12.getName(), "ci");

        ServiceReference ref3 = ipojoHelper.getServiceReferenceByName(ParentInterface2.class.getName(), "ci");
        assertNotNull("Check Parent2", ref3);

        ServiceReference ref4 = ipojoHelper.getServiceReferenceByName(ParentParentInterface.class.getName(), "ci");
        assertNotNull("Check PP", ref4);

        ci.dispose();
    }

    @Test
    public void testIP2() {
        ComponentInstance ci = ipojoHelper.createComponentInstance(pi2.getName(), "ci");

        ServiceReference ref1 = ipojoHelper.getServiceReferenceByName(ChildInterface.class.getName(), "ci");
        assertNotNull("Check Child", ref1);

        ServiceReference ref2 = ipojoHelper.getServiceReferenceByName(ParentInterface1.class.getName(), "ci");
        assertNotNull("Check Parent1", ref2);

        ServiceReference ref3 = ipojoHelper.getServiceReferenceByName(ParentInterface2.class.getName(), "ci");
        assertNotNull("Check Parent2", ref3);

        ServiceReference ref4 = ipojoHelper.getServiceReferenceByName(ParentParentInterface.class.getName(), "ci");
        assertNotNull("Check PP", ref4);

        ci.dispose();
    }

    @Test
    public void testIP21() {
        ComponentInstance ci = ipojoHelper.createComponentInstance(pi21.getName(), "ci");

        ServiceReference ref4 = ipojoHelper.getServiceReferenceByName(ParentParentInterface.class.getName(), "ci");
        assertNotNull("Check PP", ref4);

        ci.dispose();
    }

    @Test
    public void testIP3() {
        ComponentInstance ci = ipojoHelper.createComponentInstance(pi3.getName(), "ci");

        ServiceReference ref1 = ipojoHelper.getServiceReferenceByName(ChildInterface.class.getName(), "ci");
        assertNotNull("Check Child", ref1);

        ServiceReference ref2 = ipojoHelper.getServiceReferenceByName(ParentInterface1.class.getName(), "ci");
        assertNotNull("Check Parent1", ref2);

        ServiceReference ref3 = ipojoHelper.getServiceReferenceByName(ParentInterface2.class.getName(), "ci");
        assertNotNull("Check Parent2", ref3);

        ServiceReference ref4 = ipojoHelper.getServiceReferenceByName(ParentParentInterface.class.getName(), "ci");
        assertNotNull("Check PP", ref4);

        ServiceReference ref5 = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), "ci");
        assertNotNull("Check FS", ref5);

        ci.dispose();
    }


}
