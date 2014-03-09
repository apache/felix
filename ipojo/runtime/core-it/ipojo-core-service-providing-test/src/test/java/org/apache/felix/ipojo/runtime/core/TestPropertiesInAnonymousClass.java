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
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

public class TestPropertiesInAnonymousClass extends Common {

    @Before
    public void setUp() {
        String type = "PS-FooProviderTypeAnonymous-Dyn";
        ipojoHelper.createComponentInstance(type, "FooProviderAno-1");

    }


    @Test
    public void testRunnable() {
        ServiceReference sr = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), "FooProviderAno-1");
        assertNotNull("Check the availability of the FS service", sr);

        // Check service properties
        Integer intProp = (Integer) sr.getProperty("int");
        Boolean boolProp = (Boolean) sr.getProperty("boolean");
        String strProp = (String) sr.getProperty("string");
        String[] strAProp = (String[]) sr.getProperty("strAProp");
        int[] intAProp = (int[]) sr.getProperty("intAProp");

        assertEquals("Check intProp equality (1)", intProp, new Integer(2));
        assertEquals("Check longProp equality (1)", boolProp, new Boolean(false));
        assertEquals("Check strProp equality (1)", strProp, new String("foo"));
        assertNotNull("Check strAProp not nullity (1)", strAProp);
        String[] v = new String[]{"foo", "bar"};
        for (int i = 0; i < strAProp.length; i++) {
            if (!strAProp[i].equals(v[i])) {
                fail("Check the strAProp Equality (1)");
            }
        }
        assertNotNull("Check intAProp not nullity", intAProp);
        int[] v2 = new int[]{1, 2, 3};
        for (int i = 0; i < intAProp.length; i++) {
            if (intAProp[i] != v2[i]) {
                fail("Check the intAProp Equality (1)");
            }
        }

        // Invoke
        FooService fs = (FooService) osgiHelper.getRawServiceObject(sr);
        assertTrue("invoke fs", fs.foo());

        sr = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), "FooProviderAno-1");
        // Re-check the property (change)
        intProp = (Integer) sr.getProperty("int");
        boolProp = (Boolean) sr.getProperty("boolean");
        strProp = (String) sr.getProperty("string");
        strAProp = (String[]) sr.getProperty("strAProp");
        intAProp = (int[]) sr.getProperty("intAProp");

        assertEquals("Check intProp equality (2)", intProp, new Integer(3));
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
                fail("Check the intAProp Equality (2)");
            }
        }

        fs = null;
    }

    @Test
    public void testWorkerThread() {
        ServiceReference sr = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), "FooProviderAno-1");
        assertNotNull("Check the availability of the FS service", sr);

        // Check service properties
        Integer intProp = (Integer) sr.getProperty("int");
        Boolean boolProp = (Boolean) sr.getProperty("boolean");
        String strProp = (String) sr.getProperty("string");
        String[] strAProp = (String[]) sr.getProperty("strAProp");
        int[] intAProp = (int[]) sr.getProperty("intAProp");

        assertEquals("Check intProp equality (1)", intProp, new Integer(2));
        assertEquals("Check longProp equality (1)", boolProp, new Boolean(false));
        assertEquals("Check strProp equality (1)", strProp, new String("foo"));
        assertNotNull("Check strAProp not nullity (1)", strAProp);
        String[] v = new String[]{"foo", "bar"};
        for (int i = 0; i < strAProp.length; i++) {
            if (!strAProp[i].equals(v[i])) {
                fail("Check the strAProp Equality (1)");
            }
        }
        assertNotNull("Check intAProp not nullity", intAProp);
        int[] v2 = new int[]{1, 2, 3};
        for (int i = 0; i < intAProp.length; i++) {
            if (intAProp[i] != v2[i]) {
                fail("Check the intAProp Equality (1)");
            }
        }

        // Invoke
        FooService fs = (FooService) osgiHelper.getRawServiceObject(sr);
        assertTrue("invoke fs", fs.getBoolean());

        sr = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), "FooProviderAno-1");
        // Re-check the property (change)
        intProp = (Integer) sr.getProperty("int");
        boolProp = (Boolean) sr.getProperty("boolean");
        strProp = (String) sr.getProperty("string");
        strAProp = (String[]) sr.getProperty("strAProp");
        intAProp = (int[]) sr.getProperty("intAProp");

        assertEquals("Check intProp equality (2)", intProp, new Integer(3));
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
                fail("Check the intAProp Equality (2)");
            }
        }

        fs = null;
    }

}
