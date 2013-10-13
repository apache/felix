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
package org.apache.felix.dependencymanager.test2.integration.api;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.felix.dependencymanager.test2.components.Ensure;
import org.apache.felix.dependencymanager.test2.integration.common.TestBase;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;


@RunWith(PaxExam.class)
public class ConfigurationDependencyTest extends TestBase {
    @Test
    public void testComponentWithRequiredConfigurationAndServicePropertyPropagation() {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component s1 = m.createComponent().setImplementation(new ConfigurationConsumer(e)).setInterface(Runnable.class.getName(), null).add(m.createConfigurationDependency().setPid("test").setPropagate(true));
        Component s2 = m.createComponent().setImplementation(new ConfigurationCreator(e)).add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        Component s3 = m.createComponent().setImplementation(new ConfiguredServiceConsumer(e)).add(m.createServiceDependency().setService(Runnable.class, ("(testkey=testvalue)")).setRequired(true));
        m.add(s1);
        m.add(s2);
        m.add(s3);
        e.waitForStep(4, 15000);
        m.remove(s1);
        m.remove(s2);
        m.remove(s3);
        // ensure we executed all steps inside the component instance
        e.step(5);
    }
    
    @Test
    public void testFELIX2987() {
        // mimics testComponentWithRequiredConfigurationAndServicePropertyPropagation
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component s1 = m.createComponent().setImplementation(new ConfigurationConsumer2(e)).setInterface(Runnable.class.getName(), null).add(m.createConfigurationDependency().setPid("test").setPropagate(true));
        Component s2 = m.createComponent().setImplementation(new ConfigurationCreator(e)).add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        Component s3 = m.createComponent().setImplementation(new ConfiguredServiceConsumer(e)).add(m.createServiceDependency().setService(Runnable.class, ("(testkey=testvalue)")).setRequired(true));
        m.add(s1);
        m.add(s2);
        m.add(s3);
        e.waitForStep(4, 15000);
        m.remove(s1);
        m.remove(s2);
        m.remove(s3);
        // ensure we executed all steps inside the component instance
        e.step(5);
    }


    static class ConfigurationCreator {
        private volatile ConfigurationAdmin m_ca;
        private final Ensure m_ensure;
        
        public ConfigurationCreator(Ensure e) {
            m_ensure = e;
        }

        public void init() {
            try {
                m_ensure.step(1);
                org.osgi.service.cm.Configuration conf = m_ca.getConfiguration("test", null);
                Hashtable props = new Properties();
                props.put("testkey", "testvalue");
                conf.update(props);
            }
            catch (IOException e) {
                Assert.fail("Could not create configuration: " + e.getMessage());
            }
        }
    }
    
    static class ConfigurationConsumer2 extends ConfigurationConsumer {
        public ConfigurationConsumer2(Ensure e) {
            super(e);
        }
    }

    static class ConfigurationConsumer implements ManagedService, Runnable {
        private final Ensure m_ensure;

        public ConfigurationConsumer(Ensure e) {
            m_ensure = e;
        }

        public void updated(Dictionary props) throws ConfigurationException {
            if (props != null) {
                m_ensure.step(2);
                if (!"testvalue".equals(props.get("testkey"))) {
                    Assert.fail("Could not find the configured property.");
                }
            }
        }
        
        public void run() {
            m_ensure.step(4);
        }
    }

    static class ConfiguredServiceConsumer {
        private final Ensure m_ensure;
        private volatile Runnable m_runnable;

        public ConfiguredServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void init() {
            m_ensure.step(3);
            m_runnable.run();
        }
    }
}
