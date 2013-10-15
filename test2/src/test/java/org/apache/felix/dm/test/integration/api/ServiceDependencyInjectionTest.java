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
package org.apache.felix.dm.test.integration.api;

import junit.framework.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.test.components.Ensure;
import org.apache.felix.dm.test.integration.common.TestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class ServiceDependencyInjectionTest extends TestBase {
    @Test
    public void testServiceInjection() {
        DependencyManager m = new DependencyManager(context);
        Ensure e = new Ensure();
        // create a service provider and consumer
        ServiceProvider provider = new ServiceProvider(e);
        Component sp = m.createComponent().setImplementation(provider).setInterface(ServiceInterface2.class.getName(), null);
        Component sc = m.createComponent().setImplementation(new ServiceConsumer()).add(m.createServiceDependency().setService(ServiceInterface2.class).setRequired(true));
        Component sc2 = m.createComponent()
            .setImplementation(new ServiceConsumerNamedInjection())
            .add(m.createServiceDependency().setService(ServiceInterface2.class).setRequired(false).setAutoConfig("m_service"))
            .add(m.createServiceDependency().setService(ServiceInterface2.class).setRequired(false).setAutoConfig("m_service2"))
            .add(m.createServiceDependency().setService(ServiceInterface2.class).setRequired(false).setAutoConfig("m_service3"))
            ;
        m.add(sp);
        m.add(sc);
        m.remove(sc);
        m.add(sc2);
        m.remove(sc2);
        m.remove(sp);
        e.waitForStep(5, 5000);
    }
    
    static interface ServiceInterface {
        public void invoke();
    }
    
    static interface ServiceInterface2 extends ServiceInterface {
        public void invoke2();
    }

    static class ServiceProvider implements ServiceInterface2 {
        private final Ensure m_ensure;
        private Ensure.Steps m_invokeSteps = new Ensure.Steps(4, 5);
        private Ensure.Steps m_invoke2Steps = new Ensure.Steps(1, 2, 3);
        
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

    static class ServiceConsumerNamedInjection {
        private volatile ServiceInterface2 m_service;
        private volatile ServiceInterface m_service2;
        private volatile Object m_service3;

        public void init() {
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
