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

    static
    {
        aliases.put("filesystem",
            "org.apache.felix.sigil.common.core.repository.FileSystemRepositoryProvider");
        aliases.put("obr", "org.apache.felix.sigil.common.obr.OBRRepositoryProvider");
        aliases.put("project", "org.apache.felix.sigil.ivy.ProjectRepositoryProvider");
        aliases.put("system",
            "org.apache.felix.sigil.common.core.repository.SystemRepositoryProvider");
    };

    private final Map<String, Properties> repos;
    private final List<String> repositoryPath;

    public BldRepositoryManager(List<String> repositoryPath, Map<String, Properties> repos)
    {
        System.out.println("RepositoryPath=" + repositoryPath);
        System.out.println("Repos=" + repos);
        this.repositoryPath = repositoryPath;
        this.repos = repos;
    }

    @Override
    protected void loadRepositories()
    {
        scanRepositories(repositoryPath, repos, 0);
    }

    /**
     * @param repos2 
     * @param repositoryPath2
     */
    private int scanRepositories(List<String> repositoryPath, Map<String, Properties> repos, int start)
    {
        int count = start;
        for (String name : repositoryPath)
        {
            System.out.println("Building repository for " + name);
            if ( IRepositoryConfig.WILD_CARD.equals(name) ) {
                HashSet<String> defined = new HashSet<String>();
                for (String n : repositoryPath) {
                    if (!IRepositoryConfig.WILD_CARD.equals(n)) {
                        defined.add(n);
                    }
                }
                List<String> path = new LinkedList<String>();
                for (String key : repos.keySet()) {
                    if (!defined.contains(key))
                    {
                        path.add(key);
                    }
                }
                count = scanRepositories(path, repos, start + 1);
            }
            else {
                Properties props = repos.get(name);
                IBundleRepository repo = buildRepository(name, props);
                System.out.println("Built repository " + repo + " for " + name + " at " + count);
                
                if ( repo != null ) {
                    addRepository(repo, count++);
                }                
            }
        }
        
        return count;
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