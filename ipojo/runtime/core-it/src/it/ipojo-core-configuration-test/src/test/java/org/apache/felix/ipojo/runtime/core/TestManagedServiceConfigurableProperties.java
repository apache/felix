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
import org.apache.felix.ipojo.PrimitiveHandler;
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

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;


public class TestManagedServiceConfigurableProperties extends Common {


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
        osgiHelper = new OSGiHelper(bc);
        ipojoHelper = new IPOJOHelper(bc);

        cleanupConfigurationAdmin();

        String type = "CONFIG-FooProviderType-4";
        Hashtable<String, String> p = new Hashtable<String, String>();
        p.put("instance.name", "instance");
        p.put("foo", "foo");
        p.put("bar", "2");
        p.put("baz", "baz");
        instance1 = ipojoHelper.createComponentInstance(type, p);
        assertEquals("instance1 created", ComponentInstance.VALID, instance1.getState());

        type = "CONFIG-FooProviderType-3";
        Hashtable<String, String> p1 = new Hashtable<String, String>();
        p1.put("instance.name", "instance-2");
        p1.put("foo", "foo");
        p1.put("bar", "2");
        p1.put("baz", "baz");
        p1.put("managed.service.pid", "instance");
        instance2 = ipojoHelper.createComponentInstance(type, p1);

        type = "CONFIG-FooProviderType-3";
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

    private void cleanupConfigurationAdmin() {
        ConfigurationAdmin admin = (ConfigurationAdmin) osgiHelper.getServiceObject(ConfigurationAdmin.class.getName
                (), null);
        assertNotNull("Check configuration admin availability", admin);
        try {
            Configuration[] configurations = admin.listConfigurations(null);
            for (int i = 0; configurations != null && i < configurations.length; i++) {
                configurations[i].delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testStaticInstance1() {
        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check FS availability", fooRef);
        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");
        assertEquals("Check foo equality -1", fooP, "foo");
        assertEquals("Check bar equality -1", barP, new Integer(2));
        assertEquals("Check baz equality -1", bazP, "baz");

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(
                ManagedService.class.getName(), "FooProvider-3");
        assertNotNull("Check ManagedServiceFactory availability", msRef);

        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("bar", new Integer(2));
        conf.put("foo", "foo");
        ManagedService ms = (ManagedService) osgiHelper.getContext().getService(msRef);
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
        osgiHelper.getContext().ungetService(msRef);
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

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(ManagedService.class.getName(), "instance");
        assertNotNull("Check ManagedService availability", msRef);


        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("bar", new Integer(2));
        conf.put("foo", "foo");
        ManagedService ms = (ManagedService) osgiHelper.getContext().getService(msRef);
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
        osgiHelper.getContext().ungetService(fooRef);
        osgiHelper.getContext().ungetService(msRef);
    }

    @Test
    public void testStaticInstance3() {
        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance3.getInstanceName());
        assertNotNull("Check FS availability", fooRef);
        Object fooP = fooRef.getProperty("foo");
        Object barP = fooRef.getProperty("bar");
        Object bazP = fooRef.getProperty("baz");
        // No values ... no properties.
        assertEquals("Check foo equality -1", fooP, null);
        assertEquals("Check bar equality -1", barP, null);
        assertEquals("Check baz equality -1", bazP, null);

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(ManagedService.class.getName(), "instance-3");
        assertNotNull("Check ManagedService availability", msRef);


        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("bar", new Integer(2));
        conf.put("foo", "foo");
        ManagedService ms = (ManagedService) osgiHelper.getContext().getService(msRef);
        try {
            ms.updated(conf);
        } catch (ConfigurationException e) {
            fail("Configuration Exception : " + e);
        }

        // Recheck props
        fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance3.getInstanceName());
        fooP = (String) fooRef.getProperty("foo");
        barP = (Integer) fooRef.getProperty("bar");
        bazP = (String) fooRef.getProperty("baz");
        assertEquals("Check foo equality -2", fooP, "foo");
        assertEquals("Check bar equality -2", barP, new Integer(2));
        assertEquals("Check baz equality -2", bazP, "zab");
        osgiHelper.getContext().ungetService(fooRef);
        osgiHelper.getContext().ungetService(msRef);
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
        ManagedService ms = (ManagedService) osgiHelper.getContext().getService(msRef);
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
        FooService fs = (FooService) osgiHelper.getContext().getService(fooRef);
        Properties p = fs.fooProps();
        fooP = (String) p.get("foo");
        barP = (Integer) p.get("bar");

        assertEquals("Check foo field equality", fooP, "oof");
        assertEquals("Check bar field equality", barP, new Integer(0));

        osgiHelper.getContext().ungetService(fooRef);
        osgiHelper.getContext().ungetService(msRef);
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

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(ManagedService.class.getName(), "instance");
        assertNotNull("Check ManagedServiceFactory availability", msRef);

        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("foo", "oof");
        conf.put("bar", new Integer(0));
        ManagedService ms = (ManagedService) osgiHelper.getContext().getService(msRef);
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
        FooService fs = (FooService) osgiHelper.getContext().getService(fooRef);
        Properties p = fs.fooProps();
        fooP = (String) p.get("foo");
        barP = (Integer) p.get("bar");

        assertEquals("Check foo field equality", fooP, "oof");
        assertEquals("Check bar field equality", barP, new Integer(0));

        osgiHelper.getContext().ungetService(fooRef);
        osgiHelper.getContext().ungetService(msRef);
    }

    @Test
    public void testDynamicInstance3() {
        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance3.getInstanceName());
        assertNotNull("Check FS availability", fooRef);

        Object fooP = fooRef.getProperty("foo");
        Object barP = fooRef.getProperty("bar");
        Object bazP = fooRef.getProperty("baz");
        // No values ... no properties.
        assertEquals("Check foo equality", fooP, null);
        assertEquals("Check bar equality", barP, null);
        assertEquals("Check baz equality", bazP, null);

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(ManagedService.class.getName(), "instance-3");
        assertNotNull("Check ManagedServiceFactory availability", msRef);

        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("foo", "oof");
        conf.put("bar", new Integer(0));
        ManagedService ms = (ManagedService) osgiHelper.getContext().getService(msRef);
        try {
            ms.updated(conf);
        } catch (ConfigurationException e) {
            fail("Configuration Exception : " + e);
        }

        // Recheck props
        fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance3.getInstanceName());
        fooP = (String) fooRef.getProperty("foo");
        barP = (Integer) fooRef.getProperty("bar");
        bazP = (String) fooRef.getProperty("baz");

        assertEquals("Check foo equality", fooP, "oof");
        assertEquals("Check bar equality", barP, new Integer(0));
        assertEquals("Check baz equality", bazP, "zab");

        // Check field value
        FooService fs = (FooService) osgiHelper.getContext().getService(fooRef);
        Properties p = fs.fooProps();
        fooP = (String) p.get("foo");
        barP = (Integer) p.get("bar");

        assertEquals("Check foo field equality", fooP, "oof");
        assertEquals("Check bar field equality", barP, new Integer(0));

        osgiHelper.getContext().ungetService(fooRef);
        osgiHelper.getContext().ungetService(msRef);
    }

    @Test
    public void testDynamicStringInstance1() {
        assertEquals("Check instance1 state", ComponentInstance.VALID, instance1.getState());
        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check FS availability", fooRef);

        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");

        assertEquals("Check foo equality - 1", fooP, "foo");
        assertEquals("Check bar equality - 1", barP, new Integer(2));
        assertEquals("Check baz equality - 1", bazP, "baz");

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(ManagedService.class.getName(), "FooProvider-3");
        assertNotNull("Check ManagedService availability", msRef);

        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("foo", "oof");
        conf.put("bar", "0");
        assertEquals("Check instance1 state (2)", ComponentInstance.VALID, instance1.getState());
        ManagedService ms = (ManagedService) osgiHelper.getContext().getService(msRef);

        PrimitiveHandler ph = (PrimitiveHandler) ms;
        assertSame("Check the correct instance", ph.getInstanceManager(), instance1);

        try {
            ms.updated(conf);
        } catch (ConfigurationException e) {
            fail("Configuration Exception : " + e);
        }
        assertEquals("Check instance1 state (3)", ComponentInstance.VALID, instance1.getState());

        // Recheck props
        fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance1.getInstanceName());
        fooP = (String) fooRef.getProperty("foo");
        barP = (Integer) fooRef.getProperty("bar");
        bazP = (String) fooRef.getProperty("baz");

        assertEquals("Check foo equality - 2", fooP, "oof");
        assertEquals("Check bar equality - 2", barP, new Integer(0));
        assertEquals("Check baz equality - 2", bazP, "zab");

        // Check field value
        FooService fs = (FooService) osgiHelper.getContext().getService(fooRef);
        Properties p = fs.fooProps();
        fooP = (String) p.get("foo");
        barP = (Integer) p.get("bar");

        assertEquals("Check foo field equality", fooP, "oof");
        assertEquals("Check bar field equality", barP, new Integer(0));

        osgiHelper.getContext().ungetService(fooRef);
        osgiHelper.getContext().ungetService(msRef);
    }

    @Test
    public void testDynamicStringInstance2() {
        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check FS availability", fooRef);

        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");

        assertEquals("Check foo equality", fooP, "foo");
        assertEquals("Check bar equality", barP, new Integer(2));
        assertEquals("Check baz equality", bazP, "baz");

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(ManagedService.class.getName(), "instance");
        assertNotNull("Check ManagedService availability", msRef);

        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("foo", "oof");
        conf.put("bar", "0");
        ManagedService ms = (ManagedService) osgiHelper.getContext().getService(msRef);
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
        FooService fs = (FooService) osgiHelper.getContext().getService(fooRef);
        Properties p = fs.fooProps();
        fooP = (String) p.get("foo");
        barP = (Integer) p.get("bar");

        assertEquals("Check foo field equality", fooP, "oof");
        assertEquals("Check bar field equality", barP, new Integer(0));

        osgiHelper.getContext().ungetService(fooRef);
        osgiHelper.getContext().ungetService(msRef);
    }

    @Test
    public void testPropagationInstance1() {
        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check FS availability", fooRef);

        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");

        assertEquals("Check foo equality", fooP, "foo");
        assertEquals("Check bar equality", barP, new Integer(2));
        assertEquals("Check baz equality", bazP, "baz");

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(ManagedService.class.getName(), "FooProvider-3");
        assertNotNull("Check ManagedService availability", msRef);

        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("foo", "foo");
        conf.put("bar", new Integer(2));
        conf.put("propagated1", "propagated");
        conf.put("propagated2", new Integer(1));
        ManagedService ms = (ManagedService) osgiHelper.getContext().getService(msRef);
        try {
            ms.updated(conf);
        } catch (ConfigurationException e) {
            fail("Configuration Exception : " + e);
        }

        // Recheck props
        fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance1.getInstanceName());
        fooP = (String) fooRef.getProperty("foo");
        barP = (Integer) fooRef.getProperty("bar");
        bazP = (String) fooRef.getProperty("baz");
        assertNotNull("Check the propagated1 existency", fooRef.getProperty("propagated1"));
        String prop1 = (String) fooRef.getProperty("propagated1");
        assertNotNull("Check the propagated2 existency", fooRef.getProperty("propagated2"));
        Integer prop2 = (Integer) fooRef.getProperty("propagated2");

        assertEquals("Check foo equality", fooP, "foo");
        assertEquals("Check bar equality", barP, new Integer(2));
        assertEquals("Check baz equality", bazP, "zab");
        assertEquals("Check propagated1 equality", prop1, "propagated");
        assertEquals("Check propagated2 equality", prop2, new Integer(1));

        osgiHelper.getContext().ungetService(msRef);
    }

    @Test
    public void testPropagationInstance2() {
        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check FS availability", fooRef);

        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");

        assertEquals("Check foo equality", fooP, "foo");
        assertEquals("Check bar equality", barP, new Integer(2));
        assertEquals("Check baz equality", bazP, "baz");

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(ManagedService.class.getName(), "instance");
        assertNotNull("Check ManagedService availability", msRef);

        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("foo", "foo");
        conf.put("bar", new Integer(2));
        conf.put("propagated1", "propagated");
        conf.put("propagated2", new Integer(1));
        ManagedService ms = (ManagedService) osgiHelper.getContext().getService(msRef);
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
        assertNotNull("Check the propagated1 existency", fooRef.getProperty("propagated1"));
        String prop1 = (String) fooRef.getProperty("propagated1");
        assertNotNull("Check the propagated2 existency", fooRef.getProperty("propagated2"));
        Integer prop2 = (Integer) fooRef.getProperty("propagated2");

        assertEquals("Check foo equality", fooP, "foo");
        assertEquals("Check bar equality", barP, new Integer(2));
        assertEquals("Check baz equality", bazP, "zab");
        assertEquals("Check propagated1 equality", prop1, "propagated");
        assertEquals("Check propagated2 equality", prop2, new Integer(1));

        osgiHelper.getContext().ungetService(msRef);
    }

    @Test
    public void testPropagationInstance3() {
        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance3.getInstanceName());
        assertNotNull("Check FS availability", fooRef);

        Object fooP = fooRef.getProperty("foo");
        Object barP = fooRef.getProperty("bar");
        Object bazP = fooRef.getProperty("baz");

        assertEquals("Check foo equality", fooP, null);
        assertEquals("Check bar equality", barP, null);
        assertEquals("Check baz equality", bazP, null);

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(ManagedService.class.getName(), "instance-3");
        assertNotNull("Check ManagedService availability", msRef);

        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("foo", "foo");
        conf.put("bar", new Integer(2));
        conf.put("propagated1", "propagated");
        conf.put("propagated2", new Integer(1));
        ManagedService ms = (ManagedService) osgiHelper.getContext().getService(msRef);
        try {
            ms.updated(conf);
        } catch (ConfigurationException e) {
            fail("Configuration Exception : " + e);
        }

        // Recheck props
        fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance3.getInstanceName());
        fooP = (String) fooRef.getProperty("foo");
        barP = (Integer) fooRef.getProperty("bar");
        bazP = (String) fooRef.getProperty("baz");
        assertNotNull("Check the propagated1 existency", fooRef.getProperty("propagated1"));
        String prop1 = (String) fooRef.getProperty("propagated1");
        assertNotNull("Check the propagated2 existency", fooRef.getProperty("propagated2"));
        Integer prop2 = (Integer) fooRef.getProperty("propagated2");

        assertEquals("Check foo equality", fooP, "foo");
        assertEquals("Check bar equality", barP, new Integer(2));
        assertEquals("Check baz equality", bazP, "zab");
        assertEquals("Check propagated1 equality", prop1, "propagated");
        assertEquals("Check propagated2 equality", prop2, new Integer(1));

        osgiHelper.getContext().ungetService(msRef);
    }


}
