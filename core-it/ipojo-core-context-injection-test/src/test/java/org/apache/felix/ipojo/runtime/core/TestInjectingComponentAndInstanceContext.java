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
 * Checks that component can retrieve both contexts.
 * For instance bundle context, we use the 'instance.bundle.context' hidden configuration
 * property. We use the BundleContext from the iPOJO bundle context, as a mark for testing, it is obviously,
 * not recommended.
 *
 * These test cases are using Annotation descriptions.
 */
public class TestInjectingComponentAndInstanceContext extends Common {

    @Test
    public void testSetterInjectionOfBothContext() {
        BundleContext bc = osgiHelper.getBundle("org.apache.felix.ipojo").getBundleContext();
        Properties configuration = new Properties();
        configuration.put("instance.bundle.context", bc);
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.annotations.ComponentWithTwoSetters", configuration);

        CheckService check = ipojoHelper.getServiceObjectByName(CheckService.class, instance.getInstanceName());
        assertNotNull(check);

        BundleContext context = (BundleContext) check.map().get("component");
        assertNotNull(context);
        assertEquals(getTestBundle().getSymbolicName(), context.getBundle().getSymbolicName());

        context = (BundleContext) check.map().get("instance");
        assertNotNull(context);
        assertEquals(bc, context);
    }

    @Test
    public void testFieldInjectionOfBothContext() {
        BundleContext bc = osgiHelper.getBundle("org.apache.felix.ipojo").getBundleContext();
        Properties configuration = new Properties();
        configuration.put("instance.bundle.context", bc);
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.annotations.ComponentWithTwoFields", configuration);

        CheckService check = ipojoHelper.getServiceObjectByName(CheckService.class, instance.getInstanceName());
        assertNotNull(check);

        BundleContext context = (BundleContext) check.map().get("component");
        assertNotNull(context);
        assertEquals(getTestBundle().getSymbolicName(), context.getBundle().getSymbolicName());

        context = (BundleContext) check.map().get("instance");
        assertNotNull(context);
        assertEquals(bc, context);
    }

    @Test
    public void testConstructorInjectionOfBothContext() {
        BundleContext bc = osgiHelper.getBundle("org.apache.felix.ipojo").getBundleContext();
        Properties configuration = new Properties();
        configuration.put("instance.bundle.context", bc);
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.annotations.ComponentWithTwoConstructorParams", configuration);

        CheckService check = ipojoHelper.getServiceObjectByName(CheckService.class, instance.getInstanceName());
        assertNotNull(check);

        BundleContext context = (BundleContext) check.map().get("component");
        assertNotNull(context);
        assertEquals(getTestBundle().getSymbolicName(), context.getBundle().getSymbolicName());

        context = (BundleContext) check.map().get("instance");
        assertNotNull(context);
        assertEquals(bc, context);
    }

    /**
     * Mix bundle context injection with the default bc injection (legacy).
     */
    @Test
    public void testConstructorInjectionOfThreeContext() {
        BundleContext bc = osgiHelper.getBundle("org.apache.felix.ipojo").getBundleContext();
        Properties configuration = new Properties();
        configuration.put("instance.bundle.context", bc);
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".components.annotations.ComponentWithThreeConstructorParams", configuration);

        CheckService check = ipojoHelper.getServiceObjectByName(CheckService.class, instance.getInstanceName());
        assertNotNull(check);

        BundleContext context = (BundleContext) check.map().get("component");
        assertNotNull(context);
        assertEquals(getTestBundle().getSymbolicName(), context.getBundle().getSymbolicName());

        context = (BundleContext) check.map().get("instance");
        assertNotNull(context);
        assertEquals(bc, context);

        context = (BundleContext) check.map().get("bc");
        assertNotNull(context);
        assertEquals(getTestBundle().getSymbolicName(), context.getBundle().getSymbolicName());
    }

}
