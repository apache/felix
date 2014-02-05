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
package org.apache.felix.dm.impl.dependencies;

import org.apache.felix.dm.ComponentDependencyDeclaration;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyActivation;
import org.apache.felix.dm.impl.Logger;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class DependencyBase implements Dependency, DependencyActivation {
    private volatile boolean m_isRequired;
    private volatile boolean m_isInstanceBound;
    protected final Logger m_logger;
    protected volatile boolean m_isStarted;

    public DependencyBase(Logger logger) {
        m_logger = logger;
    }
    
    public DependencyBase(DependencyBase prototype) {
        m_logger = prototype.m_logger;
        m_isRequired = prototype.isRequired();
        m_isInstanceBound = prototype.m_isInstanceBound;
    }

    public boolean isRequired() {
        return m_isRequired;
    }
    
    protected void setIsRequired(boolean isRequired) {
        m_isRequired = isRequired;
    }
    
    public final boolean isInstanceBound() {
        return m_isInstanceBound;
    }

    public final void setIsInstanceBound(boolean isInstanceBound) {
        m_isInstanceBound = isInstanceBound;
    }
    
    public int getState() {
        if (m_isStarted) {
            return (isAvailable() ? 1 : 0) + (isRequired() ? 2 : 0);
        }
        else {
            return isRequired() ? ComponentDependencyDeclaration.STATE_REQUIRED : ComponentDependencyDeclaration.STATE_OPTIONAL;
        }
    }

}
