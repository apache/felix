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
package test;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.impl.ComponentImpl;
import org.apache.felix.dm.impl.ConfigurationDependencyImpl;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes", "unused"})
public class ConfigurationTest extends TestBase {
    @Test
    public void testConfigurationFailure() throws Throwable {
        final Ensure e = new Ensure();

        // Create our configuration dependency
        final ConfigurationDependencyImpl conf = new ConfigurationDependencyImpl();
        conf.setPid("some.pid");

        // Create another required dependency
        final SimpleServiceDependency requiredDependency = new SimpleServiceDependency();
        requiredDependency.setRequired(true);
        requiredDependency.setCallbacks("addDep", null);

        // Create our component, which will fail when handling configuration update
        ComponentImpl c = new ComponentImpl();

        c.setImplementation(new Object() {
            volatile Dictionary m_conf;

            public void updated(Dictionary conf) {
                debug("updated: conf=%s", conf);
                m_conf = conf;
                if ("invalid".equals(conf.get("conf"))) {
                    // We refuse the first configuration. 
                    debug("refusing configuration");
                    e.step(1);
                    // Set our acceptUpdate flag to true, so next update will be successful
                    throw new RuntimeException("update failed (expected)");
                }
                else {
                    debug("accepting configuration");
                    e.step(2);
                }
            }

            public void addDep() {
                if ("invalid".equals(m_conf.get("conf"))) {
                    e.throwable(new Exception("addDep should not be called"));
                }
                e.step(3);
                debug("addDep");
            }

            void init(Component c) {
                if ("invalid".equals(m_conf.get("conf"))) {
                    e.throwable(new Exception("init should not be called"));
                }
                e.step(4);
                debug("init");
            }

            void start() {
                if ("invalid".equals(m_conf.get("conf"))) {
                    e.throwable(new Exception("start should not be called"));
                }
                e.step(5);
                debug("start");
            }
        });

        // Add the dependencies
        c.add(conf);
        c.add(requiredDependency);

        // Start our component ("requiredDependency" is not yet available, so we'll stay in WAITING_FOR_REQUIRED state).
        c.start();
        
        // Enabled "requiredDependency"
        requiredDependency.add(new EventImpl());

        // Now, act as the configuration admin service and inject a wrong dependency
        try {
            Hashtable props = new Hashtable();
            props.put("conf", "invalid");
            conf.updated(props);
        }
        catch (ConfigurationException err) {
            warn("got expected configuration error");
        }
        e.waitForStep(1, 5000);
        e.ensure();
        
        // Now, inject another valid configuration
        try {
            Hashtable props = new Hashtable();
            props.put("conf", "valid");
            conf.updated(props);
        }
        catch (ConfigurationException err) {
            warn("got unexpected configuration error");
            e.throwable(err);
        }
        
        // This time, our component should be started properly.
        e.waitForStep(5, 5000);
        e.ensure();
    }
}
