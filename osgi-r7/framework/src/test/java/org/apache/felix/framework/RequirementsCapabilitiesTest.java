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
package org.apache.felix.framework;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

public class RequirementsCapabilitiesTest extends TestCase
{
    private File tempDir;
    private Framework felix;
    private File cacheDir;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        tempDir = File.createTempFile("felix-temp", ".dir");
        assertTrue("precondition", tempDir.delete());
        assertTrue("precondition", tempDir.mkdirs());

        cacheDir = new File(tempDir, "felix-cache");
        assertTrue("precondition", cacheDir.mkdir());

        String cache = cacheDir.getPath();

        Map<String,String> params = new HashMap<String, String>();
        params.put("felix.cache.profiledir", cache);
        params.put("felix.cache.dir", cache);
        params.put(Constants.FRAMEWORK_STORAGE, cache);

        felix = new Felix(params);
        felix.init();
        felix.start();
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();

        felix.stop(); // Note that this method is async
        felix = null;

        deleteDir(tempDir);
        tempDir = null;
        cacheDir = null;
    }

    public void testIdentityCapabilityBundleFragment() throws Exception
    {
        String bmf = "Bundle-SymbolicName: cap.bundle\n"
                + "Bundle-Version: 1.2.3.Blah\n"
                + "Bundle-ManifestVersion: 2\n"
                + "Import-Package: org.osgi.framework\n";
        File bundleFile = createBundle(bmf);

        String fmf = "Bundle-SymbolicName: cap.frag\n"
                + "Bundle-Version: 1.0.0\n"
                + "Fragment-Host: cap.bundle\n"
                + "Bundle-ManifestVersion: 2\n"
                + "Export-Package: org.foo.bar;version=\"2.0.0\"\n"
                + "Import-Package: org.osgi.util.tracker\n";
        File fragFile = createBundle(fmf);

        Bundle b = felix.getBundleContext().installBundle(bundleFile.toURI().toASCIIString());
        Bundle f = felix.getBundleContext().installBundle(fragFile.toURI().toASCIIString());

        // Check the bundle capabilities.
        // First check the capabilities on the Bundle Revision, which is available on installed bundles
        BundleRevision bbr = b.adapt(BundleRevision.class);
        List<Capability> bwbCaps = bbr.getCapabilities("osgi.wiring.bundle");
        assertEquals(1, bwbCaps.size());

        Map<String, Object> expectedBWBAttrs = new HashMap<String, Object>();
        expectedBWBAttrs.put("osgi.wiring.bundle", "cap.bundle");
        expectedBWBAttrs.put("bundle-version", Version.parseVersion("1.2.3.Blah"));
        Capability expectedBWBCap = new TestCapability("osgi.wiring.bundle",
                expectedBWBAttrs, Collections.<String, String>emptyMap());
        assertCapsEquals(expectedBWBCap, bwbCaps.get(0));

        List<Capability> bwhCaps = bbr.getCapabilities("osgi.wiring.host");
        assertEquals(1, bwhCaps.size());

        Map<String, Object> expectedBWHAttrs = new HashMap<String, Object>();
        expectedBWHAttrs.put("osgi.wiring.host", "cap.bundle");
        expectedBWHAttrs.put("bundle-version", Version.parseVersion("1.2.3.Blah"));
        Capability expectedBWHCap = new TestCapability("osgi.wiring.host",
                expectedBWHAttrs, Collections.<String, String>emptyMap());
        assertCapsEquals(expectedBWHCap, bwhCaps.get(0));

        List<Capability> bwiCaps = bbr.getCapabilities("osgi.identity");
        assertEquals(1, bwiCaps.size());

        Map<String, Object> expectedBWIAttrs = new HashMap<String, Object>();
        expectedBWIAttrs.put("osgi.identity", "cap.bundle");
        expectedBWIAttrs.put("type", "osgi.bundle");
        expectedBWIAttrs.put("version", Version.parseVersion("1.2.3.Blah"));
        Capability expectedBWICap = new TestCapability("osgi.identity",
                expectedBWIAttrs, Collections.<String, String>emptyMap());
        assertCapsEquals(expectedBWICap, bwiCaps.get(0));

        assertEquals("The Bundle should not directly expose osgi.wiring.package",
                0, bbr.getCapabilities("osgi.wiring.package").size());

        // Check the fragment's capabilities.
        // First check the capabilities on the Bundle Revision, which is available on installed fragments
        BundleRevision fbr = f.adapt(BundleRevision.class);
        List<Capability> fwpCaps = fbr.getCapabilities("osgi.wiring.package");
        assertEquals(1, fwpCaps.size());

        Map<String, Object> expectedFWAttrs = new HashMap<String, Object>();
        expectedFWAttrs.put("osgi.wiring.package", "org.foo.bar");
        expectedFWAttrs.put("version", Version.parseVersion("2"));
        expectedFWAttrs.put("bundle-symbolic-name", "cap.frag");
        expectedFWAttrs.put("bundle-version", Version.parseVersion("1.0.0"));
        Capability expectedFWCap = new TestCapability("osgi.wiring.package",
                expectedFWAttrs, Collections.<String, String>emptyMap());
        assertCapsEquals(expectedFWCap, fwpCaps.get(0));

        List<Capability> fiCaps = fbr.getCapabilities("osgi.identity");
        assertEquals(1, fiCaps.size());
        Map<String, Object> expectedFIAttrs = new HashMap<String, Object>();
        expectedFIAttrs.put("osgi.identity", "cap.frag");
        expectedFIAttrs.put("type", "osgi.fragment");
        expectedFIAttrs.put("version", Version.parseVersion("1.0.0"));
        Capability expectedFICap = new TestCapability("osgi.identity",
                expectedFIAttrs, Collections.<String, String>emptyMap());
        assertCapsEquals(expectedFICap, fiCaps.get(0));

        // Start the bundle. This will make the BundleWiring available on both the bundle and the fragment
        b.start();

        // Check the Bundle Wiring on the fragment. It should only contain the osgi.identity capability
        // All the other capabilities should have migrated to the bundle's BundleWiring.
        BundleWiring fbw = f.adapt(BundleWiring.class);
        List<BundleCapability> fbwCaps = fbw.getCapabilities(null);
        assertEquals("Fragment should only have 1 capability: it's osgi.identity", 1, fbwCaps.size());
        assertCapsEquals(expectedFICap, fbwCaps.get(0));

        // Check the Bundle Wiring on the bundle. It should contain all the capabilities originally on the
        // bundle and also contain the osgi.wiring.package capability from the fragment.
        BundleWiring bbw = b.adapt(BundleWiring.class);
        List<BundleCapability> bwbCaps2 = bbw.getCapabilities("osgi.wiring.bundle");
        assertEquals(1, bwbCaps2.size());
        assertCapsEquals(expectedBWBCap, bwbCaps2.get(0));
        List<BundleCapability> bwhCaps2 = bbw.getCapabilities("osgi.wiring.host");
        assertEquals(1, bwhCaps2.size());
        assertCapsEquals(expectedBWHCap, bwhCaps2.get(0));
        List<BundleCapability> bwiCaps2 = bbw.getCapabilities("osgi.identity");
        assertEquals(1, bwiCaps2.size());
        assertCapsEquals(expectedBWICap, bwiCaps2.get(0));
        List<BundleCapability> bwpCaps2 = bbw.getCapabilities("osgi.wiring.package");
        assertEquals("Bundle should have inherited the osgi.wiring.package capability from the fragment",
                1, bwpCaps2.size());
        assertCapsEquals(expectedFWCap, bwpCaps2.get(0));
    }

    public void testIdentityCapabilityFrameworkExtension() throws Exception
    {
        String femf = "Bundle-SymbolicName: fram.ext\n"
                + "Bundle-Version: 1.2.3.test\n"
                + "Fragment-Host: system.bundle; extension:=framework\n"
                + "Bundle-ManifestVersion: 2\n"
                + "Export-Package: org.foo.bar;version=\"2.0.0\"\n";
        File feFile = createBundle(femf);

        Bundle fe = felix.getBundleContext().installBundle(feFile.toURI().toASCIIString());

        BundleRevision fbr = fe.adapt(BundleRevision.class);

        List<Capability> feCaps = fbr.getCapabilities("osgi.identity");
        assertEquals(1, feCaps.size());
        Map<String, Object> expectedFEAttrs = new HashMap<String, Object>();
        expectedFEAttrs.put("osgi.identity", "fram.ext");
        expectedFEAttrs.put("type", "osgi.fragment");
        expectedFEAttrs.put("version", Version.parseVersion("1.2.3.test"));
        Capability expectedFICap = new TestCapability("osgi.identity",
                expectedFEAttrs, Collections.<String, String>emptyMap());
        assertCapsEquals(expectedFICap, feCaps.get(0));
    }

    private File createBundle(String manifest) throws IOException
    {
        File f = File.createTempFile("felix-bundle", ".jar", tempDir);

        Manifest mf = new Manifest(new ByteArrayInputStream(manifest.getBytes("utf-8")));
        mf.getMainAttributes().putValue("Manifest-Version", "1.0");
        JarOutputStream os = new JarOutputStream(new FileOutputStream(f), mf);
        os.close();
        return f;
    }

    private static void assertCapsEquals(Capability expected, Capability actual)
    {
        assertEquals(expected.getNamespace(), actual.getNamespace());
        assertSubMap(expected.getAttributes(), actual.getAttributes());
        assertSubMap(expected.getDirectives(), actual.getDirectives());
        // We ignore the resource in the comparison
    }

    private static void assertSubMap(Map<?,?> subMap, Map<?,?> fullMap)
    {
        for (Map.Entry<?,?> entry : subMap.entrySet())
        {
            assertEquals(entry.getValue(), fullMap.get(entry.getKey()));
        }
    }

    private static void deleteDir(File root) throws IOException
    {
        if (root.isDirectory())
        {
            for (File file : root.listFiles())
            {
                deleteDir(file);
            }
        }
        assertTrue(root.delete());
    }

    static class TestCapability implements Capability
    {
        private final String namespace;
        private final Map<String, Object> attributes;
        private final Map<String, String> directives;

        TestCapability(String ns, Map<String,Object> attrs, Map<String,String> dirs)
        {
            namespace = ns;
            attributes = attrs;
            directives = dirs;
        }

        public String getNamespace()
        {
            return namespace;
        }

        public Map<String, Object> getAttributes()
        {
            return attributes;
        }

        public Map<String, String> getDirectives()
        {
            return directives;
        }

        public Resource getResource()
        {
            return null;
        }
    }
}
