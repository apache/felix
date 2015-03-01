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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.FactoryConfigurationAdapterService;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.LifecycleController;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.itest.util.Ensure;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Test a FactoryConfigurationAdapterService which provides its interface using a @ServiceLifecycle.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes", "serial"})
public class FactoryConfigurationAdapterServiceTestWithPublisher {
    public final static String PID="FactoryConfigurationAdapterServiceTestWithPublisher.PID";
    public final static String ENSURE = "FactoryConfigurationAdapterServiceTestWithPublisher";
    
    public interface Provider {
    }

    @Component
    public static class Consumer {
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_sequencer;

        @ServiceDependency(required = false, removed = "unbind")
        void bind(Map properties, Provider provider) {
            m_sequencer.step(1);
            // check ProviderImpl properties
            if ("bar".equals(properties.get("foo"))) {
                m_sequencer.step(2);
            }
            // check extra ProviderImpl properties (returned by start method)
            if ("bar2".equals(properties.get("foo2"))) {
                m_sequencer.step(3);
            }
            // check Factory Configuration properties
            if ("bar3".equals(properties.get("foo3"))) {
                m_sequencer.step(4);
            }
        }

        void unbind(Provider provider) {
            m_sequencer.step(5);
        }
    }

    @Component
    public static class Configurator {
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_sequencer;
        
        volatile Configuration m_conf;

        @ServiceDependency
        volatile ConfigurationAdmin m_cm;
        
        @Start
        void start() throws IOException {
            m_conf = m_cm.createFactoryConfiguration(PID, null);
            m_conf.update(new Hashtable() {
                {
                    put("foo3", "bar3");
                }
            });
        }
        
        @Stop
        void stop() throws IOException {
            m_conf.delete();
        }
    }

    @FactoryConfigurationAdapterService(propagate = true, properties = {@Property(name = "foo", value = "bar")}, factoryPid = PID, updated = "updated")
    public static class ProviderImpl implements Provider {
        @LifecycleController
        volatile Runnable m_publisher; // injected and used to register our service

        @LifecycleController(start = false)
        volatile Runnable m_unpublisher; // injected and used to unregister our service

        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_sequencer;

        void updated(Dictionary conf) {
        }

        @Init
        void init() {
            // register service in 1 second
            Utils.schedule(m_publisher, 1000);
            // unregister the service in 2 seconds
            Utils.schedule(m_unpublisher, 2000);
        }

        @Start
        Map start() {
            // Add some extra service properties ... they will be appended to the one we have defined
            // in the @Service annotation.
            return new HashMap() {
                {
                    put("foo2", "bar2");
                }
            };
        }

    }
}
