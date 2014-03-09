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
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandlerDescription;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationListener;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Test that the ConfigurationHandler calls the ConfigurationListeners when watched instance is reconfigured.
 */
public class TestListeners extends Common {

    /**
     * The number of reconfigurations, incremented by the {@code CountingListener}s.
     */
    int updates = 0;

    /**
     * The {@code instance} arguments received by the {@code AppendingListener}s.
     */
    List<ComponentInstance> instances = new ArrayList<ComponentInstance>();

    /**
     * The {@code configuration} arguments received by the {@code AppendingListener}s.
     */
    List<Map<String, Object>> configs = new ArrayList<Map<String, Object>>();

    /**
     * The tested component instance.
     */
    ComponentInstance fooProvider;

    /**
     * The configuration handler description of the tested component instance.
     */
    ConfigurationHandlerDescription fooConfig;

    /**
     * The service provided by the tested component instance.
     */
    FooService foo;

    @Before
    public void setUp() {
        Properties p = new Properties();
        p.put("instance.name", "FooProvider-42");
        p.put("int", 4);
        p.put("boolean", false);
        p.put("string", "bar");
        p.put("strAProp", new String[]{"bar", "foo"});
        p.put("intAProp", new int[]{1, 2, 3});
        fooProvider = ipojoHelper.createComponentInstance("CONFIG-FooProviderType-ConfUpdated", p);
        fooConfig = (ConfigurationHandlerDescription) fooProvider.getInstanceDescription()
                .getHandlerDescription("org.apache.felix.ipojo:properties");

        // Get the service
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-42");
        foo = (FooService) osgiHelper.getRawServiceObject(ref);
    }

    @After
    public void tearDown() {
        fooConfig = null;
        fooProvider.dispose();
        fooProvider = null;
    }

    /**
     * A ConfigurationListener that just count its calls.
     */
    private class CountingListener implements ConfigurationListener {
        public void configurationChanged(ComponentInstance instance, Map<String, Object> configuration) {
            updates++;
        }
    }

    /**
     * A ConfigurationListener that just fails.
     * <p>
     * Used to ensure that a failing listener does not prevent the following listeners to be called.
     * </p>
     */
    private class ThrowingListener implements ConfigurationListener {
        public void configurationChanged(ComponentInstance instance, Map<String, Object> configuration) {
            throw new RuntimeException("I'm bad");
        }
    }

    /**
     * A ConfigurationListener that just appends its arguments.
     */
    private class AppendingListener implements ConfigurationListener {
        public void configurationChanged(ComponentInstance instance, Map<String, Object> configuration) {
            instances.add(instance);
            configs.add(configuration);
        }
    }

    /**
     * Test that the listeners registered on the tested instance are called when the instance is reconfigured.
     */
    @Test
    public void testConfigurationListener() {
        // Register listeners
        ConfigurationListener l1 = new CountingListener();
        fooConfig.addListener(l1);
        ConfigurationListener l2 = new ThrowingListener();
        fooConfig.addListener(l2);
        ConfigurationListener l3 = new AppendingListener();
        fooConfig.addListener(l3);

        // Trigger a manual reconfiguration
        Hashtable<String, Object> conf = new Hashtable<String, Object>();
        conf.put("int", 40);
        conf.put("boolean", true);
        conf.put("string", "saloon");
        conf.put("strAProp", new String[]{"bar", "bad"});
        conf.put("intAProp", new int[]{21, 22, 23});
        fooProvider.reconfigure(conf);

        // Check the listeners has been called + check the arguments.
        assertEquals(1, updates);

        assertEquals(1, instances.size());
        assertSame(fooProvider, instances.get(0));

        assertEquals(1, configs.size());
        Map<String, Object> configMap = configs.get(0);
        assertEquals(5, configMap.size());
        assertEquals(40, configMap.get("int"));
        assertEquals(true, configMap.get("boolean"));
        assertEquals("saloon", configMap.get("string"));
        assertArrayEquals(new String[]{"bar", "bad"}, (String[]) configMap.get("strAProp"));
        assertArrayEquals(new int[]{21, 22, 23}, (int[]) configMap.get("intAProp"));

        // Trigger a POJO internal "reconfiguration".
        // It should not trigger any reconfiguration event.
        foo.foo();

        // Check nothing has changed in the listeners
        assertEquals(1, updates);
        assertEquals(1, instances.size());
        assertEquals(1, configs.size());

        // Unregister the listeners
        fooConfig.removeListener(l1);
        fooConfig.removeListener(l2);
        fooConfig.removeListener(l3);

        // Trigger a manual reconfiguration
        conf.put("int", 40);
        conf.put("boolean", true);
        conf.put("string", "saloon");
        conf.put("strAProp", new String[]{"bar", "bad"});
        conf.put("intAProp", new int[]{21, 22, 23});
        fooProvider.reconfigure(conf);

        // Check nothing has changed in the listeners, since they are all removed
        assertEquals(1, updates);
        assertEquals(1, instances.size());
        assertEquals(1, configs.size());
    }

    @Test(expected = NullPointerException.class)
    public void testNullProvidedServiceListener() {
        // Should fail!
        fooConfig.addListener(null);
    }

    /**
     * This test was initially expecting a NoSuchElementException, in 1.11.2, we changed the method to ignore missing
     * listeners.
     */
    @Test
    public void testRemoveNonexistentProvidedServiceListener() {
        fooConfig.removeListener(new ThrowingListener());
    }
}
