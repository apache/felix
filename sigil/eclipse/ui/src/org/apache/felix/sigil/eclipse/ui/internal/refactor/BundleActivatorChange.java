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

import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * @author dave
 *
 */
public class BundleActivatorChange extends Change
{

    private final ISigilProjectModel sigil;
    private final String oldName;
    private final String newName;

    /**
     * @param sigil
     * @param oldName
     * @param newName
     */
    public BundleActivatorChange(ISigilProjectModel sigil, String oldName, String newName)
    {
        this.sigil = sigil;
        this.oldName = oldName;
        this.newName = newName;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.core.refactoring.Change#getModifiedElement()
     */
    @Override
    public Object getModifiedElement()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.core.refactoring.Change#getName()
     */
    @Override
    public String getName()
    {
        return "Bundle Activator Rename";
    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.core.refactoring.Change#initializeValidationData(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void initializeValidationData(IProgressMonitor monitor)
    {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.core.refactoring.Change#isValid(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public RefactoringStatus isValid(IProgressMonitor monitor) throws CoreException,
        OperationCanceledException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.core.refactoring.Change#perform(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public Change perform(IProgressMonitor monitor) throws CoreException
    {
        sigil.getBundle().getBundleInfo().setActivator(newName);
        sigil.save(monitor);
        return new BundleActivatorChange(sigil, newName, oldName);
    }

}
