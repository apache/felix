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
import org.apache.felix.ipojo.runtime.core.components.MyComponent;
import org.apache.felix.ipojo.runtime.core.components.configuration.MyConfiguration;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy;

import java.io.IOException;

import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

/**
 * Check @Configuration embedded in another bundle.
 */
public class TestConfigurationInAnotherBundle extends Common {

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
                                .add(MyComponent.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "MyComponent")
                                .build(IPOJOStrategy.withiPOJO())
                ),
                streamBundle(
                        TinyBundles.bundle()
                                .add(MyConfiguration.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "Configuration")
                                .build(IPOJOStrategy.withiPOJO())
                )
        );
    }

    /**
     * <ol>
     * <li>Check when all bundles are deployed</li>
     * <li>Check when the configuration bundle is stopped</li>
     * <li>Check when the configuration is restarted</li>
     * <li>Check when the component bundle is stopped</li>
     * <li>Check when the component bundle is restarted</li>
     * </ol>
     */
    @Test
    public void testDynamism() throws BundleException {
        if (isKnopflerfish()) {
            return; // Test disabled on KF
        }
        //1)
        osgiHelper.waitForService(FooService.class, null, 10000);
        Assert.assertNotNull(osgiHelper.getServiceReference(FooService.class));

        //2) Stopping configuration bundle
        osgiHelper.getBundle("Configuration").stop();
        Assert.assertNull(osgiHelper.getServiceReference(FooService.class));

        //3) Restart configuration bundle
        osgiHelper.getBundle("Configuration").start();
        osgiHelper.waitForService(FooService.class, null, 10000);
        Assert.assertNotNull(osgiHelper.getServiceReference(FooService.class));

        //4) Stop the component bundle
        osgiHelper.getBundle("MyComponent").stop();
        Assert.assertNull(osgiHelper.getServiceReference(FooService.class));

        //5) Restart the component bundle
        osgiHelper.getBundle("MyComponent").start();
        osgiHelper.waitForService(FooService.class, null, 10000);
        Assert.assertNotNull(osgiHelper.getServiceReference(FooService.class));
    }


}
