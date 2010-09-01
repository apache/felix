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

package org.apache.felix.sigil.eclipse.internal.repository.manager;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.felix.sigil.common.config.IRepositoryConfig;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.internal.model.repository.RepositoryModel;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel;

/**
 * @author dave
 *
 */
public abstract class AbstractEclipseRepositoryConfig implements IRepositoryConfig
{

    protected abstract IRepositoryConfig[] getConfigs();
    
    /* (non-Javadoc)
     * @see org.apache.felix.sigil.common.config.IRepositoryConfig#getAllRepositories()
     */
    public List<String> getAllRepositories()
    {
        ArrayList<String> list = new ArrayList<String>();
        for (IRepositoryConfig c : getConfigs()) {
            list.addAll(c.getAllRepositories());
        }
        list.addAll(readRepositoryNames());
        return list;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.common.config.IRepositoryConfig#getRepositoryConfig(java.lang.String)
     */
    public Properties getRepositoryConfig(String name)
    {
        Properties props = null;
        for(IRepositoryConfig c : getConfigs()) {
            props = c.getRepositoryConfig(name);
            if ( props != null ) {
                if ( !props.containsKey(RepositoryModel.NAME)) {
                    props.setProperty(RepositoryModel.NAME, name);
                }
                break;
            }
        }
        if ( props == null ) {
            props = readRepositoryConfig(name);
        }
        return props;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.common.config.IRepositoryConfig#getRepositoryDefinition(java.lang.String)
     */
    public URI getRepositoryDefinition(String name)
    {
        URI def = null;
        for(IRepositoryConfig c : getConfigs()) {
            def = c.getRepositoryDefinition(name);
            if ( def != null ) {
                break;
            }
        }
        if ( def == null ) {
            if ( readRepositoryNames().contains(name) ) {
                def = URI.create("sigil:eclipse:preferences");
            }
        }
        return def;
    }

    /**
     * @return
     */
    static List<String> readRepositoryNames()
    {
        List<IRepositoryModel> models = findRepositories();
        ArrayList<String> repos = new ArrayList<String>(models.size());
        for (IRepositoryModel repo : models)
        {
            String id = repo.getId();
            repos.add(id);
        }
        
        return repos;
    }

    static Properties readRepositoryConfig(String name) {
        for (IRepositoryModel repo : findRepositories())
        {
            String id = repo.getId();
            if ( name.equals(id) ) {
                return repo.getProperties();
            }
        }
        
        // ok not found
        return null;
    }
    
    private static final List<IRepositoryModel> findRepositories()
    {
        return SigilCore.getRepositoryPreferences().loadRepositories();
    }    
}
