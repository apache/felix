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

import static org.apache.felix.dm.lambda.DependencyManagerActivator.adapter;
import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;


/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AdapterWithInstanceBoundDependencyTest extends TestBase {
    
    public void testInstanceBoundDependency() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        
        Component sp = component(m).provides(ServiceInterface.class).impl(new ServiceProvider(e)).build();
        Component sp2 = component(m).provides(ServiceInterface2.class).impl(new ServiceProvider2(e)).build();
        Component sc = component(m).impl(new ServiceConsumer(e)).autoAdd(false).withSvc(ServiceInterface3.class, true).build();
        Component sa = adapter(m, ServiceInterface.class).provides(ServiceInterface3.class).impl(new ServiceAdapter(e)).build();
        m.add(sc);
        m.add(sp);
        m.add(sp2);
        m.add(sa);
        e.waitForStep(5, 15000);
        // cleanup
        m.remove(sa);
        m.remove(sp2);
        m.remove(sp);
        m.remove(sc);
        m.clear();
        e.waitForStep(9, 5000); // make sure all components are stopped
    }       
    
    static interface ServiceInterface {
        public void invoke();
    }
    
    static interface ServiceInterface2 {
        public void invoke();
    }
    
    static interface ServiceInterface3 {
        public void invoke();
    }
    
    static class ServiceProvider2 implements ServiceInterface2 {
        private final Ensure m_ensure;

        public ServiceProvider2(Ensure ensure) {
            m_ensure = ensure;
        }

        public void invoke() {
            m_ensure.step(4);
        }
        
        public void stop() {
            m_ensure.step();
        }
    }

    static class ServiceProvider implements ServiceInterface {
        private final Ensure m_ensure;
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void invoke() {
            m_ensure.step(5);
        }
        public void stop() {
            m_ensure.step();
        }
    }
    
    static class ServiceAdapter implements ServiceInterface3 {
        private Ensure m_ensure;
        private volatile ServiceInterface m_originalService;
        private volatile ServiceInterface2 m_injectedService;
        private volatile Component m_component;
        
        public ServiceAdapter(Ensure e) {
            m_ensure = e;
        }
        public void init() {
            m_ensure.step(1);
            component(m_component, c->c.withSvc(ServiceInterface2.class, true));
        }
        public void start() {
            m_ensure.step(2);
        }
        public void invoke() {
            m_ensure.step(3);
            m_injectedService.invoke();
            m_originalService.invoke();
        }
        
        public void stop() {
            m_ensure.step();
        }
    }

    static class ServiceConsumer implements Runnable {
        volatile ServiceInterface3 m_service;
        final Ensure m_ensure;
        
        ServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void init() {
            Thread t = new Thread(this);
            t.start();
        }
        
        public void run() {
            m_service.invoke();
        }
        public void stop() {
            m_ensure.step();
        }
    }
}


