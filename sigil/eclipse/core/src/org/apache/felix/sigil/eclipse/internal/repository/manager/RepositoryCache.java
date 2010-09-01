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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.apache.felix.sigil.common.repository.IBundleRepository;
import org.apache.felix.sigil.common.repository.IRepositoryProvider;
import org.apache.felix.sigil.common.repository.RepositoryException;

public class RepositoryCache implements IRepositoryCache
{
    public static class RepositoryStore
    {
        public final Properties pref;
        public final IBundleRepository repo;

        public RepositoryStore(Properties pref, IBundleRepository repo)
        {
            this.pref = pref;
            this.repo = repo;
        }
    }

    private HashMap<String, RepositoryStore> cachedRepositories = new HashMap<String, RepositoryStore>();

    public synchronized void retainAll(Collection<String> ids)
    {
        for (Iterator<String> i = cachedRepositories.keySet().iterator(); i.hasNext();)
        {
            if (!ids.contains(i.next()))
            {
                i.remove();
            }
        }
    }

    public IBundleRepository getRepository(String uid, Properties pref,
        IRepositoryProvider provider) throws RepositoryException
    {
        try
        {
            if (pref == null)
            {
                pref = new Properties();
            }

            RepositoryStore cache = get(uid);

            if (cache == null || !cache.pref.equals(pref))
            {
                IBundleRepository repo = provider.createRepository(uid, pref);
                cache = new RepositoryStore(pref, repo);
                put(uid, cache);
            }

            return cache.repo;
        }
        catch (RuntimeException e)
        {
            throw new RepositoryException("Failed to build repositories", e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.felix.sigil.eclipse.internal.repository.manager.IRepositoryCache#discard(java.util.Collection)
     */
    public void discard(Collection<String> ids)
    {
        cachedRepositories.keySet().removeAll(ids);
    }
    

    private RepositoryStore get(String id)
    {
        return cachedRepositories.get(id);
    }

    private void put(String id, RepositoryStore cache)
    {
        cachedRepositories.put(id, cache);
    }
}
