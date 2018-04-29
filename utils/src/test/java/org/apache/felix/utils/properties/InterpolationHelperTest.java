/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.utils.properties;

import junit.framework.TestCase;
<<<<<<< HEAD

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
=======
import org.junit.Test;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

public class InterpolationHelperTest extends TestCase {

    private MockBundleContext context;

    protected void setUp() throws Exception {
        context = new MockBundleContext();
    }

    public void testBasicSubstitution()
    {
        System.clearProperty("value2");
        System.setProperty("value1", "sub_value1");
        try
        {
<<<<<<< HEAD
            Hashtable props = new Hashtable();
=======
            Hashtable<String, String> props = new Hashtable<String, String>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            props.put("key0", "value0");
            props.put("key1", "${value1}");
            props.put("key2", "${value2}");

            for (Enumeration e = props.keys(); e.hasMoreElements();)
            {
                String name = (String) e.nextElement();
<<<<<<< HEAD
                props.put(name, InterpolationHelper.substVars((String) props.get(name), name, null, props, context));
=======
                props.put(name, InterpolationHelper.substVars(props.get(name), name, null, props, context));
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }

            assertEquals("value0", props.get("key0"));
            assertEquals("sub_value1", props.get("key1"));
            assertEquals("", props.get("key2"));
        }
        finally
        {
            System.clearProperty("value1");
            System.clearProperty("value2");
        }

    }

    public void testBasicSubstitutionWithContext()
    {
        System.setProperty("value1", "sub_value1");
        System.setProperty("value2", "sub_value2");
        try
        {
            context.setProperty("value3", "context_value1");
            context.setProperty("value2", "context_value2");

<<<<<<< HEAD
            Hashtable props = new Hashtable();
=======
            Hashtable<String, String> props = new Hashtable<String, String>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            props.put("key0", "value0");
            props.put("key1", "${value1}");
            props.put("key2", "${value2}");
            props.put("key3", "${value3}");

            for (Enumeration e = props.keys(); e.hasMoreElements();)
            {
                String name = (String) e.nextElement();
                props.put(name,
<<<<<<< HEAD
                        InterpolationHelper.substVars((String) props.get(name), name, null, props, context));
=======
                        InterpolationHelper.substVars(props.get(name), name, null, props, context));
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }

            assertEquals("value0", props.get("key0"));
            assertEquals("sub_value1", props.get("key1"));
            assertEquals("context_value2", props.get("key2"));
            assertEquals("context_value1", props.get("key3"));
        }
        finally
        {
            System.clearProperty("value1");
            System.clearProperty("value2");
        }

    }

    public void testSubstitutionFailures()
    {
<<<<<<< HEAD
        assertEquals("a}", InterpolationHelper.substVars("a}", "b", null, new Hashtable(), context));
        assertEquals("${a", InterpolationHelper.substVars("${a", "b", null, new Hashtable(), context));
    }

    public void testEmptyVariable() {
        assertEquals("", InterpolationHelper.substVars("${}", "b", null, new Hashtable(), context));
    }

    public void testInnerSubst() {
        Hashtable props = new Hashtable();
=======
        assertEquals("a}", InterpolationHelper.substVars("a}", "b", null, new Hashtable<String, String>(), context));
        assertEquals("${a", InterpolationHelper.substVars("${a", "b", null, new Hashtable<String, String>(), context));
    }

    public void testEmptyVariable() {
        assertEquals("", InterpolationHelper.substVars("${}", "b", null, new Hashtable<String, String>(), context));
    }

    public void testInnerSubst() {
        Hashtable<String, String> props = new Hashtable<String, String>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        props.put("a", "b");
        props.put("b", "c");
        assertEquals("c", InterpolationHelper.substVars("${${a}}", "z", null, props, context));
    }

    public void testSubstLoop() {
        try {
<<<<<<< HEAD
            InterpolationHelper.substVars("${a}", "a", null, new Hashtable(), context);
=======
            InterpolationHelper.substVars("${a}", "a", null, new Hashtable<String, String>(), context);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            fail("Should have thrown an exception");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testSubstitutionEscape()
    {
<<<<<<< HEAD
        assertEquals("${a}", InterpolationHelper.substVars("$\\{a${#}\\}", "b", null, new Hashtable(), context));
        assertEquals("${a}", InterpolationHelper.substVars("$\\{a\\}${#}", "b", null, new Hashtable(), context));
        assertEquals("${a}", InterpolationHelper.substVars("$\\{a\\}", "b", null, new Hashtable(), context));
=======
        assertEquals("${a}", InterpolationHelper.substVars("$\\{a${#}\\}", "b", null, new Hashtable<String, String>(), context));
        assertEquals("${a}", InterpolationHelper.substVars("$\\{a\\}${#}", "b", null, new Hashtable<String, String>(), context));
        assertEquals("${a}", InterpolationHelper.substVars("$\\{a\\}", "b", null, new Hashtable<String, String>(), context));
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    public void testSubstitutionOrder()
    {
        LinkedHashMap<String, String> map1 = new LinkedHashMap<String, String>();
        map1.put("a", "$\\\\{var}");
        map1.put("abc", "${ab}c");
        map1.put("ab", "${a}b");
        InterpolationHelper.performSubstitution(map1);

        LinkedHashMap<String, String> map2 = new LinkedHashMap<String, String>();
        map2.put("a", "$\\\\{var}");
        map2.put("ab", "${a}b");
        map2.put("abc", "${ab}c");
        InterpolationHelper.performSubstitution(map2);

        assertEquals(map1, map2);
    }

<<<<<<< HEAD
=======
    public void testMultipleEscapes()
    {
        LinkedHashMap<String, String> map1 = new LinkedHashMap<String, String>();
        map1.put("a", "$\\\\{var}");
        map1.put("abc", "${ab}c");
        map1.put("ab", "${a}b");
        InterpolationHelper.performSubstitution(map1);

        assertEquals("$\\{var}", map1.get("a"));
        assertEquals("$\\{var}b", map1.get("ab"));
        assertEquals("$\\{var}bc", map1.get("abc"));
    }

    public void testPreserveUnresolved() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("a", "${b}");
        assertEquals("", InterpolationHelper.substVars("${b}", "a", null, props, null, true, false, true));
        assertEquals("${b}", InterpolationHelper.substVars("${b}", "a", null, props, null, true, false, false));

        props.put("b", "c");
        assertEquals("c", InterpolationHelper.substVars("${b}", "a", null, props, null, true, false, true));
        assertEquals("c", InterpolationHelper.substVars("${b}", "a", null, props, null, true, false, false));

        props.put("c", "${d}${d}");
        assertEquals("${d}${d}", InterpolationHelper.substVars("${d}${d}", "c", null, props, null, false, false, false));
    }
    
    public void testExpansion() {
        Map<String, String> props = new LinkedHashMap<String, String>();
        props.put("a", "foo");
        props.put("b", "");

        props.put("a_cm", "${a:-bar}");
        props.put("b_cm", "${b:-bar}");
        props.put("c_cm", "${c:-bar}");

        props.put("a_cp", "${a:+bar}");
        props.put("b_cp", "${b:+bar}");
        props.put("c_cp", "${c:+bar}");

        InterpolationHelper.performSubstitution(props);

        assertEquals("foo", props.get("a_cm"));
        assertEquals("bar", props.get("b_cm"));
        assertEquals("bar", props.get("c_cm"));

        assertEquals("bar", props.get("a_cp"));
        assertEquals("", props.get("b_cp"));
        assertEquals("", props.get("c_cp"));
    }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
}
