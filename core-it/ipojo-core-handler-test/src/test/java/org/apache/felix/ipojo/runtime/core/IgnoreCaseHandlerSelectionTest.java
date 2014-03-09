/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.runtime.core;

import org.apache.commons.io.FileUtils;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.runtime.core.components.MyComponent;
import org.apache.felix.ipojo.runtime.core.handlers.EmptyHandler;
import org.apache.felix.ipojo.runtime.core.services.MyService;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy;

import java.io.File;
import java.io.IOException;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

/**
 * Check that the handler selection ignore case.
 * An empty handler declared with
 * name="EmPtY" and namespace="orG.apAche.feliX.iPOJO.tests.CORE.hAnDlEr"
 * is declared, and two instances using this handler are created. The test is
 * successful is the two instances are created correctly.
 * Test about Felix-1318 : Case mismatch problem of iPOJO custom handler name
 */
public class IgnoreCaseHandlerSelectionTest extends Common {

    @Configuration
    public  Option[] config() throws IOException {

        Option[] options = super.config();

        File tmp = new File("target/bundles");
        tmp.mkdirs();

        // Build handler bundle
        File serviceJar = new File("target/bundles/service.jar");
        FileUtils.copyInputStreamToFile(
                TinyBundles.bundle()
                        .add(MyService.class)
                        .set(Constants.BUNDLE_SYMBOLICNAME, "ServiceInterface")
                        .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.runtime.core.services")
                        .build(withBnd()),
                serviceJar);

        // Build component and handler bundle
        File componentJar = new File("target/bundles/componentAndHandler.jar");
        FileUtils.copyInputStreamToFile(
                TinyBundles.bundle()
                        .add(MyComponent.class)
                        .add(EmptyHandler.class)
                        .set(Constants.BUNDLE_SYMBOLICNAME, "IgnoreCase")
                        .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/ignorecase.xml"))),
                componentJar);

        return OptionUtils.combine(options,
                bundle(serviceJar.toURI().toURL().toExternalForm()),
                bundle(componentJar.toURI().toURL().toExternalForm()));
    }

    @Test
    public void testDeploy() {
        Bundle[] bundles = bc.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            Assert.assertEquals(bundles[i].getSymbolicName() + " is not active", Bundle.ACTIVE, bundles[i].getState());
        }
    }

    /**
     * Checks that the handler is declared and accessible.
     */
    @Test
    public void testHandlerAvailability() {
        ServiceReference[] refs = osgiHelper.getServiceReferences(HandlerFactory.class.getName(), null);
        for (ServiceReference ref : refs) {
            String name = (String) ref.getProperty("handler.name");
            String ns = (String) ref.getProperty("handler.namespace");
            if (name != null
                    && name.equalsIgnoreCase("EmPtY") // Check with ignore case.
                    && ns != null
                    && ns.equalsIgnoreCase("orG.apAche.feliX.iPOJO.tests.CORE.hAnDlEr")) { // Check with ignore case.
                Integer state = (Integer) ref.getProperty("factory.state");
                if (state != null) {
                    Assert.assertEquals(Factory.VALID, state.intValue());
                    return; // Handler found and valid.
                } else {
                    Assert.fail("Handler found but no state exposed");
                }
            }
        }
        Assert.fail("Handler not found");
    }

    /**
     * Check that the instance is correctly created with "empty".
     */
    @Test
    public void testCreationOfIgnoreCase1() {
          ServiceReference refv1 = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), "IgnoreCase-1");
          Assert.assertNotNull(refv1);
          Architecture arch = (Architecture) osgiHelper.getRawServiceObject(refv1);
          Assert.assertEquals(ComponentInstance.VALID, arch.getInstanceDescription().getState());

          HandlerDescription desc = arch.getInstanceDescription()
              .getHandlerDescription("orG.apAche.feliX.iPOJO.tests.CORE.hAnDlEr:EmPtY");  // Check with the declared name.

          Assert.assertNotNull(desc);
          Assert.assertTrue(desc.isValid());
    }

    /**
     * Check that the instance is correctly created with "eMptY".
     */
    @Test
    public void testCreationOfIgnoreCase2() {
          ServiceReference refv1 = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), "IgnoreCase-2");
          Assert.assertNotNull(refv1);
          Architecture arch = (Architecture) osgiHelper.getRawServiceObject(refv1);
          Assert.assertEquals(ComponentInstance.VALID, arch.getInstanceDescription().getState());

          HandlerDescription desc = arch.getInstanceDescription()
              .getHandlerDescription("org.apache.felix.ipojo.tests.core.handler:empty"); // Check with different case.
          Assert.assertNotNull(desc);
          Assert.assertTrue(desc.isValid());
    }


}
