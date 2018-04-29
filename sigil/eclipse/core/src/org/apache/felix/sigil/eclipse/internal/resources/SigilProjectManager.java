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
package org.apache.felix.sigil.eclipse.internal.resources;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.felix.sigil.common.repository.IBundleRepository;
import org.apache.felix.sigil.common.repository.IRepositoryManager;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.internal.model.project.SigilProject;
import org.apache.felix.sigil.eclipse.internal.repository.manager.ProjectRepositoryManager;
import org.apache.felix.sigil.eclipse.internal.repository.manager.IRepositoryCache;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public class SigilProjectManager
{
    private final HashMap<IProject, ISigilProjectModel> projects = new HashMap<IProject, ISigilProjectModel>();
    private final HashMap<ISigilProjectModel, ProjectRepositoryManager> repositoryManagers = new HashMap<ISigilProjectModel, ProjectRepositoryManager>();    
    private final IRepositoryCache repositoryCache;
    
    public SigilProjectManager(IRepositoryCache repositoryCache) {
        this.repositoryCache = repositoryCache;
    }

    public ISigilProjectModel getSigilProject(IProject project) throws CoreException
    {
        if (project.hasNature(SigilCore.NATURE_ID))
        {
            ISigilProjectModel p = null;
            synchronized (projects)
            {
                p = projects.get(project);
                if (p == null)
                {
                    p = new SigilProject(project);
                    projects.put(project, p);
                }
            }
            return p;
        }
        else
        {
            throw SigilCore.newCoreException("Project " + project.getName()
                + " is not a sigil project", null);
        }
    }

    public void flushSigilProject(IProject project)
    {
        synchronized (projects)
        {
            ISigilProjectModel model = projects.remove(project);
            ArrayList<String> flush = new ArrayList<String>();
            if ( model != null ) {
                ProjectRepositoryManager manager = repositoryManagers.remove(model);
                manager.destroy();
                for(IBundleRepository rep : manager.getRepositories()) {
                    flush.add(rep.getId());
                }
            }
            
            for (ProjectRepositoryManager manager : repositoryManagers.values()) {
                for(IBundleRepository rep : manager.getRepositories()) {
                    flush.remove(rep.getId());
                }
            }
            
            repositoryCache.discard(flush);
        }
    }

    /**
     * @param model
     * @param repositoryCache 
     * @throws CoreException 
     */
    public IRepositoryManager getRepositoryManager(ISigilProjectModel model) throws CoreException
    {
        synchronized( projects ) {
            ProjectRepositoryManager manager = repositoryManagers.get(model);
            
            if ( manager == null ) {
                manager = new ProjectRepositoryManager(model, repositoryCache);                
                repositoryManagers.put(model, manager);
            }
            
            return manager;
        }
    }

    /**
     * 
     */
    public synchronized void destroy()
    {
        for(ProjectRepositoryManager man : repositoryManagers.values()) {
            man.destroy();
        }
    }
}
