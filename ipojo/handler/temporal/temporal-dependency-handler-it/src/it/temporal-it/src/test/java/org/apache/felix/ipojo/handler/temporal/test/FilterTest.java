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

import java.util.Dictionary;
import java.util.Hashtable;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks <tt>requires.filter</tt>, <tt>temporal.filter</tt>, <tt>temporal.from</tt> and
 * <tt>requires.from</tt> attributes.
 */
public class FilterTest extends Common {

    /**
     * Checks <tt>temporal.filter</tt> with dependency id.
     * The filter is made to get only one provider.
     */
    @Test
    public void testWithTemporalFilters() {
        String prov = "provider";
        ComponentInstance provider1 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String prov2 = "provider2";
        ComponentInstance provider2 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov2);

        Dictionary configuration = new Hashtable();
        String un = "under-1";
        configuration.put("instance.name", un);
        Dictionary filter = new Hashtable();
        filter.put("foo", "(instance.name=provider2)");
        configuration.put("temporal.filters", filter);
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-MultipleCheckServiceProvider",
                configuration);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider2.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        long begin = System.currentTimeMillis();
        DelayedProvider dp2 = new DelayedProvider(provider2, 100);
        dp2.start();
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 2", cs.check());
        long end = System.currentTimeMillis();
        System.out.println("delay = " + (end - begin));
        assertTrue("Assert min delay", (end - begin) >= 100);
        assertTrue("Assert max delay", (end - begin) <= 1000);
        dp2.stop();

        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 3", ref_cs);
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 3", cs.check());

        provider1.stop();
        provider2.stop();
        provider1.dispose();
        provider2.dispose();
        under.stop();
        under.dispose();
    }

    /**
     * Checks <tt>requires.filter</tt> with dependency id.
     * The filter is made to get only one provider.
     */
    @Test
    public void testWithRequireFilters() {
        String prov = "provider";
        ComponentInstance provider1 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String prov2 = "provider2";
        ComponentInstance provider2 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov2);

        Dictionary configuration = new Hashtable();
        String un = "under-1";
        configuration.put("instance.name", un);
        Dictionary filter = new Hashtable();
        filter.put("foo", "(instance.name=provider2)");
        configuration.put("requires.filters", filter);
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-MultipleCheckServiceProvider", configuration);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider2.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        long begin = System.currentTimeMillis();
        DelayedProvider dp2 = new DelayedProvider(provider2, 100);
        dp2.start();
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 2", cs.check());
        long end = System.currentTimeMillis();
        System.out.println("delay = " + (end - begin));
        assertTrue("Assert min delay", (end - begin) >= 100);
        assertTrue("Assert max delay", (end - begin) <= 1000);
        dp2.stop();

        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 3", ref_cs);
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 3", cs.check());

        provider1.stop();
        provider2.stop();
        provider1.dispose();
        provider2.dispose();
        under.stop();
        under.dispose();
    }

    /**
     * Checks <tt>temporal.from</tt> with dependency id.
     * The filter is made to get only one specific provider.
     */
    @Test
    public void testWithTemporalFrom() {
        String prov = "provider";
        ComponentInstance provider1 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String prov2 = "provider2";
        ComponentInstance provider2 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov2);

        Dictionary configuration = new Hashtable();
        String un = "under-1";
        configuration.put("instance.name", un);
        Dictionary filter = new Hashtable();
        filter.put("foo", "provider2");
        configuration.put("temporal.from", filter);
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-MultipleCheckServiceProvider", configuration);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider2.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        long begin = System.currentTimeMillis();
        DelayedProvider dp2 = new DelayedProvider(provider2, 100);
        dp2.start();
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 2", cs.check());
        long end = System.currentTimeMillis();
        System.out.println("delay = " + (end - begin));
        assertTrue("Assert min delay", (end - begin) >= 100);
        assertTrue("Assert max delay", (end - begin) <= 1000);
        dp2.stop();

        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 3", ref_cs);
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 3", cs.check());

        provider1.stop();
        provider2.stop();
        provider1.dispose();
        provider2.dispose();
        under.stop();
        under.dispose();
    }

    /**
     * Checks <tt>requires.from</tt> with dependency id.
     * The filter is made to get only one specific provider.
     */
    @Test
    public void testWithRequiresFrom() {
        String prov = "provider";
        ComponentInstance provider1 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String prov2 = "provider2";
        ComponentInstance provider2 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov2);

        Dictionary configuration = new Hashtable();
        String un = "under-1";
        configuration.put("instance.name", un);
        Dictionary filter = new Hashtable();
        filter.put("foo", "provider2");
        configuration.put("requires.from", filter);
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-MultipleCheckServiceProvider", configuration);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider2.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        long begin = System.currentTimeMillis();
        DelayedProvider dp2 = new DelayedProvider(provider2, 100);
        dp2.start();
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 2", cs.check());
        long end = System.currentTimeMillis();
        System.out.println("delay = " + (end - begin));
        assertTrue("Assert min delay", (end - begin) >= 100);
        assertTrue("Assert max delay", (end - begin) <= 1000);
        dp2.stop();

        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 3", ref_cs);
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation - 3", cs.check());

        provider1.stop();
        provider2.stop();
        provider1.dispose();
        provider2.dispose();
        under.stop();
        under.dispose();
    }
}
