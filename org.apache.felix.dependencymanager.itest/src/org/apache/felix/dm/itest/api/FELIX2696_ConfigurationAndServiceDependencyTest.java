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

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;


/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings("rawtypes")
public class FELIX2696_ConfigurationAndServiceDependencyTest extends TestBase {
    final static String PID = "FELIX2696_ConfigurationAndServiceDependencyTest.pid";

    public void testComponentWithRequiredConfigurationAndServicePropertyPropagation() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component s1 = m.createComponent()
            .setImplementation(new ConfigurationConsumer(e))
            .add(m.createServiceDependency().setService(ServiceInterface.class).setRequired(true))
            .add(m.createConfigurationDependency().setPid(PID));
        Component s2 = m.createComponent()
            .setImplementation(new ConfigurationCreator(e))
            .add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        Component s3 = m.createComponent()
            .setInterface(ServiceInterface.class.getName(), null)
            .setImplementation(DependentServiceProvider.class);
        m.add(s1);
        m.add(s2);
        m.add(s3);
        e.waitForStep(2, 5000);
        warn("removing s3");
        m.remove(s3);
        warn("readding s3");
        m.add(s3);
        // after adding the required dependency again, the issue in FELIX-2696 means that the
        // updated() method is not invoked for the new instance, and init() is, so our step
        // count will only go up to 3 (not 4) causing this test to fail
        e.waitForStep(4, 5000);
        m.remove(s3);
        m.remove(s2);
        m.remove(s1);
        e.waitForStep(5, 5000);
    }

    public static class ConfigurationCreator {
        private volatile ConfigurationAdmin m_ca;
        private final Ensure m_ensure;
        Configuration m_conf;
        
        public ConfigurationCreator(Ensure e) {
            m_ensure = e;
        }

        @SuppressWarnings("unchecked")
        public void init() {
            try {
                m_conf = m_ca.getConfiguration(PID, null);
                Hashtable props = new Hashtable();
                props.put("testkey", "testvalue");
                m_conf.update(props);
            }
            catch (IOException e) {
                Assert.fail("Could not create configuration: " + e.getMessage());
            }
        }
        
        public void destroy() throws IOException {
        	m_conf.delete();
        	m_ensure.step(5);
        }
    }

    public class ConfigurationConsumer implements ManagedService {
        private final Ensure m_ensure;

        public ConfigurationConsumer(Ensure e) {
            m_ensure = e;
        }

        public void updated(Dictionary props) throws ConfigurationException {
        	warn("Consumer: updated %s", props);
            if (props != null) {
                if (!"testvalue".equals(props.get("testkey"))) {
                    Assert.fail("Could not find the configured property.");
                }
                m_ensure.step();
            }
        }
        
        public void init() {
        	warn("Consumer: init");
            m_ensure.step();
        }
    }

    public static interface ServiceInterface {
    }
    
    public static class DependentServiceProvider implements ServiceInterface {
    }
}
