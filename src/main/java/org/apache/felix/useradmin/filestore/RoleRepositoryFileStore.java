/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin.filestore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.useradmin.RoleRepositoryStore;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;


/**
 * Provides an implementation of {@link RoleRepositoryStore} using Java Serialization.
 */
public class RoleRepositoryFileStore extends RoleRepositoryMemoryStore implements Runnable, UserAdminListener, ManagedService {

    /** The PID for this service to allow its configuration to be updated. */
    public static final String PID = "org.apache.felix.useradmin.filestore";

    static final String KEY_WRITE_DISABLED = "background.write.disabled";
    static final String KEY_WRITE_DELAY_VALUE = "background.write.delay.value";
    static final String KEY_WRITE_DELAY_TIMEUNIT = "background.write.delay.timeunit";

    private static final String PREFIX = PID.concat(".");
    private static final boolean DEFAULT_WRITE_DISABLED = Boolean.parseBoolean(System.getProperty(PREFIX.concat(KEY_WRITE_DISABLED), "false"));
    private static final int DEFAULT_WRITE_DELAY_VALUE = Integer.parseInt(System.getProperty(PREFIX.concat(KEY_WRITE_DELAY_VALUE), "500"));
    private static final TimeUnit DEFAULT_WRITE_DELAY_TIMEUNIT = TimeUnit.MILLISECONDS;

    private static final String FILE_NAME = "ua_repo.dat";

    private final File m_file;
    private final AtomicReference m_timerRef;

    /**
     * Creates a new {@link RoleRepositoryStore} instance.
     * 
     * @param baseDir the base directory where we can store our serialized data, cannot be <code>null</code>.
     */
    public RoleRepositoryFileStore(File baseDir) {
        this(baseDir, !DEFAULT_WRITE_DISABLED);
    }
    
    /**
     * Creates a new {@link RoleRepositoryStore} instance.
     * 
     * @param baseDir the base directory where we can store our serialized data, cannot be <code>null</code>;
     * @param backgroundWriteEnabled <code>true</code> if background writing should be enabled, <code>false</code> to disable it. 
     */
    public RoleRepositoryFileStore(File baseDir, boolean backgroundWriteEnabled) {
        m_file = new File(baseDir, FILE_NAME);
        
        m_timerRef = new AtomicReference();

        if (backgroundWriteEnabled) {
            m_timerRef.set(new ResettableTimer(this, DEFAULT_WRITE_DELAY_VALUE, DEFAULT_WRITE_DELAY_TIMEUNIT));
        }
    }
    
    public void roleChanged(UserAdminEvent event) {
        scheduleTask();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Will be called by m_timer!</p>
     */
    public void run() {
        try {
            // Persist everything to disk...
            flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts this store by reading the latest version from disk.
     * 
     * @throws IOException in case of I/O problems retrieving the store.
     */
    public void start() throws IOException {
        m_entries.putAll(retrieve());
    }

    /**
     * Stops this store service.
     */
    public void stop() throws IOException {
        ResettableTimer timer = (ResettableTimer) m_timerRef.get();
        if (timer != null) {
            if (!timer.isShutDown()) {
                // Shutdown and await termination...
                timer.shutDown();
            }
            // Clear reference...
            m_timerRef.compareAndSet(timer, null);
        }

        // Write the latest version to disk...
        flush();
    }

    /**
     * {@inheritDoc}
     */
    public void updated(Dictionary properties) throws ConfigurationException {
        boolean writeDisabled = DEFAULT_WRITE_DISABLED;
        int writeDelayValue = DEFAULT_WRITE_DELAY_VALUE;
        TimeUnit writeDelayUnit = DEFAULT_WRITE_DELAY_TIMEUNIT;

        if (properties != null) {
            Object wd = properties.get(KEY_WRITE_DISABLED);
            if (wd == null) {
                throw new ConfigurationException(KEY_WRITE_DISABLED, "Missing write disabled value!");
            }
            try {
                writeDisabled = Boolean.parseBoolean((String) wd);
            } catch (Exception e) {
                throw new ConfigurationException(KEY_WRITE_DISABLED, "Invalid write disabled value!");
            }

            if (!writeDisabled) {
                Object wdv = properties.get(KEY_WRITE_DELAY_VALUE);
                if (wdv == null) {
                    throw new ConfigurationException(KEY_WRITE_DELAY_VALUE, "Missing write delay value!");
                }
                try {
                    writeDelayValue = Integer.parseInt((String) wdv);
                } catch (Exception e) {
                    throw new ConfigurationException(KEY_WRITE_DELAY_VALUE, "Invalid write delay value!");
                }
                if (writeDelayValue <= 0) {
                    throw new ConfigurationException(KEY_WRITE_DELAY_VALUE, "Invalid write delay value!");
                }

                Object wdu = properties.get(KEY_WRITE_DELAY_TIMEUNIT);
                if (wdu != null) {
                    try {
                        writeDelayUnit = TimeUnit.valueOf(((String) wdu).toUpperCase());
                    } catch (Exception e) {
                        throw new ConfigurationException(KEY_WRITE_DELAY_TIMEUNIT, "Invalid write delay unit!");
                    }
                }
            }
        }

        ResettableTimer timer = (ResettableTimer) m_timerRef.get();
        if (timer != null) {
            timer.shutDown();
        }
        m_timerRef.compareAndSet(timer, writeDisabled ? null : new ResettableTimer(this, writeDelayValue, writeDelayUnit));
    }

    /**
     * Retrieves the serialized repository from disk.
     * 
     * @return the retrieved repository, never <code>null</code>.
     * @throws IOException in case the retrieval of the repository failed.
     */
    protected Map retrieve() throws IOException {
        InputStream is = null;

        try {
            is = new BufferedInputStream(new FileInputStream(m_file));

            return new RoleRepositorySerializer().deserialize(is);
        } catch (FileNotFoundException exception) {
            // Don't bother; file does not exist...
            return Collections.emptyMap();
        } catch (IOException exception) {
            exception.printStackTrace();
            throw exception;
        } finally {
            closeSafely(is);
        }
    }

    /**
     * Stores the given repository to disk as serialized objects.
     * 
     * @param roleRepository the repository to store, cannot be <code>null</code>.
     * @throws IOException in case storing the repository failed.
     */
    protected void store(Map roleRepository) throws IOException {
        OutputStream os = null;

        try {
            os = new BufferedOutputStream(new FileOutputStream(m_file));

            new RoleRepositorySerializer().serialize(roleRepository, os);
        } finally {
            closeSafely(os);
        }
    }

    /**
     * Closes a given resource, ignoring any exceptions that may come out of this.
     * 
     * @param resource the resource to close, can be <code>null</code>.
     */
    private void closeSafely(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * Flushes the current repository to disk.
     * 
     * @throws IOException in case of problems storing the repository.
     */
    private void flush() throws IOException {
        store(new HashMap(m_entries));
    }

    /**
     * Notifies the background timer to schedule a task for storing the 
     * contents of this store to disk.
     */
    private void scheduleTask() {
        ResettableTimer timer = (ResettableTimer) m_timerRef.get();
        if (timer != null && !timer.isShutDown()) {
            timer.schedule();
        }
    }
}
