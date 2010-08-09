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

import java.util.List;

import org.apache.felix.sigil.common.model.ModelElementFactory;
import org.apache.felix.sigil.common.model.osgi.IPackageExport;
import org.apache.felix.sigil.common.model.osgi.IPackageImport;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.model.util.ModelHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;

/**
 * @author dave
 *
 */
class RefactorUtil
{
    static final void touch(CheckConditionsContext context, ISigilProjectModel sigil)
    {
        ResourceChangeChecker checker = (ResourceChangeChecker) context.getChecker(ResourceChangeChecker.class);
        IResourceChangeDescriptionFactory deltaFactory = checker.getDeltaFactory();
        IFile file = sigil.getProject().getFile(SigilCore.SIGIL_PROJECT_FILE);
        deltaFactory.change(file);
    }    
    
    static void createNewImport(RefactoringStatus status, List<Change> changes, ISigilProjectModel project,
        IPackageExport export)
    {
        IPackageImport newImport = ModelElementFactory.getInstance().newModelElement(
            IPackageImport.class);
        newImport.setPackageName(export.getPackageName());
        newImport.setVersions(ModelHelper.getDefaultRange(export.getVersion()));

        status.addInfo("Creating new import in " + project.getSymbolicName());
        changes.add(new ImportPackageChange(project, null, newImport));
    }

    static IPackageExport createNewExport(RefactoringStatus status,
        List<Change> changes, ISigilProjectModel project, String packageName)
    {
        IPackageExport newExport = ModelElementFactory.getInstance().newModelElement(
            IPackageExport.class);
        newExport.setPackageName(packageName);
        // new export inherits project version by default
        newExport.setVersion(project.getVersion());

        status.addInfo("Creating new export " + packageName + " in "
            + project.getSymbolicName());
        changes.add(new ExportPackageChange(project, null, newExport));
        return newExport;
    }    
}
