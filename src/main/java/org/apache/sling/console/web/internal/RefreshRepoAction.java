/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.console.web.internal;

import java.net.URL;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.assembly.installer.BundleRepositoryAdmin;
import org.apache.sling.assembly.installer.InstallerService;
import org.apache.sling.assembly.installer.Repository;
import org.apache.sling.console.web.Action;

/**
 * The <code>InstallFromRepoAction</code> TODO
 *
 * @scr.component metatype="false"
 * @scr.reference name="installer" interface="org.apache.sling.assembly.installer.InstallerService"
 * @scr.service
 */
public class RefreshRepoAction implements Action {

    public static final String NAME = "refreshOBR";
    public static final String PARAM_REPO = "repository";

    private BundleRepositoryAdmin repoAdmin;

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
        
        String repositoryURL = request.getParameter("repository");
        Iterator repos = repoAdmin.getRepositories();
        Repository repo = getRepository(repos, repositoryURL);
        
        URL repoURL = null;
        if (repo != null) {
            repoURL = repo.getURL();
        } else {
            try {
                repoURL = new URL(repositoryURL);
            } catch (Throwable t) {
                // don't care, just ignore
            }
        }
        
        // log.log(LogService.LOG_DEBUG, "Refreshing " + repo.getURL());
        if (repoURL != null) {
            try {
                repoAdmin.addRepository(repoURL);
            } catch (Exception e) {
                // TODO: log.log(LogService.LOG_ERROR, "Cannot refresh Repository " + repo.getURL());
            }
        }
        
        return true;
    }

    //---------- internal -----------------------------------------------------
    
    private Repository getRepository(Iterator repos, String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.length() == 0) {
            return null;
        }
        
        while (repos.hasNext()) {
            Repository repo = (Repository) repos.next();
            if (repositoryUrl.equals(repo.getURL().toString())) {
                return repo;
            }
        }
        
        return null;
    }

    protected void bindInstaller(InstallerService installerService) {
        this.repoAdmin = installerService.getBundleRepositoryAdmin();
    }

    protected void unbindInstaller(InstallerService installerService) {
        this.repoAdmin = null;
    }
}
