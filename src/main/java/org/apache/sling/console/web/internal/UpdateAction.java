/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.console.web.internal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.assembly.installer.Installer;
import org.apache.sling.assembly.installer.InstallerException;
import org.apache.sling.assembly.installer.InstallerService;
import org.apache.sling.assembly.installer.VersionRange;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * The <code>UpdateAction</code> TODO
 *
 * @scr.component metatype="false"
 * @scr.reference name="log" interface="org.osgi.service.log.LogService"
 * @scr.service
 */
public class UpdateAction extends BundleAction {

    public static final String NAME = "update";

    public static final String LABEL = "Update";

    /** @scr.reference */
    private InstallerService installerService;

    public String getName() {
        return NAME;
    }

    public String getLabel() {
        return LABEL;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.manager.web.internal.internal.Action#performAction(javax.servlet.http.HttpServletRequest)
     */
    public boolean performAction(HttpServletRequest request,
            HttpServletResponse response) {

        long bundleId = this.getBundleId(request);
        if (bundleId > 0) { // cannot stop system bundle !!
            Bundle bundle = this.getBundleContext().getBundle(bundleId);
            if (bundle != null) {
                try {
                    this.updateFromRepo(bundle);
                } catch (Throwable t) {
                    this.log(bundle, "Uncaught Problem", t);
                }

            }
        }

        return true;
    }

    private void updateFromRepo(final Bundle bundle) {

        final String name = bundle.getSymbolicName();
        final String version = (String) bundle.getHeaders().get(
            Constants.BUNDLE_VERSION);

        // the name is required, otherwise we can do nothing
        if (name == null) {
            return;
        }
        
        // TODO: Should be restrict to same major.micro ??

        Thread t = new Thread("Background Update") {
            public void run() {
                // wait some time for the request to settle
                try {
                    sleep(500L);
                } catch (InterruptedException ie) {
                    // don't care
                }

                Installer installer = UpdateAction.this.installerService.getInstaller();
                installer.addBundle(name, new VersionRange(version), -1);
                try {
                    installer.install(false);
                } catch (InstallerException ie) {
                    Throwable cause = (ie.getCause() != null) ? ie.getCause() : ie;
                    UpdateAction.this.log(bundle, "Cannot update", cause);
                } finally {
                    installer.dispose();
                }
            }
        };

        t.setDaemon(true); // make a daemon thread (detach from current thread)
        t.start();
    }

    protected void bindInstallerService(InstallerService installerService) {
        this.installerService = installerService;
    }

    protected void unbindInstallerService(InstallerService installerService) {
        this.installerService = null;
    }
}
