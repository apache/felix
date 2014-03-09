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
import org.apache.felix.ipojo.runtime.test.dependencies.timeout.services.CheckService;
import org.apache.felix.ipojo.runtime.test.dependencies.timeout.services.FooService;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.*;

public class NullTest extends Common {

    @Test
    public void testDelayTimeout() {
        String prov = "provider";
        ComponentInstance provider = ipojoHelper.createComponentInstance("FooProvider", prov);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("NullCheckServiceProviderTimeout", un);

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
}
