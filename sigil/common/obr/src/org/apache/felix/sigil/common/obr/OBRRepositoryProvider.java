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

package org.apache.felix.sigil.common.obr;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.felix.sigil.common.obr.impl.CachingOBRBundleRepository;
import org.apache.felix.sigil.common.obr.impl.NonCachingOBRBundleRepository;
import org.apache.felix.sigil.common.repository.IBundleRepository;
import org.apache.felix.sigil.common.repository.IRepositoryProvider;
import org.apache.felix.sigil.common.repository.RepositoryException;

public class OBRRepositoryProvider implements IRepositoryProvider
{
    private static final String IN_MEMORY = "inmemory";
    private static final String UPDATE_PERIOD = "updatePeriod";
    private static final String AUTH_FILE = "auth";
    private static final String CACHE_DIRECTORY = "cache";
    private static final String INDEX_CACHE_FILE = "index";

    public IBundleRepository createRepository(String id, Properties preferences)
        throws RepositoryException
    {
        String urlStr = preferences.getProperty("url");
        if (urlStr == null)
            throw new RepositoryException("url is not specified.");

        try
        {
            File urlFile = new File(urlStr);
            URL repositoryURL = urlFile.exists() ? urlFile.toURI().toURL() : new URL(
                urlStr);
            URL testURL = urlFile.exists() ? urlFile.toURI().toURL() : new URL(urlStr);
            File indexCache = new File(preferences.getProperty(INDEX_CACHE_FILE));
            File localCache = new File(preferences.getProperty(CACHE_DIRECTORY));
            String auth = preferences.getProperty(AUTH_FILE);
            File authFile = auth == null ? null : new File(auth);
            Long up = preferences.containsKey(UPDATE_PERIOD) ? Long.parseLong(preferences.getProperty(UPDATE_PERIOD))
                : 60 * 60 * 24 * 7;

            if (testURL.openConnection().getLastModified() == 0)
            {
                String msg = "Failed to read OBR index: ";
                if (!indexCache.exists())
                    throw new RepositoryException(msg + urlStr);
                System.err.println("WARNING: " + msg + "using cache: " + urlStr);
            }

            long updatePeriod = TimeUnit.MILLISECONDS.convert(up, TimeUnit.SECONDS);
            if (preferences.getProperty(IN_MEMORY) == null)
            {
                return new NonCachingOBRBundleRepository(id, repositoryURL, indexCache,
                    localCache, updatePeriod, authFile);
            }
            else
            {
                return new CachingOBRBundleRepository(id, repositoryURL, indexCache,
                    localCache, updatePeriod, authFile);
            }
        }
        catch (IOException e)
        {
            throw new RepositoryException("Invalid repository url", e);
        }
    }
}
