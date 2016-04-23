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

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;


/**
 * Tests for type-safe configuration using either an annotation or an interface having
 * some default methods.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX5238_TypeSafeConfigWithDefaultMethodTest extends TestBase {
    final static String PID = "ConfigurationDependencyTest.pid";
    
    /**
     * Tests that we can provision a type-safe config annotation to a component.
     */
    public void testComponentWithRequiredConfigurationWithTypeSafeConfigAnnotation() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        ConfigurationConsumer1 consumer = new ConfigurationConsumer1(e);
        Component s1 = m.createComponent()
        		.setImplementation(consumer).add(m.createConfigurationDependency().setCallback("updated", MyConfigAnnot.class).setPid(PID).setPropagate(true));
        Component s2 = m.createComponent()
        		.setImplementation(new ConfigurationCreator(e, PID, 1)).add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        m.add(s1);
        m.add(s2);
        e.waitForStep(1, 5000); // s2 called in init
        e.waitForStep(3, 5000); // s1 called in updated(), then in init()
        m.remove(s2);           // remove conf
        e.waitForStep(5, 5000); // s1 called in updated(null), s1 called in destroy()
        m.remove(s1);
    }
        
    /**
     * Tests that we can provision a type-safe config annotation to a component.
     */
    public void testComponentWithRequiredConfigurationWithTypeSafeConfigInterfaceWithDefaultMethod() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        ConfigurationConsumer2 consumer = new ConfigurationConsumer2(e);
        Component s1 = m.createComponent()
        		.setImplementation(consumer).add(m.createConfigurationDependency().setCallback("updated", MyConfigInterface.class).setPid(PID).setPropagate(true));
        Component s2 = m.createComponent()
        		.setImplementation(new ConfigurationCreator(e, PID, 1)).add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        m.add(s1);
        m.add(s2);
        e.waitForStep(1, 5000); // s2 called in init
        e.waitForStep(3, 5000); // s1 called in updated(), then in init()
        m.remove(s2);           // remove conf
        e.waitForStep(5, 5000); // s1 called in updated(null), s1 called in destroy()
        m.remove(s1);
    }
        
    public static @interface MyConfigAnnot {
        String getTestkey();
        String getTestkey2() default "123";
    }
    
    public static interface MyConfigInterface {
    	String getTestkey();
    	default String getTestkey2() { return "123"; }
    }

    static class ConfigurationConsumerBase {
        protected final Ensure m_ensure;

        public ConfigurationConsumerBase(Ensure e) {
            m_ensure = e;
        }

        // called after configuration has been injected.
        public void init() {
            m_ensure.step(3); 
        }
        
        public void destroy() {
            m_ensure.step(); 
        }        
    }

    static class ConfigurationConsumer1 extends ConfigurationConsumerBase {
        public ConfigurationConsumer1(Ensure e) {
            super(e);
        }

        // configuration updates is always the first invoked callback (before init).
        public void updated(Component component, MyConfigAnnot cfg) throws ConfigurationException {
        	if (cfg != null) {
        		Assert.assertNotNull(component);
        		Assert.assertNotNull(cfg);
        		if (!"testvalue".equals(cfg.getTestkey())) {
        			Assert.fail("Could not find the configured property.");
        		}
        		if (!"123".equals(cfg.getTestkey2())) {
        			Assert.fail("Could not find the configured property.");
        		}
        		m_ensure.step(2);
        	} else {
        		m_ensure.step();
        	}
        }
    }
    
    static class ConfigurationConsumer2 extends ConfigurationConsumerBase {
        public ConfigurationConsumer2(Ensure e) {
            super(e);
        }

        // configuration updates is always the first invoked callback (before init).
        public void updated(Component component, MyConfigInterface cfg) throws ConfigurationException {
        	if (cfg != null) {
        		Assert.assertNotNull(component);
        		Assert.assertNotNull(cfg);
        		if (!"testvalue".equals(cfg.getTestkey())) {
        			Assert.fail("Could not find the configured property.");
        		}
        		if (!"123".equals(cfg.getTestkey2())) {
        			Assert.fail("Could not find the configured property.");
        		}
        		m_ensure.step(2);
        	} else {
        		m_ensure.step();
        	}
        }
    }
}
