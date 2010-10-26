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

import java.util.Enumeration;
import java.util.Hashtable;

public class InterpolationHelperTest extends TestCase {

    private MockBundleContext context;

    protected void setUp() throws Exception {
        context = new MockBundleContext();
    }

    public void testBasicSubstitution()
    {
        System.setProperty("value1", "sub_value1");
        Hashtable props = new Hashtable();
        props.put("key0", "value0");
        props.put("key1", "${value1}");
        props.put("key2", "${value2}");

        for (Enumeration e = props.keys(); e.hasMoreElements();)
        {
            String name = (String) e.nextElement();
            props.put(name, InterpolationHelper.substVars((String) props.get(name), name, null, props, context));
        }

        assertEquals("value0", props.get("key0"));
        assertEquals("sub_value1", props.get("key1"));
        assertEquals("", props.get("key2"));

    }

    public void testBasicSubstitutionWithContext()
    {
        System.setProperty("value1", "sub_value1");
        System.setProperty("value2", "sub_value2");
        context.setProperty("value3", "context_value1");
        context.setProperty("value2", "context_value2");

        Hashtable props = new Hashtable();
        props.put("key0", "value0");
        props.put("key1", "${value1}");
        props.put("key2", "${value2}");
        props.put("key3", "${value3}");

        for (Enumeration e = props.keys(); e.hasMoreElements();)
        {
            String name = (String) e.nextElement();
            props.put(name,
                    InterpolationHelper.substVars((String) props.get(name), name, null, props, context));
        }

        assertEquals("value0", props.get("key0"));
        assertEquals("sub_value1", props.get("key1"));
        assertEquals("context_value2", props.get("key2"));
        assertEquals("context_value1", props.get("key3"));

    }

    public void testSubstitutionFailures()
    {
        assertEquals("a}", InterpolationHelper.substVars("a}", "b", null, new Hashtable(), context));
        assertEquals("${a", InterpolationHelper.substVars("${a", "b", null, new Hashtable(), context));
    }

    public void testEmptyVariable() {
        assertEquals("", InterpolationHelper.substVars("${}", "b", null, new Hashtable(), context));
    }

    public void testInnerSubst() {
        Hashtable props = new Hashtable();
        props.put("a", "b");
        props.put("b", "c");
        assertEquals("c", InterpolationHelper.substVars("${${a}}", "z", null, props, context));
    }

    public void testSubstLoop() {
        try {
            InterpolationHelper.substVars("${a}", "a", null, new Hashtable(), context);
            fail("Should have thrown an exception");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testSubstitutionEscape()
    {
        assertEquals("${a}", InterpolationHelper.substVars("$\\{a${#}\\}", "b", null, new Hashtable(), context));
        assertEquals("${a}", InterpolationHelper.substVars("$\\{a\\}${#}", "b", null, new Hashtable(), context));
        assertEquals("${a}", InterpolationHelper.substVars("$\\{a\\}", "b", null, new Hashtable(), context));
    }

}
