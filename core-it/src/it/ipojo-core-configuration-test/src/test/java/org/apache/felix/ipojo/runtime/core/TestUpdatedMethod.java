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
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import static org.junit.Assert.*;


public class TestUpdatedMethod extends Common {



    ComponentInstance fooProvider1;
    ComponentInstance fooProvider2;
    ComponentInstance fooProvider3;



    @Before
    public void setUp() {
        osgiHelper = new OSGiHelper(bc);
        ipojoHelper = new IPOJOHelper(bc);
        String type = "CONFIG-FooProviderType-ConfUpdated";

        Hashtable<String, String> p1 = new Hashtable<String, String>();
        p1.put("instance.name", "FooProvider-1");
        fooProvider1 = ipojoHelper.createComponentInstance(type, p1);

        Properties p2 = new Properties();
        p2.put("instance.name", "FooProvider-2");
        p2.put("int", new Integer(4));
        p2.put("boolean", new Boolean(false));
        p2.put("string", new String("bar"));
        p2.put("strAProp", new String[]{"bar", "foo"});
        p2.put("intAProp", new int[]{1, 2, 3});
        fooProvider2 = ipojoHelper.createComponentInstance(bc.getBundle(), type, p2);

        Hashtable<String, String> p3 = new Hashtable<String, String>();
        p3.put("instance.name", "FooProvider-3");
        fooProvider3 = ipojoHelper.createComponentInstance("CONFIG-FooProviderType-ConfNoValueUpdated", p3);
    }

    @After
    public void tearDown() {

        fooProvider1.dispose();
        fooProvider2.dispose();
        fooProvider3.dispose();
        fooProvider1 = null;
        fooProvider2 = null;
        fooProvider3 = null;
    }

    @Test
    public void testComponentTypeConfiguration() {
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooProvider1.getInstanceName());
        assertNotNull("Check FooService availability", ref);
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);
        Properties toCheck = fs.fooProps();

        Integer intProp = (Integer) toCheck.get("intProp");
        Boolean boolProp = (Boolean) toCheck.get("boolProp");
        String strProp = (String) toCheck.get("strProp");
        String[] strAProp = (String[]) toCheck.get("strAProp");
        int[] intAProp = (int[]) toCheck.get("intAProp");

        // Check updated
        Integer updated = (Integer) toCheck.get("updated");
        Dictionary dict = (Dictionary) toCheck.get("lastupdated");

        assertEquals("Check intProp equality (1)", intProp, new Integer(2));
        assertEquals("Check longProp equality (1)", boolProp, new Boolean(false));
        assertEquals("Check strProp equality (1)", strProp, new String("foo"));
        assertNotNull("Check strAProp not nullity (1)", strAProp);
        String[] v = new String[]{"foo", "bar"};
        for (int i = 0; i < strAProp.length; i++) {
            if (!strAProp[i].equals(v[i])) {
                fail("Check the strAProp Equality (1) : " + strAProp[i] + " != " + v[i]);
            }
        }
        assertNotNull("Check intAProp not nullity", intAProp);
        int[] v2 = new int[]{1, 2, 3};
        for (int i = 0; i < intAProp.length; i++) {
            if (intAProp[i] != v2[i]) {
                fail("Check the intAProp Equality (1) : " + intAProp[i] + " != " + v2[i]);
            }
        }

        assertEquals("updated count ", 1, updated.intValue());
        assertEquals("Last updated", 5, dict.size());

        // change the field value
        assertTrue("Invoke the fs service", fs.foo());
        toCheck = fs.fooProps();


        //	Re-check the property (change)
        intProp = (Integer) toCheck.get("intProp");
        boolProp = (Boolean) toCheck.get("boolProp");
        strProp = (String) toCheck.get("strProp");
        strAProp = (String[]) toCheck.get("strAProp");
        intAProp = (int[]) toCheck.get("intAProp");

        // Check updated
        updated = (Integer) toCheck.get("updated");
        dict = (Dictionary) toCheck.get("lastupdated");

        assertEquals("Check intProp equality (2) (" + intProp + ")", intProp, new Integer(3));
        assertEquals("Check longProp equality (2)", boolProp, new Boolean(true));
        assertEquals("Check strProp equality (2)", strProp, new String("bar"));
        assertNotNull("Check strAProp not nullity (2)", strAProp);
        v = new String[]{"foo", "bar", "baz"};
        for (int i = 0; i < strAProp.length; i++) {
            if (!strAProp[i].equals(v[i])) {
                fail("Check the strAProp Equality (2)");
            }
        }
        assertNotNull("Check intAProp not nullity (2)", intAProp);
        v2 = new int[]{3, 2, 1};
        for (int i = 0; i < intAProp.length; i++) {
            if (intAProp[i] != v2[i]) {
                fail("Check the intAProp Equality (2) : " + intAProp[i] + " != " + v2[i]);
            }
        }

        // This does not reconfigure...
        assertEquals("updated count -2 ", 1, updated.intValue());
        assertEquals("Last update - 2", 5, dict.size());

        fs = null;
    }

    @Test
    public void testNoValue() {
        ServiceReference sr = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-3");
        assertNotNull("Check the availability of the FS service", sr);

        FooService fs = (FooService) osgiHelper.getServiceObject(sr);
        Properties toCheck = fs.fooProps();

        // Check service properties
        Integer intProp = (Integer) toCheck.get("intProp");
        Boolean boolProp = (Boolean) toCheck.get("boolProp");
        String strProp = (String) toCheck.get("strProp");
        String[] strAProp = (String[]) toCheck.get("strAProp");
        int[] intAProp = (int[]) toCheck.get("intAProp");

        // Check updated
        Integer updated = (Integer) toCheck.get("updated");
        Dictionary dict = (Dictionary) toCheck.get("lastupdated");

        assertEquals("Check intProp equality", intProp, new Integer(0));
        assertEquals("Check longProp equality", boolProp, new Boolean(false));
        assertEquals("Check strProp equality", strProp, null);
        assertNull("Check strAProp nullity", strAProp);
        assertNull("Check intAProp  nullity", intAProp);

        assertEquals("updated count ", 1, updated.intValue());
        assertEquals("Last update", 0, dict.size());

        assertTrue("invoke fs", fs.foo());
        toCheck = fs.fooProps();

        // Re-check the property (change)
        intProp = (Integer) toCheck.get("intProp");
        boolProp = (Boolean) toCheck.get("boolProp");
        strProp = (String) toCheck.get("strProp");
        strAProp = (String[]) toCheck.get("strAProp");
        intAProp = (int[]) toCheck.get("intAProp");

        updated = (Integer) toCheck.get("updated");
        dict = (Dictionary) toCheck.get("lastupdated");

        assertEquals("Check intProp equality", intProp, new Integer(3));
        assertEquals("Check longProp equality", boolProp, new Boolean(true));
        assertEquals("Check strProp equality", strProp, new String("bar"));
        assertNotNull("Check strAProp not nullity", strAProp);
        String[] v = new String[]{"foo", "bar", "baz"};
        for (int i = 0; i < strAProp.length; i++) {
            if (!strAProp[i].equals(v[i])) {
                fail("Check the strAProp Equality");
            }
        }
        assertNotNull("Check intAProp not nullity", intAProp);
        int[] v2 = new int[]{3, 2, 1};
        for (int i = 0; i < intAProp.length; i++) {
            if (intAProp[i] != v2[i]) {
                fail("Check the intAProp Equality");
            }
        }

        fs = null;
    }


}
