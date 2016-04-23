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
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;


/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ConfigurationDependencyTest extends TestBase {
    final static String PID = "ConfigurationDependencyTest.pid";
    
    /**
     * Tests that we can provision a type-safe configuration to a component.
     */
    public void testComponentWithRequiredConfigurationWithTypeSafeConfiguration() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component s1 = m.createComponent().setImplementation(new ConfigurationConsumerWithTypeSafeConfiguration(e)).add(m.createConfigurationDependency().setCallback(null, "updated", MyConfig.class).setPid(PID).setPropagate(true));
        Component s2 = m.createComponent().setImplementation(new ConfigurationCreator(e, PID)).add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        m.add(s1);
        m.add(s2);
        e.waitForStep(1, 5000); // s2 called in init
        e.waitForStep(3, 5000); // s1 called in updated(), then in init()
        m.remove(s2);           // remove conf
        e.waitForStep(6, 5000); // s2 destroyed, s1 called in updated(null), s1 called in destroy()
        m.remove(s1);
    }
    
    public void testComponentWithRequiredConfigurationAndServicePropertyPropagation() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component s1 = m.createComponent().setImplementation(new ConfigurationConsumer(e)).setInterface(Runnable.class.getName(), null).add(m.createConfigurationDependency().setPid(PID).setPropagate(true));
        Component s2 = m.createComponent().setImplementation(new ConfigurationCreator(e, PID)).add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        Component s3 = m.createComponent().setImplementation(new ConfiguredServiceConsumer(e)).add(m.createServiceDependency().setService(Runnable.class, ("(testkey=testvalue)")).setRequired(true));
        m.add(s1);
        m.add(s2);
        m.add(s3);
        e.waitForStep(4, 5000);
        m.remove(s1);
        m.remove(s2);
        m.remove(s3);
        // ensure we executed all steps inside the component instance
        e.step(6);
    }
    
    public void testComponentWithRequiredConfigurationWithComponentArgAndServicePropertyPropagation() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component s1 = m.createComponent().setImplementation(new ConfigurationConsumerWithComponentArg(e)).setInterface(Runnable.class.getName(), null).add(m.createConfigurationDependency().setPid(PID).setPropagate(true));
        Component s2 = m.createComponent().setImplementation(new ConfigurationCreator(e, PID)).add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        Component s3 = m.createComponent().setImplementation(new ConfiguredServiceConsumer(e)).add(m.createServiceDependency().setService(Runnable.class, ("(testkey=testvalue)")).setRequired(true));
        m.add(s1);
        m.add(s2);
        m.add(s3);
        e.waitForStep(4, 50000000);
        m.remove(s1);
        m.remove(s2);
        m.remove(s3);
        // ensure we executed all steps inside the component instance
        e.step(6);
    }
    
    public void testComponentWithRequiredConfigurationAndCallbackInstanceAndServicePropertyPropagation() {
        Ensure e = new Ensure();
        ConfigurationConsumerCallbackInstance callbackInstance = new ConfigurationConsumerCallbackInstance(e);
        testComponentWithRequiredConfigurationAndCallbackInstanceAndServicePropertyPropagation(callbackInstance, "updateConfiguration", e);
    }
    
    public void testComponentWithRequiredConfigurationAndCallbackInstanceWithComponentArgAndServicePropertyPropagation() {
        Ensure e = new Ensure();
        ConfigurationConsumerCallbackInstanceWithComponentArg callbackInstance = new ConfigurationConsumerCallbackInstanceWithComponentArg(e);
        testComponentWithRequiredConfigurationAndCallbackInstanceAndServicePropertyPropagation(callbackInstance, "updateConfiguration", e);
    }
    
    public void testComponentWithRequiredConfigurationAndCallbackInstanceAndServicePropertyPropagation
    	(Object callbackInstance, String updateMethod, Ensure e) {
        DependencyManager m = getDM();
        // create a service provider and consumer
        Component s1 = m.createComponent().setImplementation(new ConfigurationConsumerWithCallbackInstance(e))
            .setInterface(Runnable.class.getName(), null)
            .add(m.createConfigurationDependency().setPid(PID).setPropagate(true).setCallback(callbackInstance, updateMethod));
        Component s2 = m.createComponent().setImplementation(new ConfigurationCreator(e, PID))
            .add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        Component s3 = m.createComponent().setImplementation(new ConfiguredServiceConsumer(e))
            .add(m.createServiceDependency().setService(Runnable.class, ("(testkey=testvalue)")).setRequired(true));
        m.add(s1);
        m.add(s2);
        m.add(s3);
        e.waitForStep(4, 5000);
        m.remove(s1);
        m.remove(s2);
        m.remove(s3);
        // ensure we executed all steps inside the component instance
        e.step(6);
    }
    
    public void testFELIX2987() {
        // mimics testComponentWithRequiredConfigurationAndServicePropertyPropagation
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component s1 = m.createComponent().setImplementation(new ConfigurationConsumer2(e)).setInterface(Runnable.class.getName(), null).add(m.createConfigurationDependency().setPid(PID).setPropagate(true));
        Component s2 = m.createComponent().setImplementation(new ConfigurationCreator(e, PID)).add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        Component s3 = m.createComponent().setImplementation(new ConfiguredServiceConsumer(e)).add(m.createServiceDependency().setService(Runnable.class, ("(testkey=testvalue)")).setRequired(true));
        m.add(s1);
        m.add(s2);
        m.add(s3);
        e.waitForStep(4, 5000);
        m.remove(s1);
        m.remove(s2);
        m.remove(s3);
        // ensure we executed all steps inside the component instance
        e.step(6);
    }
        
    public void testFELIX4907() {
        // This test validates updated(null) is not invoked when a component that have a configuration dependency is stopped.
        DependencyManager m = getDM();
        Ensure e = new Ensure();
        Component s1 = m.createComponent().setImplementation(new ConfigurationConsumer3(e)).add(m.createConfigurationDependency().setPid(PID));
        Component s2 = m.createComponent().setImplementation(new ConfigurationCreator(e, PID)).add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        m.add(s1);
        m.add(s2);
        e.waitForStep(3, 5000);
        m.remove(s1);
        e.waitForStep(4, 5000);
        m.remove(s2);
        // ensure we executed all steps inside the component instance
        e.step(6);
    }
    
    public void testFELIX4907_updated_with_null_dictionary_called_when_configuration_is_lost() {
        DependencyManager m = getDM();
        Ensure e = new Ensure();
        Component s1 = m.createComponent().setImplementation(new ConfigurationConsumer4(e)).add(m.createConfigurationDependency().setPid(PID));
        Component s2 = m.createComponent().setImplementation(new ConfigurationCreator(e, PID)).add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        m.add(s1);
        m.add(s2);
        e.waitForStep(3, 5000); // component configured and started.
        m.remove(s2); // configuration will be removed
        e.waitForStep(4, 5000); // configuration creator destroyed

        e.waitForStep(5, 5000); // consumer called in updated(null)
        e.waitForStep(6, 5000); // consumer des
        m.remove(s1);
        // ensure we executed all steps inside the component instance
        e.step(7);
    }

    class ConfigurationCreator {
        private volatile ConfigurationAdmin m_ca;
        private final Ensure m_ensure;
        Configuration m_conf;
        final String m_pid;
        
        public ConfigurationCreator(Ensure e, String pid) {
            m_ensure = e;
            m_pid = pid;
        }

        public void init() {
            try {
                warn("ConfigurationCreator.init");
            	Assert.assertNotNull(m_ca);
                m_ensure.step(1);
                m_conf = m_ca.getConfiguration(m_pid, null);
                Hashtable props = new Properties();
                props.put("testkey", "testvalue");
                m_conf.update(props);
            }
            catch (IOException e) {
                Assert.fail("Could not create configuration: " + e.getMessage());
            }
        }
        
        public void destroy() throws IOException {
            warn("ConfigurationCreator.destroy");
        	m_ensure.step();
        	m_conf.delete();  
        }
    }
    
    static class ConfigurationConsumer implements ManagedService, Runnable {
        protected final Ensure m_ensure;

        public ConfigurationConsumer(Ensure e) {
            m_ensure = e;
        }

        public void updated(Dictionary props) throws ConfigurationException {
            Assert.assertNotNull(props);
            m_ensure.step(2);
            if (!"testvalue".equals(props.get("testkey"))) {
                Assert.fail("Could not find the configured property.");
            }
        }
        
        public void run() {
            m_ensure.step(4);
        }
    }
    
    static class ConfigurationConsumer2 extends ConfigurationConsumer {
        public ConfigurationConsumer2(Ensure e) {
            super(e);
        }
    }

    static class ConfigurationConsumer3 extends ConfigurationConsumer {
    	public ConfigurationConsumer3(Ensure e) {
            super(e);
        }
        
        public void updated(Dictionary props) throws ConfigurationException {
			Assert.assertNotNull(props);
			if (!"testvalue".equals(props.get("testkey"))) {
				Assert.fail("Could not find the configured property.");
			}
			m_ensure.step(2);
        }
        
        public void start() {
            m_ensure.step(3);
        }
        
        public void stop() {
            m_ensure.step(4);
        }
    }
    
    static class ConfigurationConsumer4 extends ConfigurationConsumer {
    	volatile boolean m_configured;
    	
        public ConfigurationConsumer4(Ensure e) {
            super(e);
        }
        
        public void updated(Dictionary props) throws ConfigurationException {
        	if (! m_configured) {
                Assert.assertNotNull(props);
                if (!"testvalue".equals(props.get("testkey"))) {
                    Assert.fail("Could not find the configured property.");
                }
                m_configured = true;
                m_ensure.step(2);
        	} else {
        		m_ensure.step(5); // loosing configuration
        	}
        }
        
        public void start() {
            m_ensure.step(3);
        }
        
        public void stop() {
            m_ensure.step(6); // stopped after configuration is lost
        }
    }

    static class ConfigurationConsumerWithComponentArg extends ConfigurationConsumer {
        public ConfigurationConsumerWithComponentArg(Ensure e) {
            super(e);
        }

        public void updatedWithComponentArg(Component component, Dictionary props) throws ConfigurationException {
        	Assert.assertNotNull(component);
        	super.updated(props);
        }
    }
    
    static class ConfigurationConsumerCallbackInstance {
        private final Ensure m_ensure;

        public ConfigurationConsumerCallbackInstance(Ensure e) {
            m_ensure = e;
        }
        
        public void updateConfiguration(Dictionary props) throws Exception {
            Assert.assertNotNull(props);
            m_ensure.step(2);
            if (!"testvalue".equals(props.get("testkey"))) {
                Assert.fail("Could not find the configured property.");
            }
        }
    }
    
    static class ConfigurationConsumerCallbackInstanceWithComponentArg extends ConfigurationConsumerCallbackInstance {
    	
        public ConfigurationConsumerCallbackInstanceWithComponentArg(Ensure e) {
            super(e);
        }
        
        public void updateConfigurationWithComponentArg(Component component, Dictionary props) throws Exception {
        	Assert.assertNotNull(component);
        	super.updateConfiguration(props);
        }
    }
    
    static class ConfigurationConsumerWithCallbackInstance implements Runnable {
        private final Ensure m_ensure;

        public ConfigurationConsumerWithCallbackInstance(Ensure e) {
            m_ensure = e;
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
    
    static interface MyConfig {
        String getTestkey();
    }

    static class ConfigurationConsumerWithTypeSafeConfiguration {
        private final Ensure m_ensure;

        public ConfigurationConsumerWithTypeSafeConfiguration(Ensure e) {
            m_ensure = e;
        }

        // configuration updates is always the first invoked callback (before init).
        public void updated(Component component, MyConfig cfg) throws ConfigurationException {
        	if (cfg != null) {
        		Assert.assertNotNull(component);
        		Assert.assertNotNull(cfg);
        		m_ensure.step(2);
        		if (!"testvalue".equals(cfg.getTestkey())) {
        			Assert.fail("Could not find the configured property.");
        		}
        	} else {
        		m_ensure.step();
        	}
        }

        // called after configuration has been injected.
        public void init() {
            m_ensure.step(3); 
        }
        
        public void destroy() {
            m_ensure.step(); 
        }        
    }
}
