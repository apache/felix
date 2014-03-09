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
import org.apache.felix.ipojo.core.tests.components.MyComponent;
import org.apache.felix.ipojo.core.tests.services.MyService;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.ops4j.pax.exam.CoreOptions.bundle;

public class FactoryVersionTest extends Common {


    @org.ops4j.pax.exam.Configuration
    public Option[] config() throws IOException {

        Option[] options = super.config();

        File provider1 = new File("target/tested/provider-v1.jar");
        FileUtils.copyInputStreamToFile(
                TinyBundles.bundle()
                        .add(MyComponent.class)
                        .set(Constants.BUNDLE_SYMBOLICNAME, "ProviderV1")
                        .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.core.tests.services")
                        .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/provider-v1.xml"))),
                provider1);

        File provider2 = new File("target/tested/provider-v1.1.jar");
        FileUtils.copyInputStreamToFile(
                TinyBundles.bundle()
                        .add(MyComponent.class)
                        .set(Constants.BUNDLE_SYMBOLICNAME, "ProviderV1.1")
                        .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.core.tests.services")
                        .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/provider-v1.1.xml"))),
                provider2);

        File instance = new File("target/tested/instance.jar");
        FileUtils.copyInputStreamToFile(
                TinyBundles.bundle()
                        .set(Constants.BUNDLE_SYMBOLICNAME, "Instances")
                        .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/instances.xml"))),
                instance);

        List<Option> optionList = new ArrayList<Option>();
        optionList.addAll(Arrays.asList(options));

        optionList.add(createServiceBundleV1());
        optionList.add(bundle(provider1.toURI().toURL().toExternalForm()));
        optionList.add(bundle(provider2.toURI().toURL().toExternalForm()));
        optionList.add(bundle(instance.toURI().toURL().toExternalForm()));

        return optionList.toArray(new Option[optionList.size()]);
    }

    @Test
    public void testDeploy() {
        Bundle[] bundles = bc.getBundles();
        for (Bundle bundle : bundles) {
            System.out.println("Bundle deployed : " + bundle.getSymbolicName() + " / " + bundle.getState());
            Assert.assertEquals(bundle.getSymbolicName() + " is not active", Bundle.ACTIVE, bundle.getState());
        }
    }

    @Test
    public void testInstanceArchitecture() {
        // Version 1.0
        ServiceReference refv1 = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), "instance-v1");
        Assert.assertNotNull(refv1);
        Architecture archv1 = (Architecture) osgiHelper.getRawServiceObject(refv1);

        String version = archv1.getInstanceDescription().getComponentDescription().getVersion();
        Assert.assertEquals("1.0", version);

        // Version 1.1
        ServiceReference refv11 = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), "instance-v1.1");
        Assert.assertNotNull(refv11);
        Architecture archv11 = (Architecture) osgiHelper.getRawServiceObject(refv11);

        String version11 = archv11.getInstanceDescription().getComponentDescription().getVersion();
        Assert.assertEquals("1.1", version11);

        // No Version
        ServiceReference refany = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), "instance-any");
        Assert.assertNotNull(refany);
        Architecture archany = (Architecture) osgiHelper.getRawServiceObject(refany);

        String any = archany.getInstanceDescription().getComponentDescription().getVersion();
        Assert.assertNotNull(any);

        // No version set in the factory, so no version.
        ServiceReference refmci = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), "MyComponentInstance");
        Assert.assertNotNull(refmci);
        Architecture archmcy = (Architecture) osgiHelper.getRawServiceObject(refmci);

        String mci = archmcy.getInstanceDescription().getComponentDescription().getVersion();
        Assert.assertNull(mci);

    }

    @Test
    public void testServiceProperty() throws InvalidSyntaxException {

        // Version 1.0
        //ServiceReference refv1 = ipojoHelper.getServiceReferenceByName(MyService.class.getName(), "instance-v1");
        ServiceReference[] refv1 = bc.getAllServiceReferences(MyService.class.getName(),
                "(instance.name=instance-v1)");
        Assert.assertNotNull(refv1);
        String version = (String) refv1[0].getProperty("factory.version");
        Assert.assertEquals("1.0", version);

        // Version 1.1
        for(ServiceReference ref : bc.getAllServiceReferences(null, null)) {
            System.out.println("Service : " + ref.getProperty("instance.name"));
        }

        ServiceReference[] refv11 = bc.getAllServiceReferences(MyService.class.getName(),
                "(instance.name=instance-v1.1)");
        //ServiceReference refv11 = ipojoHelper.getServiceReferenceByName(MyService.class.getName(), "instance-v1.1");
        Assert.assertNotNull(refv11);
        String version11 = (String) refv11[0].getProperty("factory.version");

        Assert.assertEquals("1.1", version11);

        // No Version
        ServiceReference[] refany = bc.getAllServiceReferences(MyService.class.getName(),
                "(instance.name=instance-any)");

        // ServiceReference refany = ipojoHelper.getServiceReferenceByName(MyService.class.getName(), "instance-any");
        Assert.assertNotNull(refany);
        String any = (String) refany[0].getProperty("factory.version");
        Assert.assertNotNull(any);

        // No version set in the factory, so no version.
        ServiceReference[] refmci = bc.getAllServiceReferences(MyService.class.getName(),
                "(instance.name=MyComponentInstance)");
        //ServiceReference refmci = ipojoHelper.getServiceReferenceByName(MyService.class.getName(), "MyComponentInstance");
        Assert.assertNotNull(refmci);
        String mci = (String) refmci[0].getProperty("factory.version");
        Assert.assertNull(mci);
    }


}
