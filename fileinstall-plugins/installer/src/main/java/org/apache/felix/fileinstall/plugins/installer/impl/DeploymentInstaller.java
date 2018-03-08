/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.apache.felix.fileinstall.plugins.installer.impl;

import static org.apache.felix.fileinstall.plugins.installer.impl.Locking.withLock;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.fileinstall.plugins.installer.Artifact;
import org.apache.felix.fileinstall.plugins.installer.FrameworkInstaller;
import org.apache.felix.fileinstall.plugins.installer.Hash;
import org.apache.felix.fileinstall.plugins.installer.InstallableListener;
import org.apache.felix.fileinstall.plugins.installer.InstallableManager;
import org.apache.felix.fileinstall.plugins.installer.InstallableUnit;
import org.apache.felix.fileinstall.plugins.installer.InstallableUnitEvent;
import org.apache.felix.fileinstall.plugins.installer.State;
import org.apache.felix.fileinstall.plugins.resolver.PluginResolver;
import org.apache.felix.fileinstall.plugins.resolver.ResolveRequest;
import org.apache.felix.fileinstall.plugins.resolver.ResolveResult;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

import aQute.lib.hex.Hex;

@Component(immediate = true)
public class DeploymentInstaller implements ArtifactInstaller, InstallableManager {

    /**
     * Set environment variable "org.apache.felix.fileinstall.plugins.installer.debug" to enable develeroper-level debug output to the console.
     */
    public static final boolean DEBUG = Boolean.getBoolean("org.apache.felix.fileinstall.plugins.installer.debug");

    // If Deployment version is not specified, the version to return
    private static final String UNKNOWN_DEPLOYMENT_VERSION = "0.0.0";

    /**
     * OSGi Repository indexes are documented to use SHA-256 for their content hashes.
     * @see ContentNamespace#CONTENT_NAMESPACE
     */
    private static final String SHA256 = "SHA-256";

    // Magic number (not officially but good enough for our purposes) of a ZIP file is PK\0x03\0x04
    private static final byte[] ZIP_FILE_MAGIC = new byte[] { 0x50, 0x4b, 0x03, 0x04 };

    // Queues for pending resolve and install operations.
    private final Queue<Job<List<Bundle>>> pendingInstalls = new LinkedList<>();
    private final Map<File, Job<InstallableUnitImpl>> pendingResolves = new LinkedHashMap<>();

    // Currently available Installable Units, and a lock for modifying that map safely.
    private final Map<File, InstallableUnitImpl> units = new HashMap<>();
    private final ReadWriteLock unitsLock = new ReentrantReadWriteLock();

    // Listeners for install/uninstall events
    private final List<InstallableListener> installListeners = new CopyOnWriteArrayList<>();

    // The framework listener invalidates any RESOLVED install units whenever the OSGi Framework packages are refreshed.
    private final FrameworkListener frameworkListener = new FrameworkListener() {
        @Override
        public void frameworkEvent(FrameworkEvent event) {
            if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
                invalidateAllUnits("Packaged Refreshed");
            }
        }
    };
    // The bundle listener invalidated any RESOLVED install units whenever OSGi Bundles are installed, resolved, unresolved, updated or uninstalled.
    private final BundleListener bundleListener = new BundleListener() {
        @Override
        public void bundleChanged(BundleEvent event) {
            int mask = BundleEvent.INSTALLED | BundleEvent.RESOLVED | BundleEvent.UNRESOLVED | BundleEvent.UPDATED | BundleEvent.UNINSTALLED;
            if ((mask & event.getType()) > 0) {
                invalidateAllUnits(String.format("Bundle %s entered state %s", event.getBundle().getSymbolicName(), bundleEventToString(event.getType())));
            }
        }
    };

    // Component instance state.
    private BundleContext context;
    private Thread processorThread;

    // The PluginResolver service that calculates resolution results for us, but doesn't produce any effects.
    @Reference(target = "(!(" + org.osgi.framework.Constants.SERVICE_BUNDLEID + "=0))")
    PluginResolver resolver;

    // The FrameworkInstaller service is responsible for ensuring required bundles are present in the OSGi Framework -- installing them when necessary.
    @Reference
    FrameworkInstaller frameworkInstaller;

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    void addInstallListener(InstallableListener listener) {
        this.installListeners.add(listener);
    }
    void removeInstallListener(InstallableListener listener) {
        this.installListeners.remove(listener);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    LogService log;

    @Activate
    void activate(BundleContext context) {
        this.context = context;
        context.addFrameworkListener(this.frameworkListener);
        context.addBundleListener(this.bundleListener);

        this.processorThread = new Thread(() -> {
            this.log.log(LogService.LOG_INFO, "Deployment Installer thread starting");
            debug("Deployment Install thread starting");
            while (!Thread.interrupted()) {
                try {
                    Runnable job = waitForJob();
                    job.run();
                } catch (InterruptedException e) {
                    debug("Deployment Install thread interrupted");
                    this.log.log(LogService.LOG_INFO, "Deployment Installer thread interrupted");
                    break;
                } catch (Exception e) {
                    this.log.log(LogService.LOG_ERROR, "Error processing job on Deployment Installer thread", e);
                }
            }
            debug("Deployment Install thread terminated.");
            this.log.log(LogService.LOG_INFO, "Deployment Installer thread terminated");
        });
        this.processorThread.start();
    }

    @Deactivate
    void deactivate() {
        this.processorThread.interrupt();
        this.context.removeBundleListener(this.bundleListener);
        this.context.removeFrameworkListener(this.frameworkListener);
    }

    /**
     * Wait for the next job to do from the two pending queues. Note that we
     * always favour the install queue, because performing an install/uninstall
     * will invalidate any completed resolves. Therefore the resolves are only
     * processed when the install queue is empty.
     */
    private Job<?> waitForJob() throws InterruptedException {
        synchronized (this.processorThread) {
            while (true) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                // Try to get an InstallableUnit to install
                Job<List<Bundle>> installJob = this.pendingInstalls.poll();
                if (installJob != null) {
                    return installJob;
                }

                // Try to get a File to resolve. An iterator is needed to remove the first entry from the LinkedHashMap.
                Iterator<Job<InstallableUnitImpl>> resolvesIter = this.pendingResolves.values().iterator();
                if (resolvesIter.hasNext()) {
                    Job<InstallableUnitImpl> resolveJob = resolvesIter.next();
                    resolvesIter.remove();
                    return resolveJob;
                }

                // Wait for a notification
                this.processorThread.wait();
            }
        }
    }

    /**
     * Put an install job onto the queue
     */
    Promise<List<Bundle>> putInstallJob(InstallableUnitImpl unit) {
        Job<List<Bundle>> job = new Job<>(() -> installArtifacts(unit));
        synchronized (this.processorThread) {
            this.pendingInstalls.add(job);
            this.processorThread.notifyAll();
        }
        return job.getPromise();
    }

    /**
     * Put an uninstall job onto the queue
     */
    Promise<List<Bundle>> putUninstallJob(InstallableUnitImpl unit) {
        Job<List<Bundle>> job = new Job<>(() -> uninstallArtifacts(unit));
        synchronized (this.processorThread) {
            this.pendingInstalls.add(job);
            this.processorThread.notifyAll();
        }
        return job.getPromise();
    }

    private List<Bundle> installArtifacts(InstallableUnitImpl unit) {
        Supplier<List<Bundle>> func = () -> {
            State oldState = unit.getState();
            unit.setState(State.INSTALLING);
            notifyListeners(Collections.singleton(new InstallableUnitEvent(oldState, State.INSTALLING, unit)));

            try {
                // Install Bundles
                Collection<Artifact> artifacts = unit.getArtifacts();
                List<String> locations = artifacts.stream().map(Artifact::getLocation).collect(Collectors.toList());
                List<Bundle> installed = this.frameworkInstaller.addLocations(unit, locations);

                // Start bundles
                for (Bundle bundle : installed) {
                    if (!isFragment(bundle)) {
                        bundle.start();
                    }
                }

                // Mark unit installed
                oldState = unit.getState();
                unit.setState(State.INSTALLED);
                notifyListeners(Collections.singleton(new InstallableUnitEvent(oldState, State.INSTALLED, unit)));

                return installed;
            } catch (BundleException | IOException e) {
                oldState = unit.getState();
                unit.setState(State.ERROR);
                unit.setErrorMessage(e.getMessage());
                if (log != null) {
                    log.log(LogService.LOG_ERROR, "Error installing artifact(s)", e);
                }
                notifyListeners(Collections.singleton(new InstallableUnitEvent(oldState, State.ERROR, unit)));
                return Collections.emptyList();
            }
        };
        return withLock(this.unitsLock.writeLock(), func);
    }

    private List<Bundle> uninstallArtifacts(InstallableUnitImpl unit) {
        List<Bundle> bundles = this.frameworkInstaller.removeSponsor(unit);

        // Mark unit uninstalled
        withLock(this.unitsLock.writeLock(), () -> {
            State oldState = unit.getState();
            if (unit.setState(State.REMOVED)) {
                notifyListeners(Collections.singleton(new InstallableUnitEvent(oldState, State.REMOVED, unit)));
            }
        });

        return bundles;
    }

    /**
     * Put a resolve job onto the queue. The resolve queue is a little
     * different: because there is no need to resolve the same file twice, we
     * check whether there's already a pending resolve job for the given file.
     * If so, just return the same promise.
     */
    private Promise<InstallableUnitImpl> putResolveJob(File file) {
        Promise<InstallableUnitImpl> promise;

        Job<InstallableUnitImpl> newJob = new Job<>(() -> {
            InstallableUnitImpl unit = performResolve(file);
            replaceUnit(file, null, unit);
            return unit;
        });
        synchronized (this.processorThread) {
            Job<InstallableUnitImpl> existing = this.pendingResolves.putIfAbsent(file, newJob);
            if (existing != null) {
                // no need to unblock the processor thread because we haven't added anything
                debug("Not adding resolve job for %s as it is already on the queue", file);
                promise = existing.getPromise();
            } else {
                debug("Added resolve job for %s", file);
                promise = newJob.getPromise();
                this.processorThread.notifyAll();
            }
        }
        return promise;
    }

    private InstallableUnitImpl performResolve(File file) {
        debug("Starting resolve for file: %s", file);
        ResolveRequest request;
        try {
            request = analyseFile(file);
        } catch (Exception e) {
            debug("Failed to analyse file %s: %s", file, e.getMessage());
            return newFailedUnit(file, file.getName(), file.getName(), "UNKNOWN_VERSION",  String.format("Error reading archive file %s: %s", file.getAbsolutePath(), e.getMessage()));
        }

        ResolveResult result;
        try {
            result = this.resolver.resolve(request);
        } catch (Exception e) {
            debug("Failed to resolve file %s: %s", file, e.getMessage());
            return newFailedUnit(file, request.getName(), request.getSymbolicName(), request.getVersion(),  "Resolution failed: " + e.getMessage());

        }

        List<Artifact> artifacts = new ArrayList<>(result.getResources().size());
        for (Entry<Resource, String> resourceEntry : result.getResources().entrySet()) {
            Capability idCap = getIdentityCapability(resourceEntry.getKey());
            ArtifactImpl artifact = new ArtifactImpl(getIdentity(idCap), getVersion(idCap), resourceEntry.getValue(), getContentHash(resourceEntry.getKey()));
            artifacts.add(artifact);
        }
        debug("Sucessful resolve for file %s: Deployment-Name=%s, Deployment-SymbolicName=%s, Deployment-Version= %s", file, request.getName(), request.getSymbolicName(), request.getVersion());
        return newResolvedUnit(file, request.getName(), request.getSymbolicName(), request.getVersion(), artifacts);
        
    }

    private InstallableUnitImpl newResolvedUnit(File file, String name, String symbolicName, String version, List<Artifact> artifacts) {
        InstallableUnitImpl newUnit = new InstallableUnitImpl(this, file, name, symbolicName, version,  artifacts);
        newUnit.setState(State.RESOLVED);
        return newUnit;
    }

    private InstallableUnitImpl newFailedUnit(File file, String name, String symbolicName, String version, String message) {
        InstallableUnitImpl newUnit = new InstallableUnitImpl(this, file, name, symbolicName, version, Collections.emptyList());
        newUnit.setState(State.ERROR);
        newUnit.setErrorMessage(message);
        return newUnit;
    }

    @Override
    public boolean canHandle(File file) {
        String fileName = file.getName();
        if (!fileName.toLowerCase().endsWith(Constants.ARTIFACT_EXTENSION.toLowerCase())) {
            log(LogService.LOG_DEBUG, null, "Ignoring %s, does not end with '%s' extension", file.getAbsolutePath(), Constants.ARTIFACT_EXTENSION);
            return false;
        }

        // Check if it's a valid ZIP
        try {
            if (!isZipFile(file)) {
                log(LogService.LOG_WARNING, null, "Not a valid ZIP file, ignoring file: %s", file.getAbsolutePath());
                return false;
            }
        } catch (IOException e) {
            log(LogService.LOG_ERROR, e, "Failed to check ZIP header on %s", file.getAbsolutePath());
            return false;
        }

        // Read the requires header and index
        try (JarFile jar = new JarFile(file, true)) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                log(LogService.LOG_WARNING, null, "Not a valid bundle archive: no META-INF/MANIFEST.MF in %s", file.getAbsolutePath());
                return false;
            }
            String requireBundleStr = manifest.getMainAttributes().getValue(org.osgi.framework.Constants.REQUIRE_BUNDLE);
            String requireCapsStr = manifest.getMainAttributes().getValue(org.osgi.framework.Constants.REQUIRE_CAPABILITY);
            if (requireBundleStr == null && requireCapsStr == null) {
                log(LogService.LOG_WARNING, null, "Not a valid bundle archive: missing %s or %s header in manifest in %s", org.osgi.framework.Constants.REQUIRE_BUNDLE, org.osgi.framework.Constants.REQUIRE_CAPABILITY, file.getAbsolutePath());
                return false;
            }

            JarEntry indexEntry = findEntry(jar, Constants.INDEX_FILE, Constants.DEFAULT_INDEX_FILE);
            if (indexEntry == null) {
                log(LogService.LOG_WARNING, null, "Not a valid bundle archive: no index entry found in %s", file.getAbsolutePath());
                return false;
            }

            // Everything seems present and correct
            log(LogService.LOG_INFO, null, "Detected valid bundle archive in file %s", file.getAbsolutePath());
            return true;
        } catch (IOException e) {
            log(LogService.LOG_ERROR, e, "Failed to check PKZIP header for candidate archive file %s", file.getAbsolutePath());
            return false;
        }
    }

    private ResolveRequest analyseFile(File file) throws IOException {
        log(LogService.LOG_INFO, null, "Resolving bundle archive: %s", file.getAbsolutePath());

        String fileUriStr = file.toURI().toString();
        String indexUriStr;
        String name;
        String symbolicName;
        String version = "";

        List<Requirement> requirements = new LinkedList<>();
        try (JarFile jar = new JarFile(file)) {
            Attributes manifestAttribs = jar.getManifest().getMainAttributes();

            symbolicName = manifestAttribs.getValue(Constants.DEPLOYMENT_SYMBOLIC_NAME);
            if (symbolicName == null) {
                symbolicName = file.getName();
            }

            name = manifestAttribs.getValue(Constants.DEPLOYMENT_NAME);
            if (name == null) {
                name = symbolicName;
            }

            version = manifestAttribs.getValue(Constants.DEPLOYMENT_VERSION);
            if (version == null) {
                version = UNKNOWN_DEPLOYMENT_VERSION;
            }
            requirements.addAll(RequirementParser.parseRequireBundle(manifestAttribs.getValue(org.osgi.framework.Constants.REQUIRE_BUNDLE)));
            requirements.addAll(RequirementParser.parseRequireCapability(manifestAttribs.getValue(org.osgi.framework.Constants.REQUIRE_CAPABILITY)));
            if (requirements.isEmpty()) {
                throw new IllegalArgumentException(String.format("Missing %s or %s header in manifest in %s", org.osgi.framework.Constants.REQUIRE_BUNDLE, org.osgi.framework.Constants.REQUIRE_CAPABILITY, file.getAbsolutePath()));
            }

            JarEntry indexEntry = findEntry(jar, Constants.INDEX_FILE, Constants.DEFAULT_INDEX_FILE);
            if (indexEntry == null) {
                throw new IllegalArgumentException("Missing index entry in " + file.getAbsolutePath());
            }
            indexUriStr = "jar:" + fileUriStr + "!/" + indexEntry.getName();
        }

        try {
            ResolveRequest request = new ResolveRequest(name, symbolicName, version,
                    Collections.singletonList(new URI(indexUriStr)), requirements);

            return request;
        } catch (URISyntaxException e) {
            throw new IOException("Unable to convert index URI " + indexUriStr, e);
        }
    }

    @Override
    public void install(File file) {
        log(LogService.LOG_INFO, null, "Installing bundle archive: %s", file.getAbsolutePath());
        putResolveJob(file);
    }
    
    private static String getIdentity(Capability identityCap) {
        Object idObj = identityCap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
        if (!(idObj instanceof String)) {
            throw new IllegalArgumentException("Missing identity capability on resource, or incorrect type");
        }
        
        return (String) idObj;
    }
    
    private static String getVersion(Capability identityCap) {
        Object versionObj = identityCap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
        if (versionObj == null) {
            return Version.emptyVersion.toString();
        }
        if (versionObj instanceof Version) {
            return ((Version) versionObj).toString();
        }
        if (versionObj instanceof String) {
            return Version.parseVersion((String) versionObj).toString();
        }
        throw new IllegalArgumentException("Incorrect type on identity version");
    }

    private static Capability getIdentityCapability(Resource resource) {
        List<Capability> caps = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
        if (caps == null || caps.isEmpty()) {
            throw new IllegalArgumentException("Missing identity capability on resource");
        }
        return caps.get(0);
    }

    @Override
    public void uninstall(File file) throws Exception {
        log(LogService.LOG_INFO, null, "Uninstalling bundle archive: %s", file.getAbsolutePath());
        unsponsorUnit(file);
    }

    @Override
    public void update(File file) throws Exception {
        log(LogService.LOG_INFO, null, "Uninstalling bundle archive: %s", file.getAbsolutePath());
        unsponsorUnit(file).flatMap(lb -> {
            return putResolveJob(file);
        });
    }

    @Override
    public Collection<InstallableUnit> getInstallableUnits() {
        return withLock(this.unitsLock.readLock(), ArrayList<InstallableUnit>::new, this.units.values());
    }

    /**
     * Unsponsor the installable unit. Returns a promise which is resolved when the uninstall is completed.
     */
    private Promise<List<Bundle>> unsponsorUnit(File file) {
        List<InstallableUnitEvent> events = new LinkedList<>();

        Promise<List<Bundle>> bundles = withLock(this.unitsLock.writeLock(), () -> {
            Promise<List<Bundle>> promise = Promises.resolved(Collections.emptyList());
            InstallableUnitImpl existing = this.units.remove(file);
            if (existing != null) {
                State origState = existing.getState();
                if (origState.equals(State.INSTALLED)) {
                    promise = putUninstallJob(existing);
                }

                if (existing.setState(State.REMOVED)) {
                    events.add(new InstallableUnitEvent(origState, State.REMOVED, existing));
                }
            }
            return promise;
        });

        notifyListeners(events);
        return bundles;
    }

    private void invalidateAllUnits(String reason) {
        List<File> affectedFiles = new LinkedList<>();
        List<InstallableUnitEvent> events = new LinkedList<>();
        withLock(this.unitsLock.writeLock(), () -> {
            for (Entry<File, InstallableUnitImpl> entry : this.units.entrySet()) {
                InstallableUnitImpl unit = entry.getValue();
                State currentState = unit.getState();
                if (EnumSet.of(State.RESOLVED, State.ERROR).contains(currentState)) {
                    unit.setState(State.REMOVED);
                    affectedFiles.add(entry.getKey());
                    events.add(new InstallableUnitEvent(currentState, State.REMOVED, unit));
                    debug("invalidated %s because: %s%n", entry.getKey(), reason);
                }
            }
        });
        notifyListeners(events);

        // Schedule re-resolution of the affected files.
        for (File file : affectedFiles) {
            putResolveJob(file);
        }
    }

    private void replaceUnit(File file, State origState, InstallableUnitImpl newUnit) {
        Collection<InstallableUnitEvent> events = new ArrayList<>(2);
        events.add(new InstallableUnitEvent(origState, newUnit.getState(), newUnit));
        events.addAll(withLock(this.unitsLock.writeLock(), () -> {
            InstallableUnitImpl existing = this.units.put(file, newUnit);
            if (existing != null) {
                State oldState = existing.getState();
                if (existing.setState(State.REMOVED)) {
                    return Collections.singleton(new InstallableUnitEvent(oldState, State.REMOVED, existing));
                }
            }
            return Collections.emptyList();
        }));
        notifyListeners(events);
    }

    private void notifyListeners(Collection<InstallableUnitEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (InstallableListener listener : this.installListeners) {
            try {
                listener.installableUnitsChanged(events);
            } catch (Exception e) {
                log(LogService.LOG_ERROR, e, "Error dispatching installable unit event to registered listener");
            }
        }
    }

    private void log(int level, Throwable t, String format, Object... args) {
        if (this.log != null) {
            this.log.log(level, String.format(format, args), t);
        }
    }

    private static boolean isZipFile(File file) throws IOException {
        byte[] tmp = new byte[4];

        try (InputStream in = new FileInputStream(file)) {
            int bytesRead = in.read(tmp, 0, 4);
            if (bytesRead < 4) {
                throw new IOException("Insufficient bytes read for ZIP header test: possibly truncated file?");
            }
        }

        return Arrays.equals(tmp, ZIP_FILE_MAGIC);
    }

    private static JarEntry findEntry(JarFile jar, String headerName, String defaultPath) throws IOException {
        String path = defaultPath;
        String headerPath = jar.getManifest().getMainAttributes().getValue(headerName);
        if (headerPath != null) {
            path = headerPath.trim();
        }

        if (path == null) {
            // Could happen if no default and no header
            return null;
        }

        return jar.getJarEntry(path);
    }

    private static boolean isFragment(Bundle bundle) {
        return (bundle.adapt(BundleRevision.class).getTypes() & BundleRevision.TYPE_FRAGMENT) > 0;
    }

    private static String bundleEventToString(int type) {
        String s;
        switch (type) {
        case BundleEvent.INSTALLED:
            s = "INSTALLED";
            break;
        case BundleEvent.RESOLVED:
            s = "RESOLVED";
            break;
        case BundleEvent.STARTING:
            s = "STARTING";
            break;
        case BundleEvent.STARTED:
            s = "STARTED";
            break;
        case BundleEvent.STOPPING:
            s = "STOPPING";
            break;
        case BundleEvent.STOPPED:
            s = "STOPPED";
            break;
        case BundleEvent.UNRESOLVED:
            s = "UNRESOLVED";
            break;
        case BundleEvent.UPDATED:
            s = "UPDATED";
            break;
        case BundleEvent.UNINSTALLED:
            s = "UNINSTALLED";
            break;
        case BundleEvent.LAZY_ACTIVATION:
            s = "LAZY_ACTIVATION";
            break;
        default:
            s = "UNKNOWN";
        }
        return s;
    }

    private static Hash getContentHash(Resource resource) throws IllegalArgumentException {
        Hash result = null;

        List<Capability> contentCaps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        if (contentCaps != null) {
            for (Capability contentCap : contentCaps) {
                String hashHexStr = (String) contentCap.getAttributes().get(ContentNamespace.CONTENT_NAMESPACE);
                byte[] hashHex = Hex.toByteArray(hashHexStr);
                Hash hash = new Hash(SHA256, hashHex);

                if (result != null && !hash.equals(result)) {
                    throw new IllegalArgumentException("Resource '" + resource + "' has multiple inconsistent content hashes.");
                }
                result = hash;
            }
        }

        return result;
    }

    private void debug(String format, Object... args) {
        if (DEBUG) {
            System.out.printf("DEBUG: " + format + "%n", args);
        }
    }


}
