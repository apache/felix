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
package org.apache.felix.fileinstall.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.BundleContext;

public class WatcherScanner extends Scanner {

    BundleContext bundleContext;
    PathMatcher fileMatcher;
    Watcher watcher;

    Set<File> changed = new HashSet<File>();

    /**
     * Create a scanner for the specified directory and file filter
     *
     * @param directory the directory to scan
     * @param filterString a filter for file names
     */
    public WatcherScanner(BundleContext bundleContext, File directory, String filterString) throws IOException {
        super(directory, filterString);
        this.bundleContext = bundleContext;
        if (filterString != null) {
            this.fileMatcher = FileSystems.getDefault().getPathMatcher("regex:" + filterString);
        } else {
            this.fileMatcher = null;
        }
        this.watcher = new ScannerWatcher();
        this.watcher.setFileMatcher(fileMatcher);
        this.watcher.setRootDirectory(this.directory);
        this.watcher.init();
        this.watcher.rescan();
    }

    public Set<File> scan(boolean reportImmediately) {
        watcher.processEvents();
        if (changed.isEmpty()) {
            return new HashSet<File>();
        }
        Set<File> files = new HashSet<File>();
        Set<File> removed = new HashSet<File>();
        if (reportImmediately) {
            removed.addAll(storedChecksums.keySet());
        }
        for (File file : changed)
        {
            long lastChecksum = lastChecksums.get(file) != null ? (Long) lastChecksums.get(file) : 0;
            long storedChecksum = storedChecksums.get(file) != null ? (Long) storedChecksums.get(file) : 0;
            long newChecksum = checksum(file);
            lastChecksums.put(file, newChecksum);
            if (file.exists()) {
                // Only handle file when it does not change anymore and it has changed since last reported
                if ((newChecksum == lastChecksum || reportImmediately)) {
                    if (newChecksum != storedChecksum) {
                        storedChecksums.put(file, newChecksum);
                        files.add(file);
                    } else {
                        changed.remove(file);
                    }
                    if (reportImmediately) {
                        removed.remove(file);
                    }
                }
            } else {
                if (!reportImmediately) {
                    removed.add(file);
                }
            }
        }
        for (File file : removed)
        {
            // Make sure we'll handle a file that has been deleted
            files.addAll(removed);
            // Remove no longer used checksums
            lastChecksums.remove(file);
            storedChecksums.remove(file);
        }
        for (File file : files)
        {
            changed.remove(file);
        }

        return files;
    }

    public void close() throws IOException {
        watcher.close();
    }

    class ScannerWatcher extends Watcher {

        @Override
        protected void process(Path path) {
            File file = path.toFile();
            while (!file.getParentFile().equals(directory)) {
                file = file.getParentFile();
                if (file == null) {
                    return;
                }
            }
            changed.add(file);
        }

        @Override
        protected void onRemove(Path path) {
            File file = path.toFile();
            while (!file.getParentFile().equals(directory)) {
                file = file.getParentFile();
                if (file == null) {
                    return;
                }
            }
            changed.add(file);
        }

        @Override
        protected void debug(String message, Object... args) {
            log(Util.Logger.LOG_DEBUG, message, args);
        }

        @Override
        protected void warn(String message, Object... args) {
            log(Util.Logger.LOG_WARNING, message, args);
        }

        protected void log(int level, String message, Object... args) {
            String msg = String.format(message, args);
            Util.log(bundleContext, Util.getGlobalLogLevel(bundleContext),
                    level, msg, null);
        }

    }

}
