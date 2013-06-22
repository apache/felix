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
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotNull;


public class TestUpdatedNoArgMethodAndManagedService extends Common {

    /**
     * Instance where the ManagedServicePID is provided by the component type.
     */
    ComponentInstance instance1;
    /**
     * Instance where the ManagedServicePID is provided by the instance.
     */
    ComponentInstance instance2;

    /**
     * Instance without configuration.
     */
    ComponentInstance instance3;

    @Before
    public void setUp() {
        String type = "CONFIG-FooProviderType-4Updated2";
        Hashtable<String, String> p = new Hashtable<String, String>();
        p.put("instance.name", "any-instance");
        p.put("foo", "foo");
        p.put("bar", "2");
        p.put("baz", "baz");
        instance1 = ipojoHelper.createComponentInstance(type, p);
        assertEquals("instance1 created", ComponentInstance.VALID, instance1.getState());

        type = "CONFIG-FooProviderType-3Updated2";
        Hashtable<String, String> p1 = new Hashtable<String, String>();
        p1.put("instance.name", "instance-2");
        p1.put("foo", "foo");
        p1.put("bar", "2");
        p1.put("baz", "baz");
        p1.put("managed.service.pid", "instanceMSP");
        instance2 = ipojoHelper.createComponentInstance(type, p1);

        type = "CONFIG-FooProviderType-3Updated2";
        Hashtable<String, String> p2 = new Hashtable<String, String>();
        p2.put("instance.name", "instance-3");
        p2.put("managed.service.pid", "instance-3");
        instance3 = ipojoHelper.createComponentInstance(type, p2);
    }

    @After
    public void tearDown() {
        instance1.dispose();
        instance2.dispose();
        instance3.dispose();
        instance1 = null;
        instance2 = null;
        instance3 = null;
    }

    @Test
    public void testStaticInstance1() {
        ServiceReference fooRef = osgiHelper.waitForService(FooService.class.getName(),
                "(instance.name=" + instance1.getInstanceName() + ")",
                5000);
        assertNotNull("Check FS availability", fooRef);
        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");
        assertEquals("Check foo equality -1", fooP, "foo");
        assertEquals("Check bar equality -1", barP, new Integer(2));
        assertEquals("Check baz equality -1", bazP, "baz");

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(ManagedService.class.getName(), "FooProvider-3");
        assertNotNull("Check ManagedServiceFactory availability", msRef);

        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("bar", new Integer(2));
        conf.put("foo", "foo");
        ManagedService ms = (ManagedService) osgiHelper.getServiceObject(msRef);
        try {
            ms.updated(conf);
        } catch (ConfigurationException e) {
            fail("Configuration Exception : " + e);
        }

        // Re-check props
        fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance1.getInstanceName());
        fooP = (String) fooRef.getProperty("foo");
        barP = (Integer) fooRef.getProperty("bar");
        bazP = (String) fooRef.getProperty("baz");
        assertEquals("Check foo equality -2", fooP, "foo");
        assertEquals("Check bar equality -2", barP, new Integer(2));
        assertEquals("Check baz equality -2", bazP, "zab");

        // Get Service
        FooService fs = (FooService) osgiHelper.getServiceObject(fooRef);
        Integer updated = (Integer) fs.fooProps().get("updated");
        assertEquals("Check updated", 1, updated.intValue());
    }

    @Test
    public void testStaticInstance2() {
        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check FS availability", fooRef);
        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");
        assertEquals("Check foo equality -1", fooP, "foo");
        assertEquals("Check bar equality -1", barP, new Integer(2));
        assertEquals("Check baz equality -1", bazP, "baz");

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(ManagedService.class.getName(), "instanceMSP");
        assertNotNull("Check ManagedService availability", msRef);


        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("bar", new Integer(2));
        conf.put("foo", "foo");
        ManagedService ms = (ManagedService) osgiHelper.getServiceObject(msRef);
        try {
            ms.updated(conf);
        } catch (ConfigurationException e) {
            fail("Configuration Exception : " + e);
        }

        // Recheck props
        fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance2.getInstanceName());
        fooP = (String) fooRef.getProperty("foo");
        barP = (Integer) fooRef.getProperty("bar");
        bazP = (String) fooRef.getProperty("baz");
        assertEquals("Check foo equality -2", fooP, "foo");
        assertEquals("Check bar equality -2", barP, new Integer(2));
        assertEquals("Check baz equality -2", bazP, "zab");

        // Get Service
        FooService fs = (FooService) osgiHelper.getServiceObject(fooRef);
        Integer updated = (Integer) fs.fooProps().get("updated");

        assertEquals("Check updated", 1, updated.intValue());

        conf.put("baz", "zab2");
        conf.put("foo", "oof2");
        conf.put("bar", new Integer(0));
        ms = (ManagedService) osgiHelper.getServiceObject(msRef);
        try {
            ms.updated(conf);
        } catch (ConfigurationException e) {
            fail("Configuration Exception : " + e);
        }

        updated = (Integer) fs.fooProps().get("updated");

        assertEquals("Check updated -2", 2, updated.intValue());
    }

    @Test
    public void testDynamicInstance1() {
        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check FS availability", fooRef);

        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");

        assertEquals("Check foo equality", fooP, "foo");
        assertEquals("Check bar equality", barP, new Integer(2));
        assertEquals("Check baz equality", bazP, "baz");

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(ManagedService.class.getName(), "FooProvider-3");
        assertNotNull("Check ManagedServiceFactory availability", msRef);

        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("foo", "oof");
        conf.put("bar", new Integer(0));
        ManagedService ms = (ManagedService) osgiHelper.getServiceObject(msRef);
        try {
            ms.updated(conf);
        } catch (ConfigurationException e) {
            fail("Configuration Exception : " + e);
        }

        // Re-check props
        fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance1.getInstanceName());
        fooP = (String) fooRef.getProperty("foo");
        barP = (Integer) fooRef.getProperty("bar");
        bazP = (String) fooRef.getProperty("baz");

        assertEquals("Check foo equality", fooP, "oof");
        assertEquals("Check bar equality", barP, new Integer(0));
        assertEquals("Check baz equality", bazP, "zab");

        // Check field value
        FooService fs = (FooService) osgiHelper.getServiceObject(fooRef);
        Properties p = fs.fooProps();
        fooP = (String) p.get("foo");
        barP = (Integer) p.get("bar");

        assertEquals("Check foo field equality", fooP, "oof");
        assertEquals("Check bar field equality", barP, new Integer(0));

        Integer updated = (Integer) fs.fooProps().get("updated");

        assertEquals("Check updated -1", 1, updated.intValue());

        conf.put("baz", "zab2");
        conf.put("foo", "oof2");
        conf.put("bar", new Integer(0));
        ms = (ManagedService) osgiHelper.getServiceObject(msRef);
        try {
            ms.updated(conf);
        } catch (ConfigurationException e) {
            fail("Configuration Exception : " + e);
        }

        updated = (Integer) fs.fooProps().get("updated");

        assertEquals("Check updated -2", 2, updated.intValue());

    }

    @Test
    public void testDynamicInstance2() {
        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check FS availability", fooRef);

        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");

        assertEquals("Check foo equality", fooP, "foo");
        assertEquals("Check bar equality", barP, new Integer(2));
        assertEquals("Check baz equality", bazP, "baz");

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(ManagedService.class.getName(), "instanceMSP");
        assertNotNull("Check ManagedServiceFactory availability", msRef);

        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("foo", "oof");
        conf.put("bar", new Integer(0));
        ManagedService ms = (ManagedService) osgiHelper.getServiceObject(msRef);
        try {
            ms.updated(conf);
        } catch (ConfigurationException e) {
            fail("Configuration Exception : " + e);
        }

        // Recheck props
        fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance2.getInstanceName());
        fooP = (String) fooRef.getProperty("foo");
        barP = (Integer) fooRef.getProperty("bar");
        bazP = (String) fooRef.getProperty("baz");

        assertEquals("Check foo equality", fooP, "oof");
        assertEquals("Check bar equality", barP, new Integer(0));
        assertEquals("Check baz equality", bazP, "zab");

        // Check field value
        FooService fs = (FooService) osgiHelper.getServiceObject(fooRef);
        Properties p = fs.fooProps();
        fooP = (String) p.get("foo");
        barP = (Integer) p.get("bar");

        assertEquals("Check foo field equality", fooP, "oof");
        assertEquals("Check bar field equality", barP, new Integer(0));

        Integer updated = (Integer) fs.fooProps().get("updated");

        assertEquals("Check updated", 1, updated.intValue());

        conf.put("baz", "zab2");
        conf.put("foo", "oof2");
        conf.put("bar", new Integer(0));
        ms = (ManagedService) osgiHelper.getServiceObject(msRef);
        try {
            ms.updated(conf);
        } catch (ConfigurationException e) {
            fail("Configuration Exception : " + e);
        }

        updated = (Integer) fs.fooProps().get("updated");

        assertEquals("Check updated -2", 2, updated.intValue());
    }
}
