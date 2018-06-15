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
package org.apache.felix.framework.cache;

import junit.framework.TestCase;
import org.apache.felix.framework.Logger;
import org.osgi.framework.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class BundleCacheTest extends TestCase
{
    private File tempDir;
    private File cacheDir;
    private File filesDir;
    private BundleCache cache;
    private File archiveFile;
    private File jarFile;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        tempDir = File.createTempFile("felix-temp", ".dir");
        assertTrue("precondition", tempDir.delete());
        assertTrue("precondition", tempDir.mkdirs());

        cacheDir = new File(tempDir, "felix-cache");
        assertTrue("precondition", cacheDir.mkdir());

        filesDir = new File(tempDir, "files");
        String cacheDirPath = cacheDir.getPath();

        Map<String, String> params = new HashMap<String, String>();
        params.put("felix.cache.profiledir", cacheDirPath);
        params.put("felix.cache.dir", cacheDirPath);
        params.put(Constants.FRAMEWORK_STORAGE, cacheDirPath);

        cache = new BundleCache(new Logger(){
            @Override
            protected void doLog(int level, String msg, Throwable throwable) {
            }
        }, params);

        archiveFile = new File(filesDir, "bundle1");

        createTestArchive(archiveFile);

        File innerArchiveFile = new File(archiveFile, "inner");
        createTestArchive(innerArchiveFile);

        new File(innerArchiveFile, "empty").mkdirs();

        createJar(archiveFile, new File(archiveFile, "inner/i+?äö \\§$%nner.jar"));

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("foo", "bar");
        manifest.getMainAttributes().putValue("Manifest-Version", "v1");

        File mf = new File(archiveFile, "META-INF/MANIFEST.MF");
        mf.getParentFile().mkdirs();
        FileOutputStream output = new FileOutputStream(mf);
        manifest.write(output);
        output.close();
        jarFile = new File(filesDir, "bundle1.jar");
        createJar(archiveFile, jarFile);
    }

    public void testNoZipSlip() throws Exception
    {
        File bundle = new File(filesDir, "slip");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "v1");
        manifest.getMainAttributes().putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        manifest.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, "slip");

        JarOutputStream output = new JarOutputStream(new FileOutputStream(bundle),manifest);

        output.putNextEntry(new ZipEntry("../../bar.jar"));

        output.write(BundleCache.read(new FileInputStream(jarFile), jarFile.length()));

        output.closeEntry();

        output.close();

        BundleArchive archive = cache.create(1, 1, "slip", new FileInputStream(bundle));

        testNoZipSlip(archive);

        archive = cache.create(1, 1, bundle.toURI().toURL().toString(), null);

        testNoZipSlip(archive);

        archive = cache.create(1, 1, "reference:" + bundle.toURI().toURL().toString(), null);

        testNoZipSlip(archive);

        File dir = new File(filesDir, "exploded");

        dir.mkdirs();

        File test = new File(dir, "../../bar.jar");
        test.createNewFile();
        test.deleteOnExit();

        archive = cache.create(1, 1, "reference:" + dir.toURI().toURL().toString(), null);

        testNoZipSlip(archive);
    }

    public void testNoZipSlip(BundleArchive archive) throws Exception
    {
        Content content = archive.getCurrentRevision().getContent().getEntryAsContent("../../bar.jar");

        assertNull(content);

        String lib = archive.getCurrentRevision().getContent().getEntryAsNativeLibrary("../../bar.jar");

        assertNull(lib);
    }

    public void testDirectoryReference() throws Exception
    {
        testBundle("reference:" + archiveFile.toURI().toURL(), null);
    }

    public void testJarReference() throws Exception
    {
       testBundle("reference:" + jarFile.toURI().toURL().toString(), null);
    }

    public void testJar() throws Exception
    {
       testBundle(jarFile.toURI().toURL().toString(), null);
    }

    public void testInputStream() throws Exception
    {
        testBundle("bla", jarFile);
    }

    private void testBundle(String location, File file) throws Exception
    {
        BundleArchive archive = cache.create(1, 1, location, file != null ? new FileInputStream(file) : null);

        assertNotNull(archive);

        assertEquals(Long.valueOf(0), archive.getCurrentRevisionNumber());

        testRevision(archive);

        String nativeLib = archive.getCurrentRevision().getContent().getEntryAsNativeLibrary("file1");

        assertNotNull(nativeLib);
        assertTrue(new File(nativeLib).isFile());

        archive.revise(location, file != null ? new FileInputStream(file) : null);

        assertEquals(Long.valueOf(1), archive.getCurrentRevisionNumber());

        testRevision(archive);

        String nativeLib2 = archive.getCurrentRevision().getContent().getEntryAsNativeLibrary("file1");

        assertNotNull(nativeLib2);
        assertTrue(new File(nativeLib).isFile());
        assertTrue(new File(nativeLib2).isFile());

        archive.purge();

        assertEquals(Long.valueOf(1), archive.getCurrentRevisionNumber());

        testRevision(archive);

        String nativeLib3 = archive.getCurrentRevision().getContent().getEntryAsNativeLibrary("file1");

        assertNotNull(nativeLib3);
        assertNotSame(nativeLib, nativeLib2);
        assertNotSame(nativeLib2, nativeLib3);
        assertFalse(new File(nativeLib).isFile());
        assertTrue(new File(nativeLib2).isFile());
        assertTrue(new File(nativeLib3).isFile());
    }

    private void testRevision(BundleArchive archive) throws Exception
    {
        BundleArchiveRevision revision = archive.getCurrentRevision();
        assertNotNull(revision);
        assertNotNull(revision.getManifestHeader());
        assertEquals("bar", revision.getManifestHeader().get("foo"));
        perRevision(revision.getContent(),  new TreeSet<String>(Arrays.asList("META-INF/", "META-INF/MANIFEST.MF", "file1", "inner/", "inner/empty/", "inner/file1", "inner/i+?äö \\§$%nner.jar")));
        perRevision(revision.getContent().getEntryAsContent("inner"),  new TreeSet<String>(Arrays.asList("file1", "empty/", "i+?äö \\§$%nner.jar")));
        assertNull(revision.getContent().getEntryAsContent("inner/inner"));
        assertNotNull(revision.getContent().getEntryAsContent("inner/empty/"));
        assertNull(revision.getContent().getEntryAsContent("inner/empty").getEntries());
        perRevision(revision.getContent().getEntryAsContent("inner/").getEntryAsContent("i+?äö \\§$%nner.jar"), new TreeSet<String>(Arrays.asList("file1", "inner/", "inner/empty/", "inner/file1")));
    }

    private void perRevision(Content content, Set<String> expectedEntries) throws Exception
    {
        assertNotNull(content);
        Enumeration<String> entries =  content.getEntries();
        Set<String> foundEntries = new TreeSet<String>();
        while (entries.hasMoreElements())
        {
            foundEntries.add(entries.nextElement());
        }
        assertEquals(expectedEntries, foundEntries);

        assertTrue(content.hasEntry("file1"));
        assertFalse(content.hasEntry("foo"));
        assertFalse(content.hasEntry("foo/bar"));

        byte[] entry = content.getEntryAsBytes("file1");
        assertNotNull(entry);
        assertEquals("file1", new String(entry, "UTF-8"));
        assertNull(content.getEntryAsBytes("foo"));
        assertNull(content.getEntryAsBytes("foo/bar"));


        InputStream input = content.getEntryAsStream("file1");
        assertNotNull(input);
        entry = new byte[1014];
        int j = 0;
        for (int i = input.read();i != -1; i = input.read())
        {
            entry[j++] = (byte) i;
        }
        assertEquals("file1", new String(entry,  0 , j, "UTF-8"));
        assertNull(content.getEntryAsStream("foo"));
        assertNull(content.getEntryAsStream("foo/bar"));

        URL url = content.getEntryAsURL("file1");
        assertNotNull(url);
        input = url.openStream();
        assertNotNull(input);
        entry = new byte[1014];
        j = 0;
        for (int i = input.read();i != -1; i = input.read())
        {
            entry[j++] = (byte) i;
        }
        assertEquals("file1", new String(entry,  0 , j, "UTF-8"));
        assertNull(content.getEntryAsURL("foo"));
        assertNull(content.getEntryAsURL("foo/bar"));

        assertNull(content.getEntryAsNativeLibrary("blub"));
        String nativeLib = content.getEntryAsNativeLibrary("file1");
        assertNotNull(nativeLib);
        assertTrue(new File(nativeLib).isFile());
        content.close();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        cache.delete();
        assertTrue(!cacheDir.exists());
        assertTrue(BundleCache.deleteDirectoryTree(tempDir));
    }

    private void createTestArchive(File archiveFile) throws Exception
    {
        createFile(archiveFile, "file1", "file1".getBytes("UTF-8"));
    }

    private void createFile(File parent, String path, byte[] content) throws IOException
    {
        File target = new File(parent, path);

        target.getParentFile().mkdirs();

        assertTrue(target.getParentFile().isDirectory());

        FileOutputStream output = new FileOutputStream(target);
        try
        {
            output.write(content);
        }
        finally
        {
            output.close();
        }
    }

    private void createJar(File source, File target) throws Exception
    {
        File tmp = File.createTempFile("bundle", ".jar", filesDir);
        JarOutputStream output;
        if (new File(source, "META-INF/MANIFEST.MF").isFile())
        {
           output = new JarOutputStream(new FileOutputStream(tmp),new Manifest(new FileInputStream(new File(source, "META-INF/MANIFEST.MF"))));
        }
        else
        {
            output = new JarOutputStream(new FileOutputStream(tmp));
        }

        writeRecursive(source, "", output);

        output.close();
        target.delete();
        tmp.renameTo(target);
    }

    private void writeRecursive(File current, String path, JarOutputStream output) throws Exception
    {
        if (current.isDirectory())
        {
            File[] children = current.listFiles();
            if (children != null)
            {
                for (File file : children)
                {
                    if (!file.getName().equals("MANIFEST.MF"))
                    {
                        String next = path + file.getName();
                        if (file.isDirectory())
                        {
                            next += "/";
                        }
                        output.putNextEntry(new ZipEntry(next));
                        if (file.isDirectory())
                        {
                            output.closeEntry();
                        }
                        writeRecursive(file, next, output);
                    }
                }
            }
        }
        else if (current.isFile())
        {
            output.write(BundleCache.read(new FileInputStream(current), current.length()));
            output.closeEntry();
        }
    }
}
