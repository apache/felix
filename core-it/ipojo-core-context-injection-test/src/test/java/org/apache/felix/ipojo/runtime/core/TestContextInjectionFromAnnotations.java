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
import static junit.framework.Assert.assertNotSame;
import static org.junit.Assert.assertEquals;

/**
 * Checks that contexts are correctly injected.
 * For instance bundle context, we use the 'instance.bundle.context' hidden configuration
 * property. We use the BundleContext from the iPOJO bundle context, as a mark for testing, it is obviously,
 * not recommended.
 *
 * These test cases are using Annotation descriptions.
 */
public class TestContextInjectionFromAnnotations extends Common {

    @Test
    public void testFieldInjectionOfComponentBundleContext() {
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.annotations.ComponentBundleContextInjectionInField");
        CheckService check = ipojoHelper.getServiceObjectByName(CheckService.class, instance.getInstanceName());
        assertNotNull(check);
        BundleContext context = (BundleContext) check.map().get("context");
        assertNotNull(context);
        assertEquals(getTestBundle().getSymbolicName(), context.getBundle().getSymbolicName());
    }

    @Test
    public void testFieldInjectionOfInstanceBundleContext() {
        BundleContext bc = osgiHelper.getBundle("org.apache.felix.ipojo").getBundleContext();
        Properties configuration = new Properties();
        configuration.put("instance.bundle.context", bc);
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.annotations.InstanceBundleContextInjectionInField", configuration);

        CheckService check = ipojoHelper.getServiceObjectByName(CheckService.class, instance.getInstanceName());
        assertNotNull(check);
        BundleContext context = (BundleContext) check.map().get("context");
        assertNotNull(context);
        assertEquals(bc, context);
    }

    @Test
    public void testMethodInjectionOfComponentBundleContext() {
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.annotations.ComponentBundleContextInjectionInMethod");
        CheckService check = ipojoHelper.getServiceObjectByName(CheckService.class, instance.getInstanceName());
        assertNotNull(check);
        BundleContext context = (BundleContext) check.map().get("context");
        assertNotNull(context);
        assertEquals(getTestBundle().getSymbolicName(), context.getBundle().getSymbolicName());
    }

    @Test
    public void testMethodInjectionOfInstanceBundleContext() {
        BundleContext bc = osgiHelper.getBundle("org.apache.felix.ipojo").getBundleContext();
        Properties configuration = new Properties();
        configuration.put("instance.bundle.context", bc);
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.annotations.InstanceBundleContextInjectionInMethod", configuration);

        CheckService check = ipojoHelper.getServiceObjectByName(CheckService.class, instance.getInstanceName());
        assertNotNull(check);
        BundleContext context = (BundleContext) check.map().get("context");
        assertNotNull(context);
        assertEquals(bc, context);
    }

    @Test
    public void testConstructorInjectionOfComponentBundleContext() {
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.annotations.ComponentBundleContextInjectionInConstructor");
        CheckService check = ipojoHelper.getServiceObjectByName(CheckService.class, instance.getInstanceName());
        assertNotNull(check);
        BundleContext context = (BundleContext) check.map().get("context");
        assertNotNull(context);
        assertEquals(getTestBundle().getSymbolicName(), context.getBundle().getSymbolicName());
    }

    @Test
    public void testConstructorInjectionOfInstanceBundleContext() {
        BundleContext bc = osgiHelper.getBundle("org.apache.felix.ipojo").getBundleContext();
        Properties configuration = new Properties();
        configuration.put("instance.bundle.context", bc);
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.annotations.InstanceBundleContextInjectionInConstructor", configuration);

        CheckService check = ipojoHelper.getServiceObjectByName(CheckService.class, instance.getInstanceName());
        assertNotNull(check);
        BundleContext context = (BundleContext) check.map().get("context");
        assertNotNull(context);
        assertEquals(bc, context);
    }

    @Test
    public void testInstanceCreatedFromConfiguration() {
        CheckService check = ipojoHelper.getServiceObjectByName(CheckService.class, "from.configuration");
        assertNotNull(check);
        BundleContext context = (BundleContext) check.map().get("context");
        assertNotNull(context);
        assertNotSame(bc, context);
    }

}
