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
package org.apache.felix.deploymentadmin.itest;

import static org.ops4j.pax.exam.Constants.START_LEVEL_SYSTEM_BUNDLES;
import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.url;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import junit.framework.TestCase;

import org.apache.felix.deploymentadmin.itest.util.DeploymentPackageBuilder;
import org.junit.Before;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Provides a common base class for all deployment admin related integration tests.
 */
public abstract class BaseIntegrationTest extends TestCase {

    protected static final int DEFAULT_TIMEOUT = 10000;

    protected static final String TEST_SERVICE_NAME = "org.apache.felix.deploymentadmin.test.bundle1.TestService";
    protected static final String TEST_FAILING_BUNDLE_RP1 = "org.apache.felix.deploymentadmin.test.rp1";

    @Inject
    protected volatile BundleContext m_context;
    @Inject
    protected volatile DeploymentAdmin m_deploymentAdmin;
    @Inject
    protected volatile PackageAdmin m_packageAdmin;

    protected volatile AtomicInteger m_gate = new AtomicInteger(0);
    protected volatile String m_testBundleBasePath;
    protected volatile Map<String, List<Version>> m_initialBundles;
    
    private int cnt = 0;        
    
    @Configuration
    public Option[] config() {
        return options(
            bootDelegationPackage("sun.*"),
            cleanCaches(),
            CoreOptions.systemProperty("logback.configurationFile").value("file:src/test/resources/logback.xml"),
//            CoreOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8787"),

            mavenBundle("org.slf4j", "slf4j-api").version("1.6.5").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("ch.qos.logback", "logback-core").version("1.0.6").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("ch.qos.logback", "logback-classic").version("1.0.6").startLevel(START_LEVEL_SYSTEM_BUNDLES),

            url("link:classpath:META-INF/links/org.ops4j.pax.exam.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            url("link:classpath:META-INF/links/org.ops4j.pax.exam.inject.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            url("link:classpath:META-INF/links/org.ops4j.pax.extender.service.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            url("link:classpath:META-INF/links/org.ops4j.base.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.core.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.extender.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.lifecycle.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.framework.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            url("link:classpath:META-INF/links/org.apache.geronimo.specs.atinject.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),

            mavenBundle("org.osgi", "org.osgi.core").version("4.2.0").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("org.osgi", "org.osgi.compendium").version("4.2.0").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager").version("3.0.0").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("org.apache.felix", "org.apache.felix.deploymentadmin").version("0.9.1-SNAPSHOT").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("org.apache.felix", "org.apache.felix.eventadmin").version("1.2.14").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("org.apache.felix", "org.apache.felix.configadmin").version("1.2.8").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("org.apache.felix", "org.apache.felix.log").version("1.0.1").startLevel(START_LEVEL_SYSTEM_BUNDLES),
//            mavenBundle("org.apache.felix", "org.apache.felix.shell").version("1.4.3").startLevel(START_LEVEL_SYSTEM_BUNDLES),
//            mavenBundle("org.apache.felix", "org.apache.felix.shell.tui").version("1.4.1").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            
            junitBundles(),
            frameworkStartLevel(START_LEVEL_TEST_BUNDLE),
            felix());
    }

    @Before
    public void setUp() throws Exception {
        assertNotNull("No bundle context?!", m_context);

        File f = new File("../testbundles").getAbsoluteFile();
        assertTrue("Failed to find test bundles directory?!", f.exists() && f.isDirectory());

        m_testBundleBasePath = f.getAbsolutePath();

        m_context.addFrameworkListener(new FrameworkListener() {
            public void frameworkEvent(FrameworkEvent event) {
                if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
                    m_gate.getAndIncrement();
                }
            }
        });
        
        m_initialBundles = new HashMap<String, List<Version>>();
        
        for (Bundle bundle : m_context.getBundles()) {
            List<Version> versions = m_initialBundles.get(bundle.getSymbolicName());
            if (versions == null) {
                versions = new ArrayList<Version>();
                m_initialBundles.put(bundle.getSymbolicName(), versions);
            }
            versions.add(bundle.getVersion());
        }
    }

    protected void assertBundleExists(String symbolicName, String version) {
        boolean result = isBundleAdded(symbolicName, version);
        if (!result) {
            fail("Bundle " + symbolicName + ", v" + version + " does not exist?!\nCurrent additional bundles are: " + getCurrentBundles());
        }
    }

    protected void assertBundleNotExists(String symbolicName, String version) {
        boolean result = isBundleAdded(symbolicName, version);
        if (result) {
            fail("Bundle " + symbolicName + ", v" + version + " does (still) exist?!\nCurrent additional bundles are: " + getCurrentBundles());
        }
    }

    protected void assertDeploymentException(int expectedCode, DeploymentException exception) {
        assertEquals("Invalid exception code?!\nException = " + exception, expectedCode, exception.getCode());
    }

    protected void awaitRefreshPackagesEvent() throws Exception {
        long start = System.currentTimeMillis();
        while ((m_gate.get() == 0) && ((System.currentTimeMillis() - start) < DEFAULT_TIMEOUT)) {
            TimeUnit.MILLISECONDS.sleep(100);
        }
        assertTrue("Failed to obtain refresh packages event?! " + m_gate.get(), m_gate.get() > 0);
        m_gate.set(0);
    }

    protected <T> T awaitService(String serviceName) throws Exception {
        ServiceTracker tracker = new ServiceTracker(m_context, serviceName, null);
        tracker.open();
        T result;
        try {
            result = (T) tracker.waitForService(DEFAULT_TIMEOUT);
        }
        finally {
            tracker.close();
        }
        return result;
    }

    protected DeploymentPackageBuilder createNewDeploymentPackageBuilder(String version) {
        return createDeploymentPackageBuilder(String.format("itest%d", ++cnt), version);
    }
    
    protected DeploymentPackageBuilder createDeploymentPackageBuilder(String symName, String version) {
        return DeploymentPackageBuilder.create(symName, version);
    }
    
    protected Map<String, List<Version>> getCurrentBundles() {
        Map<String, List<Version>> bundles = new HashMap<String, List<Version>>();
        for (Bundle bundle : m_context.getBundles()) {
            String symbolicName = bundle.getSymbolicName();
            Version version = bundle.getVersion();
            
            // Is is not part of any of the initially provisioned bundles?
            List<Version> versions = m_initialBundles.get(symbolicName);
            if ((versions == null) || !versions.contains(version)) {
                List<Version> versions2 = bundles.get(symbolicName);
                if (versions2 == null) {
                    versions2 = new ArrayList<Version>();
                    bundles.put(symbolicName, versions2);
                }
                versions2.add(version);
            }
        }
        return bundles;
    }
    
    protected String getSymbolicName(String baseName) {
        return "testbundles.".concat(baseName);
    }

    protected URL getTestResource(String resourceName) {
        if (!resourceName.startsWith("/")) {
            resourceName = "/".concat(resourceName);
        }
        URL resource = getClass().getResource(resourceName);
        assertNotNull("No such resource: " + resourceName, resource);
        return resource;
    }

    /**
     * @param baseName
     * @return
     * @throws MalformedURLException
     */
    protected URL getTestBundle(String baseName) throws MalformedURLException {
        File f = new File(m_testBundleBasePath, String.format("%1$s/target/org.apache.felix.deploymentadmin.test.%1$s-1.0.0.jar", baseName));
        assertTrue("No such bundle: " + f, f.exists() && f.isFile());
        return f.toURI().toURL();
    }
    
    protected boolean isBundleActive(Bundle bundle) {
        return isBundleInState(bundle, Bundle.ACTIVE);
    }

    protected boolean isBundleAdded(String symbolicName, String version) {
        return isBundleAdded(symbolicName, new Version(version));
    }

    protected boolean isBundleAdded(String symbolicName, Version version) {
        Map<String, List<Version>> bundles = getCurrentBundles();

        List<Version> availableVersions = bundles.get(symbolicName);
        return (availableVersions != null) && availableVersions.contains(version);
    }

    protected boolean isBundleInstalled(Bundle bundle) {
        return isBundleInState(bundle, Bundle.INSTALLED);
    }

    protected boolean isBundleInState(Bundle bundle, int state) {
        return ((bundle.getState() & state) != 0);
    }

    protected boolean isBundleRemoved(String symbolicName, String version) {
        return isBundleRemoved(symbolicName, new Version(version));
    }
    
    protected boolean isBundleRemoved(String symbolicName, Version version) {
        Map<String, List<Version>> bundles = getCurrentBundles();

        List<Version> availableVersions = bundles.get(symbolicName);
        return (availableVersions == null) || !availableVersions.contains(version);
    }

    protected boolean isBundleResolved(Bundle bundle) {
        return isBundleInState(bundle, Bundle.RESOLVED);
    }
}
