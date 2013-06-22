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
import org.apache.felix.ipojo.handlers.dependency.DependencyDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandlerDescription;
import org.apache.felix.ipojo.runtime.core.test.dependencies.Common;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Check the contextual filters using:
 * - a system properties.
 * - instance configuration (and reconfiguration)
 */
public class TestContextualFilters extends Common {

    @Before
    public void setup() {
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.context.Provider1");

        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.context.Provider2");
    }

    @After
    public void tearDown() {
        System.clearProperty("env.id");
    }

    @Test
    public void testContextualFilterWithSystemProperty() {
        // Set the system property.
        System.setProperty("env.id", "2");

        Properties configuration = new Properties();
        Properties filters = new Properties();
        filters.put("foo", "(id=${env.id})");
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
    public void testContextualFilterWithUnsetSystemProperty() {
        // Just to be sure.
        System.clearProperty("env.id");

        Properties configuration = new Properties();
        Properties filters = new Properties();
        filters.put("foo", "(id=${env.id})");
        configuration.put("requires.filters", filters);
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.context.ContextualFilterConsumer", configuration);

        assertTrue(instance.getState() == ComponentInstance.INVALID);
        DependencyHandlerDescription desc = (DependencyHandlerDescription) instance.getInstanceDescription().getHandlerDescription("org.apache.felix" +
                ".ipojo:requires");

        // Only one dependency.
        DependencyDescription dependency = desc.getDependencies()[0];
        assertEquals("(id=${env.id})", dependency.getFilter());
    }

    @Test
    public void testContextualFilterWithInstanceProperty() {
        Properties configuration = new Properties();
        Properties filters = new Properties();
        filters.put("foo", "(id=${instance.id})");
        configuration.put("requires.filters", filters);
        configuration.put("instance.id", 2);

        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.context.ContextualFilterConsumer", configuration);

        assertTrue(instance.getState() == ComponentInstance.VALID);
        DependencyHandlerDescription desc = (DependencyHandlerDescription) instance.getInstanceDescription().getHandlerDescription("org.apache.felix" +
                ".ipojo:requires");

        // Only one dependency.
        DependencyDescription dependency = desc.getDependencies()[0];
        assertEquals("(id=2)", dependency.getFilter());
    }

    /**
     * This test check filter set using instance properties.
     * However the instance is reconfigured a couple of times to illustrate the different case:
     * - unset property
     * - property updated
     * - reconfiguration that does not impact the filter
     */
    @Test
    public void testContextualFilterAndInstanceReconfiguration() {
        Properties configuration = new Properties();
        Properties filters = new Properties();
        filters.put("foo", "(id=${instance.id})");
        configuration.put("requires.filters", filters);
        configuration.put("instance.id", 2);

        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.context.ContextualFilterConsumer", configuration);

        assertTrue(instance.getState() == ComponentInstance.VALID);
        DependencyHandlerDescription desc = (DependencyHandlerDescription) instance.getInstanceDescription().getHandlerDescription("org.apache.felix" +
                ".ipojo:requires");

        // Only one dependency.
        DependencyDescription dependency = desc.getDependencies()[0];
        assertEquals("(id=2)", dependency.getFilter());

        // Reconfigure the instance.
        Properties newProps = new Properties();
        newProps.put("instance.id", "3");
        instance.reconfigure(newProps);

        assertTrue(instance.getState() == ComponentInstance.INVALID);
        desc = (DependencyHandlerDescription) instance.getInstanceDescription().getHandlerDescription("org.apache.felix" +
                ".ipojo:requires");

        // Only one dependency.
        dependency = desc.getDependencies()[0];
        assertEquals("(id=3)", dependency.getFilter());

        // Another reconfiguration (that does not affect the filters)
        newProps = new Properties();
        newProps.put("instance.id", "3");
        newProps.put("stuff", "stuff");
        instance.reconfigure(newProps);

        assertTrue(instance.getState() == ComponentInstance.INVALID);
        desc = (DependencyHandlerDescription) instance.getInstanceDescription().getHandlerDescription("org.apache.felix" +
                ".ipojo:requires");

        // Only one dependency.
        dependency = desc.getDependencies()[0];
        assertEquals("(id=3)", dependency.getFilter());

        // Yet another reconfiguration, un-setting instance.id
        newProps = new Properties();
        newProps.put("stuff", "stuff");
        instance.reconfigure(newProps);

        assertTrue(instance.getState() == ComponentInstance.INVALID);
        desc = (DependencyHandlerDescription) instance.getInstanceDescription().getHandlerDescription("org.apache.felix" +
                ".ipojo:requires");

        // Only one dependency.
        dependency = desc.getDependencies()[0];
        assertEquals("(id=${instance.id})", dependency.getFilter());

        // Finally another reconfiguration to build a fulfilled filter
        newProps = new Properties();
        newProps.put("instance.id", "1");
        newProps.put("stuff", "stuff");
        instance.reconfigure(newProps);

        assertTrue(instance.getState() == ComponentInstance.VALID);
        desc = (DependencyHandlerDescription) instance.getInstanceDescription().getHandlerDescription("org.apache.felix" +
                ".ipojo:requires");

        // Only one dependency.
        dependency = desc.getDependencies()[0];
        assertEquals("(id=1)", dependency.getFilter());
    }


}
