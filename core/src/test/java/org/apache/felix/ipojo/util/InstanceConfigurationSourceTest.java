/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.util;

import org.apache.felix.ipojo.ContextListener;
import org.apache.felix.ipojo.ContextSource;
import org.junit.After;
import org.junit.Test;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the instance configuration context source.
 */
public class InstanceConfigurationSourceTest {

    @Test
    public void getProperties() {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("prop1", "value");
        configuration.put("prop2", 1);
        InstanceConfigurationSource cs = new InstanceConfigurationSource(configuration);
        assertEquals(cs.getContext().get("prop1"), "value");
        assertEquals(cs.getContext().get("prop2"), 1);
    }

    @Test
    public void getProperty() {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("prop1", "value");
        configuration.put("prop2", 1);
        InstanceConfigurationSource cs = new InstanceConfigurationSource(configuration);
        assertEquals(cs.getProperty("prop1"), "value");
        assertEquals(cs.getProperty("prop2"), 1);
    }

    @Test
    public void getMissingProperty() {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("prop1", "value");
        configuration.put("prop2", 1);
        InstanceConfigurationSource cs = new InstanceConfigurationSource(configuration);
        assertNull(cs.getProperty("__property"));
    }

    @Test
    public void emptyConfiguration() {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        InstanceConfigurationSource cs = new InstanceConfigurationSource(configuration);
        assertNull(cs.getProperty("__property"));
    }

    @Test
    public void addPropertyOnConfiguration() {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("prop1", "value");
        InstanceConfigurationSource cs = new InstanceConfigurationSource(configuration);

        SpyContextListener spy = new SpyContextListener();
        cs.registerContextListener(spy, null);

        Dictionary<String, Object> newConfiguration = new Hashtable<String, Object>();
        newConfiguration.put("prop1", "value");
        newConfiguration.put("prop2", "value2");

        cs.reconfigure(newConfiguration);

        assertEquals(spy.variables.size(), 1);
        assertEquals(spy.variables.get("prop2"), "value2");
    }

    @Test
    public void removePropertyOnConfiguration() {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("prop1", "value");
        configuration.put("prop2", "value2");
        InstanceConfigurationSource cs = new InstanceConfigurationSource(configuration);

        SpyContextListener spy = new SpyContextListener();
        cs.registerContextListener(spy, null);

        Dictionary<String, Object> newConfiguration = new Hashtable<String, Object>();
        newConfiguration.put("prop1", "value");

        cs.reconfigure(newConfiguration);

        assertEquals(spy.variables.size(), 1);
        assertTrue(spy.variables.containsKey("prop2"));
        assertEquals(spy.variables.get("prop2"), null);
    }

    @Test
    public void updatePropertyOnConfiguration() {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("prop1", "value");
        configuration.put("prop2", "value2");
        InstanceConfigurationSource cs = new InstanceConfigurationSource(configuration);

        SpyContextListener spy = new SpyContextListener();
        cs.registerContextListener(spy, null);

        Dictionary<String, Object> newConfiguration = new Hashtable<String, Object>();
        newConfiguration.put("prop1", "new value");
        newConfiguration.put("prop2", "value2");

        cs.reconfigure(newConfiguration);

        assertEquals(spy.variables.size(), 1);
        assertEquals(spy.variables.get("prop1"), "new value");
    }

    @Test
    public void add_remove_update() {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("prop1", "value");
        configuration.put("prop2", "value2");
        InstanceConfigurationSource cs = new InstanceConfigurationSource(configuration);

        SpyContextListener spy = new SpyContextListener();
        cs.registerContextListener(spy, null);

        Dictionary<String, Object> newConfiguration = new Hashtable<String, Object>();
        newConfiguration.put("prop1", "new value");
        newConfiguration.put("prop3", "value3");

        cs.reconfigure(newConfiguration);

        assertEquals(spy.variables.size(), 3);
        assertEquals(spy.variables.get("prop1"), "new value");
        assertEquals(spy.variables.get("prop3"), "value3");
        assertEquals(spy.variables.get("prop2"), null);

    }

    private class SpyContextListener implements ContextListener {

        Map<String, Object> variables = new HashMap<String, Object>();

        public void update(ContextSource source, String property, Object value) {
            variables.put(property, value);
        }
    }


}
