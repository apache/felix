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

package org.apache.felix.sigil.eclipse.ui.internal.editors.project;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.sigil.common.config.Resource;
import org.apache.felix.sigil.common.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.ui.SigilUI;
import org.apache.felix.sigil.eclipse.ui.internal.form.SigilPage;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * @author dave
 *
 */
public class ResourceBuildSection extends AbstractResourceSection implements ICheckStateListener, IResourceChangeListener, IPropertyChangeListener
{

    private ExcludedResourcesFilter resourcesFilter;

    /**
     * @param page
     * @param parent
     * @param project
     * @throws CoreException 
     */
    public ResourceBuildSection(SigilPage page, Composite parent, ISigilProjectModel project) throws CoreException
    {
        super(page, parent, project);
    }

    @Override
    protected void createSection(Section section, FormToolkit toolkit)
    {
        setTitle("Resources");

        Composite body = createTableWrapBody(1, toolkit);

        toolkit.createLabel(body, "Specify which resources are included in the bundle.");

        tree = toolkit.createTree(body, SWT.CHECK | SWT.BORDER);
        Link link = new Link(body, SWT.WRAP);
        link.setText("Some resources may be filtered according to preferences. <a href=\"excludedResourcePrefs\">Click here</a> to edit the list of exclusions.");

        TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
        data.heightHint = 200;
        tree.setLayoutData(data);

        viewer = new CheckboxTreeViewer(tree);
        IProject base = getProjectModel().getProject();
        viewer.setContentProvider(new BaseWorkbenchContentProvider());
        viewer.setLabelProvider(new WorkbenchLabelProvider());
        viewer.addCheckStateListener(this);
        resourcesFilter = new ExcludedResourcesFilter();
        viewer.addFilter(resourcesFilter);
        viewer.setInput(base);

        link.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
                if ("excludedResourcePrefs".equals(event.text))
                {
                    PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
                        getPage().getEditorSite().getShell(),
                        SigilCore.EXCLUDED_RESOURCES_PREFERENCES_ID, null, null);
                    dialog.open();
                }
            }
        });

        SigilCore.getDefault().getPreferenceStore().addPropertyChangeListener(this);

        startWorkspaceListener(base.getWorkspace());
    }

    @Override
    public void commit(boolean onSave)
    {
        ISigilBundle bundle = getProjectModel().getBundle();

        bundle.clearSourcePaths();

        try
        {
            SigilUI.runInUISync(new Callable<Void>()
            {
                public Void call() throws Exception
                {
                    for (Object o : viewer.getCheckedElements())
                    {
                        if (!viewer.getGrayed(o))
                        {
                            IResource r = (IResource) o;

                            getProjectModel().getBundle().addSourcePath(toBldResource(r));
                        }
                    }

                    return null;
                }
            });

            super.commit(onSave);
        }
        catch (Exception e)
        {
            SigilCore.warn("Failed to update resource", e);
        }
    }

    @Override
    protected void refreshSelections()
    {
        // zero the state
        for (Resource path : getProjectModel().getBundle().getSourcePaths())
        {
            IResource r = findResource(new Path(path.getLocalFile()));
            if (r != null)
            {
                viewer.expandToLevel(r, 0);
                viewer.setChecked(r, true);
                viewer.setGrayed(r, false);
                handleStateChanged(r, true, false, false);
            }
            else
            {
                SigilCore.error("Unknown path " + path);
            }
        }
    }

    @Override
    protected void syncResourceModel(IResource element, boolean checked)
    {
        try
        {
            Resource resource = toBldResource(element);

            if (checked)
            {
                getProjectModel().getBundle().addSourcePath(resource);
            }
            else
            {
                getProjectModel().getBundle().removeSourcePath(resource);
            }

            markDirty();
        }
        catch (CoreException e)
        {
            SigilCore.warn("Failed to sync resource " + element, e);
        }
    }

    /**
     * @param element
     * @return
     * @throws CoreException 
     */
    private Resource toBldResource(IResource element) throws CoreException
    {
        return getProjectModel().getBldProject().newResource(
            element.getProjectRelativePath().toString());
    }

    private AtomicBoolean disposed = new AtomicBoolean();

    @Override
    public void dispose()
    {
        disposed.set(true);
        SigilCore.getDefault().getPreferenceStore().removePropertyChangeListener(this);
        super.dispose();
    }

    public void propertyChange(PropertyChangeEvent event)
    {
        if (!disposed.get())
        {
            resourcesFilter.loadExclusions();
            try
            {
                viewer.refresh();
            }
            catch (SWTException e)
            {
                // can happen on dispose
            }
        }
    }
}
