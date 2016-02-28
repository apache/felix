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
import org.junit.Assert;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceDependencyInjectionTest extends TestBase {
    public void testServiceInjection() {
        DependencyManager m = getDM();
        Ensure e = new Ensure();
        // create a service provider and consumer
        ServiceProvider provider = new ServiceProvider(e);
        Component sp = component(m).impl(provider).provides(ServiceInterface2.class.getName()).build();
        Component sc = component(m).impl(new ServiceConsumer()).withSvc(ServiceInterface2.class, true).build();
           
        Component sc2 = component(m) // all dependencies are optional
            .impl(new ServiceConsumerNamedInjection(false, false)) 
            .withSvc(ServiceInterface2.class, s->s.optional().autoConfig("m_service"))
            .withSvc(ServiceInterface2.class, s->s.optional().autoConfig("m_service2"))
            .withSvc(ServiceInterface2.class, s->s.optional().autoConfig("m_service3"))
            .build();
        
        Component sc3 = component(m) // second dependency is required, first and third are optional
            .impl(new ServiceConsumerNamedInjection(false, false))
            .withSvc(ServiceInterface2.class, s->s.optional().autoConfig("m_service"))
            .withSvc(ServiceInterface2.class, s->s.required().autoConfig("m_service2"))
            .withSvc(ServiceInterface2.class, s->s.optional().autoConfig("m_service3"))
            .build();
        
        Component sc4 = component(m)
            .impl(new ServiceConsumerNamedInjection(true, false)).build();
        Component sc5 = component(m)
            .impl(new ServiceConsumerNamedInjection(true, true)).build();
        m.add(sp);
        m.add(sc);
        m.remove(sc);
        m.add(sc2);
        m.remove(sc2);
        m.add(sc3);
        m.remove(sc4);
        m.add(sc4);
        m.remove(sc4);
        m.add(sc5);
        m.remove(sc5);
        m.remove(sp);
        e.waitForStep(11, 5000);
        m.clear();
    }
    
    static interface ServiceInterface {
        public void invoke();
    }
    
    static interface ServiceInterface2 extends ServiceInterface {
        public void invoke2();
    }

    static class ServiceProvider implements ServiceInterface2 {
        private final Ensure m_ensure;
        private Ensure.Steps m_invokeSteps = new Ensure.Steps(4, 5, 7, 8, 10, 11, 13, 14);
        private Ensure.Steps m_invoke2Steps = new Ensure.Steps(1, 2, 3, 6, 9, 12);
        
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }

        public void invoke() {
            System.out.println("invoke");
            m_ensure.steps(m_invokeSteps);
        }
        
        public void invoke2() {
            System.out.println("invoke2");
            m_ensure.steps(m_invoke2Steps);
        }
    }

    static class ServiceConsumer {
        private volatile ServiceInterface2 m_service;
        private volatile ServiceInterface2 m_service2;
        
        public void init() {
            // invoke the second method of the interface via both injected members, to ensure
            // neither of them is a null object (or null)
            m_service.invoke2();
            m_service2.invoke2();
            Assert.assertEquals("Both members should have been injected with the same service.", m_service, m_service2);
        }
    }

    class ServiceConsumerNamedInjection {
        private volatile ServiceInterface2 m_service;
        private volatile ServiceInterface m_service2;
        private volatile Object m_service3;
        private final boolean m_secondDependencyRequired;
        private final boolean m_instanceBound;
        
        ServiceConsumerNamedInjection(boolean instanceBound, boolean withSecondRequired) {
            m_secondDependencyRequired = withSecondRequired;
            m_instanceBound = instanceBound;
        }

        public void init(Component c) {
            if (m_instanceBound) {
                DependencyManager m = c.getDependencyManager();
                c.add(m.createServiceDependency().setService(ServiceInterface2.class).setRequired(false).setAutoConfig("m_service"),
                    m.createServiceDependency().setService(ServiceInterface2.class).setRequired(m_secondDependencyRequired).setAutoConfig("m_service2"),
                    m.createServiceDependency().setService(ServiceInterface2.class).setRequired(false).setAutoConfig("m_service3"));
            } else {
                check();
            }
        }
        
        public void start() {
            if (m_instanceBound) {
                check();
            }
        }
        
        public void check() {
            warn("ServiceConsumerNamedInjectionInstanceBound: m_service=%s, m_service2=%s, m_service3=%s", m_service, m_service2, m_service3);
            // invoke the second method
            m_service.invoke2();
            // invoke the first method (twice)
            m_service2.invoke();
            ((ServiceInterface) m_service3).invoke();
            Assert.assertNotNull("Should have been injected", m_service);
            Assert.assertNotNull("Should have been injected", m_service2);
            Assert.assertNotNull("Should have been injected", m_service3);
            Assert.assertEquals("Members should have been injected with the same service.", m_service, m_service2);
            Assert.assertEquals("Members should have been injected with the same service.", m_service, m_service3);          
        }
    }
}
