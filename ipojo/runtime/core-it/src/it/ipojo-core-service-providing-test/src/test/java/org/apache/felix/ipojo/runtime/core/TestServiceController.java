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
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandlerDescription;
import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestServiceController extends Common {

    @Test
    public void testComponentWithAController() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Controller-1-default");
        // Controller set to true.
        osgiHelper.waitForService(FooService.class.getName(), null, 5000);
        osgiHelper.waitForService(CheckService.class.getName(), null, 5000);

        CheckService check = (CheckService) osgiHelper.getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);

        assertFalse(check.check());

        // FooService should not be there anymore
        assertNull(osgiHelper.getServiceReference(FooService.class.getName()));

        assertTrue(check.check());

        assertNotNull(osgiHelper.getServiceReference(FooService.class.getName()));

        ci.dispose();
    }

    @Test
    public void testComponentWithAControllerSetToFalse() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Controller-1-false");
        // Controller set to false.
        osgiHelper.waitForService(CheckService.class.getName(), null, 5000);
        assertNull(osgiHelper.getServiceReference(FooService.class.getName()));

        CheckService check = (CheckService) osgiHelper.getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);

        assertTrue(check.check());
        assertNotNull(osgiHelper.getServiceReference(FooService.class.getName()));

        assertFalse(check.check());
        // FooService should not be there anymore
        assertNull(osgiHelper.getServiceReference(FooService.class.getName()));

        ci.dispose();
    }

    @Test
    public void testComponentWithTwoControllersSetToTrue() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Controller-2-truetrue");

        osgiHelper.waitForService(CheckService.class.getName(), null, 5000);
        osgiHelper.waitForService(FooService.class.getName(), null, 5000);

        CheckService check = (CheckService) osgiHelper.getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);

        check.check();

        assertNull(osgiHelper.getServiceReference(CheckService.class.getName()));
        assertNotNull(osgiHelper.getServiceReference(FooService.class.getName()));

        FooService fs = (FooService) osgiHelper.getServiceObject(FooService.class.getName(), null);
        fs.foo();

        assertNull(osgiHelper.getServiceReference(CheckService.class.getName()));
        assertNull(osgiHelper.getServiceReference(FooService.class.getName()));

        ci.dispose();
    }

    @Test
    public void testComponentWithTwoControllersSetToTrueAndFalse() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Controller-2-truefalse");

        osgiHelper.waitForService(CheckService.class.getName(), null, 5000);

        assertFalse(osgiHelper.isServiceAvailable(FooService.class.getName()));

        CheckService check = (CheckService) osgiHelper.getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);

        check.getProps();

        assertFalse(osgiHelper.isServiceAvailable(CheckService.class.getName()));
        assertTrue(osgiHelper.isServiceAvailable(FooService.class.getName()));

        FooService fs = (FooService) osgiHelper.getServiceObject(FooService.class.getName(), null);
        fs.fooProps();

        assertTrue(osgiHelper.isServiceAvailable(CheckService.class.getName()));
        assertTrue(osgiHelper.isServiceAvailable(FooService.class.getName()));

        ci.dispose();
    }

    @Test
    public void testComponentWithTwoControllersUsingBothSpecificationsTrueFalse() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Controller-2-spec1");

        osgiHelper.waitForService(CheckService.class.getName(), null, 5000);

        assertFalse(osgiHelper.isServiceAvailable(FooService.class.getName()));

        CheckService check = (CheckService) osgiHelper.getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);

        check.getProps();

        assertFalse(osgiHelper.isServiceAvailable(CheckService.class.getName()));
        assertTrue(osgiHelper.isServiceAvailable(FooService.class.getName()));

        FooService fs = (FooService) osgiHelper.getServiceObject(FooService.class.getName(), null);
        fs.fooProps();

        assertTrue(osgiHelper.isServiceAvailable(CheckService.class.getName()));
        assertTrue(osgiHelper.isServiceAvailable(FooService.class.getName()));

        ci.dispose();
    }

    @Test
    public void testComponentWithTwoControllersUsingBothSpecificationsTrueTrue() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Controller-2-spec2");

        osgiHelper.waitForService(CheckService.class.getName(), null, 5000);
        osgiHelper.waitForService(FooService.class.getName(), null, 5000);

        CheckService check = (CheckService) osgiHelper.getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);

        check.check();
        // CheckService not available
        assertNull(osgiHelper.getServiceReference(CheckService.class.getName()));
        assertNotNull(osgiHelper.getServiceReference(FooService.class.getName()));

        FooService fs = (FooService) osgiHelper.getServiceObject(FooService.class.getName(), null);
        fs.foo();

        assertNull(osgiHelper.getServiceReference(CheckService.class.getName()));
        assertNull(osgiHelper.getServiceReference(FooService.class.getName()));

        ci.dispose();
    }

    @Test
    public void testComponentWithTwoControllersUsingSpecificationAndAllTrueTrue() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Controller-2-spec3");

        osgiHelper.waitForService(CheckService.class.getName(), null, 5000);
        osgiHelper.waitForService(FooService.class.getName(), null, 5000);

        CheckService check = (CheckService) osgiHelper.getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);

        check.check();
        // CheckService not available
        assertNull(osgiHelper.getServiceReference(CheckService.class.getName()));
        assertNotNull(osgiHelper.getServiceReference(FooService.class.getName()));

        FooService fs = (FooService) osgiHelper.getServiceObject(FooService.class.getName(), null);
        fs.foo();

        assertNull(osgiHelper.getServiceReference(CheckService.class.getName()));
        assertNull(osgiHelper.getServiceReference(FooService.class.getName()));

        ci.dispose();
    }

    @Test
    public void testComponentWithTwoControllersUsingSpecificationAndAllTrueFalse() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Controller-2-spec4");

        osgiHelper.waitForService(CheckService.class.getName(), null, 5000);

        assertFalse(osgiHelper.isServiceAvailable(FooService.class.getName()));

        CheckService check = (CheckService) osgiHelper.getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);

        check.getProps();

        assertFalse(osgiHelper.isServiceAvailable(CheckService.class.getName()));
        assertTrue(osgiHelper.isServiceAvailable(FooService.class.getName()));

        FooService fs = (FooService) osgiHelper.getServiceObject(FooService.class.getName(), null);
        fs.fooProps();

        assertTrue(osgiHelper.isServiceAvailable(CheckService.class.getName()));
        assertTrue(osgiHelper.isServiceAvailable(FooService.class.getName()));

        ci.dispose();
    }

    @Test
    public void testArchitecture() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Controller-1-default");
        // Controller set to true.
        osgiHelper.waitForService(FooService.class.getName(), null, 5000);
        osgiHelper.waitForService(CheckService.class.getName(), null, 5000);

        ProvidedServiceHandlerDescription pshd = null;
        pshd = (ProvidedServiceHandlerDescription) ci.getInstanceDescription()
                .getHandlerDescription("org.apache.felix.ipojo:provides");

        ProvidedServiceDescription ps = getPS(FooService.class.getName(), pshd.getProvidedServices());
        assertEquals("true", ps.getController());

        CheckService check = (CheckService) osgiHelper.getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);

        assertFalse(check.check());

        ps = getPS(FooService.class.getName(), pshd.getProvidedServices());
        assertEquals("false", ps.getController());

        assertTrue(check.check());

        ps = getPS(FooService.class.getName(), pshd.getProvidedServices());
        assertEquals("true", ps.getController());

    }

    private ProvidedServiceDescription getPS(String itf, ProvidedServiceDescription[] svc) {
        for (int i = 0; i < svc.length; i++) {
            if (svc[i].getServiceSpecifications()[0].equals(itf)) {
                return svc[i];
            }
        }

        fail("Service : " + itf + " not found");
        return null;
    }
}
