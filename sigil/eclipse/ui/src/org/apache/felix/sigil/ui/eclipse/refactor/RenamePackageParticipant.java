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

package org.apache.felix.sigil.ui.eclipse.refactor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.model.util.ModelHelper;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
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
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;

public class RenamePackageParticipant extends RenameParticipant
{
    private IPackageFragment packageFragment;
    private List<Change> changes = new LinkedList<Change>();

    @Override
    public RefactoringStatus checkConditions(IProgressMonitor pm,
        CheckConditionsContext context) throws OperationCanceledException
    {   
        RefactoringStatus status = new RefactoringStatus();
        
        if (getArguments().getUpdateReferences()) {
            try
            {
                ISigilProjectModel sigil = SigilCore.create(packageFragment.getJavaProject().getProject());
                final String packageName = packageFragment.getElementName();
                
                SigilCore.log("Rename checkConditions " + packageName);
                
                IPackageExport oldExport = ModelHelper.findExport(sigil, packageName);
                
                if (oldExport != null) {
                    // record change to check if out of sync...
                    touch(context, sigil);
                                
                    status = RefactoringStatus.createWarningStatus("Package " + packageName + " is exported. Renaming this package may effect bundles outside of this workspace");
                    SigilCore.log("Export Package " + packageName + " renamed to " + getArguments().getNewName());
    
                    IPackageExport newExport = ModelElementFactory.getInstance().newModelElement(IPackageExport.class);
                    newExport.setPackageName(getArguments().getNewName());
                    newExport.setVersion(oldExport.getVersion());
    
                    changes.add(new ExportPackageChange(sigil, oldExport, newExport));                    
    
                    for ( ISigilProjectModel other : SigilCore.getRoot().getProjects() ) {
                        if ( !sigil.equals(other) ) {
                            // record change to check if out of sync...
                            touch(context, other);
                        }
                        changes.add(createImportChange(status, other, oldExport, newExport));
                    }
                }
            }
            catch (CoreException e)
            {
                SigilCore.warn("Failed to create export package refactor", e);
                throw new OperationCanceledException("Failed to create export package refactor");
            }
        }        
        return status;
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException,
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
        return "Sigil export package rename participant";
    }

    @Override
    protected boolean initialize(Object element)
    {
        this.packageFragment = (IPackageFragment) element;
        return true;
    }

    private static final void touch(CheckConditionsContext context, ISigilProjectModel sigil)
    {
        ResourceChangeChecker checker = (ResourceChangeChecker) context.getChecker(ResourceChangeChecker.class);
        IResourceChangeDescriptionFactory deltaFactory= checker.getDeltaFactory();        
        IFile file = sigil.getProject().getFile(SigilCore.SIGIL_PROJECT_FILE);
        deltaFactory.change(file);
    }

    private Change createImportChange(RefactoringStatus status, ISigilProjectModel sigil, IPackageExport oldExport, IPackageExport newExport)
    {
        IBundleModelElement info = sigil.getBundle().getBundleInfo();
        Collection<IPackageImport> imports = info.getImports();
        
        for (IPackageImport oldImport : imports) {
            if (oldImport.accepts(oldExport)) {
                IPackageImport newImport = ModelElementFactory.getInstance().newModelElement(IPackageImport.class);
                
                newImport.setPackageName(newExport.getPackageName());
                newImport.setVersions(oldImport.getVersions());
                
                status.addInfo(buildImportChangeMsg(sigil, oldImport, newImport));
                
                return new ImportPackageChange(sigil, oldImport, newImport);
            }
        }
        
        // ok no change
        return new NullChange();
    }

    private static final String buildImportChangeMsg(ISigilProjectModel sigil,
        IPackageImport oldImport, IPackageImport newImport)
    {
        return "Updating import " + oldImport.getPackageName() + " version " + oldImport.getVersions() +
            " to " + newImport.getPackageName() + " version " + newImport.getVersions() +
            " in project " + sigil.getSymbolicName() + " version " + sigil.getVersion();
    }

}
