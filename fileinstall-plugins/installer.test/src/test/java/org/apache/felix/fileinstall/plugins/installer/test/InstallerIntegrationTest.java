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
package org.apache.felix.fileinstall.plugins.installer.test;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.fileinstall.plugins.installer.FrameworkInstaller;
import org.apache.felix.fileinstall.plugins.installer.InstallableListener;
import org.apache.felix.fileinstall.plugins.installer.InstallableUnit;
import org.apache.felix.fileinstall.plugins.installer.InstallableUnitEvent;
import org.apache.felix.fileinstall.plugins.installer.State;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class InstallerIntegrationTest {

    @Configuration
    public Option[] config() {
        return options(
                // vmOptions("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"),
                systemProperty("org.apache.felix.fileinstall.plugins.installer.debug").value("true"),
                mavenBundle(FELIX_GROUPID, "org.apache.felix.configadmin").versionAsInProject(),
                mavenBundle(FELIX_GROUPID, "org.apache.felix.scr").versionAsInProject(),
                mavenBundle(FELIX_GROUPID, "org.apache.felix.log").versionAsInProject(),
                mavenBundle(FELIX_GROUPID, "org.apache.felix.resolver").versionAsInProject(),
                // NB FileInstall included for its API, but NOT started so the directory polling doesn't happen
                mavenBundle(FELIX_GROUPID, "org.apache.felix.fileinstall").versionAsInProject().start(false),
                mavenBundle("org.osgi", "org.osgi.service.repository").versionAsInProject(),
                mavenBundle("biz.aQute.bnd", "biz.aQute.bndlib").versionAsInProject(),
                mavenBundle("biz.aQute.bnd", "biz.aQute.repository").versionAsInProject(),
                mavenBundle(FELIX_GROUPID, "org.apache.felix.fileinstall.plugins.resolver").versionAsInProject(),
                mavenBundle(FELIX_GROUPID, "org.apache.felix.fileinstall.plugins.installer").versionAsInProject(),
                junitBundles(),
                // javax.xml exports required as version=1.0 for bnd repository
                systemPackage("javax.xml.namespace;version=1.0.0"),
                systemPackage("javax.xml.stream;version=1.0.0")
                );
    }

    private static final String FELIX_GROUPID = "org.apache.felix";

    @Inject
    private BundleContext bundleContext;

    private final File dataDir = new File("./target/test-classes/");

    private ServiceTracker<ArtifactInstaller, ArtifactInstaller> artifactInstallerTracker;
    private ServiceTracker<FrameworkInstaller, FrameworkInstaller> fwkInstallerTracker;

    @Before
    public void before() throws Exception {
        assertAllBundlesResolved();

        // Track ArtifactInstaller service ONLY from bundle org.apache.felix.fileinstall.plugins.installer, to avoid potential interference
        Bundle installerBundle = findBundle("org.apache.felix.fileinstall.plugins.installer");
        Filter artifactInstallerTrackerFilter = FrameworkUtil.createFilter(String.format("(&(objectClass=%s)(service.bundleid=%d))", ArtifactInstaller.class.getName(), installerBundle.getBundleId()));
        this.artifactInstallerTracker = new ServiceTracker<>(this.bundleContext, artifactInstallerTrackerFilter, null);
        this.artifactInstallerTracker.open();

        // Wait up to 5 seconds for ArtifactInstaller to appear
        ArtifactInstaller artifactInstaller = this.artifactInstallerTracker.waitForService(5000);
        if (artifactInstaller == null) {
            fail("ArtifactInstaller service not available within 5 seconds");
        }

        this.fwkInstallerTracker = new ServiceTracker<>(this.bundleContext, FrameworkInstaller.class, null);
        this.fwkInstallerTracker.open();
    }

    @After
    public void after() {
        this.fwkInstallerTracker.close();
        this.artifactInstallerTracker.close();
    }

    @Test
    public void testErrorFromInvalidArchive() throws Exception {
        Object[] artifactInstallers = this.artifactInstallerTracker.getServices();
        assertNotNull("Should be exactly one ArtifactInstaller service", artifactInstallers);
        assertEquals("Should be exactly one ArtifactInstaller service", 1, artifactInstallers.length);

        // Register mock InstallableListener
        List<String> installEvents = new LinkedList<>();
        InstallableListener mockInstallListener = new InstallableListener() {
            @Override
            public void installableUnitsChanged(Collection<InstallableUnitEvent> events) {
                for (InstallableUnitEvent event : events) {
                    String message = String.format("%s %s", event.getNewState(), event.getUnit().getSymbolicName());
                    installEvents.add(message);
                }
            }
        };

        ServiceRegistration<InstallableListener> mockListenerReg = this.bundleContext.registerService(InstallableListener.class, mockInstallListener, null);
        try {
            assertEquals("Shouldn't be any install events yet", 0, installEvents.size());

            // Provide the sample archive
            File sampleArchive = new File(this.dataDir, "org.example.invalid-missing-rb.bar");
            ArtifactInstaller installer = this.artifactInstallerTracker.getService();
            assertTrue("installer should handle sample archive", installer.canHandle(sampleArchive));
            installer.install(sampleArchive);

            // Wait for resolve to occur
            Thread.sleep(2000);
            assertEquals(1, installEvents.size());
            assertEquals("ERROR org.example.invalid-missing-rb", installEvents.get(0));
        } finally {
            mockListenerReg.unregister();
        }
    }

    private class EventQueue implements InstallableListener {
        private final Queue<InstallableUnitEvent> q = new LinkedList<>();
        @Override
        public synchronized void installableUnitsChanged(Collection<InstallableUnitEvent> events) {
            for (InstallableUnitEvent event : events) {
                this.q.add(event);
                System.out.printf("!!! added event %s (%s), queue depth is now %d%n ", event.getNewState(), event.getUnit().getSymbolicName(), this.q.size());
            }
        }
        private synchronized InstallableUnitEvent pop() {
            if (this.q.isEmpty()) {
                throw new IllegalStateException("Trying to take from empty queue");
            }
            InstallableUnitEvent event = this.q.remove();
            System.out.printf("!!! removed event %s (%s), queue depth is now %d%n ", event.getNewState(), event.getUnit().getSymbolicName(), this.q.size());
            return event;
        }
        private synchronized int depth() {
            return this.q.size();
        }
    }

    /*
     * This is a complex case that tests the interplay of multiple archives with
     * overlapping content.
     *
     * - Archive 1 contains bundles A and B.
     *
     * - Archive 2 contains bundles A and C.
     *
     * The process is as follows:
     *
     * i.   Make installer aware of both archives. Both now present as an
     * installable unit with 2 artifacts (A+B and A+C).
     *
     * ii.  Install archive 1.
     * Archive 2 gets invalidated and re-resolved. It now presents as an
     * installable unit with 1 artifact (C) because A is already in the
     * framework.
     *
     * iii. Remove archive 1. Archive 2 gets invalidated and
     * re-resolved. It now presents as an installable unit with 2 artifacts
     * (A+C) because A is no longer in the framework.
     *
     * Note that step (iii) is equivalent to deleting the archive from
     * FileInstall's load directory. Therefore it doesn't reappear as a RESOLVED
     * installable unit. To reinstall this archive, the user would have to copy
     * it back into the load directory.
     *
     */
    @Test
    public void testInstallArchives() throws Exception {
        // Register InstallableListener
        EventQueue eventQueue = new EventQueue();
        ServiceRegistration<InstallableListener> mockListenerReg = this.bundleContext.registerService(InstallableListener.class, eventQueue, null);
        try {
            assertEquals("Shouldn't be any install events yet", 0, eventQueue.depth());
            InstallableUnitEvent event;

            // Provide the sample archive
            File sampleArchive1 = new File(this.dataDir, "valid1.bar");
            ArtifactInstaller installer = this.artifactInstallerTracker.getService();
            assertNotNull("ArtifactInstaller service should exist", installer);
            assertTrue("installer should handle sample archive", installer.canHandle(sampleArchive1));
            installer.install(sampleArchive1);

            // Wait for resolve to occur
            Thread.sleep(2000);
            assertEquals(1, eventQueue.depth());
            event = eventQueue.pop();
            InstallableUnit unit1 = event.getUnit();

            assertEquals(State.RESOLVED, event.getNewState());
            assertEquals("samples.valid1", event.getUnit().getSymbolicName());
            assertEquals("samples.valid1 requires 2 bundles to be installed", 2, event.getUnit().getArtifacts().size());

            // Add a second archive
            File sampleArchive2 = new File(this.dataDir, "valid2.bar");
            assertTrue("installer should handle sample archive", installer.canHandle(sampleArchive2));
            installer.install(sampleArchive2);

            // Wait for resolve to occur
            Thread.sleep(2000);
            assertEquals(1, eventQueue.depth());
            event = eventQueue.pop();
            assertEquals(State.RESOLVED, event.getNewState());
            assertEquals("samples.valid2", event.getUnit().getSymbolicName());
            assertEquals("samples.valid2 requires 2 bundles to be installed", 2, event.getUnit().getArtifacts().size());

            // Install first archive.
            unit1.install().getValue();
            // Check bundles were actually installed!
            assertNotNull(findBundle("org.example.a"));
            assertNotNull(findBundle("org.example.b"));

            // The installation of first archive should cause invalidation of second archive resolution result, followed by re-resolve.
            // First, installing #1
            event = eventQueue.pop();
            assertEquals(State.INSTALLING, event.getNewState());
            assertEquals("samples.valid1", event.getUnit().getSymbolicName());

            // Next installed #1
            event = eventQueue.pop();
            assertEquals(State.INSTALLED, event.getNewState());
            assertEquals("samples.valid1", event.getUnit().getSymbolicName());

            // Next removed #2 (due to invalidation)
            // Short sleep necessary because the invalidate can happen on another thread (eg framework refresh)
            Thread.sleep(500);
            event = eventQueue.pop();
            assertEquals(State.REMOVED, event.getNewState());
            assertEquals("samples.valid2", event.getUnit().getSymbolicName());

            // Next resolved #2 (re-resolve after invalidation)
            // Another sleep because the re-resolve happens after at least 1 sec delay
            Thread.sleep(2000);
            event = eventQueue.pop();
            assertEquals(State.RESOLVED, event.getNewState());
            assertEquals("samples.valid2", event.getUnit().getSymbolicName());
            assertEquals("samples.valid2 now requires 1 bundle to be installed", 1, event.getUnit().getArtifacts().size());
            assertEquals(0, eventQueue.depth());

            // Now uninstall first archive
            installer.uninstall(sampleArchive1);
            Thread.sleep(500);
            // Check bundles were actually uninstalled!
            assertNull(findBundle("org.example.a"));
            assertNull(findBundle("org.example.b"));

            // Removed event for #1
            Thread.sleep(500);
            event = eventQueue.pop();
            assertEquals(State.REMOVED, event.getNewState());
            assertEquals("samples.valid1", event.getUnit().getSymbolicName());

            // Removed event for #2 due to invalidation
            event = eventQueue.pop();
            assertEquals(State.REMOVED, event.getNewState());
            assertEquals("samples.valid2", event.getUnit().getSymbolicName());

            // Re-resolution of #2 after a little sleep
            Thread.sleep(2000);
            event = eventQueue.pop();
            assertEquals(State.RESOLVED, event.getNewState());
            assertEquals("samples.valid2", event.getUnit().getSymbolicName());
            assertEquals("samples.valid2 now requires 2 bundle to be installed", 2, event.getUnit().getArtifacts().size());
            assertEquals(0, eventQueue.depth());

        } finally {
            mockListenerReg.unregister();
        }
    }

    @Test
    public void testAddRemoveBundles() throws Exception {
        // Precondition: no example bundles
        assertNull("example bundle already present - broken prior test?", findBundle("org.example.a"));
        assertNull("example bundle already present - broken prior test?", findBundle("org.example.b"));

        FrameworkInstaller installer = this.fwkInstallerTracker.getService();
        assertNotNull("installer service not found", installer);

        // Install bundles and check they were installed
        File fileA = new File(this.dataDir, "org.example.a.jar");
        File fileB = new File(this.dataDir, "org.example.b.jar");
        List<String> locations = Stream.of(fileA, fileB)
                .map(File::toURI)
                .map(URI::toString)
                .collect(Collectors.toList());
        Object sponsor = new Object();
        List<Bundle> installed = installer.addLocations(sponsor, locations);
        assertEquals(2, installed.size());
        assertEquals("org.example.a", installed.get(0).getSymbolicName());
        assertEquals("org.example.b", installed.get(1).getSymbolicName());

        assertNotNull("example bundle not present after install", findBundle("org.example.a"));
        assertNotNull("example bundle not present after install", findBundle("org.example.b"));

        // Uninstall bundles and check they are gone
        Set<String> removed = installer.removeSponsor(sponsor).stream().map(Bundle::getSymbolicName).collect(Collectors.toSet());
        assertEquals(2, removed.size());
        assertTrue(removed.contains("org.example.a"));
        assertTrue(removed.contains("org.example.b"));

        assertNull("example bundle still present after uninstall", findBundle("org.example.a"));
        assertNull("example bundle still present after uninstall", findBundle("org.example.b"));
    }

    @Test
    public void testAddRemoveOverlappingBundles() throws Exception {
        // Precondition: no example bundles
        assertNull("example bundle already present - broken prior test?", findBundle("org.example.a"));
        assertNull("example bundle already present - broken prior test?", findBundle("org.example.b"));
        assertNull("example bundle already present - broken prior test?", findBundle("org.example.c"));

        FrameworkInstaller installer = this.fwkInstallerTracker.getService();
        assertNotNull("installer service not found", installer);

        File fileA = new File(this.dataDir, "org.example.a.jar");
        File fileB = new File(this.dataDir, "org.example.b.jar");
        File fileC = new File(this.dataDir, "org.example.c.jar");

        // Install first set including A and B
        Object sponsor1 = new Object();
        List<String> locations1 = Stream.of(fileA, fileB)
                .map(File::toURI)
                .map(URI::toString)
                .collect(Collectors.toList());
        List<Bundle> installed1 = installer.addLocations(sponsor1, locations1);
        assertEquals(2, installed1.size());
        assertEquals("org.example.a", installed1.get(0).getSymbolicName());
        assertEquals("org.example.b", installed1.get(1).getSymbolicName());

        assertNotNull("example bundle not present after install", findBundle("org.example.a"));
        assertNotNull("example bundle not present after install", findBundle("org.example.b"));
        assertNull("incorrect bundle installed", findBundle("org.example.c"));

        // Install second set including A and C
        Object sponsor2 = new Object();
        List<String> locations2 = Stream.of(fileA, fileC)
                .map(File::toURI)
                .map(URI::toString)
                .collect(Collectors.toList());
        List<Bundle> installed2 = installer.addLocations(sponsor2, locations2);
        assertEquals(1, installed2.size());

        assertEquals("org.example.c", installed2.get(0).getSymbolicName());
        assertNotNull("example bundle not present after install", findBundle("org.example.a"));
        assertNotNull("example bundle not present after install", findBundle("org.example.b"));
        assertNotNull("example bundle not present after install", findBundle("org.example.c"));

        // Uninstall first set - bundle B should go away but not A
        Set<String> removed1 = installer.removeSponsor(sponsor1).stream().map(Bundle::getSymbolicName).collect(Collectors.toSet());
        assertEquals(1, removed1.size());
        assertTrue(removed1.contains("org.example.b"));

        assertNotNull("example bundle should still be present", findBundle("org.example.a"));
        assertNull("example bundle still present after uninstall", findBundle("org.example.b"));
        assertNotNull("example bundle should still be present", findBundle("org.example.c"));

        // Uninstall second set - all bundles should go away
        Set<String> removed2 = installer.removeSponsor(sponsor2).stream().map(Bundle::getSymbolicName).collect(Collectors.toSet());
        assertEquals(2, removed2.size());
        assertTrue(removed2.contains("org.example.a"));
        assertTrue(removed2.contains("org.example.c"));

        assertNull("example bundle still present after uninstall", findBundle("org.example.a"));
        assertNull("example bundle still present after uninstall", findBundle("org.example.b"));
        assertNull("example bundle still present after uninstall", findBundle("org.example.c"));
    }

    @Test
    public void testDontRemoveExistingBundle() throws Exception {
        // Precondition: no example bundles
        assertNull("example bundle already present - broken prior test?", findBundle("org.example.a"));
        assertNull("example bundle already present - broken prior test?", findBundle("org.example.b"));

        FrameworkInstaller installer = this.fwkInstallerTracker.getService();
        assertNotNull("installer service not found", installer);

        Bundle existingBundle = findBundle("org.apache.felix.scr");
        assertNotNull("SCR bundle has been uninstalled -- broken prior test?", existingBundle);

        String locationA = new File(this.dataDir, "org.example.a.jar").toURI().toString();
        String locationB = new File(this.dataDir, "org.example.b.jar").toURI().toString();
        String existingLocation = existingBundle.getLocation();

        // Install set including A, B and SCR
        Object sponsor = new Object();
        List<String> locations = Arrays.asList(locationA, locationB, existingLocation);
        installer.addLocations(sponsor, locations);
        assertNotNull("example bundle not present after install", findBundle("org.example.a"));
        assertNotNull("example bundle not present after install", findBundle("org.example.b"));
        assertNotNull("preexisting bundle not present after install", findBundle("org.apache.felix.scr"));

        // Uninstall set - bundles A and B should go away but not SCR
        installer.removeSponsor(sponsor);
        assertNull("example bundle still present after uninstall", findBundle("org.example.a"));
        assertNull("example bundle still present after uninstall", findBundle("org.example.b"));
        assertNotNull("preexisting bundle uninstalled", findBundle("org.apache.felix.scr"));
    }


    private Bundle findBundle(String bsn) {
        Bundle found = null;
        for (Bundle bundle : this.bundleContext.getBundles()) {
            if (bsn.equals(bundle.getSymbolicName()) && bundle.getState() != Bundle.UNINSTALLED) {
                if (found != null && bundle.getBundleId() != found.getBundleId()) {
                    throw new IllegalArgumentException("Ambiguous bundle symbolic name " + bsn);
                }
                found = bundle;
            }
        }
        return found;
    }

    private void assertAllBundlesResolved() {
        int mask = Bundle.RESOLVED | Bundle.STARTING | Bundle.STOPPING | Bundle.ACTIVE;
        for (Bundle bundle : this.bundleContext.getBundles()) {
            assertTrue("Unresolved bundle " + bundle.getSymbolicName(), (bundle.getState() & mask) > 0);
        }
    }

}
