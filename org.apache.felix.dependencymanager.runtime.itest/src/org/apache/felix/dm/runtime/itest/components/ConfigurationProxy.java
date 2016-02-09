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
package org.apache.felix.dm.runtime.itest.components;

import java.io.IOException;
import java.util.Hashtable;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.itest.util.Ensure;
import org.junit.Assert;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Tests for new Configuration Proxy Types (FELIX-5177)
 */
public class ConfigurationProxy {
    // For ConfigurationDependency service
    public final static String ENSURE_CONFIG_DEPENDENCY = "ConfigurationProxy.Ensure.Configuration";
    
    public interface Config {
        String getFoo();
    }
    
    // This component configures the Consumer component.
    @Component
    public static class ConsumerConfigurator {
        @ServiceDependency(filter="(name=" + ENSURE_CONFIG_DEPENDENCY + ")")
        Ensure m_ensure;
        
        @ServiceDependency
        ConfigurationAdmin m_cm;

        private Configuration m_conf;
        
        @Start
        void start() throws IOException {
            m_conf = m_cm.getConfiguration(Config.class.getName());
            Hashtable<String, Object> props = new Hashtable<>();
            props.put("foo", "bar");
            m_conf.update(props);
        }
        
        @Stop
        void stop() throws IOException {
            m_conf.delete();
        }
    }
        
    // This consumer depends on the configuration and on the provider, using multiple method signatures.
    @Component
    public static class Consumer {
        Config m_config;
        Config m_config2;

        @ConfigurationDependency
        void updated(Config cnf) {
            if (cnf != null) {
                Assert.assertNotNull(cnf);
                m_config = cnf;
            } else {
                m_config = null;
                m_ensure.step();
            }
        }

        @ConfigurationDependency
        void updated2(org.apache.felix.dm.Component comp, Config cnf) {
            if (cnf != null) {
                Assert.assertNotNull(comp);
                Assert.assertNotNull(cnf);
                m_config2 = cnf;
            } else {
                m_config2 = null;
                m_ensure.step();
            }
        }

        @ServiceDependency(filter="(name=" + ENSURE_CONFIG_DEPENDENCY + ")")
        Ensure m_ensure;
        
        @Start
        void start() {
            Assert.assertNotNull(m_config);
            Assert.assertNotNull(m_config2);
            Assert.assertEquals("bar", m_config.getFoo());
            Assert.assertEquals("bar", m_config2.getFoo());
            m_ensure.step(1);
        }
    }
}
