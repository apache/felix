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

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.ops4j.pax.exam.junit.PaxExam;
import org.apache.felix.dependencymanager.test2.components.Ensure;
import org.apache.felix.dependencymanager.test2.integration.common.TestBase;

/**
 * Scenario: 
 * 
 * A service consumer consumes an adapter service. The adapter service adapts a service provider.
 * An aspect is added to the service provider. This should not impact the service consumer.
 * Expected behavior is transparent replacement of the service the adapter adapts with the aspect service.
 *
 */
@RunWith(PaxExam.class)
public class FELIX3186_AspectAdapterTest extends TestBase {
    @Test
    public void testAdapterWithAspectMultipleTimes() throws Exception {
        // TODO this test is broken, it assumes that the order in which listeners are added to the BundleContext will also
        // be the order in which they're invoked (which from a spec point of view is not true)
        
        
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        
        // create a service provider
        Component provider = m.createComponent()
            .setImplementation(new ServiceProvider())
            .setInterface(OriginalService.class.getName(), null);

        // create a adapter on the provider
        Component adapter = m.createAdapterService(OriginalService.class, null)
        .setInterface(AdaptedService.class.getName(), null)
        .setImplementation(ServiceAdapter.class);
        
        // create a consumer for the adapted service
        Component consumer = m.createComponent()
            .setImplementation(new ServiceConsumer(e))
            .add(m.createServiceDependency()
                .setService(AdaptedService.class)
                .setCallbacks("add", "remove")
                .setRequired(true)
            );
        
        // create an aspect on the service provider
        Component aspect = m.createAspectService(OriginalService.class, null, 10, null)
            .setImplementation(ServiceAspect.class);

        // we first start the provider, the adapter and the consumer
        m.add(provider);
        m.add(adapter);
        m.add(consumer);
        // now wait until the callback method is invoked on the consumer
        e.waitForStep(1, 5000);
        // now we add an aspect on top of the provided service, which 
        // should not affect our consumer at all
        m.add(aspect);
        m.remove(aspect);
        // now we remove the consumer, adapter and provider
        m.remove(consumer);
        m.remove(adapter);
        m.remove(provider);
        // that should have triggered step 2
        e.waitForStep(2, 5000);
        // make sure we don't have extra steps by explicitly going to step 3
        e.step(3);
    }
    
    static interface OriginalService {
        public void invoke();
    }
    
    static interface AdaptedService {
        public void invoke();
    }
    
    static class ServiceProvider implements OriginalService {
        public void invoke() {
        }
        
        @Override
        public String toString() {
            return "Provider";
        }
    }
    
    public static class ServiceAdapter implements AdaptedService {
        private volatile OriginalService m_originalService;
        
        public void invoke() {
            m_originalService.invoke();
        }
        @Override
        public String toString() {
            return "Adapter on " + m_originalService;
        }
    }
    
    public static class ServiceAspect implements OriginalService {
        volatile OriginalService m_service;
        
        public void invoke() {
            m_service.invoke();
        }
        
        @Override
        public String toString() {
            return "Aspect on " + m_service;
        }
    }

    public static class ServiceConsumer {
        Ensure m_ensure;
        
        public ServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void add(ServiceReference ref, AdaptedService service) {
            m_ensure.step();
        }
        public void remove(ServiceReference ref, AdaptedService service) {
            m_ensure.step();
        }
    }
}


