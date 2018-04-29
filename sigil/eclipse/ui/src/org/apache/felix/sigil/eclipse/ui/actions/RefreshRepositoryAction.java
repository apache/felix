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

package org.apache.felix.sigil.eclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.sigil.common.repository.IBundleRepository;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel;
import org.apache.felix.sigil.eclipse.ui.SigilUI;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class RefreshRepositoryAction extends DisplayAction
{
    private final List<IBundleRepository> repositories;

    public RefreshRepositoryAction(IBundleRepository...repositories) {
        this.repositories = Arrays.asList(repositories);
        
    }
    public RefreshRepositoryAction(IRepositoryModel... model)
    {
        super("Refresh repository");
        ArrayList<IBundleRepository> reps = new ArrayList<IBundleRepository>(model.length);
        for (IBundleRepository b : SigilCore.getGlobalRepositoryManager().getRepositories())
        {
            for (IRepositoryModel m : model)
            {
                if (b.getId().equals(m.getId()))
                {
                    reps.add(b);
                }
            }
        }
        this.repositories = reps;
    }

    @Override
    public void run()
    {
        WorkspaceModifyOperation op = new WorkspaceModifyOperation()
        {
            @Override
            protected void execute(IProgressMonitor monitor) throws CoreException,
                InvocationTargetException, InterruptedException
            {
                if ( !repositories.isEmpty() ) {
                    for (IBundleRepository rep : repositories ) {
                        rep.refresh();                        
                    }
                    
                    List<ISigilProjectModel> projects = SigilCore.getRoot().getProjects();
                    SubMonitor sub = SubMonitor.convert(monitor, projects.size() * 10);
                    for (ISigilProjectModel p : projects)
                    {
                        p.resetClasspath(sub.newChild(10), false);
                    }
                }
            }
        };

        SigilUI.runWorkspaceOperation(op, null);
    }
}
