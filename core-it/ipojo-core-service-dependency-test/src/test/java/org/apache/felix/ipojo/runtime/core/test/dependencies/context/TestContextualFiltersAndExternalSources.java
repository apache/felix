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

package org.apache.felix.ipojo.runtime.core.test.dependencies.context;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ContextListener;
import org.apache.felix.ipojo.ContextSource;
import org.apache.felix.ipojo.handlers.dependency.DependencyDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandlerDescription;
import org.apache.felix.ipojo.runtime.core.test.dependencies.Common;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Check the contextual filters using external sources
 */
public class TestContextualFiltersAndExternalSources extends Common {

    private ServiceRegistration registration;
    private ServiceRegistration registration2;

    @Before
    public void setup() {
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.context.Provider1");

        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.context.Provider2");
    }

    @After
    public void tearDown() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }

        if (registration2 != null) {
            registration2.unregister();
            registration2 = null;
        }

    }

    @Test
    public void testContextualFilterUsingOneSource() {
        MyContextSource source = new MyContextSource();
        source.set("source.id", 2);
        registration = context.registerService(ContextSource.class.getName(),
                source, null);

        Properties configuration = new Properties();
        Properties filters = new Properties();
        filters.put("foo", "(id=${source.id})");
        configuration.put("requires.filters", filters);
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.context.ContextualFilterConsumer", configuration);

        assertTrue(instance.getState() == ComponentInstance.VALID);
        DependencyHandlerDescription desc = (DependencyHandlerDescription) instance.getInstanceDescription().getHandlerDescription("org.apache.felix" +
                ".ipojo:requires");

        // Only one dependency.
        DependencyDescription dependency = desc.getDependencies()[0];
        assertEquals("(id=2)", dependency.getFilter());
    }

    @Test
    public void testContextualFilterUsingOneSourceAppearingLater() {
        MyContextSource source = new MyContextSource();
        source.set("source.id", 2);

        Properties configuration = new Properties();
        Properties filters = new Properties();
        filters.put("foo", "(id=${source.id})");
        configuration.put("requires.filters", filters);
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.context.ContextualFilterConsumer", configuration);

        assertTrue(instance.getState() == ComponentInstance.INVALID);
        DependencyHandlerDescription desc = (DependencyHandlerDescription) instance.getInstanceDescription().getHandlerDescription("org.apache.felix" +
                ".ipojo:requires");

        // Only one dependency.
        DependencyDescription dependency = desc.getDependencies()[0];
        assertEquals("(id=${source.id})", dependency.getFilter());

        registration = context.registerService(ContextSource.class.getName(),
                source, null);

        assertTrue(instance.getState() == ComponentInstance.VALID);
        desc = (DependencyHandlerDescription) instance.getInstanceDescription().getHandlerDescription("org.apache.felix" +
                ".ipojo:requires");

        // Only one dependency.
        dependency = desc.getDependencies()[0];
        assertEquals("(id=2)", dependency.getFilter());
    }

    @Test
    public void testContextualFilterUsingOneSourceWithReconfiguration() {
        MyContextSource source = new MyContextSource();
        source.set("source.id", 2);
        registration = context.registerService(ContextSource.class.getName(),
                source, null);

        Properties configuration = new Properties();
        Properties filters = new Properties();
        filters.put("foo", "(id=${source.id})");
        configuration.put("requires.filters", filters);
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.context.ContextualFilterConsumer", configuration);

        assertTrue(instance.getState() == ComponentInstance.VALID);

        // Set id to 3 => INVALID
        source.set("source.id", 3);
        assertTrue(instance.getState() == ComponentInstance.INVALID);

        // Set id to null
        source.set("source.id", null);
        assertTrue(instance.getState() == ComponentInstance.INVALID);

        DependencyHandlerDescription desc = (DependencyHandlerDescription) instance.getInstanceDescription()
                .getHandlerDescription("org.apache.felix.ipojo:requires");
        DependencyDescription dependency = desc.getDependencies()[0];
        assertEquals("(id=${source.id})", dependency.getFilter());

        // Register a new source.
        MyContextSource source2 = new MyContextSource();
        source2.set("source.id", 2);
        registration2 = context.registerService(ContextSource.class.getName(),
                source2, null);
        assertTrue(instance.getState() == ComponentInstance.VALID);

        // This new source disappear
        registration2.unregister();
        registration2 = null;

        assertTrue(instance.getState() == ComponentInstance.INVALID);

        source.set("source.id", 1);
        assertTrue(instance.getState() == ComponentInstance.VALID);
    }

    private class MyContextSource implements ContextSource {

        List<ContextListener> listeners = new ArrayList<ContextListener>();
        Hashtable<String, Object> properties = new Hashtable<String, Object>();

        void set(String key, Object value) {
            if (value == null) {
                properties.remove(key);
            } else {
                properties.put(key, value);
            }
            for (ContextListener cl : listeners) {
                cl.update(this, key, value);
            }
        }

        @Override
        public Object getProperty(String property) {
            return properties.get(property);
        }

        @Override
        public Dictionary getContext() {
            return properties;
        }

        @Override
        public void registerContextListener(ContextListener listener, String[] properties) {
            listeners.add(listener);
        }

        @Override
        public void unregisterContextListener(ContextListener listener) {
            listeners.remove(listener);
        }
    }


}
