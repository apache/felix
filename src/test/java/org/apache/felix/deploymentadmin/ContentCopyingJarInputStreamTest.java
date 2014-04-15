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
package org.apache.felix.deploymentadmin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;

import junit.framework.TestCase;

/**
 * Test cases for {@link ContentCopyingJarInputStream}.
 */
public class ContentCopyingJarInputStreamTest extends TestCase
{
    private static final String MANIFEST_NAME = JarFile.MANIFEST_NAME;
    private static final String INDEX_NAME = "META-INF/INDEX.LIST";

    private File m_tempDir;
    private File m_jarFile;

    /**
     * Tests that we can copy a {@link JarInputStream} containing only a manifest. 
     */
    public void testCopyEmptyJarWithManifestOnlyOk() throws Exception
    {
        Manifest man = createManifest();

        createEmptyJar(man);

        assertJarContents(man);
    }

    /**
     * Tests that we can copy a simple {@link JarInputStream}. 
     */
    public void testCopyJarWithIndexAndWithManifestOk() throws Exception
    {
        Manifest man = createManifest();

        createJar(man, true /* includeIndex */);

        assertJarContents(man);
    }

    /**
     * Tests that we can copy a {@link JarInputStream} even if it does not contains a manifest file. 
     */
    public void testCopyJarWithIndexAndWithoutManifestOk() throws Exception
    {
        Manifest man = null;

        createJar(man, true /* includeIndex */);

        assertJarContents(man);
    }

    /**
     * Tests that we can copy a simple {@link JarInputStream}. 
     */
    public void testCopyJarWithoutIndexAndWithManifestOk() throws Exception
    {
        Manifest man = createManifest();

        createJar(man, false /* includeIndex */);

        assertJarContents(man);
    }

    /**
     * Tests that we can copy a {@link JarInputStream} even if it does not contains a manifest file. 
     */
    public void testCopyJarWithoutIndexAndWithoutManifestOk() throws Exception
    {
        Manifest man = null;

        createJar(man, false /* includeIndex */);

        assertJarContents(man);
    }

    protected void setUp() throws IOException
    {
        m_tempDir = createTempDir();
        m_jarFile = new File(m_tempDir, "input.jar");
    }

    protected void tearDown()
    {
        Utils.delete(m_tempDir, true);
    }

    private void appendFiles(JarOutputStream jos, int count) throws IOException
    {
        int size = 1024;

        for (int i = 0, j = 1; i < count; i++, j++)
        {
            JarEntry entry = new JarEntry("sub/" + j);
            jos.putNextEntry(entry);
            for (int k = 0; k < size; k++)
            {
                jos.write('0' + j);
            }
            jos.closeEntry();
        }
    }

    private void assertJarContents(Manifest man) throws IOException
    {
        File indexFile = new File(m_tempDir, "index.txt");

        FileInputStream fis = new FileInputStream(m_jarFile);
        JarInputStream jis = new ContentCopyingJarInputStream(fis, indexFile, m_tempDir);

        try
        {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null)
            {
                File f = new File(m_tempDir, entry.getName());

                // Without reading the actual contents, the copy should already exist...
                assertTrue(entry.getName() + " does not exist?!", f.exists());

                int size = (INDEX_NAME.equals(entry.getName()) ? 33 : 1024);

                byte[] input = new byte[size];
                int read = jis.read(input);

                assertEquals("Not all bytes were read: " + entry.getName(), size, read);

                // Contents will only be completely written after closing the JAR entry itself...
                jis.closeEntry();

                verifyContents(f, input);
            }

            assertEquals("Manifest not as expected", man, jis.getManifest());
        }
        finally
        {
            jis.close();
        }
    }

    private void createEmptyJar(Manifest man) throws IOException
    {
        FileOutputStream fos = new FileOutputStream(m_jarFile);
        JarOutputStream jos = new JarOutputStream(fos, man);
        jos.close();
    }

    private void createJar(Manifest man, boolean includeIndex) throws IOException
    {
        FileOutputStream fos = new FileOutputStream(m_jarFile);
        JarOutputStream jos;

        if (man == null || includeIndex)
        {
            jos = new JarOutputStream(fos);
        }
        else
        {
            jos = new JarOutputStream(fos, man);
        }

        if (includeIndex)
        {
            // Write the INDEX.LIST file as first entry...
            jos.putNextEntry(new ZipEntry(INDEX_NAME));
            jos.write(("JarIndex-Version: 1.0\n\n" + m_jarFile.getName() + "\n").getBytes());
            jos.closeEntry();

            if (man != null)
            {
                jos.putNextEntry(new ZipEntry(MANIFEST_NAME));
                man.write(jos);
                jos.closeEntry();
            }
        }

        try
        {
            appendFiles(jos, 5);
        }
        finally
        {
            jos.close();
        }
    }

    private Manifest createManifest()
    {
        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue("Manifest-Version", "1.0");
        mf.getMainAttributes().putValue("Bundle-ManifestVersion", "2");
        mf.getMainAttributes().putValue("Bundle-Version", "1.0.0");
        mf.getMainAttributes().putValue("Bundle-SymbolicName", "com.foo.bar");
        return mf;
    }

    private File createTempDir() throws IOException
    {
        File tmpFile = File.createTempFile("ccjis_test", null);
        tmpFile.delete();
        tmpFile.mkdir();
        return tmpFile;
    }

    private void verifyContents(File file, byte[] expected) throws IOException
    {
        FileInputStream fis = new FileInputStream(file);
        GZIPInputStream gis = new GZIPInputStream(fis);
        try
        {
            byte[] b = new byte[expected.length];

            int read = gis.read(b);
            assertEquals(b.length, read);

            for (int i = 0; i < expected.length; i++)
            {
                assertEquals(expected[i], b[i]);
            }
        }
        finally
        {
            gis.close();
            fis.close();
        }
    }
}
