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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Use case: a factory configuration adapter has a required dependency on a "Required" service.
 * The factory configuration is created, the "Required" service is registered, so a factory configuration adapter INSTANCE1 is then created.
 * Now the "Required" service is unregistered: the factory config adapter INSTANCE1 is then stopped.
 * And when the "Required" service comes up again, then a new factory config adapter INSTANCE2 should be re-created, updated and started.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX5200_FactoryPidNotRestartedTest extends TestBase {
    static FactoryPidComponent m_currentInstance;
    
    static Ensure m_ensure = new Ensure();
    
    static Ensure.Steps m_steps = new Ensure.Steps(
        1, // updated is called the first time the adapter instance is created 
        2, // component started
        3, // "Required" service is unregistered, and adapter instance is stopped 
        4, // "Required" service is re-registered, a new adapter instance is created and called in updated 
        5, // the new adapter instance is then started
        6  // the configuration is removed and the new adapter instance is stopped
        );
    
    public void testFactoryAdapterNotRestartedTest() {
        DependencyManager dm = getDM();
        
        Component adapter = dm.createFactoryConfigurationAdapterService("factory.pid", "updated", false)
            .setImplementation(FactoryPidComponent.class)
            .add(dm.createServiceDependency().setService(RequiredService.class).setRequired(true));
        
        Component required = dm.createComponent()
            .setImplementation(RequiredService.class)
            .setInterface(RequiredService.class.getName(), null);
        
        Component configurator = dm.createComponent()
            .setImplementation(new Configurator("factory.pid"))
            .add(dm.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        
        dm.add(configurator);
        dm.add(adapter);
        dm.add(required);        
        // adapter instance1 updated and started
        m_ensure.waitForStep(2, 5000);     
        FactoryPidComponent currentInstance = m_currentInstance;
        dm.remove(required);        
        // adapter instance1 stopped
        m_ensure.waitForStep(3, 5000); 
        dm.add(required);
        // adapter instance2 updated and started
        m_ensure.waitForStep(5, 5000);        
        // a new adapter instance should have been created
        Assert.assertNotEquals(currentInstance, m_currentInstance);
        dm.remove(configurator);
        // adapter instance2 stopped
        m_ensure.waitForStep(6, 5000);
    }
    
    public static class FactoryPidComponent {
        RequiredService m_required;
        
        void updated(Dictionary<String, Object> properties) {
            m_ensure.steps(m_steps);  
        }
        
        void start() {
            m_ensure.steps(m_steps);
            m_currentInstance = this;
        }
        
        void stop() {
            m_ensure.steps(m_steps);
        }
    }
    
    public static class RequiredService {
    }
    
    class Configurator {
        private volatile ConfigurationAdmin m_ca;
        Configuration m_conf;
        final String m_pid;
        
        public Configurator(String pid) {
            m_pid = pid;
        }

        public void init() {
            try {
                Assert.assertNotNull(m_ca);
                m_conf = m_ca.createFactoryConfiguration(m_pid, null);
                Properties props = new Properties();
                props.setProperty("some", "properties");
                m_conf.update(props);
            }
            catch (IOException e) {
                Assert.fail("Could not create configuration: " + e.getMessage());
            }
        }
        
        public void destroy() throws IOException {
            m_conf.delete();
        }
    }
}
