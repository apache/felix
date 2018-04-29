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
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

public class RenameActivatorParticipant extends RenameParticipant
{
    private ICompilationUnit compilationUnit;
    private List<Change> changes = new LinkedList<Change>();

    @Override
    public RefactoringStatus checkConditions(IProgressMonitor monitor,
        CheckConditionsContext ctx) throws OperationCanceledException
    {
        RefactoringStatus status = null;
        if (getArguments().getUpdateReferences())
        {
            IPackageFragment pf = (IPackageFragment) compilationUnit.getAncestor(IJavaModel.PACKAGE_FRAGMENT);
            String oldName = qualifiedName(pf, compilationUnit.getElementName());
            try
            {
                ISigilProjectModel sigil = SigilCore.create(compilationUnit.getJavaProject().getProject());
                if (oldName.equals(sigil.getBundle().getBundleInfo().getActivator()))
                {
                    String newName = qualifiedName(pf, getArguments().getNewName());

                    RefactorUtil.touch(ctx, sigil);
                    changes.add(new BundleActivatorChange(sigil, oldName, newName));
                    status = RefactoringStatus.createInfoStatus("Updating bundle activator from "
                        + oldName + " to " + newName);
                }
            }
            catch (CoreException e)
            {
                SigilCore.warn("Failed to update activator", e);
            }
        }

        return status;
    }

    /**
     * @param pf 
     * @param compilationUnit2
     * @return
     */
    private static String qualifiedName(IPackageFragment pf, String name)
    {
        name = name.substring(0, name.lastIndexOf('.'));
        return pf.getElementName() + '.' + name;
    }

    @Override
    public Change createChange(IProgressMonitor monitor) throws CoreException,
        OperationCanceledException
    {
        if (changes.isEmpty())
        {
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected boolean initialize(Object element)
    {
        compilationUnit = (ICompilationUnit) element;
        return true;
    }

}
