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

package org.apache.felix.sigil.eclipse.ui.internal.refactor;

import java.util.LinkedList;
import java.util.List;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

public class RenameActivatorPackageParticipant extends RenameParticipant
{
    private IPackageFragment packageFragment;
    private List<Change> changes = new LinkedList<Change>();

    @Override
    public RefactoringStatus checkConditions(IProgressMonitor monitor,
        CheckConditionsContext ctx) throws OperationCanceledException
    {
        RefactoringStatus status = null;
        if(getArguments().getUpdateReferences()) {
            try
            {
                ISigilProjectModel sigil = SigilCore.create(packageFragment.getJavaProject().getProject());
                RefactorUtil.touch(ctx, sigil);
                
                String oldName = sigil.getBundle().getBundleInfo().getActivator();
                if ( oldName != null ) {
                    String[] parts = splitPackageClass(oldName);
                    if (parts[0].equals(packageFragment.getElementName())) {
                        String newName = getArguments().getNewName() + "." + parts[1];
                        changes.add(new BundleActivatorChange(sigil, oldName, newName));
                        status = RefactoringStatus.createInfoStatus("Updating bundle activator from " + oldName + " to " + newName );
                    }
                }
            }
            catch (CoreException e)
            {
                SigilCore.warn("Failed to update activator", e);
            }
        }
        
        return status;
    }

    private static String[] splitPackageClass(String className)
    {
        String[] parts = new String[2];
        int i = className.lastIndexOf('.');
        if ( i == -1 ) {
            parts[0] = "";
            parts[1] = className;
        }
        else {
            parts[0] = className.substring(0, i);
            parts[1] = className.substring(i + 1);
        }
        return parts;
    }

    @Override
    public Change createChange(IProgressMonitor monitor) throws CoreException,
        OperationCanceledException
    {
        if (changes.isEmpty()) {
            return new NullChange();
        }
        else 
        {
            CompositeChange ret = new CompositeChange("Export-Package update");
            
            ret.addAll(changes.toArray(new Change[changes.size()]));
            
            return ret;
        }
    }

    @Override
    public String getName()
    {        
        return "Sigil Rename Activator Package Participant";
    }

    @Override
    protected boolean initialize(Object element)
    {
        packageFragment = (IPackageFragment) element;
        return true;
    }

}
