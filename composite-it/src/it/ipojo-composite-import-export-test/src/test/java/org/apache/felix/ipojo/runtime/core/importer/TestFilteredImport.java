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

public class TestFilteredImport extends Common {

    ComponentInstance import1;
    Factory fooProvider;
    Factory fooProvider2;

    ComponentInstance foo1, foo2;

    @Before
    public void setUp() {
        Properties p = new Properties();
        p.put("instance.name", "importer");
        Factory compFact = ipojoHelper.getFactory("composite.requires.5");
        try {
            import1 = compFact.createComponentInstance(p);
        } catch (Exception e) {
            fail("Cannot instantiate the component : " + e.getMessage());
        }
        import1.stop();

        fooProvider = ipojoHelper.getFactory("COMPO-FooProviderType-1");
        assertNotNull("Check fooProvider availability", fooProvider);

        fooProvider2 = ipojoHelper.getFactory("COMPO-FooProviderType-2");
        assertNotNull("Check fooProvider availability", fooProvider2);

        Properties p1 = new Properties();
        p1.put("instance.name", "foo1");
        Properties p2 = new Properties();
        p2.put("instance.name", "foo2");
        try {
            foo1 = fooProvider.createComponentInstance(p1);
            foo2 = fooProvider2.createComponentInstance(p2);
        } catch (Exception e) {
            fail("Cannot instantiate foo providers : " + e.getMessage());
        }
    }

    @After
    public void tearDown() {
        foo1.dispose();
        foo2.dispose();
        import1.dispose();
        foo1 = null;
        foo2 = null;
        import1 = null;
    }

    @Test
    public void testSimple() {
        import1.start();
        //Two providers
        assertTrue("Test component validity", import1.getState() == ComponentInstance.VALID);
        ServiceContext sc = getServiceContext(import1);
        ServiceReference[] refs = ipojoHelper.getServiceReferences(sc, FooService.class.getName(), null);
        assertNotNull("Test foo availability inside the composite - 1", refs);
        assertEquals("Test foo availability inside the composite - 1.2", refs.length, 1);
        FooService fs = (FooService) sc.getService(refs[0]);
        assertTrue("Test foo invocation", fs.foo());
        sc.ungetService(refs[0]);

        foo1.stop();
        assertTrue("Test component validity", import1.getState() == ComponentInstance.VALID);
        sc = getServiceContext(import1);
        refs = ipojoHelper.getServiceReferences(sc, FooService.class.getName(), null);
        assertNotNull("Test foo availability inside the composite - 1", refs);
        assertEquals("Test foo availability inside the composite - 1.2", refs.length, 1);
        fs = (FooService) sc.getService(refs[0]);
        assertTrue("Test foo invocation", fs.foo());
        sc.ungetService(refs[0]);

        // Stop the second provider
        foo2.stop();
        assertTrue("Test component invalidity - 2", import1.getState() == ComponentInstance.INVALID);

        foo2.start();
        assertTrue("Test component validity", import1.getState() == ComponentInstance.VALID);
        sc = getServiceContext(import1);
        refs = ipojoHelper.getServiceReferences(sc, FooService.class.getName(), null);
        assertNotNull("Test foo availability inside the composite - 3", refs);
        assertEquals("Test foo availability inside the composite - 3.1", refs.length, 1);
        fs = (FooService) sc.getService(refs[0]);
        assertTrue("Test foo invocation", fs.foo());
        sc.ungetService(refs[0]);
    }

    @Test
    public void testSimple2() {
        import1.start();
        //Two providers
        assertTrue("Test component validity", import1.getState() == ComponentInstance.VALID);
        ServiceContext sc = getServiceContext(import1);
        ServiceReference[] refs = ipojoHelper.getServiceReferences(sc, FooService.class.getName(), null);
        assertNotNull("Test foo availability inside the composite - 1", refs);
        assertEquals("Test foo availability inside the composite - 1.2", refs.length, 1);
        FooService fs = (FooService) sc.getService(refs[0]);
        assertTrue("Test foo invocation", fs.foo());
        sc.ungetService(refs[0]);

        foo2.stop();
        assertTrue("Test component invalidity - 1", import1.getState() == ComponentInstance.INVALID);

        // Stop the second provider
        foo1.stop();
        assertTrue("Test component invalidity - 2", import1.getState() == ComponentInstance.INVALID);

        foo1.start();
        assertTrue("Test component invalidity - 3", import1.getState() == ComponentInstance.INVALID);

        foo2.start();
        assertTrue("Test component validity", import1.getState() == ComponentInstance.VALID);
        sc = getServiceContext(import1);
        refs = ipojoHelper.getServiceReferences(sc, FooService.class.getName(), null);
        assertNotNull("Test foo availability inside the composite - 3", refs);
        assertEquals("Test foo availability inside the composite - 3.1", refs.length, 1);
        fs = (FooService) sc.getService(refs[0]);
        assertTrue("Test foo invocation", fs.foo());
        sc.ungetService(refs[0]);
    }

}
