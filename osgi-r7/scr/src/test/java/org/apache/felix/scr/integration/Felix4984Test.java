/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr.integration;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import org.apache.felix.scr.integration.components.felix4984.A;
import org.apache.felix.scr.integration.components.felix4984.B;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.log.LogService;

import junit.framework.TestCase;


@RunWith(JUnit4TestRunner.class)
public class Felix4984Test extends ComponentTestBase
{
    static
    {
        descriptorFile = "/integration_test_FELIX_4984.xml";
        COMPONENT_PACKAGE = COMPONENT_PACKAGE + ".felix4984";
        DS_LOGLEVEL = "debug";
    }

    /**
     * A > 1.1 > B > 0..n > A
     * This test validates that A is bound to one B instance. See FELIX-4984 for more informations.
     */
    @Test
    public void test_A11_B0n_delayed_B_first_ABoundToAtMostOneB() throws Exception
    {
        Bundle bundle = findABBundle(bundleContext);
        bundle.stop();

        for (int i = 0; i < 1000; i ++) {
        	bundle.start();

            String componentNameA = "felix4984.A.1.1.dynamic";
            ComponentConfigurationDTO componentA = findComponentConfigurationByName( componentNameA, ComponentConfigurationDTO.SATISFIED );

            String componentNameB = "felix4984.B.0.n.dynamic";
            final ComponentConfigurationDTO componentB = findComponentConfigurationByName( componentNameB, ComponentConfigurationDTO.SATISFIED);

            ServiceReference[] serviceReferencesB = bundleContext.getServiceReferences( B.class.getName(), "(service.pid=" + componentNameB + ")" );
            assertNotNull( serviceReferencesB );
            TestCase.assertEquals( 1, serviceReferencesB.length );
            ServiceReference serviceReferenceB = serviceReferencesB[0];
            Object serviceB = bundleContext.getService( serviceReferenceB );
            assertNotNull( serviceB );

            ServiceReference[] serviceReferencesA = bundleContext.getServiceReferences( A.class.getName(), "(service.pid=" + componentNameA + ")" );
            TestCase.assertEquals( 1, serviceReferencesA.length );
            ServiceReference serviceReferenceA = serviceReferencesA[0];
            Object serviceA = bundleContext.getService( serviceReferenceA );
            assertNotNull( serviceA );

            A a = getServiceFromConfiguration(componentA, A.class);
            assertABoundToOneB(a);

            bundle.stop();
        }
    }

    private Bundle findABBundle(BundleContext ctx) {
        for (Bundle b : ctx.getBundles()) {
            if (b.getSymbolicName().equals("simplecomponent")) {
                return b;
            }
        }
        throw new IllegalStateException("bundle \"simplecomponent\" not found");
    }

    private void assertABoundToOneB(A a) {
        if (a.getBs().size() != 1) {
            log.log(LogService.LOG_WARNING, "detected problem ...");
            a.dumpStackTracesWhenBWasBound(log);
        }
        assertEquals( 1, a.getBs().size());
    }
}
