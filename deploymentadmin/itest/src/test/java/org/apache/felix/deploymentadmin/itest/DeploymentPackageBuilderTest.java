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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import junit.framework.TestCase;

import org.apache.felix.deploymentadmin.itest.util.DeploymentPackageBuilder;
import org.apache.felix.deploymentadmin.itest.util.DeploymentPackageBuilder.JarManifestManipulatingFilter;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for {@link DeploymentPackageBuilder}.
 */
public class DeploymentPackageBuilderTest extends TestCase {

    private String m_testBundleBasePath;

    @Before
    public void setUp() throws Exception {
        File f = new File("../testbundles").getAbsoluteFile();
        assertTrue("Failed to find test bundles directory?!", f.exists() && f.isDirectory());

        m_testBundleBasePath = f.getAbsolutePath();
    }

    /**
     * Tests that we can build a deployment package with a bundle resource.
     */
    @Test
    public void testCreateMissingBundleResourceOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = DeploymentPackageBuilder.create("dp-test", "1.0.0");
        dpBuilder
            .setFixPackage()
            .add(dpBuilder.createBundleResource()
                .setUrl(getTestBundle("bundle1")).setMissing()
            );

        JarInputStream jis = new JarInputStream(dpBuilder.generate());
        assertNotNull(jis);

        Manifest manifest = jis.getManifest();
        assertManifestHeader(manifest, "DeploymentPackage-SymbolicName", "dp-test");
        assertManifestHeader(manifest, "DeploymentPackage-Version", "1.0.0");

        String filename = getBundleName("bundle1");

        assertManifestEntry(manifest, filename, "Name", filename);
        assertManifestEntry(manifest, filename, "Bundle-SymbolicName", "testbundles.bundle1");
        assertManifestEntry(manifest, filename, "Bundle-Version", "1.0.0");
        assertManifestEntry(manifest, filename, "DeploymentPackage-Missing", "true");

        int count = countJarEntries(jis);

        assertEquals("Expected two entries in the JAR!", 0, count);
    }

    /**
     * Tests that we can build a deployment package with a bundle resource.
     */
    @Test
    public void testCreateMinimalSingleBundleResourceOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = DeploymentPackageBuilder.create("dp-test", "1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource()
                .setUrl(getTestBundle("bundle1"))
            );

        JarInputStream jis = new JarInputStream(dpBuilder.generate());
        assertNotNull(jis);

        Manifest manifest = jis.getManifest();
        assertManifestHeader(manifest, "DeploymentPackage-SymbolicName", "dp-test");
        assertManifestHeader(manifest, "DeploymentPackage-Version", "1.0.0");

        String filename = getBundleName("bundle1");

        assertManifestEntry(manifest, filename, "Name", filename);
        assertManifestEntry(manifest, filename, "Bundle-SymbolicName", "testbundles.bundle1");
        assertManifestEntry(manifest, filename, "Bundle-Version", "1.0.0");

        int count = countJarEntries(jis);

        assertEquals("Expected two entries in the JAR!", 1, count);
    }

    /**
     * Tests that we can filter a resource.
     */
    @Test
    public void testResourceFilterOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = DeploymentPackageBuilder.create("dp-test", "1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource()
                .setUrl(getTestBundle("bundle2")))
            .add(dpBuilder.createBundleResource()
                .setVersion("1.1.0")
                .setFilter(new JarManifestManipulatingFilter("Bundle-Version", "1.1.0", "Foo", "bar"))
                .setUrl(getTestBundle("bundle1")));

        JarInputStream jis = new JarInputStream(dpBuilder.generate());
        assertNotNull(jis);

        Manifest manifest = jis.getManifest();
        assertManifestHeader(manifest, "DeploymentPackage-SymbolicName", "dp-test");
        assertManifestHeader(manifest, "DeploymentPackage-Version", "1.0.0");

        String filename = getBundleName("bundle1");

        assertManifestEntry(manifest, filename, "Name", filename);
        assertManifestEntry(manifest, filename, "Bundle-SymbolicName", "testbundles.bundle1");
        assertManifestEntry(manifest, filename, "Bundle-Version", "1.1.0");

        filename = getBundleName("bundle2");

        assertManifestEntry(manifest, filename, "Name", filename);
        assertManifestEntry(manifest, filename, "Bundle-SymbolicName", "testbundles.bundle2");
        assertManifestEntry(manifest, filename, "Bundle-Version", "1.0.0");

        try {
            byte[] buf = new byte[32 * 1024];

            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().endsWith("valid-bundle1.jar")) {
                    int read = jis.read(buf);

                    JarInputStream jis2 = new JarInputStream(new ByteArrayInputStream(Arrays.copyOf(buf, read)));
                    Manifest manifest2 = jis2.getManifest();

                    Attributes mainAttributes = manifest2.getMainAttributes();
                    assertEquals("1.1.0", mainAttributes.getValue("Bundle-Version"));
                    assertEquals("bar", mainAttributes.getValue("Foo"));
                    
                    jis2.close();
                }
                jis.closeEntry();
            }
        }
        finally {
            jis.close();
        }
    }

    /**
     * Tests that we can build a deployment package with a "plain" resource and resource processor.
     */
    @Test
    public void testCreateMinimalSingleResourceAndProcessorOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = DeploymentPackageBuilder.create("dp-test", "1.0.0");
        dpBuilder
            .add(dpBuilder.createResourceProcessorResource()
                .setUrl(getTestBundle("rp1")))
            .add(dpBuilder.createResource()
                .setResourceProcessorPID("org.apache.felix.deploymentadmin.test.rp1")
                .setUrl(getTestResource("test-config1.xml"))
            );

        JarInputStream jis = new JarInputStream(dpBuilder.generate());
        assertNotNull(jis);

        Manifest manifest = jis.getManifest();
        assertManifestHeader(manifest, "DeploymentPackage-SymbolicName", "dp-test");
        assertManifestHeader(manifest, "DeploymentPackage-Version", "1.0.0");

        String filename = getBundleName("rp1");

        assertManifestEntry(manifest, filename, "Name", filename);
        assertManifestEntry(manifest, filename, "Bundle-SymbolicName", "testbundles.rp1");
        assertManifestEntry(manifest, filename, "Bundle-Version", "1.0.0");

        filename = "test-config1.xml";

        assertManifestEntry(manifest, filename, "Name", filename);
        assertManifestEntry(manifest, filename, "Resource-Processor", "org.apache.felix.deploymentadmin.test.rp1");

        int count = countJarEntries(jis);

        assertEquals("Expected two entries in the JAR!", 2, count);
    }

    /**
     * Tests that we can build a deployment package with two bundle resources.
     */
    @Test
    public void testCreateMinimalTwoBundleResourcesOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = DeploymentPackageBuilder.create("dp-test", "1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource()
                .setUrl(getTestBundle("bundle1"))
            )
            .add(dpBuilder.createBundleResource()
                .setUrl(getTestBundle("bundle2"))
            );

        JarInputStream jis = new JarInputStream(dpBuilder.generate());
        assertNotNull(jis);

        Manifest manifest = jis.getManifest();
        assertManifestHeader(manifest, "DeploymentPackage-SymbolicName", "dp-test");
        assertManifestHeader(manifest, "DeploymentPackage-Version", "1.0.0");

        String filename = getBundleName("bundle1");

        assertManifestEntry(manifest, filename, "Name", filename);
        assertManifestEntry(manifest, filename, "Bundle-SymbolicName", "testbundles.bundle1");
        assertManifestEntry(manifest, filename, "Bundle-Version", "1.0.0");

        filename = getBundleName("bundle2");

        assertManifestEntry(manifest, filename, "Name", filename);
        assertManifestEntry(manifest, filename, "Bundle-SymbolicName", "testbundles.bundle2");
        assertManifestEntry(manifest, filename, "Bundle-Version", "1.0.0");

        int count = countJarEntries(jis);

        assertEquals("Expected two entries in the JAR!", 2, count);
    }

    private void assertAttributes(Attributes attributes, String headerName, String expectedValue)
        throws RuntimeException {
        assertNotNull("No attributes given!", attributes);
        assertEquals(headerName, expectedValue, attributes.getValue(headerName));
    }

    private void assertManifestEntry(Manifest manifest, String key, String headerName, String expectedValue)
        throws RuntimeException {
        Attributes attributes = manifest.getEntries().get(key);
        assertNotNull("No attributes found for: " + key, attributes);
        assertAttributes(attributes, headerName, expectedValue);
    }

    private void assertManifestHeader(Manifest manifest, String headerName, String expectedValue)
        throws RuntimeException {
        assertAttributes(manifest.getMainAttributes(), headerName, expectedValue);
    }

    private int countJarEntries(JarInputStream jis) throws IOException {
        int count = 0;
        try {
            while (jis.getNextJarEntry() != null) {
                count++;
                jis.closeEntry();
            }
        }
        finally {
            jis.close();
        }
        return count;
    }

    private String getBundleName(String baseName) {
        return String.format("org.apache.felix.deploymentadmin.test.%1$s-1.0.0.jar", baseName);
    }

    private String getBundleFilename(String baseName) {
        return String.format("%1$s/target/org.apache.felix.deploymentadmin.test.%1$s-1.0.0.jar", baseName);
    }

    private URL getTestBundle(String baseName) throws MalformedURLException {
        File f = new File(m_testBundleBasePath, getBundleFilename(baseName));
        assertTrue("No such bundle: " + f, f.exists() && f.isFile());
        return f.toURI().toURL();
    }

    private URL getTestResource(String resourceName) {
        if (!resourceName.startsWith("/")) {
            resourceName = "/".concat(resourceName);
        }
        URL resource = getClass().getResource(resourceName);
        assertNotNull("No such resource: " + resourceName, resource);
        return resource;
    }
}
