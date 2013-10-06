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
package org.apache.felix.ipojo.runtime.core;

import aQute.lib.osgi.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.runtime.core.services.BazService;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.ow2.chameleon.testing.helpers.BaseTest;
import org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.ops4j.pax.exam.CoreOptions.bundle;

/**
 * Checks that native methods can still be used in components.
 */
public class TestNativeMethod extends BaseTest {

    private static final String NATIVE_CLAUSE = "" +
            //Mac
            "libs/mac/libfoo.jnilib;osname=MacOSX;osname=MacOS;processor=x86;processor=x86_64;processor=PowerPC," +
            // Linux 32 bits
            "libs/linux64/libfoo.so;processor=x86_64;osname=Linux," +
            // Linux 64 bits
            "libs/linux32/libfoo.so;processor=x86;osname=Linux";

    /**
     * We don't deploy the test bundle, a specific one will be built.
     * On KF we still deploy the bundle as the probe bundles needs the components and services.
     */
    @Override
    public boolean deployTestBundle() {
        return isKnopflerfish();
    }

    public boolean isKnopflerfish() {
        if (context != null) {
            return super.isKnopflerfish();
        } else {
            String pf = System.getProperty("pax.exam.framework");
            return pf != null  && pf.equalsIgnoreCase("knopflerfish");
        }
    }

    @Override
    protected Option[] getCustomOptions() {
        // The native bundle cannot be deployed on kf,
        // just skip
        if (isKnopflerfish()) {
            return new Option[0];
        }
        return new Option[] {
                buildBundleWithNativeLibraries()
        };
    }

    @Test
    public void testComponentWithNativeMethod() {
        if (isKnopflerfish()) {
            System.out.println("Test not supported on knopflerfish");
            return;
        }

        ComponentInstance ci = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.components" +
                ".nativ.NativeComponent");

        BazService baz = osgiHelper.getServiceObject(BazService.class, "(instance.name=" + ci.getInstanceName() +")");
        assertEquals("foo: Test program of JNI.", baz.hello(""));
    }


    public static Option buildBundleWithNativeLibraries() {
        File out = new File("target/tested/test-bundle-with-native.jar");
        if (out.exists()) {
            try {
                return bundle(out.toURI().toURL().toExternalForm());
            } catch (MalformedURLException e) {
                // Ignore it.
            }
        }

        TinyBundle tested = TinyBundles.bundle();

        // We look inside target/classes to find the class and resources
        File classes = new File("target/classes");
        Collection<File> files = FileUtils.listFilesAndDirs(classes, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        List<String> exports = new ArrayList<String>();
        for (File file : files) {
            if (file.isDirectory()) {
                // By convention we export of .services and .service package
                if (file.getAbsolutePath().contains("/services")  || file.getAbsolutePath().contains("/service")) {
                    String path = file.getAbsolutePath().substring(classes.getAbsolutePath().length() +1);
                    String packageName = path.replace('/', '.');
                    exports.add(packageName);
                }
            } else {
                // We need to compute the path
                String path = file.getAbsolutePath().substring(classes.getAbsolutePath().length() +1);
                try {
                    tested.add(path, file.toURI().toURL());
                } catch (MalformedURLException e) {
                    // Ignore it.
                }
                System.out.println(file.getName() + " added to " + path);
            }
        }

        // Depending on the the order, the probe bundle may already have detected requirements on components.
        String clause = "" +
                "org.apache.felix.ipojo.runtime.core.components, " +
                "org.apache.felix.ipojo.runtime.core.services, " +
                "org.apache.felix.ipojo.runtime.core.services.A123";
        for (String export : exports) {
            if (export.length() > 0) { export += ", "; }
            clause += export;
        }

        System.out.println("Exported packages : " + clause);

        InputStream inputStream = tested
                .set(Constants.BUNDLE_SYMBOLICNAME, BaseTest.TEST_BUNDLE_SYMBOLIC_NAME + "-with-native")
                .set(Constants.IMPORT_PACKAGE, "*")
                .set(Constants.EXPORT_PACKAGE, clause)
                .set(Constants.BUNDLE_NATIVECODE, NATIVE_CLAUSE)
                .build(IPOJOStrategy.withiPOJO(new File("src/main/resources")));

        try {
            copyInputStreamToFile(inputStream, out);
            return bundle(out.toURI().toURL().toExternalForm());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot compute the url of the manipulated bundle");
        } catch (IOException e) {
            throw new RuntimeException("Cannot write of the manipulated bundle");
        }
    }


}
