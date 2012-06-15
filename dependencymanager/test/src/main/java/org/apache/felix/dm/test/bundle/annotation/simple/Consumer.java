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
package org.apache.felix.dm.test.bundle.annotation.simple;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;
import org.osgi.framework.BundleContext;

/**
 * Consumes a service which is provided by the {@link Producer} class.
 */
@Component
public class Consumer
{
    @ServiceDependency
    volatile Runnable m_runnable;
    
    @ServiceDependency
    volatile Sequencer m_sequencer;
    
    @Inject
    volatile BundleContext m_bc;
    BundleContext m_bcNotInjected;
    
    @Inject
    volatile DependencyManager m_dm;
    DependencyManager m_dmNotInjected;
    
    @Inject
    volatile org.apache.felix.dm.Component m_component;
    org.apache.felix.dm.Component m_componentNotInjected;

    @Start
    protected void start() {
        checkInjectedFields();
        m_sequencer.step(3);
        m_runnable.run();
    }
    
    private void checkInjectedFields()
    {
        if (m_bc == null)
        {
            m_sequencer.throwable(new Exception("Bundle Context not injected"));
            return;
        }
        if (m_bcNotInjected != null)
        {
            m_sequencer.throwable(new Exception("Bundle Context must not be injected"));
            return;
        }

        if (m_dm == null)
        {
            m_sequencer.throwable(new Exception("DependencyManager not injected"));
            return;
        }
        if (m_dmNotInjected != null)
        {
            m_sequencer.throwable(new Exception("DependencyManager must not be injected"));
            return;
        }

        if (m_component == null)
        {
            m_sequencer.throwable(new Exception("Component not injected"));
            return;
        }
        if (m_componentNotInjected != null)
        {
            m_sequencer.throwable(new Exception("Component must not be injected"));
            return;
        }
    }

    @Stop
    protected void stop() {
        m_sequencer.step(6);
    }
}
