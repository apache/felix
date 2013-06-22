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
import org.apache.felix.ipojo.handlers.providedservice.ProvidedService;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandlerDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceListener;
import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Test that the ProvidedServiceHandler calls the ProvidedServiceHandlerListeners when watched provided service changes.
 */
public class TestListeners extends Common {

    /**
     * The number of provided service registrations, incremented by the {@code CountingListener}s.
     */
    int registrations = 0;

    /**
     * The number of provided service unregistrations, incremented by the {@code CountingListener}s.
     */
    int unregistrations = 0;

    /**
     * The number of provided service updates, incremented by the {@code CountingListener}s.
     */
    int updates = 0;

    /**
     * The total number of provided service events, incremented by the {@code TotalCountingListener}s.
     */
    int total = 0;

    /**
     * The {@code instance} arguments received by the {@code AppendingListener}s.
     */
    List<ComponentInstance> instances = new ArrayList<ComponentInstance>();

    /**
     * A ProvidedServiceListener that just count service registrations, updates and unregistrations.
     */
    private class CountingListener implements ProvidedServiceListener {
        public void serviceRegistered(ComponentInstance instance, ProvidedService providedService) {
            registrations++;
        }
        public void serviceModified(ComponentInstance instance, ProvidedService providedService) {
            updates++;
        }
        public void serviceUnregistered(ComponentInstance instance, ProvidedService providedService) {
            unregistrations++;
        }
    }

    /**
     * A ProvidedServiceListener that just count events.
     */
    private class TotalCountingListener implements ProvidedServiceListener {
        public void serviceRegistered(ComponentInstance instance, ProvidedService providedService) {
            total++;
        }
        public void serviceModified(ComponentInstance instance, ProvidedService providedService) {
            total++;
        }
        public void serviceUnregistered(ComponentInstance instance, ProvidedService providedService) {
            total++;
        }
    }

    /**
     * A ProvidedServiceListener that just fails.
     * <p>
     * Used to ensure that a failing listener does not prevent the following listeners to be called.
     * </p>
     */
    private class ThrowingListener implements ProvidedServiceListener {
        public void serviceRegistered(ComponentInstance instance, ProvidedService providedService) {
            throw new RuntimeException("I'm bad");
        }
        public void serviceModified(ComponentInstance instance, ProvidedService providedService) {
            throw new RuntimeException("I'm bad");
        }
        public void serviceUnregistered(ComponentInstance instance, ProvidedService providedService) {
            throw new RuntimeException("I'm bad");
        }
    }

    /**
     * A ProvidedServiceListener that just appends its arguments.
     */
    private class AppendingListener implements ProvidedServiceListener {
        public void serviceRegistered(ComponentInstance instance, ProvidedService providedService) {
            instances.add(instance);
        }
        public void serviceModified(ComponentInstance instance, ProvidedService providedService) {
            instances.add(instance);
        }
        public void serviceUnregistered(ComponentInstance instance, ProvidedService providedService) {
            instances.add(instance);
        }
    }

    /**
     * Test that the listeners registered on the tested instance are called when the instance provided service is
     * registered, updated or unregistered.
     */
    @Test
    public void testProvidedServiceListener() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Controller-1-default");
        ProvidedServiceHandlerDescription pshd = (ProvidedServiceHandlerDescription) ci.getInstanceDescription()
                .getHandlerDescription("org.apache.felix.ipojo:provides");
        ProvidedServiceDescription ps = getPS(FooService.class.getName(), pshd.getProvidedServices());

        // Controller set to true.
        osgiHelper.waitForService(FooService.class.getName(), null, 5000);
        osgiHelper.waitForService(CheckService.class.getName(), null, 5000);

        CheckService check = (CheckService) osgiHelper.getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);

        // Register listeners :
        // 1- CountingListener l1
        // 2- ThrowingListener bad
        // 3- TotalCountingListener l2
        // 4- AppendingListener l3
        ProvidedServiceListener l1 = new CountingListener();
        ps.addListener(l1);
        ProvidedServiceListener bad = new ThrowingListener();
        ps.addListener(bad);
        ProvidedServiceListener l2 = new TotalCountingListener();
        ps.addListener(l2);
        ProvidedServiceListener l3 = new AppendingListener();
        ps.addListener(l3);

        // Check initial valued are untouched
        assertEquals(0, registrations);
        assertEquals(0, unregistrations);
        assertEquals(0, updates);
        assertEquals(0, total);

        // Unregister the service and check.
        assertFalse(check.check());
        assertEquals(0, registrations);
        assertEquals(1, unregistrations);
        assertEquals(0, updates);
        assertEquals(1, total);

        // Modify the service while it is unregistered. Nothing should move.
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("change1", "1");
        ps.addProperties(props);
        assertEquals(0, registrations);
        assertEquals(1, unregistrations);
        assertEquals(0, updates);
        assertEquals(1, total);

        // Register the service and check.
        assertTrue(check.check());
        assertEquals(1, registrations);
        assertEquals(1, unregistrations);
        assertEquals(0, updates);
        assertEquals(2, total);

        // Modify the service while it is REGISTERED
        props.clear();
        props.put("change2", "2");
        ps.addProperties(props);
        assertEquals(1, registrations);
        assertEquals(1, unregistrations);
        assertEquals(1, updates);
        assertEquals(3, total);

        // One more time, just to be sure...
        assertFalse(check.check()); // Unregister
        assertEquals(1, registrations);
        assertEquals(2, unregistrations);
        assertEquals(1, updates);
        assertEquals(4, total);
        assertTrue(check.check()); // Register
        assertEquals(2, registrations);
        assertEquals(2, unregistrations);
        assertEquals(1, updates);
        assertEquals(5, total);

        // Unregister the listener
        ps.removeListener(l1);
        ps.removeListener(bad);
        ps.removeListener(l2);
        ps.removeListener(l3);

        // Play with the controller and check that nothing moves
        assertFalse(check.check()); // Unregister
        assertEquals(2, registrations);
        assertEquals(2, unregistrations);
        assertEquals(1, updates);
        assertEquals(5, total);
        assertTrue(check.check()); // Register
        assertEquals(2, registrations);
        assertEquals(2, unregistrations);
        assertEquals(1, updates);
        assertEquals(5, total);
        props.clear(); props.put("change3", "3"); ps.addProperties(props); // Modify
        assertEquals(2, registrations);
        assertEquals(2, unregistrations);
        assertEquals(1, updates);
        assertEquals(5, total);

        // Check that instances contains $total times the ci component instance, and nothing else.
        assertEquals(5, instances.size());
        instances.removeAll(Collections.singleton(ci));
        assertEquals(0, instances.size());

        ci.dispose();
    }

    @Test(expected = NullPointerException.class)
    public void testNullProvidedServiceListener() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Controller-1-default");
        ProvidedServiceHandlerDescription pshd = (ProvidedServiceHandlerDescription) ci.getInstanceDescription()
                .getHandlerDescription("org.apache.felix.ipojo:provides");
        ProvidedServiceDescription ps = getPS(FooService.class.getName(), pshd.getProvidedServices());

        // Should fail!
        ps.addListener(null);
    }

    @Test(expected = NoSuchElementException.class)
    public void testRemoveNonexistentProvidedServiceListener() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Controller-1-default");
        ProvidedServiceHandlerDescription pshd = (ProvidedServiceHandlerDescription) ci.getInstanceDescription()
                .getHandlerDescription("org.apache.felix.ipojo:provides");
        ProvidedServiceDescription ps = getPS(FooService.class.getName(), pshd.getProvidedServices());

        // Should fail!
        ps.removeListener(new ThrowingListener());
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
