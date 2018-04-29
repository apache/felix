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

import org.apache.felix.sigil.common.model.osgi.IPackageExport;
import org.eclipse.ltk.ui.refactoring.ChangePreviewViewerInput;
import org.eclipse.ltk.ui.refactoring.IChangePreviewViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * @author dave
 *
 */
public class ExportChangePreviewViewer implements IChangePreviewViewer
{

    private ViewForm control;
    private Label text;

    /* (non-Javadoc)
     * @see org.eclipse.ltk.ui.refactoring.IChangePreviewViewer#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent)
    {
        control = new ViewForm(parent, SWT.NONE);
        text = new Label(control, SWT.NONE);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.ui.refactoring.IChangePreviewViewer#getControl()
     */
    public Control getControl()
    {
        return control;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.ui.refactoring.IChangePreviewViewer#setInput(org.eclipse.ltk.ui.refactoring.ChangePreviewViewerInput)
     */
    public void setInput(ChangePreviewViewerInput input)
    {
        ExportPackageChange change = (ExportPackageChange) input.getChange();
        StringBuilder buf = new StringBuilder();
        buf.append("Export-Package: \n");
        if ( change.getOldExport() != null ) {
            buf.append("- ");
            appendPackage(change.getOldExport(), buf);
            buf.append("\n");
        }
        
        if ( change.getNewExport() != null ) {
            buf.append("+ ");
            appendPackage(change.getNewExport(), buf);
            buf.append("\n");
        }
        text.setText(buf.toString());
    }

    /**
     * @param pe
     * @param buf
     */
    private void appendPackage(IPackageExport pe, StringBuilder buf)
    {
        buf.append(pe.getPackageName());
        if (pe.getVersion() != null) {
            buf.append(";version=");
            buf.append(pe.getVersion());
        }
    }

}
