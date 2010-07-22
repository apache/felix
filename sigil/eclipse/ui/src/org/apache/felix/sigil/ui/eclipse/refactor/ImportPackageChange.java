package org.apache.felix.sigil.ui.eclipse.refactor;

import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.osgi.IPackageImport;
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
        if (oldImport!=null) {
            sigil.getBundle().getBundleInfo().removeImport(oldImport);
        }
        
        if (newImport != null) {
            sigil.getBundle().getBundleInfo().addImport(newImport);
        }
        
        sigil.save(progress);
        
        return new ImportPackageChange(sigil, newImport, oldImport);
    }

}
