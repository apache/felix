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
import org.junit.Test;
import org.osgi.framework.BundleContext;

import java.util.Properties;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Checks that component can mix property and context in constructor parameters.
 *
 * These test cases are using Annotation descriptions.
 */
public class TestInjectingContextAndProperties extends Common {

    @Test
    public void testWhenPropertyIsFirst() {
        BundleContext bc = osgiHelper.getBundle("org.apache.felix.ipojo").getBundleContext();
        Properties configuration = new Properties();
        configuration.put("instance.bundle.context", bc);
        configuration.put("message", "hello");
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.mix.MixWithProperties1", configuration);

        CheckService check = ipojoHelper.getServiceObjectByName(CheckService.class, instance.getInstanceName());
        assertNotNull(check);

        BundleContext context = (BundleContext) check.map().get("context");
        assertNotNull(context);
        assertEquals(getTestBundle().getSymbolicName(), context.getBundle().getSymbolicName());

        assertEquals("hello", check.map().get("message"));
    }

    @Test
    public void testWhenPropertyIsLast() {
        BundleContext bc = osgiHelper.getBundle("org.apache.felix.ipojo").getBundleContext();
        Properties configuration = new Properties();
        configuration.put("instance.bundle.context", bc);
        configuration.put("message", "hello");
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.mix.MixWithProperties2", configuration);

        CheckService check = ipojoHelper.getServiceObjectByName(CheckService.class, instance.getInstanceName());
        assertNotNull(check);

        BundleContext context = (BundleContext) check.map().get("context");
        assertNotNull(context);
        assertEquals(getTestBundle().getSymbolicName(), context.getBundle().getSymbolicName());

        assertEquals("hello", check.map().get("message"));
    }

}
