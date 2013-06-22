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

package org.apache.felix.ipojo.tests.core;

import org.apache.commons.io.FileUtils;
import org.apache.felix.ipojo.core.tests.services.MyService;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.ow2.chameleon.testing.helpers.BaseTest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

/**
 * Bootstrap the test from this project
 */
public class Common extends BaseTest {

    @Override
    public boolean deployTestBundle() {
        return false;
    }

    public Option createServiceBundleV1() throws MalformedURLException {
        File out = new File("target/tested/service-v1.jar");

        if (out.exists()) {
            return bundle(out.toURI().toURL().toExternalForm());
        }

        InputStream is = TinyBundles.bundle()
                .add(MyService.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "ServiceInterface")
                .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.core.tests.services")
                .build(withBnd());
        try {
            FileUtils.copyInputStreamToFile(is, out);
            return bundle(out.toURI().toURL().toExternalForm());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot compute the url of the bundle");
        } catch (IOException e) {
            throw new RuntimeException("Cannot write of the bundle");
        }
    }

    public Option createServiceBundleV2() throws MalformedURLException {
        File out = new File("target/tested/service-v2.jar");

        if (out.exists()) {
            return bundle(out.toURI().toURL().toExternalForm());
        }

        InputStream is = TinyBundles.bundle()
                .add(MyService.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "ServiceInterface")
                .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.core.tests.services; version=\"2.0.0\"")
                .build(withBnd());
        try {
            FileUtils.copyInputStreamToFile(is, out);
            return bundle(out.toURI().toURL().toExternalForm());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot compute the url of the bundle");
        } catch (IOException e) {
            throw new RuntimeException("Cannot write of the bundle");
        }
    }

}
