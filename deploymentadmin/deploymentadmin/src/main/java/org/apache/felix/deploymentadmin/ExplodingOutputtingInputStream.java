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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This class will write all entries encountered in an input stream to disk. An index of files written to disk is kept in an index file in the
 * order they were encountered. Each file is compressed using GZIP. All the work is done on a separate thread.
 */
class ExplodingOutputtingInputStream extends OutputtingInputStream {
    
    static class ReaderThread extends Thread {
        
        private final File m_contentDir;
        private final File m_indexFile;
        private final PipedInputStream m_input;
        
        private volatile Exception m_exception;

        public ReaderThread(PipedOutputStream output, File index, File root) throws IOException {
            super("Apache Felix DeploymentAdmin - ExplodingOutputtingInputStream");
            
            m_contentDir = root;
            m_indexFile = index;
            m_input = new PipedInputStream(output);
        }
        
        public void run() {
            byte[] buffer = new byte[4096];

            ZipInputStream input = null;
            PrintWriter writer = null;
            try {
                input = new ZipInputStream(m_input);
                writer = new PrintWriter(new FileWriter(m_indexFile));

                for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
                    File current = new File(m_contentDir, entry.getName());
                    if (entry.isDirectory()) {
                        current.mkdirs();
                    }
                    else {
                        writer.println(entry.getName());
                        File parent = current.getParentFile();
                        if (parent != null) {
                            parent.mkdirs();
                        }
                        OutputStream output = null;
                        try {
                            output = new GZIPOutputStream(new FileOutputStream(current));
                            for (int i = input.read(buffer); i > -1; i = input.read(buffer)) {
                                output.write(buffer, 0, i);
                            }
                        }
                        finally {
                            output.close();
                        }
                    }
                    input.closeEntry();
                    writer.flush();
                }
            }
            catch (IOException ex) {
                pushException(ex);
            }
            finally {
                if (writer != null) {
                    writer.close();
                }
            }

            try {
                Utils.readUntilEndOfStream(m_input);
            }
            catch (IOException e) {
                pushException(e);
            }
            finally {
                if (input != null) {
                    try {
                        input.close();
                    }
                    catch (IOException e) {
                        pushException(e);
                    }
                }
            }
        }

        private void pushException(Exception e) {
            Exception e2 = new Exception(e.getMessage());
            e2.setStackTrace(e.getStackTrace());
            if (m_exception != null) {
                e2.initCause(m_exception);
            }
            m_exception = e2;
        }
    }

    private final ReaderThread m_task;

    /**
     * Creates an instance of this class.
     *
     * @param inputStream The input stream that will be written to disk as individual entries as it's read.
     * @param indexFile File to be used to write the index of all encountered files.
     * @param contentDir File to be used as the directory to hold all files encountered in the stream.
     * @throws IOException If a problem occurs reading the stream resources.
     */
    public ExplodingOutputtingInputStream(InputStream inputStream, File indexFile, File contentDir) throws IOException {
        this(inputStream, new PipedOutputStream(), indexFile, contentDir);
    }

    private ExplodingOutputtingInputStream(InputStream inputStream, PipedOutputStream output, File index, File root) throws IOException {
        super(inputStream, output);
        m_task = new ReaderThread(output, index, root);
        m_task.start();
    }

    public void close() throws IOException {
        try {
            super.close();
            
            Exception exception = m_task.m_exception;
            if (exception != null) {
                throw new IOException(exception);
            }
        }
        finally {
            waitFor();
        }
    }

    private void waitFor() {
        try {
            m_task.join();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
