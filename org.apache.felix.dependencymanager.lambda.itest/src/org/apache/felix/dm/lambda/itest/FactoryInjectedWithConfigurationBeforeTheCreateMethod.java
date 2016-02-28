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
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;


/**
 * Use case: one component is instantiated using another factory object, and the 
 * factory object needs the configuration before the factory.create method is called.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FactoryInjectedWithConfigurationBeforeTheCreateMethod extends TestBase {
    Ensure m_e;
    
    public void testServiceInjection() {
        DependencyManager m = getDM();
        m_e = new Ensure();
        
        // Create the component that creates a configuration.
        Component configurator = component(m).impl(new Configurator("foobar")).withSvc(ConfigurationAdmin.class, true).build();
        
        // Create the object that has to be injected with the configuration before its create method is called.
        MyFactory factory = new MyFactory();
        
        // Create the Component for the MyComponent class that is created using the factory above.
        Component myComponent = component(m).factory(factory, "create").withCnf(b->b.pid("foobar").update(factory, "updated")).build();
        
        // provide the configuration
        m.add(configurator);
        
        m.add(myComponent);
        m_e.waitForStep(4, 10000);
        m.remove(myComponent);
        m.remove(configurator);
    }
    
    public void testServiceInjectionRef() {
        DependencyManager m = getDM();
        m_e = new Ensure();
        
        // Create the component that creates a configuration.
        Component configurator = component(m).impl(new Configurator("foobar")).withSvc(ConfigurationAdmin.class, true).build();
        
        // Create the object that has to be injected with the configuration before its create method is called.
        MyFactory factory = new MyFactory();
        
        // Create the Component for the MyComponent class that is created using the factory above.
        Component myComponent = component(m).factory(factory, "create").withCnf(b->b.pid("foobar").update(factory::updated)).build();
        
        // provide the configuration
        m.add(configurator);
        
        m.add(myComponent);
        m_e.waitForStep(4, 10000);
        m.remove(myComponent);
        m.remove(configurator);
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
                m_e.step(1);
                m_conf = m_ca.getConfiguration(m_pid, null);
                Hashtable<String, Object> props = new Hashtable<>();
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

    public class MyFactory {
        public void updated(Dictionary<String, Object> conf) {
            Assert.assertNotNull("configuration is null", conf);
            m_e.step(2);
        }
        
        public MyComponent create() {
            m_e.step(3);
            return new MyComponent();
        }
    }
    
    public class MyComponent {
        void start() {
            m_e.step(4);
        }        
    }
}
