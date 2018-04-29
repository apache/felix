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
import java.util.Set;

import org.apache.felix.sigil.common.model.ModelElementFactory;
import org.apache.felix.sigil.common.model.osgi.IPackageExport;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.model.util.JavaHelper;
import org.apache.felix.sigil.eclipse.model.util.ModelHelper;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;

public class MovePackageParticipant extends MoveParticipant
{
    private IPackageFragment packageFragment;
    private List<Change> changes = new LinkedList<Change>();

    @Override
    public RefactoringStatus checkConditions(IProgressMonitor monitor,
        CheckConditionsContext context) throws OperationCanceledException
    {
        if (getArguments().getUpdateReferences())
        {
            try
            {
                ISigilProjectModel sourceProject = SigilCore.create(packageFragment.getJavaProject().getProject());
                IPackageFragmentRoot dest = (IPackageFragmentRoot) getArguments().getDestination();
                ISigilProjectModel destProject = SigilCore.create(dest.getJavaProject().getProject());

                RefactoringStatus status = new RefactoringStatus();
                if (!sourceProject.equals(destProject))
                {
                    RefactorUtil.touch(context, sourceProject);
                    RefactorUtil.touch(context, destProject);

                    final String packageName = packageFragment.getElementName();
                    IPackageExport oldExport = ModelHelper.findExport(sourceProject,
                        packageName);

                    if (oldExport != null)
                    {
                        IPackageExport newExport = ModelElementFactory.getInstance().newModelElement(
                            IPackageExport.class);

                        newExport.setPackageName(oldExport.getPackageName());
                        newExport.setVersion(oldExport.getRawVersion());

                        changes.add(new ExportPackageChange(destProject, null, newExport));
                        changes.add(new ExportPackageChange(sourceProject, oldExport,
                            null));

                        status.addWarning("Package " + packageName + " is exported from "
                            + sourceProject.getSymbolicName()
                            + ", this may effect client bundles that use require bundle");
                    }
                    else
                    {
                        SubMonitor sub = SubMonitor.convert(monitor);
                        
                        sub.beginTask("Resolving package users", 200);
                        
                        Set<String> users = JavaHelper.findLocalPackageUsers(
                            sourceProject, packageName, sub.newChild(100));
                        Set<String> dependencies = JavaHelper.findLocalPackageDependencies(
                            sourceProject, packageName, sub.newChild(100));

                        if (users.size() > 0 && dependencies.size() > 0)
                        {
                            status.addWarning("Package " + packageName
                                + " is coupled to " + users + " and " + dependencies
                                + " this may cause a cyclical dependency");
                        }

                        if (users.size() > 0)
                        { // attempt to move an API package
                            IPackageExport newExport = RefactorUtil.createNewExport(status, changes,
                                destProject, packageName);
                            RefactorUtil.createNewImport(status, changes, sourceProject, newExport);
                        }

                        if (dependencies.size() > 0)
                        { // moved an impl package
                            for (String dep : dependencies)
                            {
                                IPackageExport newExport = RefactorUtil.createNewExport(status, changes,
                                    sourceProject, dep);
                                RefactorUtil.createNewImport(status, changes, destProject, newExport);
                            }
                        }
                    }
                }
                return status;
            }
            catch (CoreException e)
            {
                SigilCore.warn("Failed to create move refactor conditions", e);
                throw new OperationCanceledException(e.getMessage());
            }
        }
        else
        {
            return new RefactoringStatus();
        }
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
        return "Sigil package move participant";
    }

    @Override
    protected boolean initialize(Object element)
    {
        this.packageFragment = (IPackageFragment) element;
        return true;
    }

}
