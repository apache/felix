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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.packageadmin.ExportedPackage;

public class UninstallBundleTest extends TestCase
{
    private static final int DELAY = 1000;

    public void testUninstallBundleCleansUpRevision() throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
            "org.osgi.framework; version=1.4.0,"
            + "org.osgi.service.packageadmin; version=1.2.0,"
            + "org.osgi.service.startlevel; version=1.1.0,"
            + "org.osgi.util.tracker; version=1.3.3,"
            + "org.osgi.service.url; version=1.0.0");
        File cacheDir = File.createTempFile("felix-cache", ".dir");
        cacheDir.delete();
        cacheDir.mkdirs();
        String cache = cacheDir.getPath();
        params.put("felix.cache.profiledir", cache);
        params.put("felix.cache.dir", cache);
        params.put(Constants.FRAMEWORK_STORAGE, cache);

        String mfA = "Bundle-SymbolicName: A\n"
                + "Bundle-ManifestVersion: 2\n"
                + "Export-Package: org.foo.bar\n";
        File bundleAFile = createBundle(mfA, cacheDir);

        String mfB = "Bundle-SymbolicName: B\n"
                + "Bundle-ManifestVersion: 2\n"
                + "Export-Package: org.foo.bbr\n";
        File bundleBFile = createBundle(mfB, cacheDir);

        String mfC = "Bundle-SymbolicName: C\n"
                + "Bundle-ManifestVersion: 2\n"
                + "Import-Package: org.foo.bar, org.foo.bbr\n";
        File bundleCFile = createBundle(mfC, cacheDir);

        final List<Bundle> shouldNotBeRefreshed = new ArrayList<Bundle>();
        Felix felix = new Felix(params) {
            @Override
            void refreshPackages(Collection<Bundle> targets, FrameworkListener[] listeners)
            {
                if (targets != null)
                {
                    for (Bundle b : targets)
                    {
                        if (shouldNotBeRefreshed.contains(b))
                            fail("Bundle " + b + " should not be refreshed");
                    }
                }
                super.refreshPackages(targets, listeners);
            }
        };

        felix.init();
        felix.start();

        try
        {
            Bundle bundleA = felix.getBundleContext().installBundle(bundleAFile.toURI().toString());
            bundleA.start();

            Bundle bundleB = felix.getBundleContext().installBundle(bundleBFile.toURI().toString());
            bundleB.start();
            // This bundle is not going to be uninstalled, so it should not be refreshed
            shouldNotBeRefreshed.add(bundleB);

            Bundle bundleC = felix.getBundleContext().installBundle(bundleCFile.toURI().toString());
            bundleC.start();

            bundleA.uninstall();

            boolean foundBar = false;
            for (ExportedPackage ep : felix.getExportedPackages((Bundle) null))
            {
                if ("org.foo.bar".equals(ep.getName()))
                    foundBar = true;
            }
            assertTrue("The system should still export org.foo.bar as C is importing it.", foundBar);

            bundleC.uninstall();

            for (ExportedPackage ep : felix.getExportedPackages((Bundle) null))
            {
                if ("org.foo.bar".equals(ep.getName()))
                    fail("Should not export org.foo.bar any more!");
            }

            boolean foundBbr = false;
            for (ExportedPackage ep : felix.getExportedPackages((Bundle) null))
            {
                if ("org.foo.bbr".equals(ep.getName()))
                    foundBbr = true;
            }
            assertTrue("The system should still export org.foo.bbr as it was not uninstalled.", foundBbr);
        }
        finally
        {
            felix.stop();
            Thread.sleep(DELAY);
            deleteDir(cacheDir);
        }
    }

    private static File createBundle(String manifest, File tempDir) throws IOException
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
