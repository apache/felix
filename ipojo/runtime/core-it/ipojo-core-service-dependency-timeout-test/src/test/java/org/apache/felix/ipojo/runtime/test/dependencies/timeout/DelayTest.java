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

package org.apache.felix.ipojo.runtime.test.dependencies.timeout;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.runtime.test.dependencies.timeout.services.*;
import org.junit.After;
import org.junit.Test;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.helpers.TimeUtils;

import static org.junit.Assert.*;

@ExamReactorStrategy(PerMethod.class)
public class DelayTest extends Common {

    private DelayedProvider delayed;

    @After
    public void tearDown() {
        if (delayed != null) {
            delayed.stop();
        }
    }

    @Test
    public void testDelayTimeout() {
        String prov = "provider";
        ComponentInstance provider = ipojoHelper.createComponentInstance("FooProvider", prov);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("CheckServiceProviderTimeout", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getRawServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        long begin = System.currentTimeMillis();
        DelayedProvider dp = new DelayedProvider(provider, 200);
        dp.start();
        cs = (CheckService) osgiHelper.getRawServiceObject(ref_cs);
        assertTrue("Check invocation - 2", cs.check());
        long end = System.currentTimeMillis();

        assertTrue("Assert delay", (end - begin) >= 200);

        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 3", ref_cs);
        cs = (CheckService) osgiHelper.getRawServiceObject(ref_cs);
        assertTrue("Check invocation - 3", cs.check());

        provider.stop();
        provider.dispose();
        under.stop();
        under.dispose();
    }



    @Test
    public void testTimeoutWithException() {
        String prov = "provider";
        ComponentInstance provider = ipojoHelper.createComponentInstance("FooProvider", prov);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("ExceptionCheckServiceProviderTimeout", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getRawServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        delayed = new DelayedProvider(provider, 1000);
        delayed.start();
        cs = (CheckService) osgiHelper.getRawServiceObject(ref_cs);
        try {
            cs.check();
        } catch (RuntimeException e) {
            // OK
            delayed.stop();
            provider.stop();
            provider.dispose();
            under.stop();
            under.dispose();
            return;
        }
        if (TimeUtils.TIME_FACTOR == 1) {
            fail("Timeout expected");
        } else {
            System.err.println("A timeout was expected, however this test really depends on your CPU and IO speed");
        }
    }

    @Test
    public void testDelayOnMultipleDependency() {
        String prov = "provider";
        ComponentInstance provider1 = ipojoHelper.createComponentInstance("FooProvider", prov);
        String prov2 = "provider2";
        ComponentInstance provider2 = ipojoHelper.createComponentInstance("FooProvider", prov2);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("EmptyMultipleCheckServiceProviderTimeout",
                un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getRawServiceObject(ref_cs);
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
        cs = (CheckService) osgiHelper.getRawServiceObject(ref_cs);
        assertTrue("Check invocation - 2", cs.check());
        long end = System.currentTimeMillis();
        System.out.println("delay = " + (end - begin));
        assertTrue("Assert min delay", (end - begin) >= 100);
        assertTrue("Assert max delay", (end - begin) <= 1000);
        dp.stop();
        dp2.stop();

        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 3", ref_cs);
        cs = (CheckService) osgiHelper.getRawServiceObject(ref_cs);
        assertTrue("Check invocation - 3", cs.check());

        provider1.stop();
        provider2.stop();
        provider1.dispose();
        provider2.dispose();
        under.stop();
        under.dispose();
    }

    @Test
    public void testDelayOnCollectionDependency() {
        String prov = "provider";
        ComponentInstance provider1 = ipojoHelper.createComponentInstance("FooProvider", prov);
        String prov2 = "provider2";
        ComponentInstance provider2 = ipojoHelper.createComponentInstance("FooProvider", prov2);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("EmptyColCheckServiceProviderTimeout", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getRawServiceObject(ref_cs);
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
        cs = (CheckService) osgiHelper.getRawServiceObject(ref_cs);
        assertTrue("Check invocation - 2", cs.check());
        long end = System.currentTimeMillis();
        System.out.println("delay = " + (end - begin));
        assertTrue("Assert min delay", (end - begin) >= 100);
        assertTrue("Assert max delay", (end - begin) <= 1000);
        dp.stop();
        dp2.stop();

        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 3", ref_cs);
        cs = (CheckService) osgiHelper.getRawServiceObject(ref_cs);
        assertTrue("Check invocation - 3", cs.check());

        provider1.stop();
        provider2.stop();
        provider1.dispose();
        provider2.dispose();
        under.stop();
        under.dispose();
    }
}
