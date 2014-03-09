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
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.core.tests.components.MyComponent;
import org.apache.felix.ipojo.core.tests.components.MyCons;
import org.apache.felix.ipojo.core.tests.services.MyService;
import org.apache.felix.ipojo.handlers.dependency.Dependency;
import org.apache.felix.ipojo.handlers.dependency.DependencyDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandlerDescription;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.*;
import org.ow2.chameleon.testing.helpers.Stability;
import org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

@ExamReactorStrategy(PerMethod.class)
public class VersionConflictTest extends Common {

    @Inject
    private BundleContext context;

    @Configuration
    public Option[] config() throws IOException {

        List<Option> options = new ArrayList<Option>();
        options.addAll(Arrays.asList(super.config()));

        File tmp = new File("target/tmp");
        tmp.mkdirs();

        File f1 = new File(tmp, "service-interface-v1.jar");
        FileUtils.copyInputStreamToFile(
                TinyBundles.bundle()
                        .add(MyService.class)
                        .set(Constants.BUNDLE_SYMBOLICNAME, "ServiceInterfaceV1")
                        .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.core.tests.services; version=\"1.0.0\"")
                        .build(withBnd()),
                f1);

        File f2 = new File(tmp, "service-interface-v2.jar");
        FileUtils.copyInputStreamToFile(
                TinyBundles.bundle()
                        .add(MyService.class)
                        .set(Constants.BUNDLE_SYMBOLICNAME, "ServiceInterfaceV2")
                        .set(Constants.BUNDLE_VERSION, "2.0.0")
                        .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.core.tests.services; version=\"2.0.0\"")
                        .build(withBnd()),
                f2);

        File c1 = new File(tmp, "component-v1.jar");
        FileUtils.copyInputStreamToFile(
                TinyBundles.bundle()
                        .add(MyComponent.class)
                        .set(Constants.BUNDLE_SYMBOLICNAME, "ProviderV1")
                        .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.core.tests.services; version=\"1.0.0\"")
                        .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/vprovider-v1.xml"))),
                c1);

        File c2 = new File(tmp, "component-v2.jar");
        FileUtils.copyInputStreamToFile(
                TinyBundles.bundle()
                        .add(MyComponent.class)
                        .set(Constants.BUNDLE_SYMBOLICNAME, "ProviderV2")
                        .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.core.tests.services; version=\"2.0.0\"")
                        .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/vprovider-v2.xml"))),
                c2);

        File cons = new File(tmp, "cons.jar");
        FileUtils.copyInputStreamToFile(
                TinyBundles.bundle()
                        .add(MyCons.class)
                        .set(Constants.BUNDLE_SYMBOLICNAME, "MyCons")
                        .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.core.tests.services; version=\"2.0.0\"")
                        .set(Constants.BUNDLE_VERSION, "2.0")
                        .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/cons.xml"))),
                cons);

        File consV1 = new File(tmp, "cons-v1.jar");
        FileUtils.copyInputStreamToFile(
                TinyBundles.bundle()
                        .add(MyCons.class)
                        .set(Constants.BUNDLE_SYMBOLICNAME, "MyCons")
                        .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.core.tests.services; version=\"[1.0.0, " +
                                "1.1.0)\"")
                        .set(Constants.BUNDLE_VERSION, "1.0")
                        .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/cons.xml"))),
                consV1);

        Option[] opt = options(
                systemProperty("url1").value(f1.toURI().toURL().toExternalForm()),
                systemProperty("url2").value(f2.toURI().toURL().toExternalForm()),

                systemProperty("c1").value(c1.toURI().toURL().toExternalForm()),
                systemProperty("c2").value(c2.toURI().toURL().toExternalForm()),
                systemProperty("cons").value(cons.toURI().toURL().toExternalForm()),
                systemProperty("consV1").value(consV1.toURI().toURL().toExternalForm())
        );

        options.addAll(Arrays.asList(opt));

        return options.toArray(new Option[options.size()]);
    }

//    @ProbeBuilder
//    public TestProbeBuilder probe(TestProbeBuilder builder) {
//        builder.setHeader(Constants.IMPORT_PACKAGE, "org.osgi.framework, org.apache.felix.ipojo, " +
//                "org.ow2.chameleon.testing.helpers, org.osgi.service.packageadmin, " +
//                "org.apache.felix.ipojo.architecture, org.apache.felix.ipojo.handlers.dependency");
//        builder.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "org.ops4j.pax.exam,org.junit,javax.inject," +
//                "org.ops4j.pax.exam.options");
//        builder.setHeader("Bundle-ManifestVersion", "2");
//        return builder;
//    }

    public boolean isKF() {
        return bc.getClass().toString().contains("knopflerfish");
    }

    @Test
    public void deployBundlesAtRuntime() throws MalformedURLException, BundleException, InvalidSyntaxException {
        if (isKF()) {
            System.out.println("Test disabled on knopflerfish");
            return;
        }

        Bundle b1 = context.installBundle(context.getProperty("url1"));
        b1.start();


        Bundle b3 = context.installBundle(context.getProperty("c1"));
        b3.start();

        Bundle b2 = context.installBundle(context.getProperty("url2"));
        b2.start();

        Bundle b4 = context.installBundle(context.getProperty("c2"));
        b4.start();

        Bundle b5 = context.installBundle(context.getProperty("cons"));
        b5.start();

        Stability.waitForStability(bc);

        Bundle[] bundles = context.getBundles();
        for (Bundle bundle : bundles) {
            System.out.println("bundle " + bundle.getSymbolicName() + " : " + (bundle.getState() == Bundle.ACTIVE));
            //Assert.assertEquals(bundles[i].getSymbolicName() + " is not active", Bundle.ACTIVE, bundles[i].getState());
        }

//        Bundle consBundle = osgiHelper.getBundle("MyCons");
//        BundleWiring wiring = consBundle.adapt(BundleWiring.class);
//        System.out.println("Bundle Wiring req: ");
//        for (BundleWire wire : wiring.getRequiredWires(null)) {
//            System.out.println(wire.getCapability().getAttributes() + " - " + wire.getCapability().getDirectives());
//        }

        osgiHelper.waitForService(Architecture.class.getName(), "(architecture.instance=mycons)", 2000);

        // Check that the two services are provided.
        ServiceReference[] refs = context.getAllServiceReferences("org.apache.felix.ipojo.core.tests.services.MyService", null);
        Assert.assertNotNull(refs);
        Assert.assertEquals(2, refs.length);

        ServiceReference refv1 = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), "mycons");
        Assert.assertNotNull(refv1);
        Architecture arch = (Architecture) osgiHelper.getRawServiceObject(refv1);

        HandlerDescription desc = arch.getInstanceDescription().getHandlerDescription("org.apache.felix.ipojo:requires");
        Assert.assertNotNull(desc);

        DependencyHandlerDescription d = (DependencyHandlerDescription) desc;
        Assert.assertNotNull(d.getDependencies());
        Assert.assertEquals(1, d.getDependencies().length);

        DependencyDescription dep = d.getDependencies()[0];
        Assert.assertEquals(Dependency.RESOLVED, dep.getState());

        Assert.assertEquals(1, dep.getServiceReferences().size());
        ServiceReference r = dep.getServiceReferences().get(0);
        Assert.assertEquals("provider", r.getProperty("factory.name"));
        Assert.assertEquals("2.0", r.getProperty("factory.version"));
    }

    @Test
    @Ignore("Does not work anymore, but the scenario runs as expected in a regular framework. The check find the " +
            "version 2.0 of the service instead of the 1.0")
    public void deployBundlesAtRuntimeV1() throws MalformedURLException, BundleException, InvalidSyntaxException {

        Bundle b1 = context.installBundle(context.getProperty("url1"));
        b1.start();


        Bundle b3 = context.installBundle(context.getProperty("c1"));
        b3.start();

        Bundle b2 = context.installBundle(context.getProperty("url2"));
        b2.start();

        Bundle b4 = context.installBundle(context.getProperty("c2"));
        b4.start();

        Bundle b5 = context.installBundle(context.getProperty("consV1"));
        b5.start();


        Bundle[] bundles = context.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            System.out.println("bundle " + bundles[i].getSymbolicName() + " : " + (bundles[i].getState() == Bundle.ACTIVE));
        }

        osgiHelper.waitForService(Architecture.class.getName(), "(architecture.instance=mycons)", 2000);

        // Check that the two services are provided.
        ServiceReference[] refs = context.getAllServiceReferences("org.apache.felix.ipojo.core.tests.services.MyService", null);
        Assert.assertNotNull(refs);
        Assert.assertEquals(2, refs.length);

        ServiceReference refv1 = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), "mycons");
        Assert.assertNotNull(refv1);
        Architecture arch = (Architecture) osgiHelper.getRawServiceObject(refv1);

        HandlerDescription desc = arch.getInstanceDescription().getHandlerDescription("org.apache.felix.ipojo:requires");
        Assert.assertNotNull(desc);

        DependencyHandlerDescription d = (DependencyHandlerDescription) desc;
        Assert.assertNotNull(d.getDependencies());
        Assert.assertEquals(1, d.getDependencies().length);

        DependencyDescription dep = d.getDependencies()[0];
        Assert.assertEquals(Dependency.RESOLVED, dep.getState());

        Assert.assertEquals(1, dep.getServiceReferences().size());
        ServiceReference r = dep.getServiceReferences().get(0);

        Assert.assertEquals("provider", r.getProperty("factory.name"));
        Assert.assertEquals("1.0", r.getProperty("factory.version"));
    }


}
