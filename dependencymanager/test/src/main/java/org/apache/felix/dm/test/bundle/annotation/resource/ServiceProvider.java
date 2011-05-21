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
package org.apache.felix.dm.test.bundle.annotation.resource;

import java.net.URL;

import junit.framework.Assert;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ResourceAdapterService;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;
import org.osgi.framework.BundleContext;

/**
 * Our ServiceInterface provider, which service is activated by a ResourceAdapter.
 */
@ResourceAdapterService(
    filter = "(&(path=/path/to/test1.txt)(host=localhost))", 
    properties = {@Property(name="foo", value="bar")},
    propagate = true)
public class ServiceProvider implements ServiceInterface
{
    // Injected by reflection
    URL m_resource;
        
    @ServiceDependency(filter="(test=adapter)")
    Sequencer m_sequencer;
    
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

    public void run()
    {
        checkInjectedFields();
        Assert.assertNotNull("Resource has not been injected in the adapter", m_resource);
        Assert.assertEquals("ServiceProvider did not get expected resource", "file://localhost/path/to/test1.txt", m_resource.toString());
        m_sequencer.step(2);
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
