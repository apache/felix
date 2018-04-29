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
<<<<<<< HEAD
import org.junit.Test;
import org.osgi.framework.ServiceReference;
=======
import org.junit.After;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.helpers.TimeUtils;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

import static org.junit.Assert.*;

public class TimeoutTest extends Common {

<<<<<<< HEAD
=======
    private DelayedProvider delayed;

    @After
    public void tearDown() {
        if (delayed != null) {
            delayed.stop();
        }
    }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    @Test
    public void testDelay() {
        String prov = "provider";
        ComponentInstance provider = ipojoHelper.createComponentInstance("FooProvider", prov);
        String un = "under-1";
        ComponentInstance under = ipojoHelper.createComponentInstance("CheckServiceProviderTimeout", un);

        ServiceReference ref_fs = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), prov);
        assertNotNull("Check foo availability", ref_fs);

        ServiceReference ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability", ref_cs);

<<<<<<< HEAD
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
=======
        CheckService cs = (CheckService) osgiHelper.getRawServiceObject(ref_cs);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider.stop();
        assertNull("No FooService", osgiHelper.getServiceReference(FooService.class.getName(), null));
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
        long begin = System.currentTimeMillis();
        DelayedProvider dp = new DelayedProvider(provider, 200);
        dp.start();
<<<<<<< HEAD
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
=======
        cs = (CheckService) osgiHelper.getRawServiceObject(ref_cs);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        assertTrue("Check invocation - 2", cs.check());
        long end = System.currentTimeMillis();

        assertTrue("Assert delay (" + (end - begin) + ")", (end - begin) >= 200);

        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 3", ref_cs);
<<<<<<< HEAD
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
=======
        cs = (CheckService) osgiHelper.getRawServiceObject(ref_cs);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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

<<<<<<< HEAD
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
=======
        CheckService cs = (CheckService) osgiHelper.getRawServiceObject(ref_cs);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        assertTrue("Check invocation", cs.check());

        // Stop the provider.
        provider.stop();
        ref_cs = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), un);
        assertNotNull("Check cs availability - 2", ref_cs);
<<<<<<< HEAD
        DelayedProvider dp = new DelayedProvider(provider, 400);
        dp.start();
        cs = (CheckService) osgiHelper.getServiceObject(ref_cs);
=======
        delayed = new DelayedProvider(provider, 1000);
        delayed.start();
        cs = (CheckService) osgiHelper.getRawServiceObject(ref_cs);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        try {
            cs.check();
        } catch (RuntimeException e) {
            // OK
<<<<<<< HEAD
            dp.stop();
=======
            delayed.stop();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            provider.stop();
            provider.dispose();
            under.stop();
            under.dispose();
            return;
        }

<<<<<<< HEAD
        fail("Timeout expected");
=======
        if (TimeUtils.TIME_FACTOR == 1) {
            fail("An exception was expected ...");
        } else {
            System.err.println("An exception was expected, however this test really depends on your CPU and IO " +
                    "speed");
        }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }
}
