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

package org.apache.felix.ipojo.test.compatibility;

import org.apache.commons.io.FileUtils;
import org.apache.felix.ipojo.test.compatibility.ipojo.HelloServiceConsumer;
import org.apache.felix.ipojo.test.compatibility.ipojo.HelloServiceProvider;
import org.apache.felix.ipojo.test.compatibility.service.CheckService;
import org.apache.felix.ipojo.test.compatibility.service.HelloService;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.ow2.chameleon.testing.helpers.BaseTest;
import org.ow2.chameleon.testing.helpers.FrameworkHelper;
import org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy;

import java.io.*;
import java.net.MalformedURLException;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

/**
 * Configure the tests.
 */
public abstract class Common extends BaseTest {

    @Override
    public boolean deployTestBundle() {
        return false;
    }

    @Override
    protected Option[] getCustomOptions() {
        Option[] options = new Option[] {
                service(),
                wrappedBundle(maven("org.easytesting", "fest-assert").versionAsInProject()),
                wrappedBundle(maven("org.easytesting", "fest-util").versionAsInProject())
        };

        return OptionUtils.combine(options, bundles());
    }

    public abstract Option[] bundles();

    /**
     * Package the service interfaces.
     */
    public Option service() {
        File out = new File("target/bundles/service.jar");
        if (out.exists()) {
            try {
                return bundle(out.toURI().toURL().toExternalForm());
            } catch (MalformedURLException e) {
                // Ignore it.
            }
        }

        TinyBundle bundle = TinyBundles.bundle();
        bundle.add(CheckService.class);
        bundle.add(HelloService.class);
        InputStream inputStream = bundle
                .set(Constants.BUNDLE_SYMBOLICNAME, "services")
                .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.test.compatibility.service")
                .build(withBnd());

        try {
            FileUtils.copyInputStreamToFile(inputStream, out);
            return bundle(out.toURI().toURL().toExternalForm());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * iPOJO Hello Service Provider.
     */
    public Option iPOJOHelloProvider() {
        File out = new File("target/bundles/hello-provider-ipojo.jar");
        if (out.exists()) {
            try {
                return bundle(out.toURI().toURL().toExternalForm());
            } catch (MalformedURLException e) {
                // Ignore it.
            }
        }

        TinyBundle bundle = TinyBundles.bundle();
        bundle.add(HelloServiceProvider.class);
        InputStream inputStream = bundle
                .set(Constants.BUNDLE_SYMBOLICNAME, "iPOJO-Hello-Provider")
                //.set(Constants.IMPORT_PACKAGE, "*")
                .build(IPOJOStrategy.withiPOJO());

        try {
            FileUtils.copyInputStreamToFile(inputStream, out);
            return bundle(out.toURI().toURL().toExternalForm());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * iPOJO Hello Service Provider.
     */
    public Option iPOJOHelloConsumer() {
        File out = new File("target/bundles/hello-consumer-ipojo.jar");
        if (out.exists()) {
            try {
                return bundle(out.toURI().toURL().toExternalForm());
            } catch (MalformedURLException e) {
                // Ignore it.
            }
        }

        TinyBundle bundle = TinyBundles.bundle();
        bundle.add(HelloServiceConsumer.class);
        InputStream inputStream = bundle
                .set(Constants.BUNDLE_SYMBOLICNAME, "iPOJO-Hello-Consumer")
                //.set(Constants.IMPORT_PACKAGE, "*")
                .build(IPOJOStrategy.withiPOJO());

        try {
            FileUtils.copyInputStreamToFile(inputStream, out);
            return bundle(out.toURI().toURL().toExternalForm());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Package the SCR Hello Service Provider.
     */
    public Option SCRHelloProvider() {
        File out = new File("target/bundles/hello-provider-scr.jar");
        if (out.exists()) {
            try {
                return bundle(out.toURI().toURL().toExternalForm());
            } catch (MalformedURLException e) {
                // Ignore it.
            }
        }

        File metadata = new File("src/main/resources/scr", "HelloProvider.xml");

        TinyBundle bundle = TinyBundles.bundle();
        bundle.add(org.apache.felix.ipojo.test.compatibility.scr.HelloServiceProvider.class);
        try {
            bundle.add("scr/provider.xml", new FileInputStream(metadata));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Cannot find XML metadata : " + metadata.getAbsolutePath());
        }

        InputStream inputStream = bundle
                .set(Constants.BUNDLE_SYMBOLICNAME, "hello-provider-scr")
                //.set(Constants.IMPORT_PACKAGE, "*")
                .set("Service-Component", "scr/provider.xml")
                .build(withBnd());

        try {
            FileUtils.copyInputStreamToFile(inputStream, out);
            return bundle(out.toURI().toURL().toExternalForm());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Option SCRHelloConsumer() {
        File out = new File("target/bundles/hello-consumer-scr.jar");
        if (out.exists()) {
            try {
                return bundle(out.toURI().toURL().toExternalForm());
            } catch (MalformedURLException e) {
                // Ignore it.
            }
        }

        File metadata = new File("src/main/resources/scr", "HelloConsumer.xml");

        TinyBundle bundle = TinyBundles.bundle();
        bundle.add(org.apache.felix.ipojo.test.compatibility.scr.HelloServiceConsumer.class);
        try {
            bundle.add("scr/consumer.xml", new FileInputStream(metadata));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Cannot find XML metadata : " + metadata.getAbsolutePath());
        }

        InputStream inputStream = bundle
                .set(Constants.BUNDLE_SYMBOLICNAME, "hello-consumer-scr")
               // .set(Constants.IMPORT_PACKAGE, "*")
                .set("Service-Component", "scr/consumer.xml")
                .build(withBnd());

        try {
            FileUtils.copyInputStreamToFile(inputStream, out);
            return bundle(out.toURI().toURL().toExternalForm());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Package the Blueprint Hello Service Provider.
     */
    public Option BPHelloProvider() {
        File out = new File("target/bundles/hello-provider-blueprint.jar");
        if (out.exists()) {
            try {
                return bundle(out.toURI().toURL().toExternalForm());
            } catch (MalformedURLException e) {
                // Ignore it.
            }
        }

        File metadata = new File("src/main/resources/blueprint", "HelloProvider.xml");

        TinyBundle bundle = TinyBundles.bundle();
        bundle.add(org.apache.felix.ipojo.test.compatibility.scr.HelloServiceProvider.class);
        try {
            bundle.add("blueprint/provider.xml", new FileInputStream(metadata));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Cannot find XML metadata : " + metadata.getAbsolutePath());
        }

        InputStream inputStream = bundle
                .set(Constants.BUNDLE_SYMBOLICNAME, "hello-provider-blueprint")
               // .set(Constants.IMPORT_PACKAGE, "*")
                .set("Bundle-Blueprint", "blueprint/provider.xml")
                .build(withBnd());

        try {
            FileUtils.copyInputStreamToFile(inputStream, out);
            return bundle(out.toURI().toURL().toExternalForm());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Option BPHelloConsumer() {
        File out = new File("target/bundles/hello-consumer-blueprint.jar");
        if (out.exists()) {
            try {
                return bundle(out.toURI().toURL().toExternalForm());
            } catch (MalformedURLException e) {
                // Ignore it.
            }
        }

        File metadata = new File("src/main/resources/blueprint", "HelloConsumer.xml");

        TinyBundle bundle = TinyBundles.bundle();
        bundle.add(org.apache.felix.ipojo.test.compatibility.scr.HelloServiceConsumer.class);
        try {
            bundle.add("blueprint/consumer.xml", new FileInputStream(metadata));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Cannot find XML metadata : " + metadata.getAbsolutePath());
        }

        InputStream inputStream = bundle
                .set(Constants.BUNDLE_SYMBOLICNAME, "hello-consumer-blueprint")
               // .set(Constants.IMPORT_PACKAGE, "*")
                .set("Bundle-Blueprint", "blueprint/consumer.xml")
                .build(withBnd());

        try {
            FileUtils.copyInputStreamToFile(inputStream, out);
            return bundle(out.toURI().toURL().toExternalForm());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isEquinox() {
        if (context != null) {
            return FrameworkHelper.isEquinox(context)  || context.toString().contains("eclipse");
        } else {
            String pf = System.getProperty("pax.exam.framework");
            return pf != null  && pf.equalsIgnoreCase("equinox");
        }
    }
}
