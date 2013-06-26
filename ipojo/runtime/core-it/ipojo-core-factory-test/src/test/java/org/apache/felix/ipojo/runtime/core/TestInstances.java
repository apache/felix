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
package org.apache.felix.ipojo.runtime.core;


import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.junit.Test;

import java.util.Dictionary;
import java.util.Hashtable;

import static junit.framework.Assert.*;

/**
 * Checks the retrieval of instances and instance names from the Factory.
 */
public class TestInstances extends Common {

    @Test
    public void testNoInstances() {
        Factory factory = ipojoHelper.getFactory("NoInstances");
        assertNotNull(factory);

        assertNotNull(factory.getInstancesNames());
        assertNotNull(factory.getInstances());

        assertEquals(0, factory.getInstancesNames().size());
        assertEquals(0, factory.getInstances().size());
    }

    @Test
    public void testOneDuplicateInstance() {
        Factory factory = ipojoHelper.getFactory("OneDuplicateInstance");
        assertNotNull(factory);

        // Create one instance
        ComponentInstance instance = createInstanceOrFail(factory, "OneDuplicateInstance-0");

        assertNotNull(factory.getInstancesNames());
        assertNotNull(factory.getInstances());

        assertEquals(1, factory.getInstancesNames().size());
        assertEquals(1, factory.getInstances().size());

        assertContains("", factory.getInstancesNames().toArray(new String[1]), "OneDuplicateInstance-0");

        // Create another instance with the same name
        // The instance should not be created, and we should have only the first instance
        try {
            createInstance(factory, "OneDuplicateInstance-0");
            fail("The instance OneDuplicateInstance-0 has been created with factory " + factory.getName() + ". "
                    + "It's shouldn't have been created has it's a duplicate instance");
        } catch (UnacceptableConfiguration e) {
            // expected Exception
        } catch (Exception e){
            fail("Cannot create instance OneDuplicateInstance-0 with factory " + factory.getName() + ". "
                    + "The wrong exception has been raised : " + e.toString());
        }
        assertNotNull(factory.getInstancesNames());
        assertNotNull(factory.getInstances());

        assertEquals(1, factory.getInstancesNames().size());
        assertEquals(1, factory.getInstances().size());

        // Dispose of the instance
        instance.dispose();

        assertNotNull(factory.getInstancesNames());
        assertNotNull(factory.getInstances());

        assertEquals(0, factory.getInstancesNames().size());
        assertEquals(0, factory.getInstances().size());
    }

    @Test
    public void testFiveInstances() {
        Factory factory = ipojoHelper.getFactory("FiveInstances");
        assertNotNull(factory);

        // No instances for the moment
        assertNotNull(factory.getInstancesNames());
        assertNotNull(factory.getInstances());

        assertEquals(0, factory.getInstancesNames().size());
        assertEquals(0, factory.getInstances().size());

        // Create 5 instances
        ComponentInstance[] instances = createNInstanceOrFail(factory, "FiveInstances", 5);

        assertNotNull(factory.getInstancesNames());
        assertNotNull(factory.getInstances());

        assertEquals(5, factory.getInstancesNames().size());
        assertEquals(5, factory.getInstances().size());

        assertContains("", factory.getInstancesNames().toArray(new String[5]), "FiveInstances-0");
        assertContains("", factory.getInstancesNames().toArray(new String[5]), "FiveInstances-1");
        assertContains("", factory.getInstancesNames().toArray(new String[5]), "FiveInstances-2");
        assertContains("", factory.getInstancesNames().toArray(new String[5]), "FiveInstances-3");
        assertContains("", factory.getInstancesNames().toArray(new String[5]), "FiveInstances-4");

        // Dispose of instances 0, 3 and 2
        // 2 instances left
        instances[0].dispose();
        instances[3].dispose();
        instances[2].dispose();


        assertNotNull(factory.getInstancesNames());
        assertNotNull(factory.getInstances());

        assertEquals(2, factory.getInstancesNames().size());
        assertEquals(2, factory.getInstances().size());

        assertContains("", factory.getInstancesNames().toArray(new String[2]), "FiveInstances-1");
        assertContains("", factory.getInstancesNames().toArray(new String[2]), "FiveInstances-4");

        // Dispose of instances 1 and 4
        // No instances left
        instances[1].dispose();
        instances[4].dispose();

        assertNotNull(factory.getInstancesNames());
        assertNotNull(factory.getInstances());

        assertEquals(0, factory.getInstancesNames().size());
        assertEquals(0, factory.getInstances().size());
    }

    public ComponentInstance[] createNInstanceOrFail(Factory factory, String baseName, Integer n) {
        ComponentInstance[] instances = new ComponentInstance[n];
        for (int i = 0; i < n; i++) {
            instances[i] = createInstanceOrFail(factory, baseName + "-" + i);
        }
        return instances;
    }

    public ComponentInstance createInstanceOrFail(Factory factory, String name) {
        ComponentInstance instance = null;
        try {
            instance =  createInstance(factory, name);
        } catch (Exception e) {
            fail("Cannot create instance " + name + " with factory " + factory.getName() + ". "
                    + "Raised exception : " + e.toString());
        }
        return instance;
    }

    public ComponentInstance createInstance(Factory factory, String name)
            throws MissingHandlerException, UnacceptableConfiguration, ConfigurationException {
        Dictionary conf = new Hashtable();
        conf.put("instance.name", name);
        return factory.createComponentInstance(conf);
    }

}
