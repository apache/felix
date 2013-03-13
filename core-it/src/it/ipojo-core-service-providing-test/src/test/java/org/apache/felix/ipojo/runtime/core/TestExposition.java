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
import org.apache.felix.ipojo.runtime.core.services.BarService;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.*;

public class TestExposition extends Common {

    private ComponentInstance fooProviderSimple;
    private ComponentInstance fooProviderItf;
    private ComponentInstance fooBarProvider;
    private ComponentInstance fooBarProvider2;
    private ComponentInstance fooBarProvider3;

    @Before
    public void setUp() {
        fooProviderSimple = ipojoHelper.createComponentInstance("PS-FooProviderType-1", "fooProviderSimple");

        fooProviderItf = ipojoHelper.createComponentInstance("PS-FooProviderType-itf", "fooProviderItf");

        fooBarProvider = ipojoHelper.createComponentInstance("PS-FooBarProviderType-1", "fooProviderItfs");

        fooBarProvider2 = ipojoHelper.createComponentInstance("PS-FooBarProviderType-2", "fooProviderItfs2");

        fooBarProvider3 = ipojoHelper.createComponentInstance("PS-FooBarProviderType-3", "fooProviderItfs3");

        assertNotNull("Check the instance creation of fooProviderSimple", fooProviderSimple);
        assertNotNull("Check the instance creation of fooProviderItf", fooProviderItf);
        assertNotNull("Check the instance creation of fooProviderItfs", fooBarProvider);
        assertNotNull("Check the instance creation of fooProviderItfs2", fooBarProvider2);
        assertNotNull("Check the instance creation of fooProviderItfs3", fooBarProvider3);

    }


    @Test
    public void testSimpleExposition() {
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooProviderSimple.getInstanceName());
        assertNotNull("Check the availability of the FS from " + fooProviderSimple.getInstanceName(), ref);
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);
        assertTrue("Check fs invocation", fs.foo());
        fs = null;
        fooProviderSimple.stop();
        ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooProviderSimple.getInstanceName());
        assertNull("Check the absence of the FS from " + fooProviderSimple.getInstanceName(), ref);

    }

    @Test
    public void testItfExposition() {
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooProviderItf.getInstanceName());
        assertNotNull("Check the availability of the FS from " + fooProviderItf.getInstanceName(), ref);
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);
        assertTrue("Check fs invocation", fs.foo());
        fs = null;
        fooProviderItf.stop();

        ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooProviderItf.getInstanceName());
        assertNull("Check the absence of the FS from " + fooProviderItf.getInstanceName(), ref);
    }

    @Test
    public void testItfsExposition() {
        ServiceReference refFoo = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooBarProvider.getInstanceName());
        assertNotNull("Check the availability of the FS from " + fooBarProvider.getInstanceName(), refFoo);
        ServiceReference refBar = ipojoHelper.getServiceReferenceByName(BarService.class.getName(), fooBarProvider.getInstanceName());
        assertNotNull("Check the availability of the BS from " + fooBarProvider.getInstanceName(), refBar);

        assertSame("Check service reference equality", refFoo, refBar);

        FooService fs = (FooService) osgiHelper.getServiceObject(refFoo);
        assertTrue("Check fs invocation", fs.foo());
        fs = null;

        BarService bs = (BarService) osgiHelper.getServiceObject(refBar);
        assertTrue("Check bs invocation", bs.bar());
        bs = null;

        fooBarProvider.stop();

        refFoo = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooBarProvider.getInstanceName());
        refBar = ipojoHelper.getServiceReferenceByName(BarService.class.getName(), fooBarProvider.getInstanceName());
        assertNull("Check the absence of the FS from " + fooBarProvider.getInstanceName(), refFoo);
        assertNull("Check the absence of the BS from " + fooBarProvider.getInstanceName(), refBar);
    }

    @Test
    public void testItfsExposition2() {
        ServiceReference refFoo = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooBarProvider2.getInstanceName());
        assertNotNull("Check the availability of the FS from " + fooBarProvider2.getInstanceName(), refFoo);
        ServiceReference refBar = ipojoHelper.getServiceReferenceByName(BarService.class.getName(), fooBarProvider2.getInstanceName());
        assertNotNull("Check the availability of the BS from " + fooBarProvider2.getInstanceName(), refBar);

        assertSame("Check service reference equality", refFoo, refBar);

        FooService fs = (FooService) osgiHelper.getServiceObject(refFoo);
        assertTrue("Check fs invocation", fs.foo());
        fs = null;

        BarService bs = (BarService) osgiHelper.getServiceObject(refBar);
        assertTrue("Check bs invocation", bs.bar());
        bs = null;

        fooBarProvider2.stop();

        refFoo = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooBarProvider2.getInstanceName());
        refBar = ipojoHelper.getServiceReferenceByName(BarService.class.getName(), fooBarProvider2.getInstanceName());
        assertNull("Check the absence of the FS from " + fooBarProvider.getInstanceName(), refFoo);
        assertNull("Check the absence of the BS from " + fooBarProvider.getInstanceName(), refBar);
    }

    @Test
    public void testItfsExposition3() {
        ServiceReference refFoo = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooBarProvider3.getInstanceName());
        assertNotNull("Check the availability of the FS from " + fooBarProvider3.getInstanceName(), refFoo);
        ServiceReference refBar = ipojoHelper.getServiceReferenceByName(BarService.class.getName(), fooBarProvider3.getInstanceName());
        assertNotNull("Check the availability of the BS from " + fooBarProvider3.getInstanceName(), refBar);

        assertNotSame("Check service reference inequality", refFoo, refBar);

        FooService fs = (FooService) osgiHelper.getServiceObject(refFoo);
        assertTrue("Check fs invocation", fs.foo());
        fs = null;

        BarService bs = (BarService) osgiHelper.getServiceObject(refBar);
        assertTrue("Check bs invocation", bs.bar());
        bs = null;

        // Check properties
        String baz1 = (String) refFoo.getProperty("baz");
        String baz2 = (String) refBar.getProperty("baz");

        assertEquals("Check Baz Property 1", baz1, "foo");
        assertEquals("Check Baz Property 2", baz2, "bar");

        fooBarProvider3.stop();

        refFoo = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooBarProvider3.getInstanceName());
        refBar = ipojoHelper.getServiceReferenceByName(BarService.class.getName(), fooBarProvider3.getInstanceName());
        assertNull("Check the absence of the FS from " + fooBarProvider.getInstanceName(), refFoo);
        assertNull("Check the absence of the BS from " + fooBarProvider.getInstanceName(), refBar);
    }


}
