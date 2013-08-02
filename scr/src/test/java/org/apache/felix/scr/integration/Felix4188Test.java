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
package org.apache.felix.scr.integration;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import junit.framework.TestCase;
import org.apache.felix.scr.Component;
import org.apache.felix.scr.integration.components.felix4188.Felix4188Component;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

/**
 * This test validates the FELIX-4188 issue.
 */
@RunWith(JUnit4TestRunner.class)
public class Felix4188Test extends ComponentTestBase
{

    static
    {
        // uncomment to enable debugging of this test class
        //        paxRunnerVmOption = DEBUG_VM_OPTION;
        descriptorFile = "/integration_test_FELIX_4188.xml";
        restrictedLogging = true;
        //comment to get debug logging if the test fails.
//        DS_LOGLEVEL = "warn";
    }

    @Inject
    protected BundleContext bundleContext;

    @Test
    public void test_concurrent_deactivation() throws Exception
    {
        final Bundle bundle1 = installBundle("/integration_test_FELIX_4188_1.xml", "", "simplecomponent1");
        bundle1.start();

        final Bundle bundle2 = installBundle("/integration_test_FELIX_4188_2.xml", "", "simplecomponent2");
        bundle2.start();

        final Component aComp1 =
                findComponentByName("org.apache.felix.scr.integration.components.Felix4188Component-1");
        aComp1.enable();
        final Object aInst1 = aComp1.getComponentInstance().getInstance();

        final Component aComp2 =
                findComponentByName("org.apache.felix.scr.integration.components.Felix4188Component-2");
        aComp2.enable();
        final Object aInst2 = aComp2.getComponentInstance().getInstance();

        final CountDownLatch latch = new CountDownLatch(1);

        new Thread() {
            public void run() {
                Bundle scrBundle = scrTracker.getServiceReference().getBundle();
                try {
                    scrBundle.stop();
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }
        }.start();
        Thread.sleep(100);
        long t0 = System.currentTimeMillis();
        bundle1.stop();
        bundle2.stop();
        long t1 = System.currentTimeMillis();
        TestCase.assertTrue(t1 - t0 > 1000);  // It should have taken more than a second

        TestCase.assertNull(getField(aInst1, "throwable"));
        TestCase.assertNull(getField(aInst2, "throwable"));

        latch.await();

        TestCase.assertNull(getField(aInst1, "throwable"));
        TestCase.assertNull(getField(aInst2, "throwable"));
    }

    private Object getField(Object instance, String name) throws Exception {
        Field field = instance.getClass().getField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    protected Bundle installBundle( final String descriptorFile, String componentPackage, String symbolicname ) throws BundleException
    {
        final InputStream bundleStream = bundle()
                .add("OSGI-INF/components.xml", getClass().getResource(descriptorFile))
                .add(Felix4188Component.class)

                .set(Constants.BUNDLE_SYMBOLICNAME, symbolicname)
                .set(Constants.BUNDLE_VERSION, "0.0.11")
                .set(Constants.IMPORT_PACKAGE, componentPackage)
                .set("Service-Component", "OSGI-INF/components.xml")
                .build(withBnd());

        try
        {
            final String location = "test:SimpleComponent/" + System.currentTimeMillis();
            return bundleContext.installBundle( location, bundleStream );
        }
        finally
        {
            try
            {
                bundleStream.close();
            }
            catch ( IOException ioe )
            {
            }
        }
    }

}
