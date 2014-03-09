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
package org.apache.felix.ipojo.runtime.externalhandlers.test;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.HandlerManagerFactory;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.runtime.externalhandlers.services.CheckService;
import org.apache.felix.ipojo.runtime.externalhandlers.services.FooService;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.helpers.Dumps;

import java.util.Dictionary;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class HandlerWithFieldInterceptorTest extends Common {


    ComponentInstance instance;

    @Before
    public void setUp() {
        Properties props = new Properties();
        props.put("instance.name", "HandlerTest-1");
        props.put("csh.simple", "simple");
        Properties p = new Properties();
        p.put("a", "a");
        p.put("b", "b");
        p.put("c", "c");
        props.put("csh.map", p);
        props.put("foo", FooService.VALUE);
        instance = ipojoHelper.createComponentInstance("HANDLER-HandlerWithFieldInterceptorTester", props);
    }

    @Test
    public void testFieldInterception() {
        // Check the availability of CheckService
        String name = "HandlerTest-1";
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), name);
        assertNotNull("Check the check service availability", ref);
        CheckService cs = (CheckService) osgiHelper.getRawServiceObject(ref);

        Dictionary<String, Object> p = cs.getProps();
        assertEquals("Assert 'simple' equality", p.get("Simple"), "simple");
        assertEquals("Assert 'a' equality", p.get("Map1"), "a");
        assertEquals("Assert 'b' equality", p.get("Map2"), "b");
        assertEquals("Assert 'c' equality", p.get("Map3"), "c");

        assertEquals("check foo value", FooService.VALUE, cs.getProps().get("foo"));

        // Change value.

        ServiceReference ref2 = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), name);
        assertNotNull("Check the foo service availability", ref2);
        FooService fs = (FooService) osgiHelper.getRawServiceObject(ref2);

        fs.foo(); // This trigger the changes.

        assertEquals("check foo value", FooService.VALUE_2, cs.getProps().get("foo"));
    }

}
