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

package org.apache.felix.sigil.eclipse.job;


import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.ICapabilityModelElement;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.IModelWalker;
import org.apache.felix.sigil.model.IRequirementModelElement;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;


public class ResolveProjectsJob extends WorkspaceJob
{
    private final Collection<ISigilProjectModel> sigilProjects;
    private final LinkedList<ICapabilityModelElement> capabilities;
    
    public ResolveProjectsJob( IWorkspace workspace )
    {
        super( "Resolving Sigil projects" );
        setRule( workspace.getRoot() );
        sigilProjects = SigilCore.getRoot().getProjects();
        capabilities = null;
    }

    public ResolveProjectsJob(IWorkspace workspace, LinkedList<ICapabilityModelElement> capabilities)
    {
        super( "Resolving Sigil projects" );
        this.capabilities = capabilities;
        setRule( workspace.getRoot() );
        sigilProjects = SigilCore.getRoot().getProjects();
    }

    public ResolveProjectsJob(ISigilProjectModel project)
    {
        super( "Resolving Sigil project" );
        setRule( project.getProject().getWorkspace().getRoot() );
        sigilProjects = Collections.singleton(project);
        capabilities = null;
    }

    @Override
    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
    {
        MultiStatus status = new MultiStatus( SigilCore.PLUGIN_ID, 0, "Error resolving Sigil projects", null );

        boolean flush = sigilProjects.size() > 0;
        
        for ( ISigilProjectModel sigilProject : sigilProjects )
        {
            try
            {
                if ( isDependent(sigilProject) ) { 
                    if ( flush) sigilProject.flushDependencyState();
                    sigilProject.rebuildDependencies(monitor);
                }
            }
            catch ( CoreException e )
            {
                status.add( e.getStatus() );
            }
        }

        return status;
    }

    private boolean isDependent(ISigilProjectModel sigilProject)
    {
        if ( capabilities == null ) {
            return true;
        }
        else {
            ISigilBundle b = sigilProject.getBundle();
            if ( b == null ) {
                // sigil project deleted can't be a dependent
                return false;
            }
            else {
                final boolean[] dep = new boolean[1];
                
                b.visit(new IModelWalker()
                {
                    public boolean visit(IModelElement element)
                    {
                        if (element instanceof IRequirementModelElement) {
                            IRequirementModelElement r = (IRequirementModelElement) element;
        
                            for (ICapabilityModelElement c : capabilities) {
                                if( r.accepts(c)) {
                                    dep[0] = true;
                                    break;
                                }
                            }
                        }
                        // found a dependency stop walking
                        return !dep[0];
                    }
                });
                
                return dep[0];
            }
        }
    }
}
