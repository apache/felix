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
package org.apache.felix.deploymentadmin.spi;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.deploymentadmin.AbstractDeploymentPackage;
import org.apache.felix.deploymentadmin.AbstractInfo;
import org.apache.felix.deploymentadmin.BundleInfoImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.log.LogService;

/**
 * Command that installs all bundles described in the source deployment package
 * of a deployment session. If a bundle was already defined in the target
 * deployment package of the same session it is updated, otherwise the bundle is
 * simply installed.
 */
public class UpdateCommand extends Command {

    protected void doExecute(DeploymentSessionImpl session) throws Exception {
        AbstractDeploymentPackage source = session.getSourceAbstractDeploymentPackage();
        AbstractDeploymentPackage targetPackage = session.getTargetAbstractDeploymentPackage();
        BundleContext context = session.getBundleContext();
        LogService log = session.getLog();

        Map expectedBundles = new HashMap();
        AbstractInfo[] bundleInfos = (AbstractInfo[]) source.getBundleInfos();
        for (int i = 0; i < bundleInfos.length; i++) {
            AbstractInfo bundleInfo = bundleInfos[i];
            if (!bundleInfo.isMissing()) {
                expectedBundles.put(bundleInfo.getPath(), bundleInfo);
            }
        }

        try {
            while (!expectedBundles.isEmpty()) {
                AbstractInfo entry = source.getNextEntry();
                if (entry == null) {
                    throw new DeploymentException(CODE_OTHER_ERROR, "Expected more bundles in the stream: " + expectedBundles.keySet());
                }

                String name = entry.getPath();
                BundleInfoImpl bundleInfo = (BundleInfoImpl) expectedBundles.remove(name);
                if (bundleInfo == null) {
                    if (isLocalizationFile(name)) {
                        // FELIX-518: do not try to process signature or localization files...
                        continue;
                    }
                    throw new DeploymentException(CODE_OTHER_ERROR, "Resource '" + name + "' is not described in the manifest.");
                }

                String bsn = bundleInfo.getSymbolicName();
                Version sourceVersion = bundleInfo.getVersion();

                Bundle bundle = targetPackage.getBundle(bsn);
                try {
                    if (bundle == null) {
                        // new bundle, install it
                        bundle = context.installBundle(BUNDLE_LOCATION_PREFIX + bsn, new BundleInputStream(source.getCurrentEntryStream()));
                        addRollback(new UninstallBundleRunnable(bundle, log));
                    }
                    else {
                        // existing bundle, update it
                        Version currentVersion = getVersion(bundle);
                        if (!sourceVersion.equals(currentVersion)) {
                            bundle.update(new BundleInputStream(source.getCurrentEntryStream()));
                            addRollback(new UpdateBundleRunnable(bundle, targetPackage, log));
                        }
                    }
                }
                catch (Exception be) {
                    if (isCancelled()) {
                        return;
                    }
                    throw new DeploymentException(CODE_OTHER_ERROR, "Could not install new bundle '" + name + "' (" + bsn + ")", be);
                }

                if (!bundle.getSymbolicName().equals(bsn)) {
                    throw new DeploymentException(CODE_BUNDLE_NAME_ERROR, "Installed/updated bundle symbolicname (" + bundle.getSymbolicName() + ") do not match what was installed/updated: " + bsn);
                }

                Version targetVersion = getVersion(bundle);
                if (!sourceVersion.equals(targetVersion)) {
                    throw new DeploymentException(CODE_OTHER_ERROR,
                        "Installed/updated bundle version (" + targetVersion + ") do not match what was installed/updated: " + sourceVersion + ", offending bundle = " + bsn);
                }
            }
        }
        catch (IOException e) {
            throw new DeploymentException(CODE_OTHER_ERROR, "Problem while reading stream", e);
        }
    }

    private Version getVersion(Bundle bundle) {
        return Version.parseVersion((String) bundle.getHeaders().get(BUNDLE_VERSION));
    }

    private boolean isLocalizationFile(String name) {
        return name.startsWith("OSGI-INF/l10n/");
    }

    private static class UninstallBundleRunnable extends AbstractAction {
        private final Bundle m_bundle;
        private final LogService m_log;

        public UninstallBundleRunnable(Bundle bundle, LogService log) {
            m_bundle = bundle;
            m_log = log;
        }

        protected void doRun() throws Exception {
            m_bundle.uninstall();
        }

        protected void onFailure(Exception e) {
            m_log.log(LogService.LOG_WARNING, "Could not rollback update of bundle '" + m_bundle.getSymbolicName() + "'", e);
        }
    }

    private static class UpdateBundleRunnable extends AbstractAction {
        private final AbstractDeploymentPackage m_targetPackage;
        private final Bundle m_bundle;
        private final LogService m_log;

        public UpdateBundleRunnable(Bundle bundle, AbstractDeploymentPackage targetPackage, LogService log) {
            m_bundle = bundle;
            m_targetPackage = targetPackage;
            m_log = log;
        }

        protected void doRun() throws Exception {
            InputStream is = null;
            try {
                is = m_targetPackage.getBundleStream(m_bundle.getSymbolicName());
                if (is != null) {
                    m_bundle.update(is);
                }
                else {
                    throw new RuntimeException("Unable to get inputstream for bundle " + m_bundle.getSymbolicName());
                }
            }
            finally {
                closeSilently(is);
            }
        }

        protected void onFailure(Exception e) {
            m_log.log(LogService.LOG_WARNING, "Could not rollback update of bundle '" + m_bundle.getSymbolicName() + "'", e);
        }
    }

    private final class BundleInputStream extends InputStream {
        private final InputStream m_inputStream;

        private BundleInputStream(InputStream jarInputStream) {
            m_inputStream = jarInputStream;
        }

        public int read() throws IOException {
            checkCancel();
            return m_inputStream.read();
        }

        public int read(byte[] buffer) throws IOException {
            checkCancel();
            return m_inputStream.read(buffer);
        }

        public int read(byte[] buffer, int off, int len) throws IOException {
            checkCancel();
            return m_inputStream.read(buffer, off, len);
        }

        private void checkCancel() throws IOException {
            if (isCancelled()) {
                throw new IOException("Stream was cancelled");
            }
        }
    }
}
