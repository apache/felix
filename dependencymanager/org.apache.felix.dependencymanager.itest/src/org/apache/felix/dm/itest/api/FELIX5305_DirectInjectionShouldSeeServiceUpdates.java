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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;
import org.osgi.service.cm.ConfigurationAdmin;

public class FELIX5305_DirectInjectionShouldSeeServiceUpdates extends TestBase {

    final static String PID = "my.service.pid";
    final static Ensure m_ensure = new Ensure();

    public void testFieldInjectionWithFactoryConfigServices() throws InterruptedException {
        DependencyManager m = getDM();

        ConfigurationCreator configurator = new ConfigurationCreator(PID);
        Component compConfigurator = m.createComponent()
            .setImplementation(configurator)
            .add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        m.add(compConfigurator);

        m_ensure.waitForStep(1, 3000);        
        
        MyServices myServices = new MyServices();
        m.add(m.createComponent()
            .setImplementation(myServices)
            .setInterface(MyServices.class.getName(), null)
            .add(m.createServiceDependency().setService(Service.class, "(provider=*)").setRequired(true)));

        m.add(m.createFactoryConfigurationAdapterService(PID, "update", false /* propagate */)
            .setInterface(Service.class.getName(), null)
            .setImplementation(ServiceImpl.class));

        configurator.update("provider", "message1");          
        Thread.sleep(500);
        Assert.assertEquals("message1", myServices.getMessages().get("provider"));
        
        configurator.update("provider", "message2");          
        Thread.sleep(500);
        Assert.assertEquals("message2", myServices.getMessages().get("provider"));
    }

    public interface Service {
        String getMessage();
    }

    public static class ServiceImpl implements Service {
        // Managed by Felix DM...
        private volatile Component m_comp;
        // Locally managed...
        private volatile String m_msg;

        @Override
        public String getMessage() {
            return m_msg;
        }

        /**
         * Called by Felix DM.
         */
        protected final void start(Component comp) throws Exception {
            System.out.printf("ServiceImpl@%d started (msg = %s)%n", hashCode(), m_msg);
        }

        /**
         * Called by Felix DM.
         */
        protected final void stop(Component comp) throws Exception {
            System.out.printf("ServiceImpl@%d stopped (msg = %s)%n", hashCode(), m_msg);
        }

        /**
         * Called by Felix DM.
         */
        protected final void update(Dictionary<String, ?> config) throws Exception {
            String provider;
            if (config != null) {
                m_msg = (String) config.get("msg");
                provider = (String) config.get("provider");
            } else {
                m_msg = "<none set>";
                provider = "<unknown>";
            }

            System.out.printf("ServiceImpl@%d config updated (msg = %s; provider = %s)%n", hashCode(), m_msg, provider);
            
            Dictionary<Object, Object> props = m_comp.getServiceProperties();
            if (props == null) {
                props = new Hashtable<>();
            }

            props.put("provider", provider);
            m_comp.setServiceProperties(props);
        }
    }

    public static class MyServices {
        // Injected by Felix DM...
        private volatile Map<Service, Dictionary<String, ?>> m_services;

        public Map<String, String> getMessages() {
            Map<Service, Dictionary<String, ?>> services = m_services;

            Map<String, String> result = new HashMap<>(services.size());
            for (Service srv : services.keySet()) {
                String provider = (String) services.get(srv).get("provider");
                result.put(provider, srv.getMessage());
            }
            return result;
        }        
    }

    public static class ConfigurationCreator {
        private volatile ConfigurationAdmin m_ca;
        private org.osgi.service.cm.Configuration m_conf;
        private String m_factoryPid;

        public ConfigurationCreator(String factoryPid) {
            m_factoryPid = factoryPid;
        }

        public void start() {
            m_ensure.step(1);
        }

        public void update(String provider, String msg) {
            try {
                if (m_conf == null) {
                    m_conf = m_ca.createFactoryConfiguration(m_factoryPid, null);
                }
                Hashtable<String, String> props = new Hashtable<>();
                props.put("msg", msg);
                props.put("provider", provider);
                m_conf.update(props);
            } catch (IOException e) {
                Assert.fail("Could not update configuration: " + e.getMessage());
            }
        }

        public void stop() {
            try {
                System.out.println("Destroying conf");
                m_conf.delete();
            } catch (IOException e) {
                Assert.fail("Could not remove configuration: " + e.toString());
            }
        }
    }
}
