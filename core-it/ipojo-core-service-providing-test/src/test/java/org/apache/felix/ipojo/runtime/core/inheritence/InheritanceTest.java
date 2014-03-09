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

package org.apache.felix.ipojo.runtime.core.inheritence;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.handlers.dependency.DependencyDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandlerDescription;
import org.apache.felix.ipojo.runtime.core.Common;
import org.apache.felix.ipojo.runtime.core.components.inheritance.a.IA;
import org.apache.felix.ipojo.runtime.core.components.inheritance.b.IB;
import org.apache.felix.ipojo.runtime.core.components.inheritance.c.C;
import org.apache.felix.ipojo.runtime.core.components.inheritance.d.D;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy;

import java.io.File;
import java.io.IOException;

import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

/**
 * TODO This test does not work on KF, for an unknown reason.
 */
public class InheritanceTest extends Common {

    @Override
    public boolean deployTestBundle() {
        return false;
    }

    @Configuration
    public Option[] config() throws IOException {
        Option[] options = super.config();

        return OptionUtils.combine(
                options,
                streamBundle(
                        // Bundle A
                        TinyBundles.bundle()
                                .add(IA.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "A")
                                .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.runtime.core.components.inheritance.a")
                                .build(withBnd())
                ),
                streamBundle(
                        // Bundle B
                        TinyBundles.bundle()
                                .add(IB.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "B")
                                .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.runtime.core.components.inheritance.a")
                                .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.runtime.core.components" +
                                        ".inheritance.b")
                                .build(withBnd())
                ),
                streamBundle(
                        // Bundle C
                        TinyBundles.bundle()
                                .add(C.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "C")
                                .set(Constants.IMPORT_PACKAGE,
                                        "org.apache.felix.ipojo.runtime.core.components.inheritance.a, " +
                                                "org.apache.felix.ipojo.runtime.core.components.inheritance.b")
                                .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/inheritance/provider" +
                                        ".xml")))
                ),
                streamBundle(
                        // Bundle D
                        TinyBundles.bundle()
                                .add(D.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "D")
                                .set(Constants.IMPORT_PACKAGE,
                                        "org.apache.felix.ipojo.runtime.core.components.inheritance.a, " +
                                                "org.apache.felix.ipojo.runtime.core.components.inheritance.b")
                                .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/inheritance/cons" +
                                        ".xml")))
                )


        );
    }

    @Test
    public void testDeploy() {
        if (isKnopflerfish()) {
            System.out.println("Test disabled on knopflerfish");
            return;
        }

        Bundle[] bundles = bc.getBundles();
        for (Bundle bundle : bundles) {
            Assert.assertEquals(bundle.getSymbolicName() + " is not active", Bundle.ACTIVE, bundle.getState());
        }

        osgiHelper.waitForService(Architecture.class.getName(), "(architecture.instance=c)", 10000);
        osgiHelper.waitForService(Architecture.class.getName(), "(architecture.instance=d)", 10000);

        Object[] arch = osgiHelper.getServiceObjects(Architecture.class.getName(), null);
        for (Object o : arch) {
            Architecture a = (Architecture) o;
            if (a.getInstanceDescription().getState() != ComponentInstance.VALID) {
                Assert.fail("Instance " + a.getInstanceDescription().getName() + " not valid : " + a.getInstanceDescription().getDescription());
            }
        }
    }

    @Test
    public void testArchitecture() {
        if (isKnopflerfish()) {
            System.out.println("Test disabled on knopflerfish");
            return;
        }

        osgiHelper.waitForService(Architecture.class.getName(), "(architecture.instance=d)", 10000);
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), "d");
        Assert.assertNotNull(ref);

        Architecture arch = (Architecture) osgiHelper.getRawServiceObject(ref);

        System.out.println(arch.getInstanceDescription().getDescription());

        Assert.assertEquals(ComponentInstance.VALID, arch.getInstanceDescription().getState());
        DependencyDescription dd = getDependency(arch, "org.apache.felix.ipojo.runtime.core.components.inheritance.b.IB");

        Assert.assertTrue(!dd.getServiceReferences().isEmpty());

        ServiceReference dref = (ServiceReference) dd.getServiceReferences().get(0);
        Assert.assertEquals(dref.getBundle().getSymbolicName(), "C");

    }

    private DependencyDescription getDependency(Architecture arch, String id) {
        DependencyHandlerDescription hd = (DependencyHandlerDescription) arch.getInstanceDescription().getHandlerDescription("org.apache.felix.ipojo:requires");
        Assert.assertNotNull(hd);
        for (DependencyDescription dd : hd.getDependencies()) {
            if (dd.getId().equals(id)) {
                return dd;
            }
        }
        Assert.fail("Dependency " + id + " not found");
        return null;
    }

}
