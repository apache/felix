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

import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Check configuration admin reconfiguration.
 */
public class TestConfigAdmin extends Common {

    /**
     * Check creation.
     */
    @Test
    public void testCreation() throws IOException, InterruptedException {
        ConfigurationAdmin admin = osgiHelper.getServiceObject(ConfigurationAdmin.class, null);
        Configuration conf = admin.createFactoryConfiguration("Factories-FooProviderType-2", "?");

        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put("int", 3);
        p.put("long", (long) 42);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        conf.update(p);
        grace();

        String pid = conf.getPid();
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), pid);
        assertNotNull("Check instance creation", ref);

        // Deletion of the configuration
        conf.delete();
        grace();

        boolean av = ipojoHelper.isServiceAvailableByName(FooService.class.getName(), pid);
        assertFalse("Check instance deletion", av);
    }

    /**
     * Check creation (push String).
     */
    @Test
    public void testCreationString() throws IOException, InterruptedException {
        ConfigurationAdmin admin = osgiHelper.getServiceObject(ConfigurationAdmin.class, null);
        Configuration conf = admin.createFactoryConfiguration("Factories-FooProviderType-2", "?");

        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put("int", "3");
        p.put("long", "42");
        p.put("string", "absdir");
        p.put("strAProp", "{a}");
        p.put("intAProp", "{1,2}");

        conf.update(p);
        grace();

        String pid = conf.getPid();
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), pid);

        assertNotNull("Check instance creation", ref);

        conf.delete();
        grace();

        boolean av = ipojoHelper.isServiceAvailableByName(FooService.class.getName(), pid);
        assertFalse("Check instance deletion", av);
    }

    /**
     * Check update and delete.
     */
    @Test
    public void testUpdate() throws IOException, InterruptedException {
        ConfigurationAdmin admin = osgiHelper.getServiceObject(ConfigurationAdmin.class, null);
        Configuration conf = admin.createFactoryConfiguration("Factories-FooProviderType-2", "?");

        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put("int", 3);
        p.put("long", (long) 42);
        p.put("string", "absdir");
        p.put("strAProp", new String[]{"a"});
        p.put("intAProp", new int[]{1, 2});

        conf.update(p);
        grace();

        String pid = conf.getPid();
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), pid);

        assertNotNull("Check instance creation", ref);

        p.put("int", 4);
        conf.update(p);
        grace();

        ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), pid);
        Integer test = (Integer) ref.getProperty("int");
        assertEquals("Check instance modification", 4, test.intValue());

        conf.delete();
        grace();

        boolean av = ipojoHelper.isServiceAvailableByName(FooService.class.getName(), pid);
        assertFalse("Check instance deletion", av);
    }

    /**
     * Check update and delete.
     * (Push String).
     */
    @Test
    public void testUpdateString() throws IOException, InterruptedException {
        ConfigurationAdmin admin = osgiHelper.getServiceObject(ConfigurationAdmin.class, null);
        Configuration conf = admin.createFactoryConfiguration("Factories-FooProviderType-2", "?");

        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put("int", "3");
        p.put("long", "42");
        p.put("string", "absdir");
        p.put("strAProp", "{a}");
        p.put("intAProp", "{1,2}");

        conf.update(p);
        grace();

        String pid = conf.getPid();

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), pid);
        assertNotNull("Check instance creation", ref);
        p.put("int", new Integer("4"));
        conf.update(p);
        grace();

        ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), pid);
        Integer test = (Integer) ref.getProperty("int");
        assertEquals("Check instance modification", 4, test.intValue());
        conf.delete();
        grace();

        boolean av = ipojoHelper.isServiceAvailableByName(FooService.class.getName(), pid);
        assertFalse("Check instance deletion", av);
    }

}
