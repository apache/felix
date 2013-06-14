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
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandlerDescription;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Dictionary;
import java.util.Hashtable;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class TestArchitecture extends Common {

    /**
     * Instance where the ManagedServicePID is provided by the component type.
     */
    ComponentInstance instance1;
    /**
     * Instance where the ManagedServicePID is provided by the instance.
     */
    ComponentInstance instance2;

    @Before
    public void setUp() {
        String type = "CONFIG-FooProviderType-4";
        Dictionary<String, String> p = new Hashtable<String, String>();
        p.put("instance.name", "instance");
        p.put("foo", "foo");
        p.put("bar", "2");
        p.put("baz", "baz");
        instance1 = ipojoHelper.createComponentInstance(type, p);
        assertEquals("instance1 created", ComponentInstance.VALID, instance1.getState());

        type = "CONFIG-FooProviderType-3";
        Dictionary<String, String> p1 = new Hashtable<String, String>();
        p1.put("instance.name", "instance-2");
        p1.put("foo", "foo");
        p1.put("bar", "2");
        p1.put("baz", "baz");
        p1.put("managed.service.pid", "instance");
        instance2 = ipojoHelper.createComponentInstance(type, p1);

    }

    @After
    public void tearDown() {
        instance1.dispose();
        instance2.dispose();

        instance1 = null;
        instance2 = null;
    }

    @Test
    public void testArchitectureForInstance1() {

        Architecture arch = osgiHelper.getServiceObject(Architecture.class,
                "(architecture.instance=instance)");
        assertNotNull(arch);

        // Test on String representation.
        String desc = arch.getInstanceDescription().getDescription().toString();
        assertTrue(desc.contains("managed.service.pid=\"FooProvider-3\""));

        // Test on handler description
        ConfigurationHandlerDescription hd = (ConfigurationHandlerDescription) arch.getInstanceDescription().getHandlerDescription("org.apache.felix.ipojo:properties");
        assertNotNull(hd);

        assertEquals(2, hd.getProperties().length);
        assertEquals("FooProvider-3", hd.getManagedServicePid());

        // Check the getInstance() method
        assertSame(arch.getInstanceDescription().getInstance(), instance1);

    }

    @Test
    public void testArchitectureForInstance2() {
        Architecture arch = osgiHelper.getServiceObject(Architecture.class, "(architecture.instance=instance-2)");
        assertNotNull(arch);

        // Test on String representation.
        String desc = arch.getInstanceDescription().getDescription().toString();
        assertTrue(desc.contains("managed.service.pid=\"instance\""));

        // Test on handler description
        ConfigurationHandlerDescription hd = (ConfigurationHandlerDescription) arch.getInstanceDescription().getHandlerDescription("org.apache.felix.ipojo:properties");
        assertNotNull(hd);

        assertEquals(2, hd.getProperties().length);
        assertEquals("instance", hd.getManagedServicePid());

        // Check the getInstance() method
        assertSame(arch.getInstanceDescription().getInstance(), instance2);

    }

    /**
     * Test checking the availability of the architecture instance according to the instance state.
     * The architecture instance is available even in the STOPPED state.
     */
    @Test
    public void testArchitectureServiceAvailability() {
        String instanceName = instance1.getInstanceName();
        // Check architecture of instance1
        Architecture arch = ipojoHelper.getArchitectureByName(instanceName);
        assertNotNull(arch);
        assertEquals(ComponentInstance.VALID, arch.getInstanceDescription().getState());

        // We stop the instance
        instance1.stop();
        arch = ipojoHelper.getArchitectureByName(instanceName);
        assertNotNull(arch);
        assertEquals(ComponentInstance.STOPPED, arch.getInstanceDescription().getState());

        // Restart.
        instance1.start();
        arch = ipojoHelper.getArchitectureByName(instanceName);
        assertNotNull(arch);
        assertEquals(ComponentInstance.VALID, arch.getInstanceDescription().getState());

        // Disposal
        instance1.dispose();
        arch = ipojoHelper.getArchitectureByName(instanceName);
        assertNull(arch);
    }


}
