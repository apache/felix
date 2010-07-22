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

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class ExportPackageChange extends Change
{

    private final ISigilProjectModel sigil;
    private final IPackageExport oldExport;
    private final IPackageExport newExport;

    public ExportPackageChange(ISigilProjectModel sigil, IPackageExport oldExport, IPackageExport newExport)
    {
        this.sigil = sigil;
        this.oldExport = oldExport;
        this.newExport = newExport;
    }

    @Override
    public Object getModifiedElement()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName()
    {
        return "Export package update";
    }

    @Override
    public void initializeValidationData(IProgressMonitor progress)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor progress) throws CoreException,
        OperationCanceledException
    {
        // TODO check project is synchronized
        return new RefactoringStatus();
    }

    @Override
    public Change perform(IProgressMonitor progress) throws CoreException
    {   
        if (oldExport != null) {
            sigil.getBundle().getBundleInfo().removeChild(oldExport);
        }
        
        if (newExport != null) {
            sigil.getBundle().getBundleInfo().addExport(newExport);
        }
        
        sigil.save(progress);
        
        return new ExportPackageChange(sigil, newExport, oldExport);
    }

}
