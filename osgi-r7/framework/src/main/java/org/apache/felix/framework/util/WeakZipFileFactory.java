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
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class implements a factory for creating weak zip files, which behave
 * mostly like a ZipFile, but can be weakly closed to limit the number of
 * open files.
 */
public class WeakZipFileFactory
{
    private static final int WEAKLY_CLOSED = 0;
    private static final int OPEN = 1;
    private static final int CLOSED = 2;

    private static final SecureAction m_secureAction = new SecureAction();

    private final List<WeakZipFile> m_zipFiles = new ArrayList<WeakZipFile>();
    private final List<WeakZipFile> m_openFiles = new ArrayList<WeakZipFile>();
    private final Mutex m_globalMutex = new Mutex();
    private final int m_limit;

    /**
     * Constructs a weak zip file factory with the specified file limit. A limit
     * of zero signifies no limit.
     * @param limit maximum number of open zip files at any given time.
     */
    public WeakZipFileFactory(int limit)
    {
        if (limit < 0)
        {
            throw new IllegalArgumentException("Limit must be non-negative.");
        }
        m_limit = limit;
    }

    /**
     * Factory method used to create weak zip files.
     * @param file the target zip file.
     * @return the created weak zip file.
     * @throws IOException if the zip file could not be opened.
     */
    public WeakZipFile create(File file) throws IOException
    {
        WeakZipFile wzf = new WeakZipFile(file);

        if (m_limit > 0)
        {
            try
            {
                m_globalMutex.down();
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while acquiring global zip file mutex.");
            }

            try
            {
                m_zipFiles.add(wzf);
                m_openFiles.add(wzf);
                if (m_openFiles.size() > m_limit)
                {
                    WeakZipFile candidate = m_openFiles.get(0);
                    for (WeakZipFile tmp : m_openFiles)
                    {
                        if (candidate.m_timestamp > tmp.m_timestamp)
                        {
                            candidate = tmp;
                        }
                    }
                    candidate._closeWeakly();
                }
            }
            finally
            {
                m_globalMutex.up();
            }
        }

        return wzf;
    }

    /**
     * Only used for testing.
     * @return unclosed weak zip files.
     **/
    List<WeakZipFile> getZipZiles()
    {
        try
        {
            m_globalMutex.down();
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            return Collections.EMPTY_LIST;
        }

        try
        {
            return m_zipFiles;
        }
        finally
        {
            m_globalMutex.up();
        }
    }

    /**
     * Only used for testing.
     * @return open weak zip files.
     **/
    List<WeakZipFile> getOpenZipZiles()
    {
        try
        {
            m_globalMutex.down();
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            return Collections.EMPTY_LIST;
        }

        try
        {
            return m_openFiles;
        }
        finally
        {
            m_globalMutex.up();
        }
    }

    /**
     * This class wraps a ZipFile to making it possible to weakly close it;
     * this means the underlying zip file will be automatically reopened on demand
     * if anyone tries to use it.
     */
    public class WeakZipFile
    {
        private final File m_file;
        private final Mutex m_localMutex = new Mutex();
        private ZipFile m_zipFile;
        private int m_status = OPEN;
        private long m_timestamp;
        private volatile SoftReference<List<ZipEntry>> m_entries;

        /**
         * Constructor is private since instances need to be centrally
         * managed.
         * @param file the target zip file.
         * @throws IOException if the zip file could not be opened.
         */
        private WeakZipFile(File file) throws IOException
        {
            m_file = file;
            m_zipFile = m_secureAction.openZipFile(m_file);
            m_timestamp = System.currentTimeMillis();
        }

        /**
         * Returns the specified entry from the zip file.
         * @param name the name of the entry to return.
         * @return the zip entry associated with the specified name or null
         *         if it does not exist.
         */
        public ZipEntry getEntry(String name)
        {
            ensureZipFileIsOpen();

            try
            {
                ZipEntry ze = null;
                ze = m_zipFile.getEntry(name);
                if ((ze != null) && (ze.getSize() == 0) && !ze.isDirectory())
                {
                    //The attempts to fix an apparent bug in the JVM in versions
                    // 1.4.2 and lower where directory entries in ZIP/JAR files are
                    // not correctly identified.
                    ZipEntry dirEntry = m_zipFile.getEntry(name + '/');
                    if (dirEntry != null)
                    {
                        ze = dirEntry;
                    }
                }
                return ze;
            }
            finally
            {
                m_localMutex.up();
            }
        }

        /**
         * Returns an enumeration of zip entries from the zip file.
         * @return an enumeration of zip entries.
         */
        public Enumeration<ZipEntry> entries()
        {
            ensureZipFileIsOpen();

            try
            {
                List<ZipEntry> entries = null;
                if (m_entries != null)
                {
                    entries = m_entries.get();
                }
                if (entries == null)
                {
                    synchronized (this)
                    {
                        if (m_entries != null)
                        {
                            entries = m_entries.get();
                        }
                        if (entries == null)
                        {
                            // We need to suck in all of the entries since the zip
                            // file may get weakly closed during iteration. Technically,
                            // this may not be 100% correct either since if the zip file
                            // gets weakly closed and reopened, then the zip entries
                            // will be from a different zip file. It is not clear if this
                            // will cause any issues.
                            Enumeration<? extends ZipEntry> e = m_zipFile.entries();
                            entries = new ArrayList<ZipEntry>();
                            while (e.hasMoreElements())
                            {
                                entries.add(e.nextElement());
                            }
                            m_entries = new SoftReference<List<ZipEntry>>(entries);
                        }
                    }
                }
                return Collections.enumeration(entries);
            }
            finally
            {
                m_localMutex.up();
            }
        }

        /**
         * Returns an input stream for the specified zip entry.
         * @param ze the zip entry whose input stream is to be retrieved.
         * @return an input stream to the zip entry.
         * @throws IOException if the input stream cannot be opened.
         */
        public InputStream getInputStream(ZipEntry ze) throws IOException
        {
            ensureZipFileIsOpen();

            try
            {
                InputStream is = m_zipFile.getInputStream(ze);
                return new WeakZipInputStream(ze.getName(), is);
            }
            finally
            {
                m_localMutex.up();
            }
        }

        /**
         * Weakly closes the zip file, which means that it will be reopened
         * if anyone tries to use it again.
         */
        void closeWeakly()
        {
            try
            {
                m_globalMutex.down();
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                    "Interrupted while acquiring global zip file mutex.");
            }

            try
            {
                _closeWeakly();
            }
            finally
            {
                m_globalMutex.up();
            }
        }

        /**
         * This method is used internally to weakly close a zip file. It should
         * only be called when already holding the global lock, otherwise use
         * closeWeakly().
         */
        private void _closeWeakly()
        {
            try
            {
                m_localMutex.down();
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                    "Interrupted while acquiring local zip file mutex.");
            }

            try
            {
                if (m_status == OPEN)
                {
                    try
                    {
                        m_status = WEAKLY_CLOSED;
                        if (m_zipFile != null)
                        {
                            m_zipFile.close();
                            m_zipFile = null;
                        }
                        m_openFiles.remove(this);
                    }
                    catch (IOException ex)
                    {
                        __close();
                    }
                }
            }
            finally
            {
                m_localMutex.up();
            }
        }

        /**
         * This method permanently closes the zip file.
         * @throws IOException if any error occurs while trying to close the
         *         zip file.
         */
        public void close() throws IOException
        {
            if (m_limit > 0)
            {
                try
                {
                    m_globalMutex.down();
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                        "Interrupted while acquiring global zip file mutex.");
                }
                try
                {
                    m_localMutex.down();
                }
                catch (InterruptedException ex)
                {
                    m_globalMutex.up();
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                        "Interrupted while acquiring local zip file mutex.");
                }
            }

            try
            {
                ZipFile tmp = m_zipFile;
                __close();
                if (tmp != null)
                {
                    tmp.close();
                }
            }
            finally
            {
                m_localMutex.up();
                m_globalMutex.up();
            }
        }

        /**
         * This internal method is used to clear the zip file from the data
         * structures and reset its state. It should only be called when
         * holding the global and local mutexes.
         */
        private void __close()
        {
            m_status = CLOSED;
            m_zipFile = null;
            m_zipFiles.remove(this);
            m_openFiles.remove(this);
        }

        /**
         * This method ensures that the zip file associated with this
         * weak zip file instance is actually open and acquires the
         * local weak zip file mutex. If the underlying zip file is closed,
         * then the local mutex is released and an IllegalStateException is
         * thrown. If the zip file is weakly closed, then it is reopened.
         * If the zip file is already opened, then no additional action is
         * necessary. If this method does not throw an exception, then
         * the end result is the zip file member field is non-null and the
         * local mutex has been acquired.
         */
        private void ensureZipFileIsOpen()
        {
            if (m_limit == 0)
            {
                return;
            }

            // Get mutex for zip file.
            try
            {
                m_localMutex.down();
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                    "Interrupted while acquiring local zip file mutex.");
            }

            // If zip file is closed, then just return null.
            if (m_status == CLOSED)
            {
                m_localMutex.up();
                throw new IllegalStateException("Zip file is closed: " + m_file);
            }

            // If zip file is weakly closed, we need to reopen it,
            // but we have to release the zip mutex to acquire the
            // global mutex, then reacquire the zip mutex. This
            // ensures that the global mutex is always acquired
            // before any local mutex to avoid deadlocks.
            IOException cause = null;
            if (m_status == WEAKLY_CLOSED)
            {
                m_localMutex.up();

                try
                {
                    m_globalMutex.down();
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                        "Interrupted while acquiring global zip file mutex.");
                }
                try
                {
                    m_localMutex.down();
                }
                catch (InterruptedException ex)
                {
                    m_globalMutex.up();
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                        "Interrupted while acquiring local zip file mutex.");
                }

                // Double check status since it may have changed.
                if (m_status == CLOSED)
                {
                    m_localMutex.up();
                    m_globalMutex.up();
                    throw new IllegalStateException("Zip file is closed: " + m_file);
                }
                else if (m_status == WEAKLY_CLOSED)
                {
                    try
                    {
                        __reopenZipFile();
                    }
                    catch (IOException ex)
                    {
                        cause = ex;
                    }
                }

                // Release the global mutex, since it should no longer be necessary.
                m_globalMutex.up();
            }

            // It is possible that reopening the zip file failed, so we check
            // for that case and throw an exception.
            if (m_zipFile == null)
            {
                m_localMutex.up();
                IllegalStateException ise =
                    new IllegalStateException("Zip file is closed: " + m_file);
                if (cause != null)
                {
                    ise.initCause(cause);
                }
                throw ise;
            }
        }

        /**
         * Thie internal method is used to reopen a weakly closed zip file.
         * It makes a best effort, but may fail and leave the zip file member
         * field null. Any failure reopening a zip file results in it being
         * permanently closed. This method should only be invoked when holding
         * the global and local mutexes.
         */
        private void __reopenZipFile() throws IOException
        {
            if (m_status == WEAKLY_CLOSED)
            {
                try
                {
                    m_zipFile = m_secureAction.openZipFile(m_file);
                    m_status = OPEN;
                    m_timestamp = System.currentTimeMillis();
                }
                catch (IOException ex)
                {
                    __close();
                    throw ex;
                }

                if (m_zipFile != null)
                {
                    m_openFiles.add(this);
                    if (m_openFiles.size() > m_limit)
                    {
                        WeakZipFile candidate = m_openFiles.get(0);
                        for (WeakZipFile tmp : m_openFiles)
                        {
                            if (candidate.m_timestamp > tmp.m_timestamp)
                            {
                                candidate = tmp;
                            }
                        }
                        candidate._closeWeakly();
                    }
                }
            }
        }

        /**
         * This is an InputStream wrapper that will properly reopen the underlying
         * zip file if it is weakly closed and create the underlying input stream.
         */
        class WeakZipInputStream extends InputStream
        {
            private final String m_entryName;
            private InputStream m_is;
            private int m_currentPos = 0;
            private ZipFile m_zipFileSnapshot;

            WeakZipInputStream(String entryName, InputStream is)
            {
                m_entryName = entryName;
                m_is = is;
                m_zipFileSnapshot = m_zipFile;
            }

            /**
             * This internal method ensures that the zip file is open and that
             * the underlying input stream is valid. Upon successful completion,
             * the underlying input stream will be valid and the local mutex
             * will be held.
             * @throws IOException if the was an error handling the input stream.
             */
            private void ensureInputStreamIsValid() throws IOException
            {
                if (m_limit == 0)
                {
                    return;
                }

                ensureZipFileIsOpen();

                // If the underlying zip file changed, then we need
                // to get the input stream again.
                if (m_zipFileSnapshot != m_zipFile)
                {
                    m_zipFileSnapshot = m_zipFile;

                    if (m_is != null)
                    {
                        try
                        {
                            m_is.close();
                        }
                        catch (Exception ex)
                        {
                            // Not much we can do.
                        }
                    }
                    try
                    {
                        m_is = m_zipFile.getInputStream(m_zipFile.getEntry(m_entryName));
                        m_is.skip(m_currentPos);
                    }
                    catch (IOException ex)
                    {
                        m_localMutex.up();
                        throw ex;
                    }
                }
            }

            @Override
            public int available() throws IOException
            {
                ensureInputStreamIsValid();
                try
                {
                    return m_is.available();
                }
                finally
                {
                    m_localMutex.up();
                }
            }

            @Override
            public void close() throws IOException
            {
                ensureInputStreamIsValid();
                try
                {
                    InputStream is = m_is;
                    m_is = null;
                    if (is != null)
                    {
                        is.close();
                    }
                }
                finally
                {
                    m_localMutex.up();
                }
            }

            @Override
            public void mark(int i)
            {
                // Not supported.
            }

            @Override
            public boolean markSupported()
            {
                // Not supported.
                return false;
            }

            public int read() throws IOException
            {
                ensureInputStreamIsValid();
                try
                {
                    int len = m_is.read();
                    if (len > 0)
                    {
                        m_currentPos++;
                    }
                    return len;
                }
                finally
                {
                    m_localMutex.up();
                }
            }

            @Override
            public int read(byte[] bytes) throws IOException
            {
                ensureInputStreamIsValid();
                try
                {
                    int len = m_is.read(bytes);
                    if (len > 0)
                    {
                        m_currentPos += len;
                    }
                    return len;
                }
                finally
                {
                    m_localMutex.up();
                }
            }

            @Override
            public int read(byte[] bytes, int i, int i1) throws IOException
            {
                ensureInputStreamIsValid();
                try
                {
                    int len = m_is.read(bytes, i, i1);
                    if (len > 0)
                    {
                        m_currentPos += len;
                    }
                    return len;
                }
                finally
                {
                    m_localMutex.up();
                }
            }

            @Override
            public void reset() throws IOException
            {
                throw new IOException("Unsupported operation");
            }

            @Override
            public long skip(long l) throws IOException
            {
                ensureInputStreamIsValid();
                try
                {
                    long len = m_is.skip(l);
                    if (len > 0)
                    {
                        m_currentPos += len;
                    }
                    return len;
                }
                finally
                {
                    m_localMutex.up();
                }
            }
        }
    }
}
