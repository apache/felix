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
package org.apache.felix.ipojo.handler.temporal.test;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.handler.temporal.services.CheckService;
import org.apache.felix.ipojo.handler.temporal.services.FooService;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.*;

public class NullTest extends Common {

    @Test
    public void testNull() {
        String prov = "provider";
        ComponentInstance provider = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-NullCheckServiceProvider", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        long begin = System.currentTimeMillis();
        DelayedProvider dp = new DelayedProvider(provider, 200);
        dp.start();
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 2", cs.check());
        long end = System.currentTimeMillis();

        assertTrue("Assert delay", (end - begin) >= 200);

        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 3", ref_cs);
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 3", cs.check());

        provider.stop();
        provider.dispose();
        under.stop();
        under.dispose();
    }

    @Test
    public void testNullTimeout() {
        String prov = "provider";
        ComponentInstance provider = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-NullCheckServiceProvider", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        boolean res = false;
        try {
            res = cs.check();
        } catch (RuntimeException e) {
            fail("A null was expected ...");
        }
        assertFalse("Check null", res); // Return false when the foo service is null.

        provider.stop();
        provider.dispose();
        under.stop();
        under.dispose();
        return;
    }

    @Test
    public void testDelayTimeout() {
        String prov = "provider";
        ComponentInstance provider = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-NullCheckServiceProviderTimeout", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        long begin = System.currentTimeMillis();
        DelayedProvider dp = new DelayedProvider(provider, 200);
        dp.start();
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 2", cs.check());
        long end = System.currentTimeMillis();

        assertTrue("Assert delay", (end - begin) >= 200);

        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 3", ref_cs);
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 3", cs.check());

        provider.stop();
        provider.dispose();
        under.stop();
        under.dispose();
    }

    @Test
    public void testNullWithProxy() {
        String prov = "provider";
        ComponentInstance provider = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-NullProxiedCheckServiceProvider", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        long begin = System.currentTimeMillis();
        DelayedProvider dp = new DelayedProvider(provider, 200);
        dp.start();
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 2", cs.check());
        long end = System.currentTimeMillis();

        assertTrue("Assert delay", (end - begin) >= 200);

        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 3", ref_cs);
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 3", cs.check());

        provider.stop();
        provider.dispose();
        under.stop();
        under.dispose();
    }

    @Test
    public void testNullTimeoutWithProxy() {
        String prov = "provider";
        ComponentInstance provider = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-NullProxiedCheckServiceProvider", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        boolean res = false;
        try {
            res = cs.check();
        } catch (RuntimeException e) {
            fail("A null was expected ...");
        }
        assertFalse("Check null", res); // Return false when the foo service is null.

        provider.stop();
        provider.dispose();
        under.stop();
        under.dispose();
        return;
    }

    @Test
    public void testDelayTimeoutWithProxy() {
        String prov = "provider";
        ComponentInstance provider = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-NullProxiedCheckServiceProviderTimeout", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        long begin = System.currentTimeMillis();
        DelayedProvider dp = new DelayedProvider(provider, 200);
        dp.start();
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 2", cs.check());
        long end = System.currentTimeMillis();

        assertTrue("Assert delay", (end - begin) >= 200);

        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 3", ref_cs);
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 3", cs.check());

        provider.stop();
        provider.dispose();
        under.stop();
        under.dispose();
    }

    @Test
    public void testNullMultipleTimeout() {
        String prov = "provider";
        ComponentInstance provider = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-NullMultipleCheckServiceProviderTimeout", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        DelayedProvider dp = new DelayedProvider(provider, 400);
        dp.start();
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        boolean res = false;
        try {
            res = cs.check();
        } catch (RuntimeException e) {
            fail("A null was expected ...");
        }
        assertTrue("Check nullable", res);

        dp.stop();
        provider.stop();
        provider.dispose();
        under.stop();
        under.dispose();
        return;
    }

    @Test
    public void testNullCollectionTimeout() {
        String prov = "provider";
        ComponentInstance provider = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-NullColCheckServiceProviderTimeout", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        DelayedProvider dp = new DelayedProvider(provider, 400);
        dp.start();
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        boolean res = false;
        try {
            res = cs.check();
        } catch (RuntimeException e) {
            fail("A null was expected ...");
        }
        assertTrue("Check nullable", res);

        dp.stop();
        provider.stop();
        provider.dispose();
        under.stop();
        under.dispose();
        return;
    }

    @Test
    public void testNullProxiedCollectionTimeout() {
        String prov = "provider";
        ComponentInstance provider = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-NullProxiedColCheckServiceProviderTimeout", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        DelayedProvider dp = new DelayedProvider(provider, 400);
        dp.start();
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        boolean res = false;
        try {
            res = cs.check();
        } catch (RuntimeException e) {
            fail("A null was expected ... : " + e.getMessage());
            e.printStackTrace();
        }
        assertTrue("Check nullable", res);

        dp.stop();
        provider.stop();
        provider.dispose();
        under.stop();
        under.dispose();
        return;
    }

    @Test
    public void testDelayOnMultipleDependency() {
        String prov = "provider";
        ComponentInstance provider1 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String prov2 = "provider2";
        ComponentInstance provider2 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov2);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-NullableMultipleCheckServiceProviderTimeout", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the providers.
        provider1.stop();
        provider2.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        long begin = System.currentTimeMillis();
        DelayedProvider dp = new DelayedProvider(provider1, 1500);
        DelayedProvider dp2 = new DelayedProvider(provider2, 100);
        dp.start();
        dp2.start();
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 2", cs.check());
        long end = System.currentTimeMillis();
        System.out.println("delay = " + (end - begin));
        assertTrue("Assert min delay", (end - begin) >= 100);
        assertTrue("Assert max delay", (end - begin) <= 1000);
        dp.stop();
        dp2.stop();

        provider1.stop();
        provider2.stop();

        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 3", ref_cs);
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);

        assertFalse("Check invocation - 3", cs.check()); // Will return false as the contained nullable will return false to the foo method.

        provider1.dispose();
        provider2.dispose();
        under.stop();
        under.dispose();
    }

    @Test
    public void testDelayOnCollectionDependency() {
        String prov = "provider";
        ComponentInstance provider1 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String prov2 = "provider2";
        ComponentInstance provider2 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov2);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-NullableColCheckServiceProviderTimeout", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the providers.
        provider1.stop();
        provider2.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        long begin = System.currentTimeMillis();
        DelayedProvider dp = new DelayedProvider(provider1, 1500);
        DelayedProvider dp2 = new DelayedProvider(provider2, 100);
        dp.start();
        dp2.start();
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 2", cs.check());
        long end = System.currentTimeMillis();
        System.out.println("delay = " + (end - begin));
        assertTrue("Assert min delay", (end - begin) >= 100);
        assertTrue("Assert max delay", (end - begin) <= 1000);
        dp.stop();
        dp2.stop();

        provider1.stop();
        provider2.stop();

        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 3", ref_cs);
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);

        assertFalse("Check invocation - 3", cs.check()); // Will return false as the contained nullable will return false to the foo method.

        provider1.dispose();
        provider2.dispose();
        under.stop();
        under.dispose();
    }

    @Test
    public void testDelayOnProxiedCollectionDependency() {
        String prov = "provider";
        ComponentInstance provider1 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String prov2 = "provider2";
        ComponentInstance provider2 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov2);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-NullableProxiedColCheckServiceProviderTimeout", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the providers.
        provider1.stop();
        provider2.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        long begin = System.currentTimeMillis();
        DelayedProvider dp = new DelayedProvider(provider1, 1500);
        DelayedProvider dp2 = new DelayedProvider(provider2, 100);
        dp.start();
        dp2.start();
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 2", cs.check());
        long end = System.currentTimeMillis();
        System.out.println("delay = " + (end - begin));
        assertTrue("Assert min delay", (end - begin) >= 100);
        assertTrue("Assert max delay", (end - begin) <= 1000);
        dp.stop();
        dp2.stop();

        provider1.stop();
        provider2.stop();

        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 3", ref_cs);
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);

        assertFalse("Check invocation - 3", cs.check()); // Will return false as the contained nullable will return false to the foo method.

        provider1.dispose();
        provider2.dispose();
        under.stop();
        under.dispose();
    }
}
