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

package org.apache.felix.sigil.eclipse.ui.wizard.project;

import org.apache.felix.sigil.common.osgi.VersionTable;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.osgi.framework.Version;

/**
 * @author dave
 *
 */
public class SigilProjectWizardFirstPage extends WizardNewProjectCreationPage
{

    private volatile String bsn;
    private volatile String description;
    private volatile Version version;
    private volatile String vendor;
    private volatile String name;

    private Button customBSN;
    private Text txtBSN;
    private Text txtDescription;
    private Text txtVersion;
    private Text txtVendor;
    private Text txtName;
    private boolean initialized;

    public SigilProjectWizardFirstPage()
    {
        super("newSigilProjectPage");
        setTitle("Sigil Project");
        setDescription("Create a new Sigil project");
        setImageDescriptor(ImageDescriptor.createFromFile(
            SigilProjectWizardFirstPage.class, "/icons/logo64x64.gif"));
    }

    public boolean isInWorkspace()
    {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        IPath defaultDefaultLocation = workspace.getRoot().getLocation();

        return defaultDefaultLocation.isPrefixOf(getLocationPath());
    }

    @Override
    public boolean isPageComplete()
    {
        boolean result = super.isPageComplete();
        return result;
    }

    @Override
    public void createControl(Composite parent)
    {
        // Create controls
        super.createControl(parent);
        Composite control = (Composite) getControl();

        Group grpProjectSettings = buildComponents(control);
        setDefaultValues();
        initListeners();
        doLayout(control, grpProjectSettings);
        initialized = true;
    }

    private Group buildComponents(Composite control)
    {
        Group grpProjectSettings = new Group(control, SWT.NONE);
        grpProjectSettings.setText("OSGi Bundle Settings");

        new Label(grpProjectSettings, SWT.NONE).setText("Symbolic Name:");
        txtBSN = new Text(grpProjectSettings, SWT.BORDER);
        txtBSN.setEnabled(false);
        customBSN = new Button(grpProjectSettings, SWT.CHECK | SWT.RIGHT);
        customBSN.setText("Custom");

        new Label(grpProjectSettings, SWT.NONE).setText("Version:");
        txtVersion = new Text(grpProjectSettings, SWT.BORDER);

        new Label(grpProjectSettings, SWT.NONE).setText("Name:");
        txtName = new Text(grpProjectSettings, SWT.BORDER);

        new Label(grpProjectSettings, SWT.NONE).setText("Description:");
        txtDescription = new Text(grpProjectSettings, SWT.BORDER);

        new Label(grpProjectSettings, SWT.NONE).setText("Provider:");
        txtVendor = new Text(grpProjectSettings, SWT.BORDER);

        decorateComponents();
        
        return grpProjectSettings;
    }

    /**
     * 
     */
    private void decorateComponents()
    {
        FieldDecoration infoDecor = FieldDecorationRegistry.getDefault().getFieldDecoration(
            FieldDecorationRegistry.DEC_INFORMATION);

        ControlDecoration txtBSNDecor = new ControlDecoration(txtBSN, SWT.LEFT
            | SWT.CENTER);
        txtBSNDecor.setImage(infoDecor.getImage());
        txtBSNDecor.setDescriptionText("The unique name of this bundle - should follow reverse domain name pattern");
        
        ControlDecoration txtVersionDecor = new ControlDecoration(txtVersion, SWT.LEFT
            | SWT.CENTER);
        txtVersionDecor.setImage(infoDecor.getImage());
        txtVersionDecor.setDescriptionText("The version of this bundle");
        
        ControlDecoration txtNameDecor = new ControlDecoration(txtName, SWT.LEFT
            | SWT.CENTER);
        txtNameDecor.setImage(infoDecor.getImage());
        txtNameDecor.setDescriptionText("Defines a human-readable name for the bundle");

        ControlDecoration txtDescDecor = new ControlDecoration(txtDescription, SWT.LEFT
            | SWT.CENTER);
        txtDescDecor.setImage(infoDecor.getImage());
        txtDescDecor.setDescriptionText("Defines a short human-readable description for the bundle");

        ControlDecoration txtVendorDecor = new ControlDecoration(txtVendor, SWT.LEFT
            | SWT.CENTER);
        txtVendorDecor.setImage(infoDecor.getImage());
        txtVendorDecor.setDescriptionText("The name of the company, organisation or individual providing the bundle");
    }

    private void setDefaultValues()
    {
        version = VersionTable.getVersion(1, 0, 0);
        txtVersion.setText(version.toString());
    }

    private void initListeners()
    {
        // Add listeners
        ModifyListener txtModListener = new ModifyListener()
        {
            public void modifyText(ModifyEvent e)
            {
                validatePage();
            }
        };

        customBSN.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                txtBSN.setEnabled(customBSN.getSelection());
                validatePage();
            }
        });
        
        txtBSN.addModifyListener(txtModListener);
        txtDescription.addModifyListener(txtModListener);
        txtVersion.addModifyListener(txtModListener);
        txtVendor.addModifyListener(txtModListener);
        txtName.addModifyListener(txtModListener);
    }

    private void doLayout(Composite control, Group grpProjectSettings)
    {
        control.setLayout(new GridLayout());
        grpProjectSettings.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        grpProjectSettings.setLayout(new GridLayout(3, false));
        txtBSN.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        customBSN.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
        txtDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        txtVersion.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        txtVendor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
    }

    @Override
    protected boolean validatePage()
    {
        readValues();
        if ( super.validatePage() ) {
            if (getBundleSymbolicName() == null)
            {
                setErrorMessage("Invalid bundle symbolic name");
                setPageComplete(false);
                return false;
            }
    
            if (getVersion() == null)
            {
                setErrorMessage("Invalid version");
                setPageComplete(false);
                return false;
            }
    
            // ok all must be fine...
            setErrorMessage(null);
            setPageComplete(true);
        }
        
        return true;
    }

    private static final ThreadLocal<Object> reentrant = new ThreadLocal<Object>();
    private static final Object REENTRANCE = new Object();
    
    /**
     * 
     */
    private void readValues()
    {
        if ( initialized && reentrant.get() != REENTRANCE ) {
            if ( customBSN.getSelection() ) {
                bsn = nullIfEmpty(txtBSN.getText());
            }
            else {
                bsn = getProjectName();
                reentrant.set(REENTRANCE);
                txtBSN.setText(bsn);
                reentrant.set(null);
            }
            
            description = nullIfEmpty(txtDescription.getText());
            name = nullIfEmpty(txtName.getText());
            vendor = nullIfEmpty(txtVendor.getText());
            version = nullIfInvalidVersion(txtVersion.getText());
        }
    }

    public Version getVersion()
    {
        return version;
    }

    public String getVendor()
    {
        return vendor;
    }

    public String getDescription()
    {
        return description;
    }

    public String getBundleSymbolicName()
    {
        return bsn;
    }

    public String getName()
    {
        return name;
    }

    // utility methods
    private static String nullIfEmpty(String str)
    {
        return str == null ? null : (str.trim().length() == 0 ? null : str.trim());
    }

    private static Version nullIfInvalidVersion(String text)
    {
        Version version = null;
        try
        {
            String v = nullIfEmpty(text);
            if (v != null)
            {
                version = VersionTable.getVersion(v);
            }
        }
        catch (IllegalArgumentException e)
        {
            // fine version is invalid - handled by validateSettings
        }

        return version;
    }

}
