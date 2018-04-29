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

import org.apache.felix.sigil.common.model.osgi.IPackageImport;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class ImportPackageChange extends Change
{

    private final ISigilProjectModel sigil;
    private final IPackageImport oldImport;
    private final IPackageImport newImport;

    public ImportPackageChange(ISigilProjectModel sigil, IPackageImport oldImport, IPackageImport newImport)
    {
        this.sigil = sigil;
        this.oldImport = oldImport;
        this.newImport = newImport;
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
        return "Import package update";
    }

    @Override
    public void initializeValidationData(IProgressMonitor arg0)
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
        if (oldImport != null)
        {
            sigil.getBundle().getBundleInfo().removeImport(oldImport);
        }

        if (newImport != null)
        {
            sigil.getBundle().getBundleInfo().addImport(newImport);
        }

        sigil.save(progress);

        return new ImportPackageChange(sigil, newImport, oldImport);
    }

}
