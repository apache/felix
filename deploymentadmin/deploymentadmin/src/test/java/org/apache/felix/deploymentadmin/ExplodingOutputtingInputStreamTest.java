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
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import junit.framework.TestCase;

public class ExplodingOutputtingInputStreamTest extends TestCase {
    public void testStream() throws Exception {
        // fill up a stringbuffer with some test data
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 1000; i++) {
            sb.append("DATAdataDATAdata");
        }
        String data = sb.toString();
        
        // create a temporary folder
        File tempDir = File.createTempFile("temp", "dir");
        tempDir.delete();
        tempDir.mkdirs();
        System.out.println("Dir: " + tempDir);
        
        // create a zip file with two entries in it
        File zipfile = new File(tempDir, "zipfile");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipfile));
        String dummy1 = "dummy";
        zos.putNextEntry(new ZipEntry(dummy1));
        zos.write(data.getBytes());
        zos.closeEntry();
        String dummy2 = "dummy2";
        zos.putNextEntry(new ZipEntry(dummy2));
        zos.write(data.getBytes());
        zos.closeEntry();
        zos.close();
        
        // create another temporary folder
        File dir = new File(tempDir, "dir");
        dir.mkdirs();
        File index = new File(tempDir, "list");
        ExplodingOutputtingInputStream stream = new ExplodingOutputtingInputStream(new FileInputStream(zipfile), index, dir);
        byte[] buffer = new byte[2];
        int read = stream.read(buffer);
        while (read != -1) {
            read = stream.read(buffer);
        }
        stream.close();
        
        // create references to the unpacked dummy files
        File d1 = new File(dir, dummy1);
        File d2 = new File(dir, dummy2);
        
        // cleanup
        zipfile.delete();
        index.delete();
        d1.delete();
        d2.delete();
        dir.delete();
        tempDir.delete();
    }
    
    public void testStreamReadWithJARStream() throws Exception {
        // fill up a stringbuffer with some test data
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 1000; i++) {
            sb.append("DATAdataDATAdata");
        }
        String data = sb.toString();
        
        // create a temporary folder
        File tempDir = File.createTempFile("temp", "dir");
        tempDir.delete();
        tempDir.mkdirs();
        System.out.println("Dir: " + tempDir);
        
        // create a zip file with two entries in it
        File jarfile = new File(tempDir, "jarfile");
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarfile));
        String dummy1 = "dummy";
        jos.putNextEntry(new JarEntry(dummy1));
        jos.write(data.getBytes());
        jos.closeEntry();
        String dummy2 = "dummy2";
        jos.putNextEntry(new JarEntry(dummy2));
        jos.write(data.getBytes());
        jos.closeEntry();
        jos.close();
        
        // create another temporary folder
        File dir = new File(tempDir, "dir");
        dir.mkdirs();
        File index = new File(tempDir, "list");
        ExplodingOutputtingInputStream stream = new ExplodingOutputtingInputStream(new FileInputStream(jarfile), index, dir);
        JarInputStream jarInputStream = new JarInputStream(stream);

        JarEntry entry;
        while ((entry = jarInputStream.getNextJarEntry()) != null) {
            int size = 0;
            byte[] buffer = new byte[4096];
            for (int i = jarInputStream.read(buffer); i > -1; i = jarInputStream.read(buffer)) {
                size += i;
            }
            System.out.println("read JAR entry: " + entry + " of " + size + " bytes.");
            jarInputStream.closeEntry();
        }
        stream.close();
        
        // create references to the unpacked dummy files
        File d1 = new File(dir, dummy1);
        File d2 = new File(dir, dummy2);
        
        // cleanup
        jarfile.delete();
        index.delete();
        d1.delete();
        d2.delete();
        dir.delete();
        tempDir.delete();
    }
}
