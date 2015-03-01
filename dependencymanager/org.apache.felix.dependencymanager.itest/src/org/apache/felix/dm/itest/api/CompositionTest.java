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
public class CompositionTest extends TestBase {
    public void testComposition() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component sp = m.createComponent().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), null);
        Component sc = m.createComponent().setImplementation(new ServiceConsumer(e))
                                      .setComposition("getComposition")
                                      .add(m.createServiceDependency().setService(ServiceInterface.class).setRequired(true).setCallbacks("add", null));
        m.add(sp);
        m.add(sc);
        // ensure we executed all steps inside the component instance
        e.step(6);
        m.clear();
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
            m_ensure.step(4);
        }
    }

    static class ServiceConsumer {
        private final Ensure m_ensure;
        private ServiceConsumerComposite m_composite;
        @SuppressWarnings("unused")
        private ServiceInterface m_service;

        public ServiceConsumer(Ensure e) {
            m_ensure = e;
            m_composite = new ServiceConsumerComposite(m_ensure);
        }
        
        public Object[] getComposition() {
            return new Object[] { this, m_composite };
        }
        
        void add(ServiceInterface service) {
            m_ensure.step(1);
            m_service = service; // This method seems to not being called anymore 
        }
        
        void start() {
            m_composite.invoke();
            m_ensure.step(5); 
        }
    }
    
    static class ServiceConsumerComposite {
        ServiceInterface m_service;
        private Ensure m_ensure;
        
        ServiceConsumerComposite(Ensure ensure)
        {
            m_ensure = ensure;
        }

        void add(ServiceInterface service) {

            m_ensure.step(2);
            m_service = service;
        }

        void invoke()
        {
            m_ensure.step(3);
            m_service.invoke();
        }
    }
}
