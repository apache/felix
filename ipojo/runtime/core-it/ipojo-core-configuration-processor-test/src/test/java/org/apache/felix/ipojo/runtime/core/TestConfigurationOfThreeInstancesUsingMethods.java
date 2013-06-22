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

import junit.framework.Assert;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.runtime.core.components.ConfigureThreeInstancesUsingMethods;
import org.apache.felix.ipojo.runtime.core.components.ConfigureTwoInstances;
import org.apache.felix.ipojo.runtime.core.components.MyComponent;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.helpers.TimeUtils;
import org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy;

import java.io.IOException;
import java.util.List;

import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

/**
 * Check a @Configuration with 2 instances.
 */
public class TestConfigurationOfThreeInstancesUsingMethods extends Common {

    @Configuration
    public Option[] config() throws IOException {

        Option[] options = super.config();

        // Build a service bundle
        return OptionUtils.combine(options,
                streamBundle(
                        TinyBundles.bundle()
                                .add(FooService.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "ServiceInterface")
                                .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.runtime.core.services")
                                .build(withBnd())
                ),
                streamBundle(
                        TinyBundles.bundle()
                                .add(ConfigureThreeInstancesUsingMethods.class)
                                .add(MyComponent.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "MyComponent")
                                .build(IPOJOStrategy.withiPOJO())
                )
        );
    }

    @Test
    public void testConfiguration() {
        if (isKnopflerfish()) {
            return; // Test disabled on KF
        }

        TimeUtils.grace(500);
        osgiHelper.waitForService(FooService.class, null, 10000);

        // Check we have three instances
        ServiceReference[] refs = osgiHelper.getServiceReferences(FooService.class, null);
        Assert.assertEquals(refs.length, 3);

        List<Architecture> arch = osgiHelper.getServiceObjects(Architecture.class, null);
        for (Architecture a : arch) {
            System.out.println("Instance " + a.getInstanceDescription().getName());
        }

        // Check name
        Assert.assertNotNull(ipojoHelper.getServiceReferenceByName(FooService.class.getName(), "myInstance"));
        Assert.assertNotNull(ipojoHelper.getServiceReferenceByName(FooService.class.getName(), "instance1"));
        Assert.assertNotNull(ipojoHelper.getServiceReferenceByName(FooService.class.getName(), "instance2"));

    }


}
