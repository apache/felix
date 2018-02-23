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
package org.apache.felix.framework.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import junit.framework.TestCase;
import org.apache.felix.framework.util.WeakZipFileFactory.WeakZipFile;

public class WeakZipFileTest extends TestCase
{
    private static final String ENTRY_NAME = "entry.txt";

    public void testWeakClose()
    {
        // Create a reasonably big random string.
        byte[] contentBytes = new byte[16384];
        for (int i = 0; i < contentBytes.length; i++)
        {
            contentBytes[i] = (byte) ((i % 65) + 65);
        }
        String contentString = new String(contentBytes);

        // Create a temporary zip file.
        File tmpZip = null;
        try
        {
            tmpZip = File.createTempFile("felix.test", ".zip");
            tmpZip.deleteOnExit();
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmpZip));
            ZipEntry ze = new ZipEntry(ENTRY_NAME);
            zos.putNextEntry(ze);
            zos.write(contentBytes, 0, contentBytes.length);
            zos.close();
        }
        catch (IOException ex)
        {
            fail("Unable to create temporary zip file: " + ex);
        }

        try
        {
            WeakZipFileFactory factory = new WeakZipFileFactory(1);
            WeakZipFile zipFile = factory.create(tmpZip);
            assertTrue("Zip file not recorded.",
                factory.getZipZiles().contains(zipFile));
            assertTrue("Open zip file not recorded.",
                factory.getOpenZipZiles().contains(zipFile));
            ZipEntry ze = zipFile.getEntry(ENTRY_NAME);
            assertNotNull("Zip entry not found", ze);
            byte[] firstHalf = new byte[contentBytes.length / 2];
            byte[] secondHalf = new byte[contentBytes.length - firstHalf.length];
            InputStream is = zipFile.getInputStream(ze);
            is.read(firstHalf);
            zipFile.closeWeakly();
            assertTrue("Zip file not recorded.",
                factory.getZipZiles().contains(zipFile));
            assertFalse("Open zip file still recorded.",
                factory.getOpenZipZiles().contains(zipFile));
            is.read(secondHalf);
            assertTrue("Zip file not recorded.",
                factory.getZipZiles().contains(zipFile));
            assertTrue("Open zip file not recorded.",
                factory.getOpenZipZiles().contains(zipFile));
            byte[] complete = new byte[firstHalf.length + secondHalf.length];
            System.arraycopy(firstHalf, 0, complete, 0, firstHalf.length);
            System.arraycopy(secondHalf, 0, complete, firstHalf.length, secondHalf.length);
            String completeString = new String(complete);
            assertEquals(contentString, completeString);
            zipFile.close();
        }
        catch (IOException ex)
        {
            fail("Unable to read zip file entry: " + ex);
        }
    }
}