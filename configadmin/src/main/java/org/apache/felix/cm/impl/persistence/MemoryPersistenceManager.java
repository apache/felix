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
package org.apache.felix.cm.impl.persistence;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.cm.NotCachablePersistenceManager;
import org.apache.felix.cm.impl.CaseInsensitiveDictionary;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;


/**
 * The <code>MemoryPersistenceManager</code> holds all configurations in memory.
 */
public class MemoryPersistenceManager implements NotCachablePersistenceManager
{

    /** Cached dictionaries */
    private final Map<String, CaseInsensitiveDictionary> cache = new HashMap<>();

    /** Factory configuration cache. */
    private final Map<String, Set<String>> factoryConfigCache = new HashMap<>();

    @Override
    public void delete( final String pid ) throws IOException
    {
        final Dictionary props = cache.remove(pid);
        if (props != null)
        {
            final String factoryPid = (String) props.get(ConfigurationAdmin.SERVICE_FACTORYPID);
            if (factoryPid != null)
            {
                final Set<String> factoryPids = this.factoryConfigCache.get(factoryPid);
                if (factoryPids != null)
                {
                    factoryPids.remove(pid);
                    if (factoryPids.isEmpty())
                    {
                        this.factoryConfigCache.remove(factoryPid);
                    }
                }
            }
        }
    }

    @Override
    public boolean exists( final String pid )
    {
        return cache.containsKey(pid);
    }

    @Override
    public Enumeration getDictionaries() throws IOException
    {
        // Deep copy the configuration to avoid any threading issue
        final List<Dictionary> configs = new ArrayList<>();
        for (final Dictionary d : cache.values()) {
            if (d.get(Constants.SERVICE_PID) != null) {
                configs.add(new CaseInsensitiveDictionary(d));
            }
        }
        return Collections.enumeration(configs);
    }

    private final CaseInsensitiveDictionary cache(final Dictionary props)
    {
        final String pid = (String) props.get( Constants.SERVICE_PID );
        CaseInsensitiveDictionary dict = null;
        if ( pid != null )
        {
            dict = cache.get(pid);
            if ( dict == null )
            {
                dict = new CaseInsensitiveDictionary(props);
                cache.put( pid, dict );
                final String factoryPid = (String)props.get(ConfigurationAdmin.SERVICE_FACTORYPID);
                if ( factoryPid != null )
                {
                    Set<String> factoryPids = this.factoryConfigCache.get(factoryPid);
                    if ( factoryPids == null )
                    {
                        factoryPids = new HashSet<>();
                        this.factoryConfigCache.put(factoryPid, factoryPids);
                    }
                    factoryPids.add(pid);
                }
            }
        }
        return dict;
    }

    @Override
    public Dictionary load( final String pid ) throws IOException
    {
        CaseInsensitiveDictionary loaded = cache.get(pid);
        return loaded == null ? null : new CaseInsensitiveDictionary(loaded);
    }

    @Override
    public void store( final String pid, final Dictionary properties ) throws IOException
    {
        this.cache.remove(pid);
        this.cache(properties);
    }
}
