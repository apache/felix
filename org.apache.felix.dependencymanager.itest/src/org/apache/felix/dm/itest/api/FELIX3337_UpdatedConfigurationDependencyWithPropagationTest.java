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
import java.util.Hashtable;
import java.util.Properties;

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;


/**
 * This test validates the following scenario:
 * - Service S1 depends on a ConfigurationDependency with propagate = true
 * - Service S2 depends on S1 (and has access to the S1 configuration using the S1 service 
 *   properties (because the ConfigurationDependency is propagated)
 * - then the S1 PID is updated from ConfigAdmin
 * - S1 is then called in its updated callback
 * - S2 is called in its "change" callback.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class FELIX3337_UpdatedConfigurationDependencyWithPropagationTest extends TestBase {    
    /*
     * This Pojo creates the configuration pid "test".
     */
    static class ConfigurationCreator {
        private volatile ConfigurationAdmin m_ca;
        org.osgi.service.cm.Configuration m_conf;
        
        public void init() {
            try {
                m_conf = m_ca.getConfiguration("test", null);
                Hashtable props = new Properties();
                props.put("testkey", "testvalue");
                m_conf.update(props);
            }
            catch (IOException e) {
                Assert.fail("Could not create configuration: " + e.getMessage());
            }
        }
        
        public void update() {
            try {
                Hashtable props = new Properties();
                props.put("testkey", "testvalue");
                props.put("testkey2", "testvalue2");
                m_conf.update(props);
            } catch (IOException e) {
                Assert.fail("Could not update the configured property: " + e.toString());
            }
        }
    }

    static class S1 implements ManagedService {
        private Ensure m_ensure;
        private boolean m_initialized;

        public S1(Ensure e) {
            m_ensure = e;
        }
        
        public void updated(Dictionary props) throws ConfigurationException {
        	if (props != null) {
				if (!m_initialized) {
					m_ensure.step(1);
					m_initialized = true;
				} else {
					// we are updated
					m_ensure.step(3);
				}
        	}
        }
    }

    static class S2 {
        private final Ensure m_ensure;

        public S2(Ensure e) {
            m_ensure = e;
        }
                        
        public void add(S1 s1) {
            m_ensure.step(2);
        }
        
        public void change(S1 runnable) {
            m_ensure.step(4);
        }
    }
    
    public void testComponentWithRequiredUpdatedConfigurationAndServicePropertyPropagation() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();        
        ConfigurationCreator confCreator = new ConfigurationCreator();
        Component s1 = m.createComponent()
                .setImplementation(new S1(e))
                .setInterface(S1.class.getName(), null)
                .add(m.createConfigurationDependency()
                     .setPid("test")
                     .setPropagate(true));
        Component s2 = m.createComponent()
                .setImplementation(new S2(e))
                .add(m.createServiceDependency()
                     .setService(S1.class, ("(testkey=testvalue)"))
                     .setRequired(true)
                     .setCallbacks("add", "change", null));
        Component s3 = m.createComponent()
                .setImplementation(confCreator)
                .add(m.createServiceDependency()
                     .setService(ConfigurationAdmin.class)
                     .setRequired(true));

        m.add(s1);
        m.add(s2);
        m.add(s3);
        e.waitForStep(2, 5000);
        confCreator.update();
        e.waitForStep(4, 5000);
        m.remove(s1);
        m.remove(s2);
        m.remove(s3);
    }
}
