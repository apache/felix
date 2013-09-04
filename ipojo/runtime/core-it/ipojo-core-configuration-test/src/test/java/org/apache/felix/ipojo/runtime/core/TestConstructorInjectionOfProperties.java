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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;


public class TestConstructorInjectionOfProperties extends Common {


    @Test
    public void testInjectionOfNamedProperty() {
        Dictionary<String, String> conf = new Hashtable<String, String>();
        conf.put("message", "message");
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.constructor.CheckServiceProviderWithNamedProperty", conf);

        ServiceReference ref = osgiHelper.waitForService(CheckService.class.getName(),
                "(instance.name=" + instance.getInstanceName() +")",
                1000);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        assertEquals(cs.getProps().getProperty("message"), "message");
    }

    @Test
    public void testInjectionOfUnnamedProperty() {
        Dictionary<String, String> conf = new Hashtable<String, String>();
        conf.put("message", "message");

        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.constructor.CheckServiceProviderWithUnnamedProperty", conf);

        ServiceReference ref = osgiHelper.waitForService(CheckService.class.getName(),
                "(instance.name=" + instance.getInstanceName() +")",
                1000);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        assertEquals(cs.getProps().getProperty("message"), "message");
    }

    @Test
    public void testInjectionOfPropertyWithDefaultValue() {
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.constructor.CheckServiceProviderWithDefaultValueProperty");

        ServiceReference ref = osgiHelper.waitForService(CheckService.class.getName(),
                "(instance.name=" + instance.getInstanceName() +")",
                1000);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        assertEquals(cs.getProps().getProperty("message"), "message");
    }

    @Test
    public void testInjectionOfTwoProperties() {
        Dictionary<String, String> conf = new Hashtable<String, String>();
        conf.put("message", "message");

        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.constructor.CheckServiceProviderWithTwoProperties", conf);

        ServiceReference ref = osgiHelper.waitForService(CheckService.class.getName(),
                "(instance.name=" + instance.getInstanceName() +")",
                1000);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        assertEquals(cs.getProps().getProperty("message"), "message");
        assertEquals(cs.getProps().getProperty("product"), "ipojo");
    }

    @Test
    public void testInjectionOfAPropertyAndBundleContext() {
        Dictionary<String, String> conf = new Hashtable<String, String>();
        conf.put("message", "message");

        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.constructor.CheckServiceProviderWithBundleContextAndProperty", conf);

        ServiceReference ref = osgiHelper.waitForService(CheckService.class.getName(),
                "(instance.name=" + instance.getInstanceName() +")",
                1000);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        assertEquals(cs.getProps().getProperty("message"), "message");
        assertNotNull(cs.getProps().get("context"));
        assertTrue(cs.getProps().get("context") instanceof BundleContext);
    }

}
