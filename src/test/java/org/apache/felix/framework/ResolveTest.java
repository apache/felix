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

import junit.framework.TestCase;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;

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

public class ResolveTest extends TestCase
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

    public void testResolveFragmentWithHost() throws Exception
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

        Bundle h = felix.getBundleContext().installBundle(bundleFile.toURI().toASCIIString());
        Bundle f = felix.getBundleContext().installBundle(fragFile.toURI().toASCIIString());

        assertEquals(Bundle.INSTALLED, h.getState());
        assertEquals(Bundle.INSTALLED, f.getState());

        felix.adapt(FrameworkWiring.class).resolveBundles(Collections.singletonList(h));

        assertEquals(Bundle.RESOLVED, h.getState());
        assertEquals(Bundle.RESOLVED, f.getState());
    }

    public void testResolveOnlyMatchingFragmentWithHost() throws Exception
    {
        String bmf = "Bundle-SymbolicName: cap.bundle\n"
            + "Bundle-Version: 1.2.3.Blah\n"
            + "Bundle-ManifestVersion: 2\n"
            + "Import-Package: org.osgi.framework\n";
        File bundleFile = createBundle(bmf);

        String bmfo = "Bundle-SymbolicName: cap.bundleo\n"
            + "Bundle-Version: 1.2.3.Blah\n"
            + "Bundle-ManifestVersion: 2\n"
            + "Import-Package: org.osgi.framework\n";
        File bundleFileO = createBundle(bmfo);

        String fmf = "Bundle-SymbolicName: cap.frag\n"
            + "Bundle-Version: 1.0.0\n"
            + "Fragment-Host: cap.bundle\n"
            + "Bundle-ManifestVersion: 2\n"
            + "Export-Package: org.foo.bar;version=\"2.0.0\"\n"
            + "Import-Package: org.osgi.util.tracker\n";
        File fragFile = createBundle(fmf);

        String fmfo = "Bundle-SymbolicName: cap.frago\n"
            + "Bundle-Version: 1.0.0\n"
            + "Fragment-Host: cap.bundleo\n"
            + "Bundle-ManifestVersion: 2\n"
            + "Export-Package: org.foo.bar;version=\"2.0.0\"\n"
            + "Import-Package: org.osgi.util.tracker\n";
        File fragFileO = createBundle(fmfo);

        Bundle h = felix.getBundleContext().installBundle(bundleFile.toURI().toASCIIString());
        Bundle f = felix.getBundleContext().installBundle(fragFile.toURI().toASCIIString());

        Bundle ho = felix.getBundleContext().installBundle(bundleFileO.toURI().toASCIIString());
        Bundle fo = felix.getBundleContext().installBundle(fragFileO.toURI().toASCIIString());

        assertEquals(Bundle.INSTALLED, h.getState());
        assertEquals(Bundle.INSTALLED, f.getState());
        assertEquals(Bundle.INSTALLED, ho.getState());
        assertEquals(Bundle.INSTALLED, fo.getState());

        felix.adapt(FrameworkWiring.class).resolveBundles(Collections.singletonList(h));

        assertEquals(Bundle.RESOLVED, h.getState());
        assertEquals(Bundle.RESOLVED, f.getState());
        assertEquals(Bundle.INSTALLED, ho.getState());
        assertEquals(Bundle.INSTALLED, fo.getState());
    }

    public void testResolveDynamicWithOnlyMatchingFragmentWithHost() throws Exception
    {
        String bmf = "Bundle-SymbolicName: cap.bundle\n"
            + "Bundle-Version: 1.2.3.Blah\n"
            + "Bundle-ManifestVersion: 2\n"
            + "Import-Package: org.osgi.framework\n";
        File bundleFile = createBundle(bmf);

        String bmfo = "Bundle-SymbolicName: cap.bundleo\n"
            + "Bundle-Version: 1.2.3.Blah\n"
            + "Bundle-ManifestVersion: 2\n"
            + "Import-Package: org.osgi.framework\n";
        File bundleFileO = createBundle(bmfo);

        String fmf = "Bundle-SymbolicName: cap.frag\n"
            + "Bundle-Version: 1.0.0\n"
            + "Fragment-Host: cap.bundle\n"
            + "Bundle-ManifestVersion: 2\n"
            + "Export-Package: org.foo.bar;version=\"2.0.0\"\n"
            + "Import-Package: org.osgi.util.tracker,test.baz\n";
        File fragFile = createBundle(fmf);

        String fmfo = "Bundle-SymbolicName: cap.frago\n"
            + "Bundle-Version: 1.0.0\n"
            + "Fragment-Host: cap.bundleo\n"
            + "Bundle-ManifestVersion: 2\n"
            + "Export-Package: org.foo.baz;version=\"2.0.0\"\n"
            + "Import-Package: org.osgi.util.tracker\n";
        File fragFileO = createBundle(fmfo);

        String dynm = "Bundle-SymbolicName: cap.dyn\n"
            + "Bundle-Version: 1.0.0\n"
            + "Bundle-ManifestVersion: 2\n"
            + "DynamicImport-Package: org.foo.*,org.osgi.*\n";
        File dynFile = createBundle(dynm);

        String reqm = "Bundle-SymbolicName: cap.req\n"
            + "Bundle-Version: 1.0.0\n"
            + "Bundle-ManifestVersion: 2\n"
            + "Export-Package: test.baz\n";
        File reqFile = createBundle(reqm);

        String reqnm = "Bundle-SymbolicName: cap.reqn\n"
            + "Bundle-Version: 1.0.0\n"
            + "Bundle-ManifestVersion: 2\n"
            + "Export-Package: test.bazz,org.foo.bar.blub\n";
        File reqnFile = createBundle(reqnm);

        Bundle h = felix.getBundleContext().installBundle(bundleFile.toURI().toASCIIString());
        Bundle f = felix.getBundleContext().installBundle(fragFile.toURI().toASCIIString());

        Bundle ho = felix.getBundleContext().installBundle(bundleFileO.toURI().toASCIIString());
        Bundle fo = felix.getBundleContext().installBundle(fragFileO.toURI().toASCIIString());

        Bundle dyn = felix.getBundleContext().installBundle(dynFile.toURI().toASCIIString());
        Bundle req = felix.getBundleContext().installBundle(reqFile.toURI().toASCIIString());
        Bundle reqn = felix.getBundleContext().installBundle(reqnFile.toURI().toASCIIString());

        assertEquals(Bundle.INSTALLED, h.getState());
        assertEquals(Bundle.INSTALLED, f.getState());
        assertEquals(Bundle.INSTALLED, ho.getState());
        assertEquals(Bundle.INSTALLED, fo.getState());
        assertEquals(Bundle.INSTALLED, dyn.getState());
        assertEquals(Bundle.INSTALLED, req.getState());
        assertEquals(Bundle.INSTALLED, reqn.getState());

        felix.adapt(FrameworkWiring.class).resolveBundles(Collections.singletonList(dyn));

        assertEquals(Bundle.INSTALLED, h.getState());
        assertEquals(Bundle.INSTALLED, f.getState());
        assertEquals(Bundle.INSTALLED, ho.getState());
        assertEquals(Bundle.INSTALLED, fo.getState());
        assertEquals(Bundle.RESOLVED, dyn.getState());
        assertEquals(Bundle.INSTALLED, req.getState());
        assertEquals(Bundle.INSTALLED, reqn.getState());

        try
        {
            dyn.loadClass("org.foo.bar.Bar");
            fail();
        }
        catch (Exception ex)
        {
            // Expected
        }
        assertEquals(Bundle.RESOLVED, h.getState());
        assertEquals(Bundle.RESOLVED, f.getState());
        assertEquals(Bundle.INSTALLED, ho.getState());
        assertEquals(Bundle.INSTALLED, fo.getState());
        assertEquals(Bundle.RESOLVED, dyn.getState());
        assertEquals(Bundle.RESOLVED, req.getState());
        assertEquals(Bundle.INSTALLED, reqn.getState());
        List<BundleWire> requiredWires = dyn.adapt(BundleWiring.class).getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
        assertEquals(1, requiredWires.size());
        assertEquals(requiredWires.get(0).getProvider().getBundle(), h);

        try
        {
            dyn.loadClass("org.foo.baz.Bar");
            fail();
        }
        catch (Exception ex)
        {
            // Expected
        }
        assertEquals(Bundle.RESOLVED, h.getState());
        assertEquals(Bundle.RESOLVED, f.getState());
        assertEquals(Bundle.RESOLVED, ho.getState());
        assertEquals(Bundle.RESOLVED, fo.getState());
        assertEquals(Bundle.RESOLVED, dyn.getState());
        assertEquals(Bundle.RESOLVED, req.getState());
        assertEquals(Bundle.INSTALLED, reqn.getState());
        requiredWires = dyn.adapt(BundleWiring.class).getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
        assertEquals(2, requiredWires.size());
        assertEquals(requiredWires.get(0).getProvider().getBundle(), h);
        assertEquals(requiredWires.get(1).getProvider().getBundle(), ho);
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
}
