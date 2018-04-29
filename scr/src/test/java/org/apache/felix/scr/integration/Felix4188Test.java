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

<<<<<<< HEAD
=======
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

<<<<<<< HEAD
import junit.framework.TestCase;
import org.apache.felix.scr.Component;
import org.apache.felix.scr.integration.components.felix4188.Felix4188Component;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
=======
import org.apache.felix.scr.integration.components.felix4188.Felix4188Component;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
<<<<<<< HEAD

import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;
=======
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

import junit.framework.Assert;
import junit.framework.TestCase;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

/**
 * This test validates the FELIX-4188 issue.
 */
<<<<<<< HEAD
@RunWith(JUnit4TestRunner.class)
=======
@RunWith(PaxExam.class)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
public class Felix4188Test extends ComponentTestBase
{

    static
    {
        // uncomment to enable debugging of this test class
<<<<<<< HEAD
        //        paxRunnerVmOption = DEBUG_VM_OPTION;
        descriptorFile = "/integration_test_FELIX_4188.xml";
        restrictedLogging = true;
        //comment to get debug logging if the test fails.
//        DS_LOGLEVEL = "warn";
=======
        //                paxRunnerVmOption = DEBUG_VM_OPTION;
        descriptorFile = "/integration_test_FELIX_4188.xml";
        //        restrictedLogging = true;
        //comment to get debug logging if the test fails.
        //        DS_LOGLEVEL = "warn";
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    @Inject
    protected BundleContext bundleContext;

    @Test
    public void test_concurrent_deactivation() throws Exception
    {
<<<<<<< HEAD
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
=======
        final Bundle bundle1 = installBundle("/integration_test_FELIX_4188_1.xml", "org.apache.felix.scr.integration.components", "simplecomponent1");
        bundle1.start();

        final Bundle bundle2 = installBundle("/integration_test_FELIX_4188_2.xml", "org.apache.felix.scr.integration.components", "simplecomponent2");
        bundle2.start();

        final ComponentConfigurationDTO aComp1 =
                findComponentConfigurationByName( bundle1, "org.apache.felix.scr.integration.components.Felix4188Component-1", ComponentConfigurationDTO.SATISFIED);
        final Object aInst1 = getServiceFromConfigurationInAllClassSpaces(aComp1, Felix4188Component.class.getName());

        final ComponentConfigurationDTO aComp2 =
                findComponentConfigurationByName( bundle2, "org.apache.felix.scr.integration.components.Felix4188Component-2", ComponentConfigurationDTO.SATISFIED);
        final Object aInst2 = getServiceFromConfigurationInAllClassSpaces(aComp2, Felix4188Component.class.getName());
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        final CountDownLatch latch = new CountDownLatch(1);

        new Thread() {
<<<<<<< HEAD
=======
            @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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

<<<<<<< HEAD
=======
    // Note that this test installs two bundles both with the same class in it.
    // This causes multiple class spaces to be created by the framework.
    private Object getServiceFromConfigurationInAllClassSpaces( ComponentConfigurationDTO dto, String clazz ) throws InvalidSyntaxException
    {
        long id = dto.id;
        String filter = "(component.id=" + id + ")";
        ServiceReference<?>[] srs;

        srs = bundleContext.getAllServiceReferences(clazz, filter);
        Assert.assertEquals(1, srs.length);
        ServiceReference<?> sr = srs[0];
        Object s = bundleContext.getService(sr);
        Assert.assertNotNull(s);
        return s;
    }


>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
