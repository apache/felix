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
import java.util.Collections;
import java.util.List;

import org.apache.felix.sigil.common.config.IRepositoryConfig;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.eclipse.core.runtime.CoreException;

/**
 * @author dave
 *
 */
public class GlobalRepositoryConfig extends AbstractEclipseRepositoryConfig
{
    /* (non-Javadoc)
     * @see org.apache.felix.sigil.common.config.IRepositoryConfig#getRepositoryPath()
     */
    public List<String> getRepositoryPath()
    {
        return Collections.singletonList(IRepositoryConfig.WILD_CARD);
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.eclipse.internal.repository.manager.AbstractEclipseRepositoryConfig#getConfigs()
     */
    @Override
    protected IRepositoryConfig[] getConfigs()
    {
        ArrayList<IRepositoryConfig> list = new ArrayList<IRepositoryConfig>(); 
        for (ISigilProjectModel p : SigilCore.getRoot().getProjects() ) {
            try
            {
                IRepositoryConfig c = p.getRepositoryConfig();
                list.add( c );
            }
            catch (CoreException e)
            {
                SigilCore.error("Failed to read repository config", e);
            }            
        }
        return list.toArray( new IRepositoryConfig[list.size()] );
    }
}
