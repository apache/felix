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

import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;

/**
 * Test for FELIX-4172 Updated method called twice at the bundle start
 */
public class TestUpdated extends Common {

    private String factoryName = "org.apache.felix.ipojo.runtime.core.components.ImmediateConfigurableFooProvider";

    @Test
    public void testNumberOfUpdatedCalls() throws IOException {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("message", "message");
        props.put("propagated", "propagated");
        props.put(".private", "wow");

        Configuration configuration = admin.createFactoryConfiguration(factoryName, "?");
        configuration.update(props);

        FooService fs = osgiHelper.waitForService(FooService.class,
                "(instance.name=" + configuration.getPid() + ")",
                1000);

        assertEquals(fs.getInt(), 1);

        // Update the property
        props.put("message", "message2");
        props.put("propagated", "propagated2");
        props.put(".private", "wow2");
        configuration.update(props);

        grace();

        assertEquals(fs.getInt(), 2);

        // Remove a property
        props.remove("propagated");
        configuration.update(props);

        grace();

        assertEquals(fs.getInt(), 3);


        configuration.delete();
    }

    @Test
    public void testNumberOfUpdatedCallsWithManagedService() throws IOException {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("message", "message");
        props.put("propagated", "propagated");
        props.put(".private", "wow");
        props.put("managed.service.pid", "config");

        Configuration configuration = admin.createFactoryConfiguration(factoryName, "?");
        configuration.update(props);

        FooService fs = osgiHelper.waitForService(FooService.class,
                "(instance.name=" + configuration.getPid() + ")",
                1000);

        assertEquals(fs.getInt(), 1);

        // Update the property using the managed service.

        Configuration managedConfiguration = admin.getConfiguration("config", "?");
        props.put("message", "message2");
        props.put("propagated", "propagated2");
        props.put(".private", "wow2");
        managedConfiguration.update(props);

        grace();

        assertEquals(fs.getInt(), 2);

        // Remove a property
        props.remove("propagated");
        managedConfiguration.update(props);

        grace();

        assertEquals(fs.getInt(), 3);

        managedConfiguration.delete();
        configuration.delete();
    }


}
