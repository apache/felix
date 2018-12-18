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

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;

/** Test utilities */
public class U {

    // the name of the system property providing the bundle file to be installed and tested
    private static final String BUNDLE_JAR_SYS_PROP = "project.bundle.file";

    /** Wait until the specified number of health checks are seen by supplied executor */
    static void expectHealthChecks(int howMany, HealthCheckExecutor executor, String... tags) {
        expectHealthChecks(howMany, executor, new HealthCheckExecutionOptions(), tags);
    }

    /** Wait until the specified number of health checks are seen by supplied executor */
    static void expectHealthChecks(int howMany, HealthCheckExecutor executor, HealthCheckExecutionOptions options, String... tags) {
        final long timeout = System.currentTimeMillis() + 10000L;
        int count = 0;
        while (System.currentTimeMillis() < timeout) {
            final List<HealthCheckExecutionResult> results = executor.execute(HealthCheckSelector.tags(tags), options);
            count = results.size();
            if (count == howMany) {
                return;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException iex) {
                throw new RuntimeException("Unexpected InterruptedException");
            }
        }
        fail("Did not get " + howMany + " health checks with tags " + Arrays.asList(tags) + " after " + timeout + " msec (last count="
                + count + ")");
    }

    static Option[] config() {
        final String localRepo = System.getProperty("maven.repo.local", "");
        final boolean felixShell = "true".equals(System.getProperty("felix.shell", "false"));

        final String bundleFileName = System.getProperty(BUNDLE_JAR_SYS_PROP);
        final File bundleFile = new File(bundleFileName);
        if (!bundleFile.canRead()) {
            throw new IllegalArgumentException("Cannot read from bundle file " + bundleFileName + " specified in the "
                    + BUNDLE_JAR_SYS_PROP + " system property");
        }

        // As we're using the forked pax exam container, we need to add a VM
        // option to activate the jacoco test coverage agent.
        final String coverageCommand = System.getProperty("coverage.command");

        return options(
                when(localRepo.length() > 0).useOptions(
                        systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepo)),
                junitBundles(),
                when(coverageCommand != null && coverageCommand.trim().length() > 0).useOptions(
                        CoreOptions.vmOption(coverageCommand)),
                when(felixShell).useOptions(
                        provision(
                                mavenBundle("org.apache.felix", "org.apache.felix.gogo.shell", "0.10.0"),
                                mavenBundle("org.apache.felix", "org.apache.felix.gogo.runtime", "0.10.0"),
                                mavenBundle("org.apache.felix", "org.apache.felix.gogo.command", "0.12.0"),
                                mavenBundle("org.apache.felix", "org.apache.felix.shell.remote", "1.1.2"))),
                provision(
                        bundle(bundleFile.toURI().toString()),
                        mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.0.14"),
                        mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.8.16"),
                        mavenBundle("org.apache.felix", "org.apache.felix.healthcheck.api").versionAsInProject(),
                        mavenBundle().groupId("org.apache.commons").artifactId("commons-lang3").versionAsInProject(),
                        mavenBundle().groupId("javax.servlet").artifactId("javax.servlet-api").versionAsInProject(),
                        mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.quartz")
                                .versionAsInProject()));
    }
}
