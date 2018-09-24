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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class ConcurrentClassLoaderTest extends TestCase
{
    private static final int CONCURRENCY_LEVEL = 1000;

    private File m_cacheDir;
    private Framework m_felix;

    public void testCanResolveClassInParallel() throws Exception
    {
        m_cacheDir = createCacheDir();
        m_felix = createFramework(m_cacheDir);
        m_felix.init();
        m_felix.getBundleContext().installBundle(createBundleWithDynamicImportPackage());
        m_felix.start();

        Bundle[] bundles = m_felix.getBundleContext().getBundles();
        assertEquals("Two, system and mine: " + Arrays.toString(bundles), 2, bundles.length);
        final Bundle bundle = bundles[1];

        // This latch ensures that all threads start at the same time
        final CountDownLatch latch = new CountDownLatch(CONCURRENCY_LEVEL);
        final AtomicInteger doneCount = new AtomicInteger();
        for (int i = 0; i < CONCURRENCY_LEVEL; i++) {
            new Thread()
            {
                public void run()
                {
                    try {
                        latch.countDown();
                        latch.await();
                        bundle.loadClass("com.sun.this.class.does.not.exist.but.asking.for.it.must.not.block");
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        doneCount.incrementAndGet();
                    }
                }
            }.start();
        }

        // Wait for 1 minute for all threads to catch up
        long timeout = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        while (System.currentTimeMillis() < timeout && doneCount.get() != CONCURRENCY_LEVEL) {
            Thread.sleep(50);
        }

        assertEquals("Class resolution all started", 0, latch.getCount());
        assertEquals("Class resolution all finished", CONCURRENCY_LEVEL, doneCount.get());
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();

        m_felix.stop();
        m_felix.waitForStop(1000);
        m_felix = null;

        delete(m_cacheDir);
    }

    private static File createCacheDir() throws IOException
    {
        File cacheDir = File.createTempFile("felix-cache", ".dir");
        cacheDir.delete();
        cacheDir.mkdirs();
        return cacheDir;
    }

    private static Framework createFramework(File cacheDir)
    {
        Map<String, String> params = new HashMap<String, String>();
        params.put(Constants.FRAMEWORK_SYSTEMPACKAGES, "org.osgi.framework; version=1.4.0,"
                + "org.osgi.service.packageadmin; version=1.2.0,"
                + "org.osgi.service.startlevel; version=1.1.0,"
                + "org.osgi.util.tracker; version=1.3.3,"
                + "org.osgi.service.url; version=1.0.0");

        params.put("felix.cache.profiledir", cacheDir.getPath());
        params.put("felix.cache.dir", cacheDir.getPath());
        params.put(Constants.FRAMEWORK_STORAGE, cacheDir.getPath());

        return new Felix(params);
    }


    private static String createBundleWithDynamicImportPackage() throws IOException
    {
        String manifest = "Bundle-SymbolicName: boot.test\n" + "Bundle-Version: 1.1.0\n" + "Bundle-ManifestVersion: 2\n" + "DynamicImport-Package: com.sun.*";

        File f = File.createTempFile("felix-bundle", ".jar");
        f.deleteOnExit();

        Manifest mf = new Manifest(new ByteArrayInputStream(manifest.getBytes("utf-8")));
        mf.getMainAttributes().putValue("Manifest-Version", "1.0");
        JarOutputStream os = new JarOutputStream(new FileOutputStream(f), mf);
        os.close();

        return f.toURI().toString();
    }

    private static void delete(File file) throws IOException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    delete(child);
                }
            }
        }
        file.delete();
    }
}
