package org.apache.felix.deploymentadmin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
}
