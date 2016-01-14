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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarInputStream;

import org.apache.felix.deploymentadmin.spi.CommitResourceCommand;
import org.apache.felix.deploymentadmin.spi.DeploymentSessionImpl;
import org.apache.felix.deploymentadmin.spi.DropAllBundlesCommand;
import org.apache.felix.deploymentadmin.spi.DropAllResourcesCommand;
import org.apache.felix.deploymentadmin.spi.DropBundleCommand;
import org.apache.felix.deploymentadmin.spi.DropResourceCommand;
import org.apache.felix.deploymentadmin.spi.GetStorageAreaCommand;
import org.apache.felix.deploymentadmin.spi.ProcessResourceCommand;
import org.apache.felix.deploymentadmin.spi.SnapshotCommand;
import org.apache.felix.deploymentadmin.spi.StartBundleCommand;
import org.apache.felix.deploymentadmin.spi.StartCustomizerCommand;
import org.apache.felix.deploymentadmin.spi.StopBundleCommand;
import org.apache.felix.deploymentadmin.spi.UpdateCommand;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;

public class DeploymentAdminImpl implements DeploymentAdmin, Constants {
    public static final String PACKAGE_DIR = "packages";
    public static final String TEMP_DIR = "temp";
    public static final String PACKAGECONTENTS_DIR = "contents";
    public static final String PACKAGEINDEX_FILE = "index.txt";
    public static final String TEMP_PREFIX = "pkg";
    public static final String TEMP_POSTFIX = "";

    private static final long TIMEOUT = 10000;

    private volatile BundleContext m_context; /* will be injected by dependencymanager */
    private volatile PackageAdmin m_packageAdmin; /* will be injected by dependencymanager */
    private volatile EventAdmin m_eventAdmin; /* will be injected by dependencymanager */
    private volatile LogService m_log; /* will be injected by dependencymanager */
    private volatile DeploymentSessionImpl m_session;

    private final Map /* BSN -> DeploymentPackage */m_packages = new HashMap();
    private final Semaphore m_semaphore = new Semaphore();

    /**
     * Creates a new {@link DeploymentAdminImpl} instance.
     */
    public DeploymentAdminImpl() {
        // Nop
    }

    /**
     * Creates a new {@link DeploymentAdminImpl} instance.
     */
    DeploymentAdminImpl(BundleContext context) {
        m_context = context;
    }

    public boolean cancel() {
        DeploymentSessionImpl session = m_session;
        if (session != null) {
            session.cancel();
            return true;
        }
        return false;
    }

    /**
     * Returns reference to this bundle's <code>BundleContext</code>
     *
     * @return This bundle's <code>BundleContext</code>
     */
    public BundleContext getBundleContext() {
        return m_context;
    }

    public DeploymentPackage getDeploymentPackage(Bundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle can not be null");
        }
        return getDeploymentPackageContainingBundleWithSymbolicName(bundle.getSymbolicName());
    }

    public DeploymentPackage getDeploymentPackage(String symbName) {
        if (symbName == null) {
            throw new IllegalArgumentException("Symbolic name may not be null");
        }
        return (DeploymentPackage) m_packages.get(symbName);
    }

    /**
     * Returns reference to the current logging service defined in the framework.
     *
     * @return Currently active <code>LogService</code>.
     */
    public LogService getLog() {
        return m_log;
    }

    /**
     * Returns reference to the current package admin defined in the framework.
     *
     * @return Currently active <code>PackageAdmin</code>.
     */
    public PackageAdmin getPackageAdmin() {
        return m_packageAdmin;
    }

    public DeploymentPackage installDeploymentPackage(InputStream sourceInput) throws DeploymentException {
        if (sourceInput == null) {
            throw new IllegalArgumentException("Inputstream may not be null");
        }

        try {
            if (!m_semaphore.tryAcquire(TIMEOUT)) {
                throw new DeploymentException(CODE_TIMEOUT, "Timeout exceeded while waiting to install deployment package (" + TIMEOUT + " ms)");
            }
        }
        catch (InterruptedException ie) {
            throw new DeploymentException(CODE_TIMEOUT, "Thread interrupted");
        }

        File tempPackage = null;
        StreamDeploymentPackage source = null;
        AbstractDeploymentPackage target = null;
        boolean succeeded = false;

        try {
            JarInputStream jarInput = null;
            File tempIndex = null;
            File tempContents = null;
            try {
                File tempDir = m_context.getDataFile(TEMP_DIR);
                tempDir.mkdirs();
                tempPackage = File.createTempFile(TEMP_PREFIX, TEMP_POSTFIX, tempDir);
                tempPackage.delete();
                tempPackage.mkdirs();
                tempIndex = new File(tempPackage, PACKAGEINDEX_FILE);
                tempContents = new File(tempPackage, PACKAGECONTENTS_DIR);
                tempContents.mkdirs();
            }
            catch (IOException e) {
                m_log.log(LogService.LOG_ERROR, "Error writing package to disk", e);
                throw new DeploymentException(CODE_OTHER_ERROR, "Error writing package to disk", e);
            }

            try {
                jarInput = new ContentCopyingJarInputStream(sourceInput, tempIndex, tempContents);

                if (jarInput.getManifest() == null) {
                    Utils.closeSilently(jarInput);
                    
                    m_log.log(LogService.LOG_ERROR, "Stream does not contain a valid deployment package: missing manifest!");
                    throw new DeploymentException(CODE_MISSING_HEADER, "No manifest present in deployment package!");
                }
            }
            catch (IOException e) {
                m_log.log(LogService.LOG_ERROR, "Stream does not contain a valid Jar", e);
                throw new DeploymentException(CODE_NOT_A_JAR, "Stream does not contain a valid Jar", e);
            }

            source = new StreamDeploymentPackage(jarInput, m_context, this);
            String dpSymbolicName = source.getName();

            target = getExistingOrEmptyDeploymentPackage(dpSymbolicName);

            // Fire an event that we're about to install a new package
            sendStartedEvent(source, target);

            // Assert that:
            // the source has no bundles that exists in other packages than the target.
            verifyNoResourcesShared(source, target);

            if (source.isFixPackage()) {
                // Assert that:
                // a. the version of the target matches the required fix-package range;
                // b. all missing source bundles are present in the target.
                verifyFixPackage(source, target);
            }
            else {
                // Assert that:
                // no missing resources or bundles are declared.
                verifySourcePackage(source);
            }

            try {
                m_session = new DeploymentSessionImpl(source, target, createInstallCommandChain(), this, new DeploymentAdminConfig(m_context));
                m_session.call(false /* ignoreExceptions */);
            }
            catch (DeploymentException de) {
                throw de;
            }
            finally {
                // We're done at this point with the JAR input stream, close it here as to avoid keeping
                // files open unnecessary (otherwise it fails on Windows)...
                Utils.closeSilently(jarInput);
            }

            String dpInstallBaseDirectory = PACKAGE_DIR + File.separator + dpSymbolicName;

            File targetContents = m_context.getDataFile(dpInstallBaseDirectory + File.separator + PACKAGECONTENTS_DIR);
            File targetIndex = m_context.getDataFile(dpInstallBaseDirectory + File.separator + PACKAGEINDEX_FILE);

            if (source.isFixPackage()) {
                try {
                    Utils.merge(targetIndex, targetContents, tempIndex, tempContents);
                }
                catch (IOException e) {
                    m_log.log(LogService.LOG_ERROR, "Could not merge source fix package with target deployment package", e);
                    throw new DeploymentException(CODE_OTHER_ERROR, "Could not merge source fix package with target deployment package", e);
                }
            }
            else {
                File targetPackage = m_context.getDataFile(dpInstallBaseDirectory);
                targetPackage.mkdirs();
                if (!Utils.replace(targetPackage, tempPackage)) {
                    throw new DeploymentException(CODE_OTHER_ERROR, "Could not replace " + targetPackage + " with " + tempPackage);
                }
            }

            FileDeploymentPackage fileDeploymentPackage = null;
            try {
                fileDeploymentPackage = new FileDeploymentPackage(targetIndex, targetContents, m_context, this);
                m_packages.put(dpSymbolicName, fileDeploymentPackage);
            }
            catch (IOException e) {
                m_log.log(LogService.LOG_ERROR, "Could not create installed deployment package from disk", e);
                throw new DeploymentException(CODE_OTHER_ERROR, "Could not create installed deployment package from disk", e);
            }

            // Since we're here, it means everything went OK, so we might as well raise our success flag...
            succeeded = true;

            return fileDeploymentPackage;
        }
        finally {
            if (tempPackage != null) {
                if (!Utils.delete(tempPackage, true)) {
                    m_log.log(LogService.LOG_ERROR, "Could not delete temporary deployment package from disk");
                    succeeded = false;
                }
            }

            sendCompleteEvent(source, target, succeeded);
            m_semaphore.release();
        }
    }

    public DeploymentPackage[] listDeploymentPackages() {
        Collection packages = m_packages.values();
        return (DeploymentPackage[]) packages.toArray(new DeploymentPackage[packages.size()]);
    }

    /**
     * Called by dependency manager upon start of this component.
     */
    public void start() throws DeploymentException {
        File packageDir = m_context.getDataFile(PACKAGE_DIR);
        if (packageDir == null) {
            throw new DeploymentException(CODE_OTHER_ERROR, "Could not create directories needed for deployment package persistence");
        }
        else if (packageDir.isDirectory()) {
            File[] dpPackages = packageDir.listFiles();
            for (int i = 0; i < dpPackages.length; i++) {
                File dpPackageDir = dpPackages[i];
                if (!dpPackageDir.isDirectory()) {
                    continue;
                }

                try {
                    File index = new File(dpPackageDir, PACKAGEINDEX_FILE);
                    File contents = new File(dpPackageDir, PACKAGECONTENTS_DIR);
                    FileDeploymentPackage dp = new FileDeploymentPackage(index, contents, m_context, this);
                    m_packages.put(dp.getName(), dp);
                }
                catch (IOException e) {
                    m_log.log(LogService.LOG_WARNING, "Could not read deployment package from disk, skipping: '" + dpPackageDir.getAbsolutePath() + "'");
                }
            }
        }
    }

    /**
     * Called by dependency manager when stopping this component.
     */
    public void stop() {
        cancel();
    }

    /**
     * Uninstalls the given deployment package from the system.
     * 
     * @param dp the deployment package to uninstall, cannot be <code>null</code>;
     * @param forced <code>true</code> to force the uninstall, meaning that any exceptions are ignored during the
     *            uninstallation.
     * @throws DeploymentException in case the uninstall failed.
     */
    public void uninstallDeploymentPackage(DeploymentPackage dp, boolean forced) throws DeploymentException {
        try {
            if (!m_semaphore.tryAcquire(TIMEOUT)) {
                throw new DeploymentException(CODE_TIMEOUT, "Timeout exceeded while waiting to uninstall deployment package (" + TIMEOUT + " ms)");
            }
        }
        catch (InterruptedException ie) {
            throw new DeploymentException(CODE_TIMEOUT, "Thread interrupted");
        }

        boolean succeeded = false;
        AbstractDeploymentPackage source = AbstractDeploymentPackage.EMPTY_PACKAGE;
        AbstractDeploymentPackage target = (AbstractDeploymentPackage) dp;

        // Notify listeners that we've about to uninstall the deployment package...
        sendUninstallEvent(source, target);

        try {
            try {
                m_session = new DeploymentSessionImpl(source, target, createUninstallCommandChain(), this, new DeploymentAdminConfig(m_context));
                m_session.call(forced /* ignoreExceptions */);
            }
            catch (DeploymentException de) {
                throw de;
            }

            File targetPackage = m_context.getDataFile(PACKAGE_DIR + File.separator + source.getName());
            if (!Utils.delete(targetPackage, true)) {
                m_log.log(LogService.LOG_ERROR, "Could not delete deployment package from disk");
                throw new DeploymentException(CODE_OTHER_ERROR, "Could not delete deployment package from disk");
            }

            m_packages.remove(dp.getName());

            succeeded = true;
        }
        finally {
            sendCompleteEvent(source, target, succeeded);
            m_semaphore.release();
        }
    }

    /**
     * Creates the properties for a new event.
     * 
     * @param source the source package being installed;
     * @param target the current installed package (can be new).
     * @return the event properties, never <code>null</code>.
     */
    private Dictionary createEventProperties(AbstractDeploymentPackage source, AbstractDeploymentPackage target) {
        Dictionary props = new Properties();
        if (source != null) {
            String displayName = source.getDisplayName();
            if (displayName == null) {
                displayName = source.getName();
            }

            props.put(EVENTPROPERTY_DEPLOYMENTPACKAGE_NAME, source.getName());
            props.put(EVENTPROPERTY_DEPLOYMENTPACKAGE_READABLENAME, displayName);
            if (!source.isNew()) {
                props.put(EVENTPROPERTY_DEPLOYMENTPACKAGE_NEXTVERSION, source.getVersion());
            }
        }
        if ((target != null) && !target.isNew()) {
            props.put(EVENTPROPERTY_DEPLOYMENTPACKAGE_CURRENTVERSION, target.getVersion());
        }
        return props;
    }

    private List createInstallCommandChain() {
        List commandChain = new ArrayList();

        GetStorageAreaCommand getStorageAreaCommand = new GetStorageAreaCommand();
        commandChain.add(getStorageAreaCommand);
        commandChain.add(new StopBundleCommand());
        commandChain.add(new SnapshotCommand(getStorageAreaCommand));
        commandChain.add(new UpdateCommand());
        commandChain.add(new StartCustomizerCommand());
        CommitResourceCommand commitCommand = new CommitResourceCommand();
        commandChain.add(new ProcessResourceCommand(commitCommand));
        commandChain.add(new DropResourceCommand(commitCommand));
        commandChain.add(new DropBundleCommand());
        commandChain.add(commitCommand);
        commandChain.add(new StartBundleCommand());

        return commandChain;
    }

    private List createUninstallCommandChain() {
        List commandChain = new ArrayList();

        GetStorageAreaCommand getStorageAreaCommand = new GetStorageAreaCommand();
        commandChain.add(getStorageAreaCommand);
        commandChain.add(new StopBundleCommand());
        commandChain.add(new SnapshotCommand(getStorageAreaCommand));
        commandChain.add(new StartCustomizerCommand());
        CommitResourceCommand commitCommand = new CommitResourceCommand();
        commandChain.add(new DropAllResourcesCommand(commitCommand));
        commandChain.add(commitCommand);
        commandChain.add(new DropAllBundlesCommand());

        return commandChain;
    }

    /**
     * Searches for a deployment package that contains a bundle with the given symbolic name.
     * 
     * @param symbolicName the symbolic name of the <em>bundle</em> to return the containing deployment package for,
     *            cannot be <code>null</code>.
     * @return the deployment package containing the given bundle, or <code>null</code> if no deployment package
     *         contained such bundle.
     */
    private AbstractDeploymentPackage getDeploymentPackageContainingBundleWithSymbolicName(String symbolicName) {
        for (Iterator i = m_packages.values().iterator(); i.hasNext();) {
            AbstractDeploymentPackage dp = (AbstractDeploymentPackage) i.next();
            if (dp.getBundle(symbolicName) != null) {
                return dp;
            }
        }
        return null;
    }

    /**
     * Returns either an existing deployment package, or if no such package exists, an empty package.
     * 
     * @param symbolicName the name of the deployment package to retrieve, cannot be <code>null</code>.
     * @return a deployment package, never <code>null</code>.
     */
    private AbstractDeploymentPackage getExistingOrEmptyDeploymentPackage(String symbolicName) {
        AbstractDeploymentPackage result = (AbstractDeploymentPackage) m_packages.get(symbolicName);
        if (result == null) {
            result = AbstractDeploymentPackage.EMPTY_PACKAGE;
        }
        return result;
    }

    /**
     * Returns all bundles that are not present in any deployment package. Ultimately, this should only
     * be one bundle, the system bundle, but this is not enforced in any way by the specification.
     * 
     * @return an array of non-deployment packaged bundles, never <code>null</code>.
     */
    private Bundle[] getNonDeploymentPackagedBundles() {
        List result = new ArrayList(Arrays.asList(m_context.getBundles()));

        Iterator iter = result.iterator();
        while (iter.hasNext()) {
            Bundle suspect = (Bundle) iter.next();
            if (suspect.getLocation().startsWith(BUNDLE_LOCATION_PREFIX)) {
                iter.remove();
            }
        }

        return (Bundle[]) result.toArray(new Bundle[result.size()]);
    }

    /**
     * Sends out an event that the {@link #installDeploymentPackage(InputStream)} is
     * completed its installation of a deployment package.
     * 
     * @param source the source package being installed;
     * @param target the current installed package (can be new);
     * @param success <code>true</code> if the installation was successful, <code>false</code> otherwise.
     */
    private void sendCompleteEvent(AbstractDeploymentPackage source, AbstractDeploymentPackage target, boolean success) {
        Dictionary props = createEventProperties(source, target);
        props.put(EVENTPROPERTY_SUCCESSFUL, Boolean.valueOf(success));

        m_eventAdmin.postEvent(new Event(EVENTTOPIC_COMPLETE, props));
    }

    /**
     * Sends out an event that the {@link #installDeploymentPackage(InputStream)} is about
     * to install a new deployment package.
     * 
     * @param source the source package being installed;
     * @param target the current installed package (can be new).
     */
    private void sendStartedEvent(AbstractDeploymentPackage source, AbstractDeploymentPackage target) {
        Dictionary props = createEventProperties(source, target);

        m_eventAdmin.postEvent(new Event(EVENTTOPIC_INSTALL, props));
    }

    /**
     * Sends out an event that the {@link #uninstallDeploymentPackage(DeploymentPackage)} is about
     * to uninstall a deployment package.
     * 
     * @param source the source package being uninstalled;
     * @param target the current installed package (can be new).
     */
    private void sendUninstallEvent(AbstractDeploymentPackage source, AbstractDeploymentPackage target) {
        Dictionary props = createEventProperties(source, target);

        m_eventAdmin.postEvent(new Event(EVENTTOPIC_UNINSTALL, props));
    }

    /**
     * Verifies that the version of the target matches the required source version range, and
     * whether all missing source resources are available in the target.
     * 
     * @param source the fix-package source to verify;
     * @param target the target package to verify against.
     * @throws DeploymentException in case verification failed.
     */
    private void verifyFixPackage(AbstractDeploymentPackage source, AbstractDeploymentPackage target) throws DeploymentException {
        boolean newPackage = target.isNew();

        // Verify whether the target package exists, and if so, falls in the requested fix-package range...
        if (newPackage || (!source.getVersionRange().isInRange(target.getVersion()))) {
            m_log.log(LogService.LOG_ERROR, "Target package version '" + target.getVersion() + "' is not in source range '" + source.getVersionRange() + "'");
            throw new DeploymentException(CODE_MISSING_FIXPACK_TARGET, "Target package version '" + target.getVersion() + "' is not in source range '" + source.getVersionRange() + "'");
        }

        // Verify whether all missing bundles are available in the target package...
        BundleInfoImpl[] bundleInfos = source.getBundleInfoImpls();
        for (int i = 0; i < bundleInfos.length; i++) {
            if (bundleInfos[i].isMissing()) {
                // Check whether the bundle exists in the target package...
                BundleInfoImpl targetBundleInfo = target.getBundleInfoByPath(bundleInfos[i].getPath());
                if (targetBundleInfo == null) {
                    m_log.log(LogService.LOG_ERROR, "Missing bundle '" + bundleInfos[i].getSymbolicName() + "/" + bundleInfos[i].getVersion() + " does not exist in target package!");
                    throw new DeploymentException(CODE_MISSING_BUNDLE, "Missing bundle '" + bundleInfos[i].getSymbolicName() + "/" + bundleInfos[i].getVersion() + " does not exist in target package!");
                }
            }
        }

        // Verify whether all missing resources are available in the target package...
        ResourceInfoImpl[] resourceInfos = source.getResourceInfos();
        for (int i = 0; i < resourceInfos.length; i++) {
            if (resourceInfos[i].isMissing()) {
                // Check whether the resource exists in the target package...
                ResourceInfoImpl targetResourceInfo = target.getResourceInfoByPath(resourceInfos[i].getPath());
                if (targetResourceInfo == null) {
                    m_log.log(LogService.LOG_ERROR, "Missing resource '" + resourceInfos[i].getPath() + " does not exist in target package!");
                    throw new DeploymentException(CODE_MISSING_RESOURCE, "Missing resource '" + resourceInfos[i].getPath() + " does not exist in target package!");
                }
            }
        }
    }

    /**
     * Verifies whether none of the mentioned resources in the source package are present in
     * deployment packages other than the given target.
     * 
     * @param source the source package to verify;
     * @param target the target package to verify against.
     * @throws DeploymentException in case verification fails.
     */
    private void verifyNoResourcesShared(AbstractDeploymentPackage source, AbstractDeploymentPackage target) throws DeploymentException {
        Bundle[] foreignBundles = getNonDeploymentPackagedBundles();

        // Verify whether all source bundles are available in the target package or absent...
        BundleInfoImpl[] bundleInfos = source.getBundleInfoImpls();
        for (int i = 0; i < bundleInfos.length; i++) {
            String symbolicName = bundleInfos[i].getSymbolicName();
            Version version = bundleInfos[i].getVersion();

            DeploymentPackage targetPackage = getDeploymentPackageContainingBundleWithSymbolicName(symbolicName);
            // If found, it should match the given target DP; not found is also ok...
            if ((targetPackage != null) && !targetPackage.equals(target)) {
                m_log.log(LogService.LOG_ERROR, "Bundle '" + symbolicName + "/" + version + " already present in other deployment packages!");
                throw new DeploymentException(CODE_BUNDLE_SHARING_VIOLATION, "Bundle '" + symbolicName + "/" + version + " already present in other deployment packages!");
            }

            if (targetPackage == null) {
                // Maybe the bundle is installed without deployment admin...
                for (int j = 0; j < foreignBundles.length; j++) {
                    if (symbolicName.equals(foreignBundles[j].getSymbolicName()) && version.equals(foreignBundles[j].getVersion())) {
                        m_log.log(LogService.LOG_ERROR, "Bundle '" + symbolicName + "/" + version + " already present!");
                        throw new DeploymentException(CODE_BUNDLE_SHARING_VIOLATION, "Bundle '" + symbolicName + "/" + version + " already present!");
                    }
                }
            }
        }

        // TODO verify other resources as well...
    }

    private void verifySourcePackage(AbstractDeploymentPackage source) throws DeploymentException {
        // TODO this method should do a X-ref check between DP-manifest and JAR-entries...
// m_log.log(LogService.LOG_ERROR, "Missing bundle '" + symbolicName + "/" + bundleInfos[i].getVersion() +
// " does not exist in target package!");
// throw new DeploymentException(CODE_OTHER_ERROR, "Missing bundle '" + symbolicName + "/" + bundleInfos[i].getVersion()
// + " is not part of target package!");
    }
}