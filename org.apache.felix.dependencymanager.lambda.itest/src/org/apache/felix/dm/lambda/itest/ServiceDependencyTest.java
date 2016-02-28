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

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceDependencyTest extends TestBase {
    public void testServiceRegistrationAndConsumption() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component sp = component(m).impl(new ServiceProvider(e)).provides(ServiceInterface.class).build();
        Component sc = component(m).impl(new ServiceConsumer(e)).withSvc(ServiceInterface.class, true).build();
        		
        Component sc2 = component(m).impl(new ServiceConsumerCallbacks(e))
            .withSvc(ServiceInterface.class, srv -> srv.required(false).add(ServiceConsumerCallbacks::add).remove(ServiceConsumerCallbacks::remove))
            .build();

        m.add(sp);
        m.add(sc);
        m.remove(sc);
        m.add(sc2);
        m.remove(sp);
        m.remove(sc2);
        // ensure we executed all steps inside the component instance
        e.step(6);
    }
    
    static interface ServiceInterface {
        public void invoke();
    }

    static class ServiceProvider implements ServiceInterface {
        private final Ensure m_ensure;
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void invoke() {
            m_ensure.step(2);
        }
    }

    static class ServiceConsumer {
        private volatile ServiceInterface m_service;
        private final Ensure m_ensure;

        public ServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void init() {
            m_ensure.step(1);
            m_service.invoke();
        }
        
        public void destroy() {
            m_ensure.step(3);
        }
    }

    static class ServiceConsumerCallbacks {
        private final Ensure m_ensure;

        public ServiceConsumerCallbacks(Ensure e) {
            m_ensure = e;
        }
        
        public void add(ServiceInterface service) {
            m_ensure.step(4);
        }
        public void remove(ServiceInterface service) {
            m_ensure.step(5);
        }
    }
}
