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

package org.apache.felix.sigil.eclipse.internal.model.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.felix.sigil.common.config.IRepositoryConfig;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryPreferences;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryType;
import org.apache.felix.sigil.eclipse.preferences.PrefsUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

public class RepositoryPreferences implements IRepositoryPreferences
{

    private static final String REPOSITORY = "repository.";
    private static final String REPOSITORY_ORDER = REPOSITORY + "order";
    private static final String INSTANCES = ".instances";
    private static final String NAME = ".name";
    private static final String LOC = ".loc";
    private static final String TIMESTAMP = ".timestamp";

    public List<IRepositoryModel> loadRepositories()
    {
        IPreferenceStore prefs = SigilCore.getDefault().getPreferenceStore();

        ArrayList<IRepositoryModel> repositories = new ArrayList<IRepositoryModel>();

        for (IRepositoryType type : loadRepositoryTypes())
        {
            String typeID = type.getId();

            if (type.isDynamic())
            {
                String instances = prefs.getString(REPOSITORY + typeID + INSTANCES);
                if (instances.trim().length() > 0)
                {
                    for (String instance : instances.split(","))
                    {
                        String key = REPOSITORY + typeID + "." + instance;
                        repositories.add(loadRepository(instance, key, type, prefs));
                    }
                }
            }
            else
            {
                String key = REPOSITORY + typeID;
                repositories.add(loadRepository(typeID, key, type, prefs));
            }

        }
        
        final List<String> order = PrefsUtils.stringToList(prefs.getString(REPOSITORY_ORDER));
        
        Collections.sort(repositories, new Comparator<IRepositoryModel>() {
            public int compare(IRepositoryModel o1, IRepositoryModel o2)
            {
                int i1 = order.indexOf(o1.getId());
                int i2 = order.indexOf(o2.getId());
                
                if ( i1 < i2 ) {
                    return -1;
                }
                else if ( i1 > i2 ) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
            
        });

        return repositories;
    }

    public IRepositoryModel findRepository(String id)
    {
        for (IRepositoryModel model : loadRepositories())
        {
            if (model.getId().equals(id))
            {
                return model;
            }
        }
        return null;
    }

    public void saveRepositories(List<IRepositoryModel> repositories)
        throws CoreException
    {
        IPreferenceStore prefs = getPreferences();

        HashMap<IRepositoryType, List<IRepositoryModel>> mapped = new HashMap<IRepositoryType, List<IRepositoryModel>>(
            repositories.size());

        saveRepositoryPreferences(repositories, mapped);
        createNewEntries(mapped, prefs);
        deleteOldEntries(repositories, prefs);
        // do this last as it is a signal to preferences
        // listeners to read repo config
        setRepositoryOrder(repositories, prefs);
    }

    /**
     * @param repositories
     * @param prefs
     */
    private void setRepositoryOrder(List<IRepositoryModel> repositories,
        IPreferenceStore prefs)
    {        
        ArrayList<String> ids = new ArrayList<String>();
        for(IRepositoryModel model : repositories) {
            ids.add(model.getId());
        }
        prefs.setValue(REPOSITORY_ORDER, PrefsUtils.listToString(ids));
    }
    

    public IRepositoryModel newRepositoryElement(IRepositoryType type)
    {
        String id = UUID.randomUUID().toString();
        RepositoryModel element = new RepositoryModel(id, type);
        element.getProperties().setProperty("id", id);
        return element;
    }

    private IPreferenceStore getPreferences()
    {
        return SigilCore.getDefault().getPreferenceStore();
    }

    private void deleteOldEntries(List<IRepositoryModel> repositories,
        IPreferenceStore prefs)
    {
        for (IRepositoryModel e : loadRepositories())
        {
            if (!repositories.contains(e))
            {
                new File(makeFileName(e)).delete();
                String key = makeKey(e);
                prefs.setToDefault(key + LOC);
                prefs.setToDefault(key + NAME);
            }
        }

        for (IRepositoryType type : loadRepositoryTypes())
        {
            boolean found = false;
            for (IRepositoryModel e : repositories)
            {
                if (e.getType().equals(type))
                {
                    found = true;
                    break;
                }
            }

            if (!found)
            {
                prefs.setToDefault(REPOSITORY + type.getId() + INSTANCES);
            }
        }
    }

    private static void createNewEntries(
        HashMap<IRepositoryType, List<IRepositoryModel>> mapped, IPreferenceStore prefs)
    {
        for (Map.Entry<IRepositoryType, List<IRepositoryModel>> entry : mapped.entrySet())
        {
            IRepositoryType type = entry.getKey();
            if (type.isDynamic())
            {
                StringBuffer buf = new StringBuffer();

                for (IRepositoryModel element : entry.getValue())
                {
                    if (buf.length() > 0)
                    {
                        buf.append(",");
                    }
                    buf.append(element.getId());
                    saveRepository(element, prefs);
                }

                prefs.setValue(REPOSITORY + type.getId() + INSTANCES, buf.toString());
            }
            else
            {
                IRepositoryModel element = entry.getValue().get(0);
                saveRepository(element, prefs);
            }
        }
    }

    private void saveRepositoryPreferences(List<IRepositoryModel> repositories,
        HashMap<IRepositoryType, List<IRepositoryModel>> mapped) throws CoreException
    {
        for (IRepositoryModel rep : repositories)
        {
            try
            {
                createDir(makeFileName(rep));
                toPreferenceStore(rep).save();
                List<IRepositoryModel> list = mapped.get(rep.getType());
                if (list == null)
                {
                    list = new ArrayList<IRepositoryModel>(1);
                    mapped.put(rep.getType(), list);
                }
                list.add(rep);
            }
            catch (IOException e)
            {
                throw SigilCore.newCoreException("Failed to save repository preferences",
                    e);
            }
        }
    }

    private static void createDir(String fileName)
    {
        File file = new File(fileName);
        file.getParentFile().mkdirs();
    }

    private static void saveRepository(IRepositoryModel element, IPreferenceStore prefs)
    {
        String key = makeKey(element);
        prefs.setValue(key + LOC, makeFileName(element));
        prefs.setValue(key + TIMESTAMP, now());
    }

    private static long now()
    {
        return System.currentTimeMillis();
    }

    private static String makeKey(IRepositoryModel element)
    {
        IRepositoryType type = element.getType();

        String key = REPOSITORY + type.getId();
        if (type.isDynamic())
            key = key + "." + element.getId();

        return key;
    }

    private static String makeFileName(IRepositoryModel element)
    {
        IPath path = SigilCore.getDefault().getStateLocation();
        path = path.append("repository");
        path = path.append(element.getType().getId());
        path = path.append(element.getId());
        return path.toOSString();
    }

    private RepositoryModel loadRepository(String id, String key,
        IRepositoryType type, IPreferenceStore prefs)
    {
        RepositoryModel element = new RepositoryModel(id, type);

        Properties props = element.getProperties();
        if ( type.isDynamic()) {
            String loc = prefs.getString(key + LOC);

            if (loc == null || loc.trim().length() == 0)
            {
                loc = makeFileName(element);
            }

            if (new File(loc).exists())
            {
                FileInputStream in = null; 
                try
                {
                    in = new FileInputStream(loc);
                    props.load(in);
                    
                    if (type.isDynamic() && !props.containsKey(RepositoryModel.NAME)) {
                        String name = prefs.getString(key + NAME);
                        props.setProperty(RepositoryModel.NAME, name);
                    }
                    
                }
                catch (IOException e)
                {
                    SigilCore.error("Failed to load properties for repository " + key, e);
                }
                finally {
                    if ( in != null ) {
                        try
                        {
                            in.close();
                        }
                        catch (IOException e)
                        {
                            SigilCore.error("Failed to close properties file " + loc, e);
                        }
                    }
                }
            }
        }

        if (!props.containsKey(IRepositoryConfig.REPOSITORY_PROVIDER)) {
            props.setProperty(IRepositoryConfig.REPOSITORY_PROVIDER, type.getProvider());
        }

        props.setProperty("id", id);

        return element;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.eclipse.model.repository.IRepositoryPreferences#loadRepositoryTypes()
     */
    public List<IRepositoryType> loadRepositoryTypes()
    {
        return ExtensionUtils.loadRepositoryTypes();
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.eclipse.model.repository.IRepositoryPreferences#getPreferenceStore(org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel)
     */
    public PreferenceStore toPreferenceStore(final IRepositoryModel model)
    {
        PreferenceStore store = new PreferenceStore();
        store.setFilename(makeFileName(model));
        
        for (Map.Entry<Object, Object> e : model.getProperties().entrySet()) {
            store.setValue((String) e.getKey(), (String) e.getValue());
        }
        
        store.setValue("provider", model.getType().getProvider());

        store.addPropertyChangeListener(new IPropertyChangeListener()
        {            
            public void propertyChange(PropertyChangeEvent event)
            {
                model.getProperties().setProperty(event.getProperty(), event.getNewValue().toString());
            }
        });
        
        return store;
    }
}
