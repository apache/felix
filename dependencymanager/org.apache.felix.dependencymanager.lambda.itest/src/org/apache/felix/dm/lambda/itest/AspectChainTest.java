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

import static org.apache.felix.dm.lambda.DependencyManagerActivator.aspect;
import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AspectChainTest extends TestBase {

	public void testBuildAspectChain() {
	    DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component sp = component(m).impl(new ServiceProvider(e)).provides(ServiceInterface.class).build();
        Component sc = component(m).impl(new ServiceConsumer(e)).withSvc(ServiceInterface.class, true).build();
        Component sa2 = aspect(m, ServiceInterface.class).rank(20).impl(new ServiceAspect(e, 3)).build();
        Component sa3 = aspect(m, ServiceInterface.class).rank(30).impl(new ServiceAspect(e, 2)).build();
        Component sa1 = aspect(m, ServiceInterface.class).rank(10).impl(new ServiceAspect(e, 4)).build();
        m.add(sc);

        m.add(sp);
        m.add(sa2);
        m.add(sa3);
        m.add(sa1);
        e.step();
        e.waitForStep(5,  5000);
        
        m.remove(sa3);
        m.remove(sa2);
        m.remove(sa1);
        m.remove(sp);
        
        m.remove(sc);
    }
    
    static interface ServiceInterface {
        public void invoke(Runnable run);
    }
    
    static class ServiceProvider implements ServiceInterface {
        @SuppressWarnings("unused")
        private final Ensure m_ensure;
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void invoke(Runnable run) {
            run.run();
        }
    }
    
    static class ServiceAspect implements ServiceInterface {
        private final Ensure m_ensure;
        private volatile ServiceInterface m_parentService;
        private final int m_step;
        
        public ServiceAspect(Ensure e, int step) {
            m_ensure = e;
            m_step = step;
        }
        public void start() {
        }
        
        public void invoke(Runnable run) {
            m_ensure.step(m_step);
            m_parentService.invoke(run);
        }
        
        public void stop() {
        }
    }

    static class ServiceConsumer implements Runnable {
        private volatile ServiceInterface m_service;
        private final Ensure m_ensure;

        public ServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void init() {
            Thread t = new Thread(this);
            t.start();
        }
        
        public void run() {
            m_ensure.waitForStep(1, 2000);
            m_service.invoke(Ensure.createRunnableStep(m_ensure, 5));
        }
    }
}
