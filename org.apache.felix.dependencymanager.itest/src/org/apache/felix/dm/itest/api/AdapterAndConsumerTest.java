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

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AdapterAndConsumerTest extends TestBase {
    
    public void testServiceWithAdapterAndConsumer() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();

        Component provider = m.createComponent()
            .setInterface(OriginalService.class.getName(), null)
            .setImplementation(new ServiceProvider(e));

        Component consumer = m.createComponent()
            .setImplementation(new ServiceConsumer(e))
            .add(m.createServiceDependency()
                .setService(AdaptedService.class)
                .setRequired(true)
            );

        Component adapter = m.createAdapterService(OriginalService.class, null)
            .setInterface(AdaptedService.class.getName(), null)
            .setImplementation(ServiceAdapter.class);
        
        // add the provider and the adapter
        m.add(provider);
        m.add(adapter);
        // add a consumer that will invoke the adapter
        // which will in turn invoke the original provider
        m.add(consumer);
        // now validate that both have been invoked in the right order
        e.waitForStep(2, 5000);
        // remove the provider again
        m.remove(provider);
        // ensure that the consumer is stopped
        e.waitForStep(3, 5000);
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
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void invoke() {
            m_ensure.step(2);
        }
    }
    
    public static class ServiceAdapter implements AdaptedService {
        private volatile OriginalService m_originalService;
        
        public void start() { System.out.println("start"); }
        public void stop() { System.out.println("stop"); }
        public void invoke() {
            m_originalService.invoke();
        }
    }

    static class ServiceConsumer {
        private volatile AdaptedService m_service;
        private final Ensure m_ensure;
        
        public ServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        public void start() {
            m_ensure.step(1);
            m_service.invoke();
        }
        public void stop() {
            m_ensure.step(3);
        }
    }
}


