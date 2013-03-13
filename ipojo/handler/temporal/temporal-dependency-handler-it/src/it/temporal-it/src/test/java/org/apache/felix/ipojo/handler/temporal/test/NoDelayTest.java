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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NoDelayTest extends Common {

    @Test
    public void testNoDelay() {
        String prov = "provider";
        ComponentInstance provider = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-CheckServiceProvider", un);
        assertNotNull("Check creation", under);
        assertNotNull("Check provider creation", prov);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        provider.stop();
        provider.dispose();
        under.stop();
        under.dispose();
    }

    @Test
    public void testNoDelayWithProxy() {
        String prov = "provider";
        ComponentInstance provider = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-ProxiedCheckServiceProvider", un);
        assertNotNull("Check creation", under);
        assertNotNull("Check provider creation", prov);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        provider.stop();
        provider.dispose();
        under.stop();
        under.dispose();
    }

    @Test
    public void testMultipleNoDelay() {
        String prov1 = "provider-1";
        ComponentInstance provider1 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov1);
        String un = "under-2";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-MultipleCheckServiceProvider", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov1);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        provider1.stop();
        provider1.dispose();
        under.stop();
        under.dispose();
    }

    @Test
    public void testCollectionNoDelay() {
        String prov1 = "provider-1";
        ComponentInstance provider1 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov1);
        String un = "under-2";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-ColCheckServiceProvider", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov1);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        provider1.stop();
        provider1.dispose();
        under.stop();
        under.dispose();
    }

    @Test
    public void testProxiedCollectionNoDelay() {
        String prov1 = "provider-1";
        ComponentInstance provider1 = ipojoHelper.createComponentInstance("TEMPORAL-FooProvider", prov1);
        String un = "under-2";
        ComponentInstance under = ipojoHelper.createComponentInstance("TEMPORAL-ProxiedColCheckServiceProvider", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov1);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
        assertTrue("Check invocation", cs.check());

        provider1.stop();
        provider1.dispose();
        under.stop();
        under.dispose();
    }

}
