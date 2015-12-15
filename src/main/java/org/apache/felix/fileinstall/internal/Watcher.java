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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * A File watching service
 */
public abstract class Watcher implements Closeable {

    private Path root;
    private boolean watch = true;
    private WatchService watcher;
    private PathMatcher dirMatcher;
    private PathMatcher fileMatcher;
    private final Map<WatchKey, Path> keys = new ConcurrentHashMap<WatchKey, Path>();
    private volatile long lastModified;
    private final Map<Path, Boolean> processedMap = new ConcurrentHashMap<Path, Boolean>();

    public void init() throws IOException {
        if (root == null) {
            Iterable<Path> rootDirectories = getFileSystem().getRootDirectories();
            for (Path rootDirectory : rootDirectories) {
                if (rootDirectory != null) {
                    root = rootDirectory;
                    break;
                }
            }
        }
        if (!Files.exists(root)) {
            fail("Root path does not exist: " + root);
        } else if (!Files.isDirectory(root)) {
            fail("Root path is not a directory: " + root);
        }
        if (watcher == null) {
            watcher = watch ? getFileSystem().newWatchService() : null;
        }
    }

    public void close() throws IOException {
        if (watcher != null) {
            watcher.close();
        }
    }

    public long getLastModified() {
        return lastModified;
    }

    // Properties
    //-------------------------------------------------------------------------


    public void setRootPath(String rootPath) {
        Path path = new File(rootPath).getAbsoluteFile().toPath();
        setRoot(path);
    }

    public void setRootDirectory(File directory) {
        setRoot(directory.toPath());
    }

    public Path getRoot() {
        return root;
    }

    public void setRoot(Path root) {
        this.root = root;
    }

    public boolean isWatch() {
        return watch;
    }

    public void setWatch(boolean watch) {
        this.watch = watch;
    }

    public WatchService getWatcher() {
        return watcher;
    }

    public void setWatcher(WatchService watcher) {
        this.watcher = watcher;
    }

    public PathMatcher getDirMatcher() {
        return dirMatcher;
    }

    public void setDirMatcher(PathMatcher dirMatcher) {
        this.dirMatcher = dirMatcher;
    }

    public PathMatcher getFileMatcher() {
        return fileMatcher;
    }

    public void setFileMatcher(PathMatcher fileMatcher) {
        this.fileMatcher = fileMatcher;
    }


    // Implementation methods
    //-------------------------------------------------------------------------

    public void rescan() throws IOException {
        for (WatchKey key : keys.keySet()) {
            key.cancel();
        }
        keys.clear();
        Files.walkFileTree(root,
                           EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                           Integer.MAX_VALUE,
                           new FilteringFileVisitor());
    }

    public void processEvents() {
        while (true) {
            WatchKey key = watcher.poll();
            if (key == null) {
                break;
            }
            Path dir = keys.get(key);
            if (dir == null) {
                warn("Could not find key for " + key);
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();
                WatchEvent<Path> ev = (WatchEvent<Path>)event;

                // Context for directory entry event is the file name of entry
                Path name = ev.context();
                Path child = dir.resolve(name);

                debug("Processing event {} on path {}", kind, child);

                if (kind == OVERFLOW) {
//                    rescan();
                    continue;
                }

                try {
                    if (kind == ENTRY_CREATE) {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {

                            // if directory is created, and watching recursively, then
                            // register it and its sub-directories
                            Files.walkFileTree(child, new FilteringFileVisitor());
                        } else if (Files.isRegularFile(child, NOFOLLOW_LINKS)) {
                            scan(child);
                        }
                    } else if (kind == ENTRY_MODIFY) {
                        if (Files.isRegularFile(child, NOFOLLOW_LINKS)) {
                            scan(child);
                        }
                    } else if (kind == ENTRY_DELETE) {
                        unscan(child);
                    }
                } catch (IOException x) {
                    // ignore to keep sample readbale
                    x.printStackTrace();
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                debug("Removing key " + key + " and dir " + dir + " from keys");
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    private void scan(final Path file) throws IOException {
        if (isMatchesFile(file)) {
            process(file);
            processedMap.put(file, Boolean.TRUE);
        }
    }

    protected boolean isMatchesFile(Path file) {
        boolean matches = true;
        if (fileMatcher != null) {
            Path rel = root.relativize(file);
            matches = fileMatcher.matches(rel);
        }
        return matches;
    }

    private void unscan(final Path file) throws IOException {
        if (isMatchesFile(file)) {
            onRemove(file);
            lastModified = System.currentTimeMillis();
        } else {
            // lets find all the files that now no longer exist
            List<Path> files = new ArrayList<Path>(processedMap.keySet());
            for (Path path : files) {
                if (!Files.exists(path)) {
                    debug("File has been deleted: " + path);
                    processedMap.remove(path);
                    if (isMatchesFile(path)) {
                        onRemove(file);
                        lastModified = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    private void watch(final Path path) throws IOException {
        if (watcher != null) {
            WatchKey key = path.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
            keys.put(key, path);
            debug("Watched path " + path + " key " + key);
        } else {
            warn("No watcher yet for path " + path);
        }
    }

    protected FileSystem getFileSystem() {
        return FileSystems.getDefault();
    }

    public class FilteringFileVisitor implements FileVisitor<Path> {

        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (Thread.interrupted()) {
                throw new InterruptedIOException();
            }
            if (dirMatcher != null) {
                Path rel = root.relativize(dir);
                if (!"".equals(rel.toString()) && !dirMatcher.matches(rel)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }
            watch(dir);
            return FileVisitResult.CONTINUE;
        }

        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (Thread.interrupted()) {
                throw new InterruptedIOException();
            }
            scan(file);
            return FileVisitResult.CONTINUE;
        }

        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }
    }


    /**
     * Throws an invalid argument exception after logging a warning
     * just in case the stack trace gets gobbled up by application containers
     * like spring or blueprint, at least the error message will be clearly shown in the log
     *
     */
    public void fail(String message) {
        warn(message);
        throw new IllegalArgumentException(message);
    }

    protected abstract void debug(String message, Object... args);
    protected abstract void warn(String message, Object... args);
    protected abstract void process(Path path);
    protected abstract void onRemove(Path path);
}
