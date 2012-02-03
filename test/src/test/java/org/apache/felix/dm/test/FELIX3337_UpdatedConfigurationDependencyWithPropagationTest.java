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
package org.apache.felix.dm.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * This test validates the following scenario:
 * - Service S1 depends on a ConfigurationDependency with propagate = true
 * - Service S2 depends on S1 (and has access to the S1 configuration using the S1 service 
 *   properties (because the ConfigurationDependency is propagated)
 * - then the ConfigurationDependency is updated
 * - S1 is then called in its updated callback
 * - but S2 is not called in its "change" callback.
 * 
 * this test is related to the issue FELIX3337 
 */
@RunWith(JUnit4TestRunner.class)
public class FELIX3337_UpdatedConfigurationDependencyWithPropagationTest
{
    @Configuration
    public static Option[] configuration() {
        return options(
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version(Base.OSGI_SPEC_VERSION),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").version("1.2.4"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject()
            )
        );
    }    
    
    /*
     * This Pojo creates the configuration pid "test".
     */
    static class ConfigurationCreator {
        private volatile ConfigurationAdmin m_ca;
        org.osgi.service.cm.Configuration m_conf;
        
        public void init() {
            try {
                m_conf = m_ca.getConfiguration("test", null);
                Properties props = new Properties();
                props.put("testkey", "testvalue");
                m_conf.update(props);
            }
            catch (IOException e) {
                Assert.fail("Could not create configuration: " + e.getMessage());
            }
        }
        
        public void update() {
            try {
                Properties props = new Properties();
                props.put("testkey", "testmodifiedvalue");
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
            if (! m_initialized) {
                m_ensure.step(1);
                m_initialized = true;
            } else {
                // we are updated
                m_ensure.step(3);
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
    
    @Test
    public void testComponentWithRequiredUpdatedConfigurationAndServicePropertyPropagation(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
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
        e.waitForStep(2, 15000);
        confCreator.update();
        e.waitForStep(4, 15000);
        m.remove(s1);
        m.remove(s2);
        m.remove(s3);
        // ensure we executed all steps inside the component instance
    }
}
