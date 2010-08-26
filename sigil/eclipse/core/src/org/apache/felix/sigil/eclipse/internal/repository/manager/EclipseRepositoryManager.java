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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.felix.sigil.common.config.IRepositoryConfig;
import org.apache.felix.sigil.common.repository.AbstractRepositoryManager;
import org.apache.felix.sigil.common.repository.IBundleRepository;
import org.apache.felix.sigil.common.repository.IRepositoryProvider;
import org.apache.felix.sigil.common.repository.RepositoryException;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.IRepositoryMap;
import org.apache.felix.sigil.eclipse.model.project.IRepositoryMap.RepositoryCache;

/**
 * @author dave
 *
 */
public class EclipseRepositoryManager extends AbstractRepositoryManager
{
    private final IRepositoryConfig config;
    private final IRepositoryMap repositoryMap;
    
    /**
     * @param config
     * @param repositoryMap2 
     */
    public EclipseRepositoryManager(IRepositoryConfig config, IRepositoryMap repositoryMap)
    {
        this.config = config;
        this.repositoryMap = repositoryMap;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.common.repository.AbstractRepositoryManager#loadRepositories()
     */
    @Override
    protected void loadRepositories()
    {
        List<IBundleRepository> list = new ArrayList<IBundleRepository>();
        scanRepositories(config.getRepositoryPath(), list);
        setRepositories(list.toArray(new IBundleRepository[list.size()]));
    }
    
    /**
     * @param list 
     * @param config2
     * @param i
     */
    private void scanRepositories(List<String> path, List<IBundleRepository> list)
    {
        for (String name : path)
        {
            if ( IRepositoryConfig.WILD_CARD.equals(name) ) {
                HashSet<String> defined = new HashSet<String>();
                for (String n : path) {
                    if (!IRepositoryConfig.WILD_CARD.equals(n)) {
                        defined.add(n);
                    }
                }
                List<String> subpath = new LinkedList<String>();
                for (String key : config.getAllRepositories()) {
                    if (!defined.contains(key))
                    {
                        subpath.add(key);
                    }
                }
                scanRepositories(subpath, list);
            }
            else {
                Properties props = config.getRepositoryConfig(name);
                String uid = config.getRepositoryDefinition(name).toString() + name;
                IBundleRepository repo = buildRepository(uid, name, props);
                
                if ( repo != null ) {
                    list.add(repo);
                }                
            }
        }
    }

    /**
     * @param repo
     * @return 
     */
    private IBundleRepository buildRepository(String uid, String name, Properties repo)
    {
        String disabled = repo.getProperty("disabled");
        if ("true".equalsIgnoreCase(disabled == null ? null : disabled.trim())) return null;
        
        String optStr = repo.getProperty("optional", "false");
        boolean optional = Boolean.parseBoolean(optStr.trim());

        String alias = repo.getProperty(IRepositoryConfig.REPOSITORY_PROVIDER);
        if (alias == null)
        {
            String msg = "provider not specified for repository: " + name;
            
            if (optional)            
                SigilCore.log(msg);
            else
                SigilCore.warn(msg);
        }

        try
        {
            IRepositoryProvider instance = EclipseRepositoryFactory.getProvider(alias);
            IBundleRepository repository = loadRepository(uid, name, repo, instance);
            return repository;
        }
        catch (Exception e)
        {
            String msg = "failed to create repository: ";
            
            if (optional)            
                SigilCore.log(msg + e);
            else
                SigilCore.warn(msg, e);
        }
        
        return null;
    }
    
    private IBundleRepository loadRepository(String uid, String id, Properties pref,
        IRepositoryProvider provider) throws RepositoryException
    {
        try
        {
            if (pref == null)
            {
                pref = new Properties();
            }

            RepositoryCache cache = repositoryMap.get(uid);

            if (cache == null || !cache.pref.equals(pref))
            {
                IBundleRepository repo = provider.createRepository(id, pref);
                cache = new RepositoryCache(pref, repo);
                repositoryMap.put(uid, cache);
            }

            return cache.repo;
        }
        catch (RuntimeException e)
        {
            throw new RepositoryException("Failed to build repositories", e);
        }
    }
    
}
