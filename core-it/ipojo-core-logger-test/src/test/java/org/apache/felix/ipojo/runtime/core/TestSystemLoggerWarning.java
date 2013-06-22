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

import org.apache.felix.ipojo.runtime.core.components.MyComponent;
import org.apache.felix.ipojo.runtime.core.services.MyService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

public class TestSystemLoggerWarning extends Common {

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
                                .build(TinyBundles.withBnd())
                ),
                streamBundle(
                        TinyBundles.bundle()
                                .add(MyComponent.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "MyComponent")
                                .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/component.xml")))
                ),
                systemProperty("ipojo.log.level").value("warning")
        );
    }

    @Test
    public void testMessages() throws InterruptedException {
        List<String> messages = getMessages(log.getLog());
        Assert.assertTrue(messages.contains("Ready"));
        // Not logged.
        Assert.assertFalse(messages.contains("[INFO] org.apache.felix.ipojo.runtime.core.components.MyComponent : Instance org.apache.felix.ipojo.runtime.core.components.MyComponent-0 from factory org.apache.felix.ipojo.runtime.core.components.MyComponent created"));
        Assert.assertFalse(messages.contains("[INFO] org.apache.felix.ipojo.runtime.core.components.MyComponent : New factory created : org.apache.felix.ipojo.runtime.core.components.MyComponent"));
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
