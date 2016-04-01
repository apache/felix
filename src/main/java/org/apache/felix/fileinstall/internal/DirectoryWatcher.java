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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.fileinstall.ArtifactListener;
import org.apache.felix.fileinstall.ArtifactTransformer;
import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.apache.felix.fileinstall.internal.Util.Logger;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;

/**
 * -DirectoryWatcher-
 *
 * This class runs a background task that checks a directory for new files or
 * removed files. These files can be configuration files or jars.
 * For jar files, its behavior is defined below:
 * - If there are new jar files, it installs them and optionally starts them.
 *    - If it fails to install a jar, it does not try to install it again until
 *      the jar has been modified.
 *    - If it fail to start a bundle, it attempts to start it in following
 *      iterations until it succeeds or the corresponding jar is uninstalled.
 * - If some jar files have been deleted, it uninstalls them.
 * - If some jar files have been updated, it updates them.
 *    - If it fails to update a bundle, it tries to update it in following
 *      iterations until it is successful.
 * - If any bundle gets updated or uninstalled, it refreshes the framework
 *   for the changes to take effect.
 * - If it detects any new installations, uninstallations or updations,
 *   it tries to start all the managed bundle unless it has been configured
 *   to only install bundles.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DirectoryWatcher extends Thread implements BundleListener
{
    public final static String FILENAME = "felix.fileinstall.filename";
    public final static String POLL = "felix.fileinstall.poll";
    public final static String DIR = "felix.fileinstall.dir";
    public final static String LOG_LEVEL = "felix.fileinstall.log.level";
    public final static String LOG_DEFAULT = "felix.fileinstall.log.default";
    public final static String TMPDIR = "felix.fileinstall.tmpdir";
    public final static String FILTER = "felix.fileinstall.filter";
    public final static String START_NEW_BUNDLES = "felix.fileinstall.bundles.new.start";
    public final static String USE_START_TRANSIENT = "felix.fileinstall.bundles.startTransient";
    public final static String USE_START_ACTIVATION_POLICY = "felix.fileinstall.bundles.startActivationPolicy";
    public final static String NO_INITIAL_DELAY = "felix.fileinstall.noInitialDelay";
    public final static String DISABLE_CONFIG_SAVE = "felix.fileinstall.disableConfigSave";
    public final static String ENABLE_CONFIG_SAVE = "felix.fileinstall.enableConfigSave";
    public final static String START_LEVEL = "felix.fileinstall.start.level";
    public final static String ACTIVE_LEVEL = "felix.fileinstall.active.level";
    public final static String UPDATE_WITH_LISTENERS = "felix.fileinstall.bundles.updateWithListeners";
    public final static String OPTIONAL_SCOPE = "felix.fileinstall.optionalImportRefreshScope";
    public final static String FRAGMENT_SCOPE = "felix.fileinstall.fragmentRefreshScope";
    public final static String DISABLE_NIO2 = "felix.fileinstall.disableNio2";
    public final static String SUBDIR_MODE = "felix.fileinstall.subdir.mode";

    public final static String SCOPE_NONE = "none";
    public final static String SCOPE_MANAGED = "managed";
    public final static String SCOPE_ALL = "all";

    public final static String LOG_STDOUT = "stdout";
    public final static String LOG_JUL = "jul";

    final FileInstall fileInstall;

    Map<String, String> properties;
    File watchedDirectory;
    File tmpDir;
    long poll;
    int logLevel;
    boolean startBundles;
    boolean useStartTransient;
    boolean useStartActivationPolicy;
    String filter;
    BundleContext context;
    private Bundle systemBundle;
    String originatingFileName;
    boolean noInitialDelay;
    int startLevel;
    int activeLevel;
    boolean updateWithListeners;
    String fragmentScope;
    String optionalScope;
    boolean disableNio2;

    // Map of all installed artifacts
    final Map<File, Artifact> currentManagedArtifacts = new HashMap<File, Artifact>();

    // The scanner to report files changes
    Scanner scanner;

    // Represents files that could not be processed because of a missing artifact listener
    final Set<File> processingFailures = new HashSet<File>();
    
    // Represents installed artifacts which need to be started later because they failed to start
    Set<Bundle> delayedStart = new HashSet<Bundle>();

    // Represents consistently failing bundles
    Set<Bundle> consistentlyFailingBundles = new HashSet<Bundle>();

    // Represents artifacts that could not be installed
    final Map<File, Artifact> installationFailures = new HashMap<File, Artifact>();

    // flag (acces to which must be synchronized) that indicates wheter there's a change in state of system,
    // which may result in an attempt to start the watched bundles
    private AtomicBoolean stateChanged = new AtomicBoolean();

    public DirectoryWatcher(FileInstall fileInstall, Map<String, String> properties, BundleContext context)
    {
        super("fileinstall-" + getThreadName(properties));
        this.fileInstall = fileInstall;
        this.properties = properties;
        this.context = context;
        systemBundle = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        poll = getLong(properties, POLL, 2000);
        logLevel = getInt(properties, LOG_LEVEL, Util.getGlobalLogLevel(context));
        originatingFileName = properties.get(FILENAME);
        watchedDirectory = getFile(properties, DIR, new File("./load"));
        verifyWatchedDir();
        tmpDir = getFile(properties, TMPDIR, null);
        prepareTempDir();
        startBundles = getBoolean(properties, START_NEW_BUNDLES, true);  // by default, we start bundles.
        useStartTransient = getBoolean(properties, USE_START_TRANSIENT, false);  // by default, we start bundles persistently.
        useStartActivationPolicy = getBoolean(properties, USE_START_ACTIVATION_POLICY, true);  // by default, we start bundles using activation policy.
        filter = properties.get(FILTER);
        noInitialDelay = getBoolean(properties, NO_INITIAL_DELAY, false);
        startLevel = getInt(properties, START_LEVEL, 0);    // by default, do not touch start level
        activeLevel = getInt(properties, ACTIVE_LEVEL, 0);    // by default, always scan
        updateWithListeners = getBoolean(properties, UPDATE_WITH_LISTENERS, false); // Do not update bundles when listeners are updated
        fragmentScope = properties.get(FRAGMENT_SCOPE);
        optionalScope = properties.get(OPTIONAL_SCOPE);
        disableNio2 = getBoolean(properties, DISABLE_NIO2, false);
        this.context.addBundleListener(this);

        if (disableNio2) {
            scanner = new Scanner(watchedDirectory, filter, properties.get(SUBDIR_MODE));
        } else {
            try {
                scanner = new WatcherScanner(context, watchedDirectory, filter, properties.get(SUBDIR_MODE));
            } catch (Throwable t) {
                scanner = new Scanner(watchedDirectory, filter, properties.get(SUBDIR_MODE));
            }
        }
    }

    private void verifyWatchedDir()
    {
        if (!watchedDirectory.exists())
        {
            // Issue #2069: Do not create the directory if it does not exist,
            // instead, warn user and continue. We will automatically start
            // monitoring the dir when it becomes available.
            log(Logger.LOG_WARNING,
                watchedDirectory + " does not exist, please create it.",
                null);
        }
        else if (!watchedDirectory.isDirectory())
        {
            log(Logger.LOG_ERROR,
                "Cannot use "
                + watchedDirectory
                + " because it's not a directory", null);
            throw new RuntimeException(
                "File Install can't monitor " + watchedDirectory + " because it is not a directory");
        }
    }

    public static String getThreadName(Map<String, String> properties)
    {
        return (properties.get(DIR) != null ? properties.get(DIR) : "./load");
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }

    public void start()
    {
        if (noInitialDelay)
        {
            log(Logger.LOG_DEBUG, "Starting initial scan", null);
            initializeCurrentManagedBundles();
            Set<File> files = scanner.scan(true);
            if (files != null)
            {
                try
                {
                    process(files);
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
        super.start();
    }

    /**
     * Main run loop, will traverse the directory, and then handle the delta
     * between installed and newly found/lost bundles and configurations.
     *
     */
    public void run()
    {
        // We must wait for FileInstall to complete initialisation
        // to avoid race conditions observed in FELIX-2791
        try
        {
            fileInstall.lock.readLock().lockInterruptibly();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            log(Logger.LOG_INFO, "Watcher for " + watchedDirectory + " exiting because of interruption.", e);
            return;
        }
        try {
            log(Logger.LOG_DEBUG,
                    "{" + POLL + " (ms) = " + poll + ", "
                            + DIR + " = " + watchedDirectory.getAbsolutePath() + ", "
                            + LOG_LEVEL + " = " + logLevel + ", "
                            + START_NEW_BUNDLES + " = " + startBundles + ", "
                            + TMPDIR + " = " + tmpDir + ", "
                            + FILTER + " = " + filter + ", "
                            + START_LEVEL + " = " + startLevel + "}", null
            );

            if (!noInitialDelay) {
                try {
                    // enforce a delay before the first directory scan
                    Thread.sleep(poll);
                } catch (InterruptedException e) {
                    log(Logger.LOG_DEBUG, "Watcher for " + watchedDirectory + " was interrupted while waiting "
                            + poll + " milliseconds for initial directory scan.", e);
                    return;
                }
                initializeCurrentManagedBundles();
            }
        }
        finally
        {
            fileInstall.lock.readLock().unlock();
        }

        while (!interrupted()) {
            try {
                FrameworkStartLevel startLevelSvc = systemBundle.adapt(FrameworkStartLevel.class);
                // Don't access the disk when the framework is still in a startup phase.
                if (startLevelSvc.getStartLevel() >= activeLevel
                        && systemBundle.getState() == Bundle.ACTIVE) {
                    Set<File> files = scanner.scan(false);
                    // Check that there is a result.  If not, this means that the directory can not be listed,
                    // so it's presumably not a valid directory (it may have been deleted by someone).
                    // In such case, just sleep
                    if (files != null) {
                        process(files);
                    }
                }
                synchronized (this) {
                    wait(poll);
                }
            } catch (InterruptedException e) {
                interrupt();
                return;
            } catch (Throwable e) {
                try {
                    context.getBundle();
                } catch (IllegalStateException t) {
                    // FileInstall bundle has been uninstalled, exiting loop
                    return;
                }
                log(Logger.LOG_ERROR, "In main loop, we have serious trouble", e);
            }
        }
    }

    public void bundleChanged(BundleEvent bundleEvent)
    {
        int type = bundleEvent.getType();
        if (type == BundleEvent.UNINSTALLED)
        {
            for (Iterator<?> it = getArtifacts().iterator(); it.hasNext();)
            {
                Artifact artifact = (Artifact) it.next();
                if (artifact.getBundleId() == bundleEvent.getBundle().getBundleId())
                {
                    log(Logger.LOG_DEBUG, "Bundle " + bundleEvent.getBundle().getBundleId()
                            + " has been uninstalled", null);
                    it.remove();
                    break;
                }
            }
        }
        if (type == BundleEvent.INSTALLED || type == BundleEvent.RESOLVED || type == BundleEvent.UNINSTALLED ||
            type == BundleEvent.UNRESOLVED || type == BundleEvent.UPDATED) {
            setStateChanged(true);
        }
    }

    private void process(Set<File> files) throws InterruptedException
    {
        fileInstall.lock.readLock().lockInterruptibly();
        try
        {
            doProcess(files);
        }
        finally
        {
            fileInstall.lock.readLock().unlock();
        }
    }

    private void doProcess(Set<File> files) throws InterruptedException
    {
        List<ArtifactListener> listeners = fileInstall.getListeners();
        List<Artifact> deleted = new ArrayList<Artifact>();
        List<Artifact> modified = new ArrayList<Artifact>();
        List<Artifact> created = new ArrayList<Artifact>();

        // Try to process again files that could not be processed
        synchronized (processingFailures)
        {
            files.addAll(processingFailures);
            processingFailures.clear();
        }

        for (File file : files) {
            boolean exists = file.exists();
            Artifact artifact = getArtifact(file);
            // File has been deleted
            if (!exists) {
                if (artifact != null) {
                    deleteJaredDirectory(artifact);
                    deleteTransformedFile(artifact);
                    deleted.add(artifact);
                }
            }
            // File exists
            else {
                File jar = file;
                URL jaredUrl = null;
                try {
                    jaredUrl = file.toURI().toURL();
                } catch (MalformedURLException e) {
                    // Ignore, can't happen
                }
                // Jar up the directory if needed
                if (file.isDirectory()) {
                    prepareTempDir();
                    try {
                        jar = new File(tmpDir, file.getName() + ".jar");
                        Util.jarDir(file, jar);
                        jaredUrl = new URL(JarDirUrlHandler.PROTOCOL, null, file.getPath());

                    } catch (IOException e) {
                        // Notify user of problem, won't retry until the dir is updated.
                        log(Logger.LOG_ERROR,
                                "Unable to create jar for: " + file.getAbsolutePath(), e);
                        continue;
                    }
                }
                // File has been modified
                if (artifact != null) {
                    artifact.setChecksum(scanner.getChecksum(file));
                    // If there's no listener, this is because this artifact has been installed before
                    // fileinstall has been restarted.  In this case, try to find a listener.
                    if (artifact.getListener() == null) {
                        ArtifactListener listener = findListener(jar, listeners);
                        // If no listener can handle this artifact, we need to defer the
                        // processing for this artifact until one is found
                        if (listener == null) {
                            synchronized (processingFailures) {
                                processingFailures.add(file);
                            }
                            continue;
                        }
                        artifact.setListener(listener);
                    }
                    // If the listener can not handle this file anymore,
                    // uninstall the artifact and try as if is was new
                    if (!listeners.contains(artifact.getListener()) || !artifact.getListener().canHandle(jar)) {
                        deleted.add(artifact);
                    }
                    // The listener is still ok
                    else {
                        deleteTransformedFile(artifact);
                        artifact.setJaredDirectory(jar);
                        artifact.setJaredUrl(jaredUrl);
                        if (transformArtifact(artifact)) {
                            modified.add(artifact);
                        } else {
                            deleteJaredDirectory(artifact);
                            deleted.add(artifact);
                        }
                    }
                }
                // File has been added
                else {
                    // Find the listener
                    ArtifactListener listener = findListener(jar, listeners);
                    // If no listener can handle this artifact, we need to defer the
                    // processing for this artifact until one is found
                    if (listener == null) {
                        synchronized (processingFailures) {
                            processingFailures.add(file);
                        }
                        continue;
                    }
                    // Create the artifact
                    artifact = new Artifact();
                    artifact.setPath(file);
                    artifact.setJaredDirectory(jar);
                    artifact.setJaredUrl(jaredUrl);
                    artifact.setListener(listener);
                    artifact.setChecksum(scanner.getChecksum(file));
                    if (transformArtifact(artifact)) {
                        created.add(artifact);
                    } else {
                        deleteJaredDirectory(artifact);
                    }
                }
            }
        }
        // Handle deleted artifacts
        // We do the operations in the following order:
        // uninstall, update, install, refresh & start.
        Collection<Bundle> uninstalledBundles = uninstall(deleted);
        Collection<Bundle> updatedBundles = update(modified);
        Collection<Bundle> installedBundles = install(created);

        if (!uninstalledBundles.isEmpty() || !updatedBundles.isEmpty() || !installedBundles.isEmpty())
        {
            Set<Bundle> toRefresh = new HashSet<Bundle>();
            toRefresh.addAll(uninstalledBundles);
            toRefresh.addAll(updatedBundles);
            toRefresh.addAll(installedBundles);
            findBundlesWithFragmentsToRefresh(toRefresh);
            findBundlesWithOptionalPackagesToRefresh(toRefresh);
            if (toRefresh.size() > 0)
            {
                // Refresh if any bundle got uninstalled or updated.
                refresh(toRefresh);
                // set the state to reattempt starting managed bundles which aren't already STARTING or ACTIVE
                setStateChanged(true);
            }
        }

        if (startBundles && isStateChanged())
        {
            // Try to start all the bundles that are not persistently stopped
            startAllBundles();
            
            delayedStart.addAll(installedBundles);
            delayedStart.removeAll(uninstalledBundles);
            // Try to start newly installed bundles, or bundles which we missed on a previous round
            startBundles(delayedStart);
            consistentlyFailingBundles.clear();
            consistentlyFailingBundles.addAll(delayedStart);

            // set the state as unchanged to not reattempt starting failed bundles
            setStateChanged(false);
        }
    }

    ArtifactListener findListener(File artifact, List<ArtifactListener> listeners)
    {
        for (ArtifactListener listener : listeners) {
            if (listener.canHandle(artifact)) {
                return listener;
            }
        }
        return null;
    }

    boolean transformArtifact(Artifact artifact)
    {
        if (artifact.getListener() instanceof ArtifactTransformer)
        {
            prepareTempDir();
            try
            {
                File transformed = ((ArtifactTransformer) artifact.getListener()).transform(artifact.getJaredDirectory(), tmpDir);
                if (transformed != null)
                {
                    artifact.setTransformed(transformed);
                    return true;
                }
            }
            catch (Exception e)
            {
                log(Logger.LOG_WARNING,
                    "Unable to transform artifact: " + artifact.getPath().getAbsolutePath(), e);
            }
            return false;
        }
        else if (artifact.getListener() instanceof ArtifactUrlTransformer)
        {
            try
            {
                URL url = artifact.getJaredUrl();
                URL transformed = ((ArtifactUrlTransformer) artifact.getListener()).transform(url);
                if (transformed != null)
                {
                    artifact.setTransformedUrl(transformed);
                    return true;
                }
            }
            catch (Exception e)
            {
                log(Logger.LOG_WARNING,
                    "Unable to transform artifact: " + artifact.getPath().getAbsolutePath(), e);
            }
            return false;
        }
        return true;
    }

    private void deleteTransformedFile(Artifact artifact)
    {
        if (artifact.getTransformed() != null
                && !artifact.getTransformed().equals(artifact.getPath())
                && !artifact.getTransformed().delete())
        {
            log(Logger.LOG_WARNING,
                "Unable to delete transformed artifact: " + artifact.getTransformed().getAbsolutePath(), null);
        }
    }

    private void deleteJaredDirectory(Artifact artifact)
    {
        if (artifact.getJaredDirectory() != null
                && !artifact.getJaredDirectory().equals(artifact.getPath())
                && !artifact.getJaredDirectory().delete())
        {
            log(Logger.LOG_WARNING,
                "Unable to delete jared artifact: " + artifact.getJaredDirectory().getAbsolutePath(), null);
        }
    }


    private void prepareTempDir()
    {
        if (tmpDir == null)
        {
            File javaIoTmpdir = new File(System.getProperty("java.io.tmpdir"));
            if (!javaIoTmpdir.exists() && !javaIoTmpdir.mkdirs()) {
                throw new IllegalStateException("Unable to create temporary directory " + javaIoTmpdir);
            }
            Random random = new Random();
            while (tmpDir == null)
            {
                File f = new File(javaIoTmpdir, "fileinstall-" + Long.toString(random.nextLong()));
                if (!f.exists() && f.mkdirs())
                {
                    tmpDir = f;
                    tmpDir.deleteOnExit();
                }
            }
        }
        else
        {
            prepareDir(tmpDir);
        }
    }

    /**
     * Create the watched directory, if not existing.
     * Throws a runtime exception if the directory cannot be created,
     * or if the provided File parameter does not refer to a directory.
     *
     * @param dir
     *            The directory File Install will monitor
     */
    private void prepareDir(File dir)
    {
        if (!dir.exists() && !dir.mkdirs())
        {
            log(Logger.LOG_ERROR,
                "Cannot create folder "
                + dir
                + ". Is the folder write-protected?", null);
            throw new RuntimeException("Cannot create folder: " + dir);
        }

        if (!dir.isDirectory())
        {
            log(Logger.LOG_ERROR,
                "Cannot use "
                + dir
                + " because it's not a directory", null);
            throw new RuntimeException(
                "Cannot start FileInstall using something that is not a directory");
        }
    }

    /**
     * Log a message and optional throwable. If there is a log service we use
     * it, otherwise we log to the console
     *
     * @param message
     *            The message to log
     * @param e
     *            The throwable to log
     */
    void log(int msgLevel, String message, Throwable e)
    {
        Util.log(context, logLevel, msgLevel, message, e);
    }

    /**
     * Check if a bundle is a fragment.
     */
    boolean isFragment(Bundle bundle)
    {
        BundleRevision rev = bundle.adapt(BundleRevision.class);
        return (rev.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
    }

    /**
     * Convenience to refresh the packages
     */
    void refresh(Collection<Bundle> bundles) throws InterruptedException
    {
        FileInstall.refresh(systemBundle, bundles);
    }

    /**
     * Retrieve a property as a long.
     *
     * @param properties the properties to retrieve the value from
     * @param property the name of the property to retrieve
     * @param dflt the default value
     * @return the property as a long or the default value
     */
    int getInt(Map<String, String> properties, String property, int dflt)
    {
        String value = properties.get(property);
        if (value != null)
        {
            try
            {
                return Integer.parseInt(value);
            }
            catch (Exception e)
            {
                log(Logger.LOG_WARNING, property + " set, but not a int: " + value, null);
            }
        }
        return dflt;
    }

    /**
     * Retrieve a property as a long.
     *
     * @param properties the properties to retrieve the value from
     * @param property the name of the property to retrieve
     * @param dflt the default value
     * @return the property as a long or the default value
     */
    long getLong(Map<String, String> properties, String property, long dflt)
    {
        String value = properties.get(property);
        if (value != null)
        {
            try
            {
                return Long.parseLong(value);
            }
            catch (Exception e)
            {
                log(Logger.LOG_WARNING, property + " set, but not a long: " + value, null);
            }
        }
        return dflt;
    }

    /**
     * Retrieve a property as a File.
     *
     * @param properties the properties to retrieve the value from
     * @param property the name of the property to retrieve
     * @param dflt the default value
     * @return the property as a File or the default value
     */
    File getFile(Map<String, String> properties, String property, File dflt)
    {
        String value = properties.get(property);
        if (value != null)
        {
            return new File(value);
        }
        return dflt;
    }

    /**
     * Retrieve a property as a boolan.
     *
     * @param properties the properties to retrieve the value from
     * @param property the name of the property to retrieve
     * @param dflt the default value
     * @return the property as a boolean or the default value
     */
    boolean getBoolean(Map<String, String> properties, String property, boolean dflt)
    {
        String value = properties.get(property);
        if (value != null)
        {
            return Boolean.valueOf(value);
        }
        return dflt;
    }

    public void close()
    {
        this.context.removeBundleListener(this);
        interrupt();
        for (Artifact artifact : getArtifacts()) {
            deleteTransformedFile(artifact);
            deleteJaredDirectory(artifact);
        }
        try
        {
            scanner.close();
        }
        catch (IOException e)
        {
            // Ignore
        }
        try
        {
            join(10000);
        }
        catch (InterruptedException ie)
        {
            // Ignore
        }
    }

    /**
     * This method goes through all the currently installed bundles
     * and returns information about those bundles whose location
     * refers to a file in our {@link #watchedDirectory}.
     */
    private void initializeCurrentManagedBundles()
    {
        Bundle[] bundles = this.context.getBundles();
        String watchedDirPath = watchedDirectory.toURI().normalize().getPath();
        Map<File, Long> checksums = new HashMap<File, Long>();
        for (Bundle bundle : bundles) {
            // Convert to a URI because the location of a bundle
            // is typically a URI. At least, that's the case for
            // autostart bundles and bundles installed by fileinstall.
            // Normalisation is needed to ensure that we don't treat (e.g.)
            // /tmp/foo and /tmp//foo differently.
            String location = bundle.getLocation();
            String path = null;
            if (location != null &&
                    !location.equals(Constants.SYSTEM_BUNDLE_LOCATION)) {
                URI uri;
                try {
                    uri = new URI(bundle.getLocation()).normalize();
                } catch (URISyntaxException e) {
                    // Let's try to interpret the location as a file path
                    uri = new File(location).toURI().normalize();
                }
                if (uri.isOpaque() && uri.getSchemeSpecificPart() != null) {
                    // blueprint:file:/tmp/foo/baa.jar -> file:/tmp/foo/baa.jar
                    // blueprint:mvn:foo.baa/baa/0.0.1 -> mvn:foo.baa/baa/0.0.1
                    // wrap:file:/tmp/foo/baa-1.0.jar$Symbolic-Name=baa&Version=1.0 -> file:/tmp/foo/baa-1.0.jar$Symbolic-Name=baa&Version1.0
                    final String schemeSpecificPart = uri.getSchemeSpecificPart();
                    // extract content behind the 'file:' protocol of scheme specific path
                    final int lastIndexOfFileProtocol = schemeSpecificPart.lastIndexOf("file:");
                    final int offsetFileProtocol = lastIndexOfFileProtocol >= 0 ? lastIndexOfFileProtocol + "file:".length() : 0;
                    final int firstIndexOfDollar = schemeSpecificPart.indexOf("$");
                    final int endOfPath = firstIndexOfDollar >= 0 ? firstIndexOfDollar : schemeSpecificPart.length();
                    // file:/tmp/foo/baa.jar -> /tmp/foo/baa.jar
                    // mvn:foo.baa/baa/0.0.1 -> mvn:foo.baa/baa/0.0.1
                    // file:/tmp/foo/baa-1.0.jar$Symbolic-Name=baa&Version=1.0 -> /tmp/foo/baa-1.0.jar
                    path = schemeSpecificPart.substring(offsetFileProtocol, endOfPath);
                } else {
                    // file:/tmp/foo/baa.jar -> /tmp/foo/baa.jar
                    // mnv:foo.baa/baa/0.0.1 -> foo.baa/baa/0.0.1
                    path = uri.getPath();
                }
            }
            if (path == null) {
                // jar.getPath is null means we could not parse the location
                // as a meaningful URI or file path.
                // We can't do any meaningful processing for this bundle.
                continue;
            }
            final int index = path.lastIndexOf('/');
            if (index != -1 && path.startsWith(watchedDirPath)) {
                Artifact artifact = new Artifact();
                artifact.setBundleId(bundle.getBundleId());
                artifact.setChecksum(Util.loadChecksum(bundle, context));
                artifact.setListener(null);
                artifact.setPath(new File(path));
                setArtifact(new File(path), artifact);
                checksums.put(new File(path), artifact.getChecksum());
            }
        }
        scanner.initialize(checksums);
    }

    /**
     * This method installs a collection of artifacts.
     * @param artifacts Collection of {@link Artifact}s to be installed
     * @return List of Bundles just installed
     */
    private Collection<Bundle> install(Collection<Artifact> artifacts)
    {
        List<Bundle> bundles = new ArrayList<Bundle>();
        for (Artifact artifact : artifacts) {
            Bundle bundle = install(artifact);
            if (bundle != null) {
                bundles.add(bundle);
            }
        }
        return bundles;
    }

    /**
     * This method uninstalls a collection of artifacts.
     * @param artifacts Collection of {@link Artifact}s to be uninstalled
     * @return Collection of Bundles that got uninstalled
     */
    private Collection<Bundle> uninstall(Collection<Artifact> artifacts)
    {
        List<Bundle> bundles = new ArrayList<Bundle>();
        for (Artifact artifact : artifacts) {
            Bundle bundle = uninstall(artifact);
            if (bundle != null) {
                bundles.add(bundle);
            }
        }
        return bundles;
    }

    /**
     * This method updates a collection of artifacts.
     *
     * @param artifacts    Collection of {@link Artifact}s to be updated.
     * @return Collection of bundles that got updated
     */
    private Collection<Bundle> update(Collection<Artifact> artifacts)
    {
        List<Bundle> bundles = new ArrayList<Bundle>();
        for (Artifact artifact : artifacts) {
            Bundle bundle = update(artifact);
            if (bundle != null) {
                bundles.add(bundle);
            }
        }
        return bundles;
    }

    /**
     * Install an artifact and return the bundle object.
     * It uses {@link Artifact#getPath()} as location
     * of the new bundle. Before installing a file,
     * it sees if the file has been identified as a bad file in
     * earlier run. If yes, then it compares to see if the file has changed
     * since then. It installs the file if the file has changed.
     * If the file has not been identified as a bad file in earlier run,
     * then it always installs it.
     *
     * @param artifact the artifact to be installed
     * @return Bundle object that was installed
     */
    private Bundle install(Artifact artifact)
    {
        File path = artifact.getPath();
        Bundle bundle = null;
        AtomicBoolean modified = new AtomicBoolean();
        try
        {
            // If the listener is an installer, ask for an update
            if (artifact.getListener() instanceof ArtifactInstaller)
            {
                ((ArtifactInstaller) artifact.getListener()).install(path);
            }
            // if the listener is an url transformer
            else if (artifact.getListener() instanceof ArtifactUrlTransformer)
            {
                Artifact badArtifact = installationFailures.get(path);
                if (badArtifact != null && badArtifact.getChecksum() == artifact.getChecksum())
                {
                    return null; // Don't attempt to install it; nothing has changed.
                }
                URL transformed = artifact.getTransformedUrl();
                String location = transformed.toString();
                BufferedInputStream in = new BufferedInputStream(transformed.openStream());
                try
                {
                    bundle = installOrUpdateBundle(location, in, artifact.getChecksum(), modified);
                }
                finally
                {
                    in.close();
                }
                artifact.setBundleId(bundle.getBundleId());
            }
            // if the listener is an artifact transformer
            else if (artifact.getListener() instanceof ArtifactTransformer)
            {
                Artifact badArtifact = installationFailures.get(path);
                if (badArtifact != null && badArtifact.getChecksum() == artifact.getChecksum())
                {
                    return null; // Don't attempt to install it; nothing has changed.
                }
                File transformed = artifact.getTransformed();
                String location = path.toURI().normalize().toString();
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(transformed != null ? transformed : path));
                try
                {
                    bundle = installOrUpdateBundle(location, in, artifact.getChecksum(), modified);
                }
                finally
                {
                    in.close();
                }
                artifact.setBundleId(bundle.getBundleId());
            }
            installationFailures.remove(path);
            setArtifact(path, artifact);
        }
        catch (Exception e)
        {
            log(Logger.LOG_ERROR, "Failed to install artifact: " + path, e);

            // Add it our bad jars list, so that we don't
            // attempt to install it again and again until the underlying
            // jar has been modified.
            installationFailures.put(path, artifact);
        }
        return modified.get() ? bundle : null;
    }

    private Bundle installOrUpdateBundle(
        String bundleLocation, BufferedInputStream is, long checksum, AtomicBoolean modified)
        throws IOException, BundleException
    {
        is.mark(256 * 1024);
        JarInputStream jar = new JarInputStream(is);
        Manifest m = jar.getManifest();
        if( m == null ) {
            throw new BundleException(
                "The bundle " + bundleLocation + " does not have a META-INF/MANIFEST.MF! "+
                    "Make sure, META-INF and MANIFEST.MF are the first 2 entries in your JAR!");
        }
        String sn = m.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
        String vStr = m.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
        Version v = vStr == null ? Version.emptyVersion : Version.parseVersion(vStr);
        Bundle[] bundles = context.getBundles();
        for (Bundle b : bundles) {
            if (b.getSymbolicName() != null && b.getSymbolicName().equals(sn)) {
                vStr = b.getHeaders().get(Constants.BUNDLE_VERSION);
                Version bv = vStr == null ? Version.emptyVersion : Version.parseVersion(vStr);
                if (v.equals(bv)) {
                    is.reset();
                    if (Util.loadChecksum(b, context) != checksum) {
                        log(Logger.LOG_WARNING,
                                "A bundle with the same symbolic name ("
                                        + sn + ") and version (" + vStr
                                        + ") is already installed.  Updating this bundle instead.", null
                        );
                        stopTransient(b);
                        Util.storeChecksum(b, checksum, context);
                        b.update(is);
                        modified.set(true);
                    }
                    return b;
                }
            }
        }
        is.reset();
        Util.log(context, Logger.LOG_INFO, "Installing bundle " + sn
                + " / " + v, null);
        Bundle b = context.installBundle(bundleLocation, is);
        Util.storeChecksum(b, checksum, context);
        modified.set(true);

        // Set default start level at install time, the user can override it if he wants
        if (startLevel != 0)
        {
            b.adapt(BundleStartLevel.class).setStartLevel(startLevel);
        }
        
        return b;
    }

    /**
     * Uninstall a jar file.
     */
    private Bundle uninstall(Artifact artifact)
    {
        Bundle bundle = null;
        try
        {
            File path = artifact.getPath();
            // Find a listener for this artifact if needed
            if (artifact.getListener() == null)
            {
                artifact.setListener(findListener(path, fileInstall.getListeners()));
            }
            // Forget this artifact
            removeArtifact(path);
            // Delete transformed file
            deleteTransformedFile(artifact);
            // if the listener is an installer, uninstall the artifact
            if (artifact.getListener() instanceof ArtifactInstaller)
            {
                ((ArtifactInstaller) artifact.getListener()).uninstall(path);
            }
            // else we need uninstall the bundle
            else if (artifact.getBundleId() != 0)
            {
                // old can't be null because of the way we calculate deleted list.
                bundle = context.getBundle(artifact.getBundleId());
                if (bundle == null)
                {
                    log(Logger.LOG_WARNING,
                        "Failed to uninstall bundle: "
                        + path + " with id: "
                        + artifact.getBundleId()
                        + ". The bundle has already been uninstalled", null);
                    return null;
                }
                log(Logger.LOG_INFO,
                    "Uninstalling bundle "
                    + bundle.getBundleId() + " ("
                    + bundle.getSymbolicName() + ")", null);
                bundle.uninstall();
            }
        }
        catch (Exception e)
        {
            log(Logger.LOG_WARNING, "Failed to uninstall artifact: " + artifact.getPath(), e);
        }
        return bundle;
    }

    private Bundle update(Artifact artifact)
    {
        Bundle bundle = null;
        try
        {
            File path = artifact.getPath();
            // If the listener is an installer, ask for an update
            if (artifact.getListener() instanceof ArtifactInstaller)
            {
                ((ArtifactInstaller) artifact.getListener()).update(path);
            }
            // if the listener is an url transformer
            else if (artifact.getListener() instanceof ArtifactUrlTransformer)
            {
                URL transformed = artifact.getTransformedUrl();
                bundle = context.getBundle(artifact.getBundleId());
                if (bundle == null)
                {
                    log(Logger.LOG_WARNING,
                        "Failed to update bundle: "
                        + path + " with ID "
                        + artifact.getBundleId()
                        + ". The bundle has been uninstalled", null);
                    return null;
                }
                Util.log(context, Logger.LOG_INFO, "Updating bundle " + bundle.getSymbolicName()
                        + " / " + bundle.getVersion(), null);
                stopTransient(bundle);
                Util.storeChecksum(bundle, artifact.getChecksum(), context);
                InputStream in = (transformed != null)
                    ? transformed.openStream()
                    : new FileInputStream(path);
                try
                {
                    bundle.update(in);
                }
                finally
                {
                    in.close();
                }
            }
            // else we need to ask for an update on the bundle
            else if (artifact.getListener() instanceof ArtifactTransformer)
            {
                File transformed = artifact.getTransformed();
                bundle = context.getBundle(artifact.getBundleId());
                if (bundle == null)
                {
                    log(Logger.LOG_WARNING,
                        "Failed to update bundle: "
                        + path + " with ID "
                        + artifact.getBundleId()
                        + ". The bundle has been uninstalled", null);
                    return null;
                }
                Util.log(context, Logger.LOG_INFO, "Updating bundle " + bundle.getSymbolicName()
                        + " / " + bundle.getVersion(), null);
                stopTransient(bundle);
                Util.storeChecksum(bundle, artifact.getChecksum(), context);
                InputStream in = new FileInputStream(transformed != null ? transformed : path);
                try
                {
                    bundle.update(in);
                }
                finally
                {
                    in.close();
                }
            }
        }
        catch (Throwable t)
        {
            log(Logger.LOG_WARNING, "Failed to update artifact " + artifact.getPath(), t);
        }
        return bundle;
    }

    private void stopTransient(Bundle bundle) throws BundleException
    {
        // Stop the bundle transiently so that it will be restarted when startAllBundles() is called
        // but this avoids the need to restart the bundle twice (once for the update and another one
        // when refreshing packages).
        if (startBundles)
        {
            if (!isFragment(bundle)) 
            {
                bundle.stop(Bundle.STOP_TRANSIENT);
            }
        }
    }

    /**
     * Tries to start all the bundles which somehow got stopped transiently.
     * The File Install component will only retry the start When {@link #USE_START_TRANSIENT}
     * is set to true or when a bundle is persistently started. Persistently stopped bundles
     * are ignored.
     */
    private void startAllBundles()
    {
        FrameworkStartLevel startLevelSvc = systemBundle.adapt(FrameworkStartLevel.class);
        List<Bundle> bundles = new ArrayList<Bundle>();
        for (Artifact artifact : getArtifacts()) {
            if (artifact.getBundleId() > 0) {
                Bundle bundle = context.getBundle(artifact.getBundleId());
                if (bundle != null) {
                    if (bundle.getState() != Bundle.STARTING && bundle.getState() != Bundle.ACTIVE
                            && (useStartTransient || bundle.adapt(BundleStartLevel.class).isPersistentlyStarted())
                            && startLevelSvc.getStartLevel() >= bundle.adapt(BundleStartLevel.class).getStartLevel()) {
                        bundles.add(bundle);
                    }
                }
            }
        }
        startBundles(bundles);
    }

     /**
      * Starts a bundle and removes it from the Collection when successfully started.
      */
    private void startBundles(Collection<Bundle> bundles)
    {
        // Check if this is the consistent set of bundles which failed previously.
        boolean logFailures = bundles.equals(consistentlyFailingBundles);
        for (Iterator<Bundle> b = bundles.iterator(); b.hasNext(); )
        {
            if (startBundle(b.next(), logFailures))
            {
                b.remove();
            }
        }
    }

     /**
      * Start a bundle, if the framework's startlevel allows it.
      * @param bundle the bundle to start.
      * @return whether the bundle was started.
      */
    private boolean startBundle(Bundle bundle, boolean logFailures)
    {
        FrameworkStartLevel startLevelSvc = systemBundle.adapt(FrameworkStartLevel.class);
        // Fragments can never be started.
        // Bundles can only be started transient when the start level of the framework is high
        // enough. Persistent (i.e. non-transient) starts will simply make the framework start the
        // bundle when the start level is high enough.
        if (startBundles
                && bundle.getState() != Bundle.UNINSTALLED
                && !isFragment(bundle)
                && startLevelSvc.getStartLevel() >= bundle.adapt(BundleStartLevel.class).getStartLevel())
        {
            try
            {
                int options = useStartTransient ? Bundle.START_TRANSIENT : 0;
                options |= useStartActivationPolicy ? Bundle.START_ACTIVATION_POLICY : 0;
                bundle.start(options);
                log(Logger.LOG_INFO, "Started bundle: " + bundle.getLocation(), null);
                return true;
            }
            catch (BundleException e)
            {
                // Don't log this as an error, instead we start the bundle repeatedly.
                if (logFailures)
                {
                    log(Logger.LOG_WARNING, "Error while starting bundle: " + bundle.getLocation(), e);
                }
            }
        }
        return false;
    }

    protected Set<Bundle> getScopedBundles(String scope) {
        // No bundles to check
        if (SCOPE_NONE.equals(scope)) {
            return new HashSet<Bundle>();
        }
        // Go through managed bundles
        else if (SCOPE_MANAGED.equals(scope)) {
            Set<Bundle> bundles = new HashSet<Bundle>();
            for (Artifact artifact : getArtifacts()) {
                if (artifact.getBundleId() > 0) {
                    Bundle bundle = context.getBundle(artifact.getBundleId());
                    if (bundle != null) {
                        bundles.add(bundle);
                    }
                }
            }
            return bundles;
        // Go through all bundles
        } else {
            return new HashSet<Bundle>(Arrays.asList(context.getBundles()));
        }
    }

    protected void findBundlesWithFragmentsToRefresh(Set<Bundle> toRefresh) {
        Set<Bundle> fragments = new HashSet<Bundle>();
        Set<Bundle> bundles = getScopedBundles(fragmentScope);
        for (Bundle b : toRefresh) {
            if (b.getState() != Bundle.UNINSTALLED) {
                String hostHeader = b.getHeaders().get(Constants.FRAGMENT_HOST);
                if (hostHeader != null) {
                    Clause[] clauses = Parser.parseHeader(hostHeader);
                    if (clauses != null && clauses.length > 0) {
                        Clause path = clauses[0];
                        for (Bundle hostBundle : bundles) {
                            if (hostBundle.getSymbolicName() != null &&
                                    hostBundle.getSymbolicName().equals(path.getName())) {
                                String ver = path.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
                                if (ver != null) {
                                    VersionRange v = VersionRange.parseVersionRange(ver);
                                    if (v.contains(VersionTable.getVersion(hostBundle.getHeaders().get(Constants.BUNDLE_VERSION)))) {
                                        fragments.add(hostBundle);
                                    }
                                } else {
                                    fragments.add(hostBundle);
                                }
                            }
                        }
                    }
                }
            }
        }
        toRefresh.addAll(fragments);
    }

    protected void findBundlesWithOptionalPackagesToRefresh(Set<Bundle> toRefresh) {
        Set<Bundle> bundles = getScopedBundles(optionalScope);
        // First pass: include all bundles contained in these features
        bundles.removeAll(toRefresh);
        if (bundles.isEmpty()) {
            return;
        }
        // Second pass: for each bundle, check if there is any unresolved optional package that could be resolved
        Map<Bundle, List<Clause>> imports = new HashMap<Bundle, List<Clause>>();
        for (Iterator<Bundle> it = bundles.iterator(); it.hasNext(); ) {
            Bundle b = it.next();
            String importsStr = b.getHeaders().get(Constants.IMPORT_PACKAGE);
            List<Clause> importsList = getOptionalImports(importsStr);
            if (importsList.isEmpty()) {
                it.remove();
            } else {
                imports.put(b, importsList);
            }
        }
        if (bundles.isEmpty()) {
            return;
        }
        // Third pass: compute a list of packages that are exported by our bundles and see if
        //             some exported packages can be wired to the optional imports
        List<Clause> exports = new ArrayList<Clause>();
        for (Bundle b : toRefresh) {
            if (b.getState() != Bundle.UNINSTALLED) {
                String exportsStr = b.getHeaders().get(Constants.EXPORT_PACKAGE);
                if (exportsStr != null) {
                    Clause[] exportsList = Parser.parseHeader(exportsStr);
                    exports.addAll(Arrays.asList(exportsList));
                }
            }
        }
        for (Iterator<Bundle> it = bundles.iterator(); it.hasNext(); ) {
            Bundle b = it.next();
            List<Clause> importsList = imports.get(b);
            for (Iterator<Clause> itpi = importsList.iterator(); itpi.hasNext(); ) {
                Clause pi = itpi.next();
                boolean matching = false;
                for (Clause pe : exports) {
                    if (pi.getName().equals(pe.getName())) {
                        String evStr = pe.getAttribute(Constants.VERSION_ATTRIBUTE);
                        String ivStr = pi.getAttribute(Constants.VERSION_ATTRIBUTE);
                        Version exported = evStr != null ? Version.parseVersion(evStr) : Version.emptyVersion;
                        VersionRange imported = ivStr != null ? VersionRange.parseVersionRange(ivStr) : VersionRange.ANY_VERSION;
                        if (imported.contains(exported)) {
                            matching = true;
                            break;
                        }
                    }
                }
                if (!matching) {
                    itpi.remove();
                }
            }
            if (importsList.isEmpty()) {
                it.remove();
//            } else {
//                LOGGER.debug("Refreshing bundle {} ({}) to solve the following optional imports", b.getSymbolicName(), b.getBundleId());
//                for (Clause p : importsList) {
//                    LOGGER.debug("    {}", p);
//                }
//
            }
        }
        toRefresh.addAll(bundles);
    }

    protected List<Clause> getOptionalImports(String importsStr)
    {
        Clause[] imports = Parser.parseHeader(importsStr);
        List<Clause> result = new LinkedList<Clause>();
        for (Clause anImport : imports)
        {
            String resolution = anImport.getDirective(Constants.RESOLUTION_DIRECTIVE);
            if (Constants.RESOLUTION_OPTIONAL.equals(resolution))
            {
                result.add(anImport);
            }
        }
        return result;
    }

    public void addListener(ArtifactListener listener, long stamp)
    {
        if (updateWithListeners)
        {
            for (Artifact artifact : getArtifacts())
            {
                if (artifact.getListener() == null && artifact.getBundleId() > 0)
                {
                    Bundle bundle = context.getBundle(artifact.getBundleId());
                    if (bundle != null && bundle.getLastModified() < stamp)
                    {
                        File path = artifact.getPath();
                        if (listener.canHandle(path))
                        {
                            synchronized (processingFailures)
                            {
                                processingFailures.add(path);
                            }
                        }
                    }
                }
            }
        }
        synchronized (this)
        {
            this.notifyAll();
        }
    }

    public void removeListener(ArtifactListener listener)
    {
        for (Artifact artifact : getArtifacts())
        {
            if (artifact.getListener() == listener)
            {
                artifact.setListener(null);
            }
        }
        synchronized (this)
        {
            this.notifyAll();
        }
    }

    private Artifact getArtifact(File file)
    {
        synchronized (currentManagedArtifacts)
        {
            return currentManagedArtifacts.get(file);
        }
    }

    private List<Artifact> getArtifacts()
    {
        synchronized (currentManagedArtifacts)
        {
            return new ArrayList<Artifact>(currentManagedArtifacts.values());
        }
    }

    private void setArtifact(File file, Artifact artifact)
    {
        synchronized (currentManagedArtifacts)
        {
            currentManagedArtifacts.put(file, artifact);
        }
    }

    private void removeArtifact(File file)
    {
        synchronized (currentManagedArtifacts)
        {
            currentManagedArtifacts.remove(file);
        }
    }
    
    private void setStateChanged(boolean changed) {
        this.stateChanged.set(changed);
    }

    private boolean isStateChanged() {
        return stateChanged.get();
    }

}
