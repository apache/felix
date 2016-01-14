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

import static org.apache.felix.deploymentadmin.Utils.closeSilently;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;

/**
 * Provides a custom {@link JarInputStream} that copies all entries read from the original {@link InputStream} to a
 * given directory and index file. It does this by tracking thecommon usage of the {@link JarInputStream} API. For each
 * entry that is read it streams all read bytes to a separate file compressing it on the fly. The caller does not notice
 * anything, although it might be that the {@link #read(byte[], int, int)} is blocked for a little while during the
 * writing of the file contents.
 * <p>
 * This implementation replaces the old <tt>ExplodingOutputtingInputStream</tt> that used
 * at least two threads and was difficult to understand and maintain. See FELIX-4486.
 * </p>
 */
class ContentCopyingJarInputStream extends JarInputStream {
    private static final String MANIFEST_FILE = JarFile.MANIFEST_NAME;

    private final File m_contentDir;

    private PrintWriter m_indexFileWriter;
    /** Used to copy the contents of the *next* entry. */
    private OutputStream m_entryOS;

    public ContentCopyingJarInputStream(InputStream in, File indexFile, File contentDir) throws IOException {
        super(in, true /* verify */);

        m_contentDir = contentDir;

        m_indexFileWriter = new PrintWriter(new FileWriter(indexFile));
        m_entryOS = null;

        // the manifest of the JAR is already read by JarInputStream, so we need to write this one as well...
        Manifest manifest = getManifest();
        if (manifest != null) {
            copyManifest(manifest);
        }
    }

    public void close() throws IOException {
        closeCopy();
        closeIndex();
        // Do NOT close our parent, as it is the original input stream which is not under our control...
    }

    public void closeEntry() throws IOException {
        closeCopy();
        super.closeEntry();
    }

    public ZipEntry getNextEntry() throws IOException {
        closeCopy();

        ZipEntry entry = super.getNextEntry();
        if (entry != null) {
            File current = new File(m_contentDir, entry.getName());
            if (!entry.isDirectory()) {
                addToIndex(entry.getName());

                m_entryOS = createOutputStream(current);
            }
        }

        return entry;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int r = super.read(b, off, len);
        if (m_entryOS != null) {
            if (r > 0) {
                m_entryOS.write(b, off, r);
            }
            else {
                closeCopy();
            }
        }
        return r;
    }

    private void addToIndex(String name) throws IOException {
        m_indexFileWriter.println(name);
        m_indexFileWriter.flush();
    }

    private void closeCopy() {
        closeSilently(m_entryOS);
        m_entryOS = null;
    }

    private void closeIndex() {
        closeSilently(m_indexFileWriter);
        m_indexFileWriter = null;
    }

    /**
     * Creates a verbatim copy of the manifest, when it is read from the original JAR.
     */
    private void copyManifest(Manifest manifest) throws IOException {
        addToIndex(MANIFEST_FILE);

        OutputStream os = createOutputStream(new File(m_contentDir, MANIFEST_FILE));
        try {
            manifest.write(os);
        }
        finally {
            closeSilently(os);
        }
    }

    private OutputStream createOutputStream(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        if (!file.createNewFile()) {
            throw new IOException("Attempt to overwrite file: " + file);
        }
        return new GZIPOutputStream(new FileOutputStream(file));
    }
}
