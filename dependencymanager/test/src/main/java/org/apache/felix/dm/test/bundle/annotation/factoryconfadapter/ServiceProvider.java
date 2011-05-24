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
package org.apache.felix.dm.test.bundle.annotation.factoryconfadapter;

import java.util.Dictionary;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.annotation.api.FactoryConfigurationAdapterService;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Registered;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;
import org.osgi.framework.BundleContext;

/**
 * This service is instantiated when a factory configuration is created from ConfigAdmin
 */
@FactoryConfigurationAdapterService(factoryPid = "FactoryPidTest", properties = { @Property(name = "foo", value = "bar") }, propagate = true)
public class ServiceProvider implements ServiceInterface
{
    @ServiceDependency
    private Sequencer m_sequencer;
    
    private volatile boolean m_started;

    // Check auto config injections
    @Inject
    BundleContext m_bc;
    BundleContext m_bcNotInjected;
    
    @Inject
    DependencyManager m_dm;
    DependencyManager m_dmNotInjected;
    
    @Inject
    org.apache.felix.dm.Component m_component;
    org.apache.felix.dm.Component m_componentNotInjected;

    // Either initial config, or an updated config
    protected void updated(Dictionary conf)
    {
        if (m_started)
        {
            // conf should contain foo2=bar2_modified
            if (! "bar2_modified".equals(conf.get("foo2")))
            {
                m_sequencer.throwable(new Exception("configuration does not contain foo=bar"));
            }
            m_sequencer.step(4);
        }
        else
        {
            // conf should contain foo2=bar2
            if (! "bar2".equals(conf.get("foo2")))
            {
                throw new IllegalArgumentException("configuration does not contain foo2=bar2: " + conf);
            }
        }
    }

    @Start
    void start()
    {
        checkInjectedFields();
        m_started = true;
        m_sequencer.step(1);
    }    

    @Registered
    void registered()
    {
        m_sequencer.step(3);
    }
    
    // The ServiceClient is invoking our service
    public void doService()
    {
       m_sequencer.step(); /* 2 or 5 */
    }

    @Stop
    void stop() 
    {
        m_sequencer.step(6);
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
}
