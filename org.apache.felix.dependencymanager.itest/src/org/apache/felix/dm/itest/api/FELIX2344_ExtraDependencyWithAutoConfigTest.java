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
package org.apache.felix.dm.itest.api;

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;


/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings("unused")
public class FELIX2344_ExtraDependencyWithAutoConfigTest extends TestBase {
    /**
     * Test if an auto config extra dependency is injected in the expected order.
     */
    public void testExtraDependencyWithAutoConfig() {  
        DependencyManager m = getDM();
        // Helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // Create a service provider
        Component sp = m.createComponent().setInterface(ProviderInterface.class.getName(), null).setImplementation(ProviderImpl.class);
        // Create a service consumer with a required/autoconfig dependency over the service provider.
        Client c1;
        Component sc1 = m.createComponent().setImplementation((c1 = new Client(e, true, 1)));
        // Create a second service consumer with an optional/autoconfig dependency over the service provider.
        Client c2;
        Component sc2 = m.createComponent().setImplementation(c2 = new Client(e, false, 3));

        // Add service provider and consumer sc1 (required dependency over provider)
        m.add(sc1);
        m.add(sp);
        e.waitForStep(2, 5000); 
        
        // Remove provider and consumer
        m.remove(sc1);
        m.remove(sp);
        
        // Add consumer sc2 (optional dependency over provider)
        m.add(sc2);
        e.waitForStep(4, 5000);    
        m.clear();
    }

    public interface ProviderInterface {
        public boolean action();
    }

    public static class ProviderImpl implements ProviderInterface {
        public boolean action()
        {
            return true;
        }
    }
    
    // This client is not using callbacks, but instead, it uses auto config.
    public static class Client {
        volatile ProviderInterface m_provider;
        private Ensure m_ensure;
        private final boolean m_required;
        private final int m_startStep;

        public Client(Ensure e, boolean required, int startStep) {
            m_ensure = e;
            m_required = required;
            m_startStep = startStep;
        }
        
        public void init(Component s) {
            DependencyManager dm = s.getDependencyManager();
            m_ensure.step(m_startStep);
            s.add(dm.createServiceDependency()
                .setService(ProviderInterface.class)
                .setRequired(m_required)
                .setAutoConfig("m_provider"));
        }

        public void start() {
            // if required dependency: we must have been injected with the service provider
            // else, we have been injected with a null object.
            Assert.assertNotNull("provider has not been injected", m_provider);
            Assert.assertEquals(m_required, m_provider.action()); // action returns false if null object
            m_ensure.step();
        } 
    }
}

