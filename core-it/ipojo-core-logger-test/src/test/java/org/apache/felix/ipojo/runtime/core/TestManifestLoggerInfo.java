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

import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.runtime.core.components.MyComponent;
import org.apache.felix.ipojo.runtime.core.services.MyService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

public class TestManifestLoggerInfo extends Common {

    private LogReaderService log;

    @Before
    public void init() {
        log = (LogReaderService) osgiHelper.getServiceObject(LogReaderService.class.getName(), null);
        if (log == null) {
            throw new RuntimeException("No Log Service !");
        }

        LogService logs = (LogService) osgiHelper.getServiceObject(LogService.class.getName(), null);
        logs.log(LogService.LOG_WARNING, "Ready");
    }

    @Configuration
    public Option[] config() throws IOException {
        Option[] options = super.config();

        return OptionUtils.combine(options,
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.log").version(asInProject()),
                streamBundle(
                        TinyBundles.bundle()
                                .add(MyService.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "ServiceInterface")
                                .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.runtime.core.services")
                                .build(withBnd())
                ),
                streamBundle(
                        TinyBundles.bundle()
                                .add(MyComponent.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "MyComponent")
                                .set("Ipojo-log-level", "info")
                                .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/component.xml")))
                )
        );
    }

    @Test
    public void testMessages() throws InterruptedException, InvalidSyntaxException {
        Bundle bundle = osgiHelper.getBundle("MyComponent");
        Assert.assertNotNull(bundle);
        Assert.assertEquals(Bundle.ACTIVE, bundle.getState());

        ServiceReference r = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(),
                "org.apache.felix.ipojo.runtime.core.components.MyComponent-0");
        Assert.assertNotNull(r);

        ServiceReference[] refs = bc.getAllServiceReferences(null, null);
        for (ServiceReference ref : refs) {
            System.out.println(ref.getBundle().getBundleId() + " -> " + Arrays.asList((String[]) ref.getProperty(Constants.OBJECTCLASS)));
        }


        Assert.assertNotNull(osgiHelper.getServiceObject(MyService.class.getName(), null));

        osgiHelper.waitForService(MyService.class.getName(), null, 5000);
        List<String> messages = getMessages(log.getLog());
        System.out.println(messages);
        Assert.assertTrue(messages.contains("Ready"));
//        Assert.assertTrue(messages.contains("[INFO] org.apache.felix.ipojo.runtime.core.components.MyComponent : Instance org.apache.felix.ipojo.runtime.core.components.MyComponent-0 from factory org.apache.felix.ipojo.runtime.core.components.MyComponent created"));
//        Assert.assertTrue(messages.contains("[INFO] org.apache.felix.ipojo.runtime.core.components.MyComponent : New factory created : org.apache.felix.ipojo.runtime.core.components.MyComponent"));
    }

    private List<String> getMessages(Enumeration<LogEntry> log2) {
        List<String> list = new ArrayList<String>();
        while (log2.hasMoreElements()) {
            LogEntry entry = (LogEntry) log2.nextElement();
            list.add(entry.getMessage());
        }
        return list;
    }


}
