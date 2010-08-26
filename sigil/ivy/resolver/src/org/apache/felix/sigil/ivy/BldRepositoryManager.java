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

package org.apache.felix.sigil.ivy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.sigil.common.config.IRepositoryConfig;
import org.apache.felix.sigil.common.repository.AbstractRepositoryManager;
import org.apache.felix.sigil.common.repository.IBundleRepository;
import org.apache.felix.sigil.common.repository.IRepositoryProvider;

public class BldRepositoryManager extends AbstractRepositoryManager
{
    private static Map<String, String> aliases = new HashMap<String, String>();
    private final IRepositoryConfig config;

    static
    {
        aliases.put("filesystem",
            "org.apache.felix.sigil.common.core.repository.FileSystemRepositoryProvider");
        aliases.put("obr", "org.apache.felix.sigil.common.obr.OBRRepositoryProvider");
        aliases.put("project", "org.apache.felix.sigil.ivy.ProjectRepositoryProvider");
        aliases.put("system",
            "org.apache.felix.sigil.common.core.repository.SystemRepositoryProvider");
    };

    /**
     * @param config
     */
    public BldRepositoryManager(IRepositoryConfig config)
    {
        this.config = config;
    }

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
                IBundleRepository repo = buildRepository(name, props);
                
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
    private static IBundleRepository buildRepository(String name, Properties repo)
    {
        String optStr = repo.getProperty("optional", "false");
        boolean optional = Boolean.parseBoolean(optStr.trim());

        String alias = repo.getProperty(IRepositoryConfig.REPOSITORY_PROVIDER);
        if (alias == null)
        {
            String msg = "provider not specified for repository: " + name;
            
            if (!optional)
                throw new IllegalStateException(msg);
            
            Log.warn(msg);
        }

        String provider = (aliases.containsKey(alias) ? aliases.get(alias) : alias);

        if (alias.equals("obr"))
        {
            // cache is directory where synchronized bundles are stored;
            // not needed in ivy.
            repo.setProperty("cache", "/no-cache");
            String index = repo.getProperty("index");

            if (index == null)
            {
                // index is created to cache OBR url
                File indexFile = new File(System.getProperty("java.io.tmpdir"),
                    "obr-index-" + name);
                indexFile.deleteOnExit();
                repo.setProperty("index", indexFile.getAbsolutePath());
            }
            else
            {
                if (!new File(index).getParentFile().mkdirs())
                {
                    // ignore - but keeps findbugs happy
                }
            }
        }

        try
        {
            IRepositoryProvider instance = (IRepositoryProvider) (Class.forName(provider).newInstance());
            IBundleRepository repository = instance.createRepository(name, repo);
            return repository;
        }
        catch (Exception e)
        {
            String msg = "failed to create repository: ";
            if (!optional)
                throw new IllegalStateException(msg + repo + " " + e, e);
            Log.warn(msg + e);
        }
        
        return null;
    }
}