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

import java.util.Hashtable;
import java.util.Map;

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.osgi.framework.ServiceRegistration;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AdapterWithCallbackInstanceTest extends TestBase {
    
    public void testServiceWithAdapterAndConsumer() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();

        ServiceProvider serviceProvider = new ServiceProvider(e);
        Component provider = m.createComponent()
            .setInterface(OriginalService.class.getName(), null)
            .setImplementation(serviceProvider);

        Component consumer = m.createComponent()
            .setImplementation(new ServiceConsumer(e))
            .add(m.createServiceDependency()
                .setService(AdaptedService.class)
                .setRequired(true)
            );

        ServiceAdapterCallbackInstance callbackInstance = new ServiceAdapterCallbackInstance(e);
        Component adapter = m.createAdapterService(OriginalService.class, null, "m_originalService", 
                                                   callbackInstance, "set", "changed","unset", null, true)
            .setInterface(AdaptedService.class.getName(), null)
            .setImplementation(new ServiceAdapter(e));
        
        // add the provider and the adapter
        m.add(provider);
        m.add(adapter);
        // Checks if the callbackInstances is called, and if the adapter start method is called
        e.waitForStep(2, 5000);
        
        // add a consumer that will invoke the adapter
        // which will in turn invoke the original provider
        m.add(consumer);
        // now validate that both have been invoked in the right order
        e.waitForStep(4, 5000);
        
        // change the service properties of the provider, and check that the adapter callback instance is changed.
        serviceProvider.changeServiceProperties();
        e.waitForStep(5, 5000);
        
        // remove the provider
        m.remove(provider);
        // ensure that the consumer is stopped, the adapter callback is called in its unset method, and the adapter is stopped.
        e.waitForStep(8, 5000);
        // remove adapter and consumer
        m.remove(adapter);
        m.remove(consumer);
    }

    static interface OriginalService {
        public void invoke();
    }
    
    static interface AdaptedService {
        public void invoke();
    }
    
    static class ServiceProvider implements OriginalService {
        private final Ensure m_ensure;
        private volatile ServiceRegistration m_registration; // auto injected when started.
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void changeServiceProperties() {
            Hashtable<String, String> props = new Hashtable<>();
            props.put("foo", "bar");
            m_registration.setProperties(props);
        }
        public void invoke() {
            m_ensure.step(4);
        }
    }
    
    public static class ServiceAdapter implements AdaptedService {
        private volatile OriginalService m_originalService;
        private final Ensure m_ensure;
        
        public ServiceAdapter(Ensure e) {
            m_ensure = e;
        }

        public void start() { m_ensure.step(2); }
        public void stop() { m_ensure.step(7); }
        public void invoke() {
            m_originalService.invoke();
        }
    }

    public static class ServiceAdapterCallbackInstance {
        private final Ensure m_ensure;
        public ServiceAdapterCallbackInstance(Ensure e) {
            m_ensure = e;
        }
        
        public void set(OriginalService m_originalService) {
            m_ensure.step(1);
        }
        
        public void changed(Map<String, String> props, OriginalService m_originalService) {   
            Assert.assertEquals("bar", props.get("foo"));
            m_ensure.step(5);
        }
        
        public void unset(Map<String, String> props, OriginalService m_originalService) {            
            m_ensure.step(8);
        }
    }

    static class ServiceConsumer {
        private volatile AdaptedService m_service;
        private final Ensure m_ensure;
        
        public ServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        public void start() {
            m_ensure.step(3);
            m_service.invoke();
        }
        public void stop() {
            m_ensure.step(6);
        }
    }
}


