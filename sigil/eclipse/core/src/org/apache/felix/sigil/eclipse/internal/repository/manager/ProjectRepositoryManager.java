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


import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.eclipse.core.runtime.CoreException;

/**
 * @author dave
 *
 */
public class ProjectRepositoryManager extends AbstractEclipseRepositoryManager
{
    /**
     * @param model
     * @param repositoryMap2 
     * @throws CoreException 
     */
    public ProjectRepositoryManager(ISigilProjectModel model, IRepositoryCache repositoryMap) throws CoreException
    {
        super(new ProjectRepositoryConfig(model.getRepositoryConfig()), repositoryMap);
    }
}
