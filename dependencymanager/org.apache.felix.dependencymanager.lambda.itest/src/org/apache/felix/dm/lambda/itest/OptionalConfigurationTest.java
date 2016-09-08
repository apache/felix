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
package org.apache.felix.dm.lambda.itest;

import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Integration test for Optional ConfigurationDependency.
 * The following behaviors are tested:
 * 
 * 1) an OptionalConfigurationConsumer component defines an optional configuration dependency.
 * 2) then the component is activated: since there is no currently an available configuration, the component is called in its
 *    updated callback with a type-safe config, and the component can then be initialized using the default methods from the type-safe config interface.
 * 3) then the configuration is registered: at this point the OptionalConfigurationConsumer is called again in its updated callback , but
 *    with the real just registered configuration. So, the component can be updated with the newly just registered config.
 * 4) then the configuration is unregistered: at this point, the OptionalConfigurationConsumer is called again in updated callback, like in step 2).
 *    So, the component is re-initilized using the default methods from the type-safe config.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class OptionalConfigurationTest extends TestBase {
    final static String PID = "ConfigurationDependencyTest.pid";
    
    public void testOptionalConfigurationConsumer() {
        DependencyManager m = getDM();
        Ensure e = new Ensure();
        
        ConfigurationCreator confCreate = new ConfigurationCreator(e, PID, 3);
        Component c1 = component(m, comp -> comp.impl(confCreate).autoAdd(false).withSvc(ConfigurationAdmin.class, true));
        		
        Pojo pojo = new Pojo(e);
        Component c2 = component(m, comp -> comp.impl(pojo).autoAdd(false).withCnf(cnf -> cnf.update(Config.class, Pojo::updated).pid(PID).optional()));
        		
        m.add(c2);
        e.waitForStep(1, 5000);  // c2 called in updated with testKey="default" config
        e.waitForStep(2,  5000); // c2 called in start()
        m.add(c1);               // c1 registers a "testKey=testvalue" configuration, using config admin.
        e.waitForStep(3, 5000);  // c1 created the conf.
        e.waitForStep(4, 5000);  // c2 called in updated with testKey="testvalue"
        m.remove(c1);            // remove configuration.
        e.waitForStep(5, 5000);  // c2 called in updated with default "testkey=default" property (using the default method from the config interface.        
        m.remove(c2);            // stop the OptionalConfigurationConsumer component
        e.waitForStep(6, 5000);  // c2 stopped
    }

    
    public static interface Config {
        public default String getTestkey() { return "default"; }
    }

    static class Pojo {
        protected final Ensure m_ensure;
        protected volatile int m_updateCount;

        public Pojo(Ensure e) {
            m_ensure = e;
        }
        
        public void updated(Config cnf) { // optional configuration, called after start(), like any other optional dependency callbacks.
        	if (cnf != null) {
        		m_updateCount ++;
        		if (m_updateCount == 1) {
        			if (!"default".equals(cnf.getTestkey())) {
        				Assert.fail("Could not find the configured property.");
        			}
            		m_ensure.step(1);
        		} else if (m_updateCount == 2) {
        			if (!"testvalue".equals(cnf.getTestkey())) {
        				Assert.fail("Could not find the configured property.");
        			}
            		m_ensure.step(4);
        		} else if (m_updateCount == 3) {
        			if (!"default".equals(cnf.getTestkey())) {
        				Assert.fail("Could not find the configured property.");
        			}
            		m_ensure.step(5);
        		}
        	} else {
        		// configuration destroyed: should never happen
        		m_ensure.throwable(new Exception("lost configuration"));
        	}
        }

        public void start() {
        	m_ensure.step(2);
        }
        
        public void stop() {
        	m_ensure.step(6);
        }
    }
    
    public static class ConfigurationCreator {
        private volatile ConfigurationAdmin m_ca;
        private final Ensure m_ensure;
        Configuration m_conf;
        final String m_pid;
        final int m_initStep;
        
        public ConfigurationCreator(Ensure e, String pid, int initStep) {
            m_ensure = e;
            m_pid = pid;
            m_initStep = initStep;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
    	public void init() {
            try {
            	Assert.assertNotNull(m_ca);
                m_ensure.step(m_initStep);
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
        	m_conf.delete();  
        }
    }
}
