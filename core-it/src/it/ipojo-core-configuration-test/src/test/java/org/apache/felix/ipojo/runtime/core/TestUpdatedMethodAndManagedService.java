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
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.extender.InstanceDeclaration;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotNull;


@ExamReactorStrategy(PerMethod.class)
public class TestUpdatedMethodAndManagedService extends Common {



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

    private void cleanupConfigurationAdmin() {
        ConfigurationAdmin admin = (ConfigurationAdmin) osgiHelper.getServiceObject(ConfigurationAdmin.class.getName
                (), null);
        assertNotNull("Check configuration admin availability", admin);
        try {
            int found = 0;
            Configuration[] configurations = admin.listConfigurations(null);
            for (int i = 0; configurations != null && i < configurations.length; i++) {
                System.out.println("Deleting configuration " + configurations[i].getPid());
                configurations[i].delete();
                found++;
            }

            // Wait the dispatching.
            Thread.sleep(found * 500);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void setUp() throws IOException {
        osgiHelper = new OSGiHelper(bc);
        ipojoHelper = new IPOJOHelper(bc);

        for (HandlerFactory handler : osgiHelper.getServiceObjects(HandlerFactory.class)) {
            System.out.println("handler : " + handler.getHandlerName() + " - " + handler.getState() + " - " + handler
                    .getMissingHandlers());
        }

        cleanupConfigurationAdmin();

        String type = "CONFIG-FooProviderType-4Updated";
        Hashtable<String, String> p = new Hashtable<String, String>();
        p.put("instance.name", "instance");
        p.put("foo", "foo");
        p.put("bar", "2");
        p.put("baz", "baz");
        instance1 = ipojoHelper.createComponentInstance(type, p);
        System.out.println(instance1.getInstanceDescription().getDescription());

        assertEquals("instance1 created", ComponentInstance.VALID, instance1.getState());

        System.out.println(instance1.getInstanceDescription().getDescription());

        type = "CONFIG-FooProviderType-3Updated";
        Hashtable<String, String> p1 = new Hashtable<String, String>();
        p1.put("instance.name", "instance-2");
        p1.put("foo", "foo");
        p1.put("bar", "2");
        p1.put("baz", "baz");
        p1.put("managed.service.pid", "instance");
        instance2 = ipojoHelper.createComponentInstance(type, p1);

        type = "CONFIG-FooProviderType-3Updated";
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
    public void testStaticInstance1() throws IOException {
        for (Architecture architecture : osgiHelper.getServiceObjects(Architecture.class)) {
            System.out.println(architecture.getInstanceDescription().getName() + " " + architecture
                    .getInstanceDescription().getState());
        }
        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check FS availability", fooRef);
        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");
        assertEquals("Check foo equality -1", fooP, "foo");
        assertEquals("Check bar equality -1", barP, new Integer(2));
        assertEquals("Check baz equality -1", bazP, "baz");

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(ManagedService.class.getName(),
                "FooProvider-3");
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
        Dictionary dict = (Dictionary) fs.fooProps().get("lastupdated");

        assertEquals("Check updated", 1, updated.intValue());
        assertEquals("Check last updated", 3, dict.size());

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

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(ManagedService.class.getName(),
                "instance");
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
        Dictionary dict = (Dictionary) fs.fooProps().get("lastupdated");

        assertEquals("Check updated", 1, updated.intValue());
        assertEquals("Check last updated", 3, dict.size());

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
        dict = (Dictionary) fs.fooProps().get("lastupdated");

        assertEquals("Check updated -2", 2, updated.intValue());
        assertEquals("Check last updated", 3, dict.size());
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

        ServiceReference msRef = osgiHelper.getServiceReferenceByPID(ManagedService.class.getName(),
                "FooProvider-3");
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
        Dictionary dict = (Dictionary) fs.fooProps().get("lastupdated");

        assertEquals("Check updated -1", 1, updated.intValue());
        assertEquals("Check last updated", 3, dict.size());

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
        dict = (Dictionary) fs.fooProps().get("lastupdated");

        assertEquals("Check updated -2", 2, updated.intValue());
        assertEquals("Check last updated", 3, dict.size());
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
        Dictionary dict = (Dictionary) fs.fooProps().get("lastupdated");

        assertEquals("Check updated", 1, updated.intValue());
        assertEquals("Check last updated", 3, dict.size());

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
        dict = (Dictionary) fs.fooProps().get("lastupdated");

        assertEquals("Check updated -2", 2, updated.intValue());
        assertEquals("Check last updated", 3, dict.size());

    }

    public static void dump(BundleContext bc, File output) throws IOException {
        if (!output.exists()) {
            output.mkdirs();
        }

        for (Bundle bundle : bc.getBundles()) {
            if (bundle.getBundleId() == 0) {
                continue;
            }
            System.out.println("Location : " + bundle.getLocation());
            if ("local".equals(bundle.getLocation())) {
                continue; // Pax Exam, when you hug me, I feel so...
            }
            URL location = new URL(bundle.getLocation());
            FileOutputStream outputStream = null;
            if (bundle.getVersion() != null) {
                outputStream = new FileOutputStream(new File(output,
                        bundle.getSymbolicName() + "-" + bundle.getVersion().toString() + ".jar"));
            } else {
                outputStream = new FileOutputStream(new File(output, bundle.getSymbolicName() + ".jar"));
            }

            int read = 0;
            byte[] bytes = new byte[1024];

            InputStream inputStream = location.openStream();
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            inputStream.close();
            outputStream.close();
        }
    }
}
