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

package org.apache.felix.sigil.eclipse.internal.model.project;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.sigil.common.config.IRepositoryConfig;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * @author dave
 *
 */
public class EclipseRepositoryConfig implements IRepositoryConfig
{

    private final IRepositoryConfig projectConfig;
    
    public EclipseRepositoryConfig(IRepositoryConfig projectConfig) {
        this.projectConfig = projectConfig;
    }
    
    /* (non-Javadoc)
     * @see org.apache.felix.sigil.common.config.IRepositoryConfig#getAllRepositories()
     */
    public List<String> getAllRepositories()
    {
        ArrayList<String> list = new ArrayList<String>(readRepositories().keySet());
        list.addAll(projectConfig.getAllRepositories());
        return list;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.common.config.IRepositoryConfig#getRepositoryConfig(java.lang.String)
     */
    public Properties getRepositoryConfig(String name)
    {
        Properties props = projectConfig.getRepositoryConfig(name);
        if ( props.isEmpty() ) {
            props = readRepositories().get(name);
        }
        return props;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.common.config.IRepositoryConfig#getRepositoryDefinition(java.lang.String)
     */
    public URI getRepositoryDefinition(String name)
    {
        URI def = projectConfig.getRepositoryDefinition(name);
        if ( def == null ) {
            if ( readRepositories().containsKey(name) ) {
                def = URI.create("sigil:eclipse:preferences");
            }
        }
        return def;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.common.config.IRepositoryConfig#getRepositoryPath()
     */
    public List<String> getRepositoryPath()
    {
        return projectConfig.getRepositoryPath();
    }

    private Map<String, Properties> readRepositories() {
        HashMap<String, Properties> repos = new HashMap<String, Properties>();
        IPreferenceStore prefs = SigilCore.getDefault().getPreferenceStore();

        IRepositoryModel[] models = findRepositories();
        for (IRepositoryModel repo : models)
        {
            try
            {
                String id = repo.getId();
                Properties pref = null;
                if (repo.getType().isDynamic())
                {
                    String instance = "repository." + repo.getType().getId() + "." + id;
                    String loc = prefs.getString(instance + ".loc");
                    pref = loadPreferences(loc);
                }
                else
                {
                    pref = new Properties();
                }
                
                repos.put(id, pref);
            }
            catch (IOException e)
            {
                SigilCore.error("Failed to load repository for " + repo, e);
            }
        }
        
        return repos;
    }
    
    private IRepositoryModel[] findRepositories()
    {
        return SigilCore.getRepositoryConfiguration().getDefaultRepositorySet().getRepositories();
    }
    
    private Properties loadPreferences(String loc) throws FileNotFoundException,
    IOException
    {
        FileInputStream in = null;
        try
        {
            Properties pref = new Properties();
            pref.load(new FileInputStream(loc));
            return pref;
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException e)
                {
                    SigilCore.error("Failed to close file", e);
                }
            }
        }
    }    
}
