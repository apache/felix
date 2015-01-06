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
package org.apache.felix.deploymentadmin.spi;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import junit.framework.TestCase;

import org.apache.felix.deploymentadmin.Utils;

/**
 * Test cases for {@link SnapshotCommand}.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SnapshotCommandTest extends TestCase {
    private final List m_cleanup = new ArrayList();

    /**
     * Tests that an archive can be correctly restored.
     * <p>
     * This tests FELIX-4719.
     * </p>
     */
    public void testRestoreArchiveOk() throws Exception {
        // Set up a file-hierarchy we can archive...
        File baseDir = createFileHierarchy();

        File archiveFile = new File(baseDir, "../archive.zip");
        m_cleanup.add(archiveFile.getCanonicalFile());

        SnapshotCommand.store(baseDir, archiveFile);
        assertTrue("Archive not created?!", archiveFile.exists());

        File targetDir = createTempDir();
        SnapshotCommand.restore(archiveFile, targetDir);

        verifyDirContents(baseDir, targetDir);
    }

    /**
     * Tests that a directory (data-area) is correctly archived, and that the contents of that archive are as expected.
     * <p>
     * This tests FELIX-4718.
     * </p>
     */
    public void testStoreDataAreaOk() throws Exception {
        // Set up a file-hierarchy we can archive...
        File baseDir = createFileHierarchy();

        File archiveFile = new File(baseDir, "../archive.zip");
        m_cleanup.add(archiveFile.getCanonicalFile());

        SnapshotCommand.store(baseDir, archiveFile);
        assertTrue("Archive not created?!", archiveFile.exists());

        verifyArchiveContents(archiveFile, 3 /* dirs */, 6 /* files */);
    }

    protected void tearDown() throws Exception {
        Iterator iter = m_cleanup.iterator();
        while (iter.hasNext()) {
            File file = (File) iter.next();
            if (file.isFile()) {
                file.delete();
            }
            else if (file.isDirectory()) {
                Utils.delete(file, true /* deleteRoot */);
            }
            iter.remove();
        }
    }

    private void close(Closeable resource) throws IOException {
        if (resource != null) {
            resource.close();
        }
    }

    private void createFile(File file, int size) throws IOException {
        assertTrue(file.createNewFile());
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);

            byte[] data = new byte[size];
            new Random().nextBytes(data);

            fos.write(data);
        }
        finally {
            close(fos);
        }
    }

    /**
     * Creates a simple file hierarchy with a couple of files and directories (6 files and 3 directories).
     * 
     * @return the base directory where the file hierarchy is stored, never <code>null</code>.
     */
    private File createFileHierarchy() throws IOException {
        File base = createTempDir();

        File dir1 = new File(base, "dir1");
        assertTrue(dir1.mkdir());

        File dir2 = new File(dir1, "dir1a");
        assertTrue(dir2.mkdir());

        File dir3 = new File(dir1, "dir1b");
        assertTrue(dir3.mkdir());

        createFile(new File(base, "file1"), 1024);
        createFile(new File(dir1, "file2"), 2048);
        createFile(new File(dir2, "file3"), 4096);
        createFile(new File(base, "file4"), 8192);
        createFile(new File(dir2, "file5"), 16384);
        createFile(new File(dir3, "file6"), 32768);

        return base;
    }

    private File createTempDir() throws IOException {
        File dir = File.createTempFile("felix4718-", "");
        assertTrue(dir.delete());
        assertTrue(dir.mkdirs());
        // For test cleanup...
        m_cleanup.add(dir);
        return dir;
    }

    private void verifyArchiveContents(File archive, int expectedDirCount, int expectedFileCount) throws IOException {
        FileInputStream fis = null;
        ZipInputStream zis = null;

        try {
            fis = new FileInputStream(archive);
            zis = new ZipInputStream(fis);

            int dirCount = 0;
            int fileCount = 0;

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    dirCount++;
                }
                else {
                    fileCount++;
                }
                zis.closeEntry();
            }

            assertEquals("Unexpected number of files", expectedFileCount, fileCount);
            assertEquals("Unexpected number of directories", expectedDirCount, dirCount);
        }
        finally {
            close(fis);
            close(zis);
        }
    }

    private void verifyDirContents(File expectedBase, File actualBase) throws IOException {
        String[] expectedFiles = expectedBase.list();
        for (int i = 0; i < expectedFiles.length; i++) {
            File expected = new File(expectedBase, expectedFiles[i]);
            File actual = new File(actualBase, expectedFiles[i]);

            if (expected.isDirectory()) {
                assertTrue("Directory '" + expectedFiles[i] + "' does not exist in " + actualBase, actual.isDirectory());
                verifyDirContents(expected, actual);
            }
            else if (expected.isFile()) {
                assertTrue("File '" + expectedFiles[i] + "' does not exist in " + actualBase, actual.isFile());
                verifyFileContents(expected, actual);
            }
            else {
                fail("Unknown entity: '" + expectedFiles[i] + "'");
            }
        }
    }

    private void verifyFileContents(File expected, File actual) throws IOException {
        assertEquals("File size mismatch!", expected.length(), actual.length());

        FileInputStream fis1 = null;
        FileInputStream fis2 = null;
        try {
            fis1 = new FileInputStream(expected);
            fis2 = new FileInputStream(actual);

            int eb;
            while ((eb = fis1.read()) != -1) {
                int ab = fis2.read();
                assertEquals(eb, ab);
            }
        }
        finally {
            close(fis2);
            close(fis1);
        }
    }
}
