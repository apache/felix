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

import java.io.InputStream;

import org.apache.felix.deploymentadmin.AbstractDeploymentPackage;
import org.apache.felix.deploymentadmin.BundleInfoImpl;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

/**
 * Command that uninstalls all bundles, if rolled back the bundles are restored.
 */
public class DropAllBundlesCommand extends Command {

    protected void doExecute(DeploymentSessionImpl session) throws Exception {
        AbstractDeploymentPackage target = session.getTargetAbstractDeploymentPackage();
        LogService log = session.getLog();

        BundleInfoImpl[] orderedTargetBundles = target.getOrderedBundleInfos();
        for (int i = orderedTargetBundles.length - 1; i >= 0; i--) {
            BundleInfoImpl bundleInfo = orderedTargetBundles[i];
            // stale bundle, save a copy for rolling back and uninstall it
            String symbolicName = bundleInfo.getSymbolicName();
            try {
                Bundle bundle = target.getBundle(symbolicName);
                if (bundle != null) {
                    bundle.uninstall();
                    addRollback(new InstallBundleRunnable(bundle, target, log));
                }
            }
            catch (Exception be) {
                log.log(LogService.LOG_WARNING, "Bundle '" + symbolicName + "' could not be uninstalled", be);
            }
        }
    }

    private static class InstallBundleRunnable extends AbstractAction {
        private final AbstractDeploymentPackage m_target;
        private final Bundle m_bundle;
        private final LogService m_log;

        public InstallBundleRunnable(Bundle bundle, AbstractDeploymentPackage target, LogService log) {
            m_bundle = bundle;
            m_target = target;
            m_log = log;
        }

        protected void doRun() throws Exception {
            InputStream is = null;
            try {
                is = m_target.getBundleStream(m_bundle.getSymbolicName());
                if (is != null) {
                    m_bundle.update(is);
                }
                else {
                    throw new RuntimeException("Unable to get inputstream for bundle '" + m_bundle.getSymbolicName() + "'");
                }
            }
            finally {
                closeSilently(is);
            }
        }

        protected void onFailure(Exception e) {
            m_log.log(LogService.LOG_WARNING, "Could not rollback uninstallation of bundle '" + m_bundle.getSymbolicName() + "'", e);
        }
    }
}
