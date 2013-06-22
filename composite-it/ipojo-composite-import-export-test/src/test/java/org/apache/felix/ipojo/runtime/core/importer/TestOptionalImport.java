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
package org.apache.felix.ipojo.runtime.core.importer;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.runtime.core.Common;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

import static org.junit.Assert.*;

public class TestOptionalImport extends Common {

    ComponentInstance import3;
    Factory fooProvider;

    @Before
    public void setUp() {
        osgiHelper.waitForService(Factory.class, "(factory.name=COMPO-FooProviderType-1)", 1000);
        fooProvider = ipojoHelper.getFactory("COMPO-FooProviderType-1");
        assertNotNull("Check fooProvider availability", fooProvider);

        Properties p = new Properties();
        p.put("instance.name", "importer");
        Factory compFact = ipojoHelper.getFactory("composite.requires.3");
        try {
            import3 = compFact.createComponentInstance(p);
        } catch (Exception e) {
            fail("Cannot instantiate the component : " + e.getMessage());
        }
    }

    @After
    public void tearDown() {
        import3.dispose();
        import3 = null;
    }

    @Test
    public void testSimple() {
        // No provider -> valid
        assertTrue("Test component invalidity", import3.getState() == ComponentInstance.VALID);

        ComponentInstance foo = null;
        Properties p = new Properties();
        p.put("instance.name", "foo");
        try {
            foo = fooProvider.createComponentInstance(p);
        } catch (Exception e) {
            fail("Fail to instantiate the foo component " + e.getMessage());
        }

        ComponentInstance foo2 = null;
        Properties p2 = new Properties();
        p2.put("instance.name", "foo2");
        try {
            foo2 = fooProvider.createComponentInstance(p2);
        } catch (Exception e) {
            fail("Fail to instantiate the foo2 component " + e.getMessage());
        }

        // The foo service is available => import1 must be valid
        assertTrue("Test component validity", import3.getState() == ComponentInstance.VALID);
        ServiceContext sc = getServiceContext(import3);
        ServiceReference[] refs = ipojoHelper.getServiceReferences(sc, FooService.class.getName(), null);
        assertNotNull("Test foo availability inside the composite - 1", refs);
        assertEquals("Test foo availability inside the composite - 1.2", refs.length, 1);
        FooService fs = (FooService) sc.getService(refs[0]);
        assertTrue("Test foo invocation", fs.foo());
        sc.ungetService(refs[0]);

        // Stop the second provider
        foo2.dispose();
        assertTrue("Test component validity", import3.getState() == ComponentInstance.VALID);
        sc = getServiceContext(import3);
        refs = ipojoHelper.getServiceReferences(sc, FooService.class.getName(), null);
        assertNotNull("Test foo availability inside the composite - 2", refs);
        assertEquals("Test foo availability inside the composite - 2.1 (" + refs.length + ")", refs.length, 1);
        fs = (FooService) sc.getService(refs[0]);
        assertTrue("Test foo invocation", fs.foo());
        sc.ungetService(refs[0]);

        // stop the foo provider
        foo.stop();

        // No provider -> Invalid
        assertTrue("Test component invalidity - 2", import3.getState() == ComponentInstance.VALID);

        foo.start();
        assertTrue("Test component validity", import3.getState() == ComponentInstance.VALID);
        sc = getServiceContext(import3);
        refs = ipojoHelper.getServiceReferences(sc, FooService.class.getName(), null);
        assertNotNull("Test foo availability inside the composite - 3", refs);
        assertEquals("Test foo availability inside the composite - 3.1", refs.length, 1);
        fs = (FooService) sc.getService(refs[0]);
        assertTrue("Test foo invocation", fs.foo());
        sc.ungetService(refs[0]);

        foo.dispose();
        // No provider -> Invalid
        assertTrue("Test component invalidity - 3", import3.getState() == ComponentInstance.VALID);
    }

    @Test
    public void testSimple2() {
        // No provider -> valid
        assertTrue("Test component invalidity", import3.getState() == ComponentInstance.VALID);

        ComponentInstance foo1 = null;
        Properties p = new Properties();
        p.put("instance.name", "foo");
        try {
            foo1 = fooProvider.createComponentInstance(p);
        } catch (Exception e) {
            fail("Fail to instantiate the foo component " + e.getMessage());
        }

        ComponentInstance foo2 = null;
        Properties p2 = new Properties();
        p2.put("instance.name", "foo2");
        try {
            foo2 = fooProvider.createComponentInstance(p2);
        } catch (Exception e) {
            fail("Fail to instantiate the foo2 component " + e.getMessage());
        }

        // The foo service is available => import1 must be valid
        assertTrue("Test component validity", import3.getState() == ComponentInstance.VALID);
        ServiceContext sc = getServiceContext(import3);
        ServiceReference[] refs = ipojoHelper.getServiceReferences(sc, FooService.class.getName(), null);
        assertNotNull("Test foo availability inside the composite - 1", refs);
        assertEquals("Test foo availability inside the composite - 1.2", refs.length, 1);
        FooService fs = (FooService) sc.getService(refs[0]);
        assertTrue("Test foo invocation", fs.foo());
        sc.ungetService(refs[0]);

        // Stop the second provider
        foo1.stop();
        assertTrue("Test component validity", import3.getState() == ComponentInstance.VALID);
        sc = getServiceContext(import3);
        refs = ipojoHelper.getServiceReferences(sc, FooService.class.getName(), null);
        assertNotNull("Test foo availability inside the composite - 2", refs);
        assertEquals("Test foo availability inside the composite - 2.1 (" + refs.length + ")", refs.length, 1);
        fs = (FooService) sc.getService(refs[0]);
        assertTrue("Test foo invocation", fs.foo());
        sc.ungetService(refs[0]);

        // stop the foo provider
        foo2.dispose();

        // No provider -> Invalid
        assertTrue("Test component invalidity - 2", import3.getState() == ComponentInstance.VALID);

        foo1.start();
        assertTrue("Test component validity", import3.getState() == ComponentInstance.VALID);
        sc = getServiceContext(import3);
        refs = ipojoHelper.getServiceReferences(sc, FooService.class.getName(), null);
        assertNotNull("Test foo availability inside the composite - 3", refs);
        assertEquals("Test foo availability inside the composite - 3.1", refs.length, 1);
        fs = (FooService) sc.getService(refs[0]);
        assertTrue("Test foo invocation", fs.foo());
        sc.ungetService(refs[0]);

        foo1.dispose();
        // No provider -> Invalid
        assertTrue("Test component invalidity - 3", import3.getState() == ComponentInstance.VALID);
    }


}
