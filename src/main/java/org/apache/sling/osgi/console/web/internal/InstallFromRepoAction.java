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
package org.apache.sling.osgi.console.web.internal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.osgi.assembly.installer.Installer;
import org.apache.sling.osgi.assembly.installer.InstallerException;
import org.apache.sling.osgi.assembly.installer.InstallerService;
import org.apache.sling.osgi.assembly.installer.VersionRange;
import org.apache.sling.osgi.console.web.Action;
import org.osgi.service.log.LogService;

/**
 * The <code>InstallFromRepoAction</code> TODO
 *
 * @scr.component metatype="false"
 * @scr.service
 */
public class InstallFromRepoAction implements Action {

    public static final String NAME = "installFromOBR";

    /** @scr.reference */
    private LogService log;

    /** @scr.reference */
    private InstallerService installerService;

    public String getName() {
        return NAME;
    }

    public String getLabel() {
        return NAME;
    }

   /* (non-Javadoc)
     * @see org.apache.sling.manager.web.internal.Action#performAction(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public boolean performAction(HttpServletRequest request,
            HttpServletResponse response)  {

        // check whether we have to do something
        String[] bundles = request.getParameterValues("bundle");
        if (bundles == null || bundles.length == 0) {
            this.log.log(LogService.LOG_INFO, "No resources to deploy");
            return true;
        }

        Installer installer = this.installerService.getInstaller();

        // prepare the deployment
        for (int i=0; i < bundles.length; i++) {
            String bundle = bundles[i];
            int comma = bundle.indexOf(',');
            String name = (comma > 0) ? bundle.substring(0, comma) : bundle;
            String version = (comma < bundle.length()-1) ? bundle.substring(comma+1) : null;

            if (name.length() > 0) {
                // no name, ignore this one
                VersionRange versionRange = new VersionRange(version);
                installer.addBundle(name, versionRange, -1);
            }
        }

        // check whether the "deploystart" button was clicked
        boolean start = request.getParameter("deploystart") != null;

        try {
            installer.install(start);
        } catch (InstallerException ie) {
            Throwable cause = (ie.getCause() != null) ? ie.getCause() : ie;
            this.log.log(LogService.LOG_ERROR, "Cannot install bundles", cause);
        } finally {
            installer.dispose();
        }

        // redirect to bundle list
        return true;
    }

    protected void bindLog(LogService logService) {
        this.log = logService;
    }

    protected void unbindLog(LogService logService) {
        this.log = null;
    }

    protected void bindInstallerService(InstallerService installerService) {
        this.installerService = installerService;
    }

    protected void unbindInstallerService(InstallerService installerService) {
        this.installerService = null;
    }
}
