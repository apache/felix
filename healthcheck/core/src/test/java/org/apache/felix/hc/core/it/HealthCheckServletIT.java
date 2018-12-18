/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.felix.hc.core.it;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.HttpService;

/** Verify that the HealthCheckExecutorServlet becomes available after creating the corresponding config */
@RunWith(PaxExam.class)
public class HealthCheckServletIT {

    @Inject
    private ConfigurationAdmin configAdmin;

    @Inject
    private BundleContext bundleContext;

    private MockHttpService httpService;
    private ServiceRegistration reg;

    @Configuration
    public Option[] config() {
        return U.config();
    }

    private int countServletServices(String packageNamePrefix) throws InvalidSyntaxException {
        final List<String> classNames = httpService.getServletClassNames();
        int count = 0;
        for (final String className : classNames) {
            if (className.startsWith(packageNamePrefix)) {
                count++;
            }
        }
        return count;
    }

    @Before
    public void setup() {
        httpService = new MockHttpService();
        reg = bundleContext.registerService(HttpService.class.getName(), httpService, null);
    }

    @After
    public void cleanup() {
        reg.unregister();
        reg = null;
        httpService = null;
    }

    @Test
    public void testServletBecomesActive() throws InvalidSyntaxException, IOException, InterruptedException {
        final String property = "servletPath";
        final String path = "/test/" + UUID.randomUUID();
        final String packagePrefix = "org.apache.felix.hc";
        assertEquals("Initially expecting no servlet from " + packagePrefix, 0, countServletServices(packagePrefix));
        final int pathsBefore = httpService.getPaths().size();

        // Activate servlet and wait for it to show up
        final String pid = "org.apache.felix.hc.core.impl.servlet.HealthCheckExecutorServlet";
        final org.osgi.service.cm.Configuration cfg = configAdmin.getConfiguration(pid, null);
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(property, path);
        cfg.update(props);

        final long timeoutMsec = 5000L;
        final long endTime = System.currentTimeMillis() + timeoutMsec;
        while (System.currentTimeMillis() < endTime) {
            if (countServletServices(packagePrefix) > 0) {
                break;
            }
            Thread.sleep(50L);
        }

        int expectedServletCount = 6;
        assertEquals("After adding configuration, expecting six servlets from " + packagePrefix, expectedServletCount,
                countServletServices(packagePrefix));
        final List<String> paths = httpService.getPaths();
        assertEquals("Expecting six new servlet registration", pathsBefore + expectedServletCount, paths.size());
        assertEquals("Expecting the HC servlet to be registered at " + path, path, paths.get(paths.size() - 6)); // paths list is longer,
                                                                                                                 // use last entries in list
        assertEquals("Expecting the HTML HC servlet to be registered at " + path + ".html", path + ".html", paths.get(paths.size() - 5));
        assertEquals("Expecting the JSON HC servlet to be registered at " + path + ".json", path + ".json", paths.get(paths.size() - 4));
        assertEquals("Expecting the JSONP HC servlet to be registered at " + path + ".jsonp", path + ".jsonp", paths.get(paths.size() - 3));
        assertEquals("Expecting the TXT HC servlet to be registered at " + path + ".txt", path + ".txt", paths.get(paths.size() - 2));
        assertEquals("Expecting the verbose TXT HC servlet to be registered at " + path + ".verbose.txt", path + ".verbose.txt",
                paths.get(paths.size() - 1));
    }
}
