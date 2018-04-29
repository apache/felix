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

package org.apache.felix.sigil.obr.eclipse;

import java.util.Properties;

import org.apache.felix.sigil.common.obr.OBRRepositoryProvider;
import org.apache.felix.sigil.common.repository.IBundleRepository;
import org.apache.felix.sigil.common.repository.RepositoryException;
import org.eclipse.core.runtime.IPath;

/**
 * @author dave
 *
 */
public class EclipseOBRRepositoryProvider extends OBRRepositoryProvider
{

    @Override
    public IBundleRepository createRepository(String id, Properties preferences)
        throws RepositoryException
    {
        Properties props = new Properties(preferences);
        
        if ( !preferences.containsKey(INDEX_CACHE_FILE) ) {
            String index = newStatePath(id, "index.obr");
            props.put(INDEX_CACHE_FILE, index);
        }
        
        if ( !preferences.containsKey(CACHE_DIRECTORY) ) {
            String dir = newStatePath(id, "dir");
            props.put(CACHE_DIRECTORY, dir);
        }
        
        return super.createRepository(id, props);
    }

    /**
     * @param id
     * @param string
     * @return
     */
    private String newStatePath(String id, String string)
    {
        IPath state = Activator.getDefault().getStateLocation();
        state.append("cache").append(id).append(string);
        return state.toOSString();
    }

}
