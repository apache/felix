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

import java.util.HashMap;

import org.apache.felix.sigil.common.config.IRepositoryConfig;
import org.apache.felix.sigil.common.repository.IRepositoryManager;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.internal.model.project.SigilProject;
import org.apache.felix.sigil.eclipse.internal.repository.manager.EclipseRepositoryManager;
import org.apache.felix.sigil.eclipse.internal.repository.manager.IRepositoryMap;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public class SigilProjectManager
{
    private static HashMap<IProject, ISigilProjectModel> projects = new HashMap<IProject, ISigilProjectModel>();
    private static HashMap<ISigilProjectModel, EclipseRepositoryManager> repositoryManagers = new HashMap<ISigilProjectModel, EclipseRepositoryManager>();

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
            if ( model != null ) {
                EclipseRepositoryManager manager = repositoryManagers.remove(model);
                manager.destroy();           
            }            
        }
    }

    /**
     * @param model
     * @param repositoryMap 
     * @throws CoreException 
     */
    public IRepositoryManager getRepositoryManager(ISigilProjectModel model, IRepositoryMap repositoryMap) throws CoreException
    {
        synchronized( projects ) {
            EclipseRepositoryManager manager = repositoryManagers.get(model);
            
            if ( manager == null ) {
                IRepositoryConfig config = model.getRepositoryConfig();
                
                manager = new EclipseRepositoryManager(config, repositoryMap);
                
                repositoryManagers.put(model, manager);
            }
            
            return manager;
        }
    }
}
