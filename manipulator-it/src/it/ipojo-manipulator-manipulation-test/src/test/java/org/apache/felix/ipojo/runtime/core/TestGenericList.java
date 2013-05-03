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
import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.helpers.BaseTest;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

public class TestGenericList extends BaseTest {

    ComponentInstance foo1, foo2;
    ComponentInstance checker;

    @Before
    public void setUp() {
        foo1 = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.components.FooServiceImpl", "foo1");
        foo2 = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.components.FooServiceImpl", "foo2");
        checker = ipojoHelper.createComponentInstance("TypedList", "checker");
        foo1.stop();
        foo2.stop();
    }

    @Test
    public void testTypedList() {
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), checker.getInstanceName());
        CheckService check = (CheckService) osgiHelper.getServiceObject(ref);
        assertNotNull("Checker availability", check);
        // Check without providers
        assertFalse("Empty list", check.check());

        // Start the first provider
        foo1.start();
        assertTrue("List with one element", check.check());
        Properties props = check.getProps();
        List<FooService> list = (List<FooService>) props.get("list");
        assertEquals("Check size - 1", 1, list.size());

        // Start the second provider 
        foo2.start();
        assertTrue("List with two element", check.check());
        props = check.getProps();
        list = (List<FooService>) props.get("list");
        assertEquals("Check size - 2", 2, list.size());

        // Stop the first one
        foo1.stop();
        assertTrue("List with one element (2)", check.check());
        props = check.getProps();
        list = (List<FooService>) props.get("list");
        assertEquals("Check size - 3", 1, list.size());
    }

    @Override
    protected List<String> getExtraExports() {
        return Arrays.asList("org.apache.felix.ipojo.runtime.core.components");
    }

}
