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
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotNull;


@ExamReactorStrategy(PerMethod.class)
public class TestUpdatedMethodAndConfigAdmin extends Common {

    ComponentInstance instance, instance2;

    @Before
    public void setUp() {
        osgiHelper = new OSGiHelper(bc);
        ipojoHelper = new IPOJOHelper(bc);

        cleanupConfigurationAdmin();

        String type = "CONFIG-FooProviderType-3Updated";

        Hashtable<String, String> p1 = new Hashtable<String, String>();
        p1.put("instance.name", "instance");
        p1.put("foo", "foo");
        p1.put("bar", "2");
        p1.put("baz", "baz");
        instance = ipojoHelper.createComponentInstance(type, p1);

        Hashtable<String, String> p2 = new Hashtable<String, String>();
        p2.put("instance.name", "instance2");

        instance2 = ipojoHelper.createComponentInstance(type, p2);
    }

    @After
    public void tearDown() {

        instance.dispose();
        instance2.dispose();
        instance2 = null;
        instance = null;
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
    public void testStatic() throws IOException, InterruptedException {

        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance.getInstanceName());
        assertNotNull("Check FS availability", fooRef);
        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");
        assertEquals("Check foo equality -1", fooP, "foo");
        assertEquals("Check bar equality -1", barP, new Integer(2));
        assertEquals("Check baz equality -1", bazP, "baz");

        ConfigurationAdmin admin = osgiHelper.getServiceObject(ConfigurationAdmin.class);
        assertNotNull("Check Configuration Admin availability", admin);

        Configuration configuration = admin.getConfiguration(instance.getInstanceName(),
                getTestBundle().getLocation());

        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("bar", new Integer(2));
        conf.put("foo", "foo");

        // Asynchronous dispatching of the configuration
        configuration.update(conf);
        grace();

        // Recheck props
        fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance.getInstanceName());
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
        System.out.println("Dictionary : " + dict);
        assertEquals("Check last updated", 3, dict.size()); // foo bar and baz as a service prooperties.
    }

    @Test
    public void testStaticNoValue() throws IOException, InterruptedException {
        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check FS availability", fooRef);
        Object fooP = fooRef.getProperty("foo");
        Object barP = fooRef.getProperty("bar");
        Object bazP = fooRef.getProperty("baz");
        assertEquals("Check foo equality -1", fooP, null);
        assertEquals("Check bar equality -1", barP, null);
        assertEquals("Check baz equality -1", bazP, null);

        ConfigurationAdmin admin = osgiHelper.getServiceObject(ConfigurationAdmin.class);
        assertNotNull("Check Configuration Admin availability", admin);

        Configuration configuration = admin.getConfiguration(instance2.getInstanceName(),
                getTestBundle().getLocation());

        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("bar", new Integer(2));
        conf.put("foo", "foo");

        // Asynchronous dispatching of the configuration
        configuration.update(conf);
        grace();

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
    }

    @Test
    public void testDynamic() throws IOException, InterruptedException {
        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance.getInstanceName());
        assertNotNull("Check FS availability", fooRef);

        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");

        assertEquals("Check foo equality", fooP, "foo");
        assertEquals("Check bar equality", barP, new Integer(2));
        assertEquals("Check baz equality", bazP, "baz");

        ConfigurationAdmin admin = osgiHelper.getServiceObject(ConfigurationAdmin.class);
        assertNotNull("Check Configuration Admin availability", admin);

        Configuration configuration = admin.getConfiguration(instance.getInstanceName(),
                getTestBundle().getLocation());

        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("foo", "oof");
        conf.put("bar", new Integer(0));

        // Asynchronous dispatching of the configuration
        configuration.update(conf);
        grace();

        // Recheck props
        fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance.getInstanceName());
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
    }

    @Test
    public void testDynamicNoValue() throws IOException, InterruptedException {
        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check FS availability", fooRef);

        Object fooP = fooRef.getProperty("foo");
        Object barP = fooRef.getProperty("bar");
        Object bazP = fooRef.getProperty("baz");
        assertEquals("Check foo equality -1", fooP, null);
        assertEquals("Check bar equality -1", barP, null);
        assertEquals("Check baz equality -1", bazP, null);

        ConfigurationAdmin admin = osgiHelper.getServiceObject(ConfigurationAdmin.class);
        assertNotNull("Check Configuration Admin availability", admin);

        Configuration configuration = admin.getConfiguration(instance2.getInstanceName(),
                getTestBundle().getLocation());

        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("foo", "oof");
        conf.put("bar", new Integer(0));

        // Asynchronous dispatching of the configuration
        configuration.update(conf);
        grace();

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

    }


    @Test
    public void testDynamicString() throws IOException, InterruptedException {
        ServiceReference fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance.getInstanceName());
        assertNotNull("Check FS availability", fooRef);

        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");

        assertEquals("Check foo equality", fooP, "foo");
        assertEquals("Check bar equality", barP, new Integer(2));
        assertEquals("Check baz equality", bazP, "baz");

        ConfigurationAdmin admin = osgiHelper.getServiceObject(ConfigurationAdmin.class);
        assertNotNull("Check Configuration Admin availability", admin);

        Configuration configuration = admin.getConfiguration(instance.getInstanceName(),
                getTestBundle().getLocation());

        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("foo", "oof");
        conf.put("bar", "0");

        // Asynchronous dispatching of the configuration
        configuration.update(conf);
        grace();

        // Recheck props
        fooRef = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), instance.getInstanceName());
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
    }

}
