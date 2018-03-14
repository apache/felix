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
import org.osgi.framework.wiring.BundleWiring;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class MultiReleaseVersionTest extends TestCase
{
    public void testMultiReleaseVersionBundle() throws Exception
    {
        Map params = new HashMap();
        /*params.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
                "org.osgi.framework; version=1.4.0,"
                        + "org.osgi.service.packageadmin; version=1.2.0,"
                        + "org.osgi.service.startlevel; version=1.1.0,"
                        + "org.osgi.util.tracker; version=1.3.3,"
                        + "org.osgi.service.url; version=1.0.0");*/
        File cacheDir = File.createTempFile("felix-cache", ".dir");
        cacheDir.delete();
        cacheDir.mkdirs();
        String cache = cacheDir.getPath();
        params.put("felix.cache.profiledir", cache);
        params.put("felix.cache.dir", cache);
        params.put("java.specification.version", "9");
        params.put(Constants.FRAMEWORK_STORAGE, cache);

        String mf = "Bundle-SymbolicName: multi.test\n"
                + "Bundle-Version: 1.0.0\n"
                + "Bundle-ManifestVersion: 2\n"
                + "Multi-Release: true\n";

        File bundleFile = createBundle(mf, cacheDir);

        Framework f = null;
        try
        {
            f = new Felix(params);
            f.init();
            f.start();

            Bundle b = f.getBundleContext().installBundle(bundleFile.toURI().toURL().toExternalForm());
            b.start();
            assertEquals(b.loadClass(Test.class.getName()).getName(), Test.class.getName());
            Collection<String> org = b.adapt(BundleWiring.class).listResources("org", "*.class", BundleWiring.LISTRESOURCES_RECURSE);
            assertEquals(1, org.size());
            assertEquals("org.osgi.framework", b.getHeaders().get(Constants.IMPORT_PACKAGE));
        }
        finally
        {
            try
            {
                if (f != null)
                {
                    f.stop();
                    f.waitForStop(1000);
                }
            }
            finally {
                deleteDir(cacheDir);
            }
        }
    }

    private static File createBundle(String manifest, File tempDir) throws IOException
    {
        File f = File.createTempFile("felix-bundle", ".jar", tempDir);

        Manifest mf = new Manifest(new ByteArrayInputStream(manifest.getBytes("utf-8")));
        mf.getMainAttributes().putValue("Manifest-Version", "1.0");
        //mf.getMainAttributes().putValue(Constants.BUNDLE_ACTIVATOR, StartStopBundleTest.TestBundleActivator.class.getName());
        JarOutputStream os = new JarOutputStream(new FileOutputStream(f), mf);

        String path = Test.class.getName().replace('.', '/') + ".class";
        os.putNextEntry(new ZipEntry("META-INF/versions/9/" + path));

        InputStream is = Test.class.getClassLoader().getResourceAsStream(path);
        byte[] b = new byte[is.available()];
        is.read(b);
        is.close();
        os.write(b);

        os.putNextEntry(new ZipEntry("META-INF/versions/9/OSGI-INF/MANIFEST.MF"));
        Manifest subMF = new Manifest( );
        subMF.getMainAttributes().putValue("Import-Package", "org.osgi.framework");
        subMF.getMainAttributes().putValue("Manifest-Version", "1.0"); 
        subMF.write(os);
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

    public static class Test {}
}
