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

import org.apache.felix.dependencymanager.test2.components.Ensure;
import org.apache.felix.dependencymanager.test2.integration.common.TestBase;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class TemporalServiceDependencyTest extends TestBase {
    @Test
    public void testServiceConsumptionAndIntermittentAvailability() {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component sp = m.createComponent().setImplementation(new TemporalServiceProvider(e)).setInterface(TemporalServiceInterface.class.getName(), null);
        Component sp2 = m.createComponent().setImplementation(new TemporalServiceProvider2(e)).setInterface(TemporalServiceInterface.class.getName(), null);
        Component sc = m.createComponent().setImplementation(new TemporalServiceConsumer(e)).add(m.createTemporalServiceDependency().setService(TemporalServiceInterface.class).setRequired(true));
        // add the service consumer
        m.add(sc);
        // now add the first provider
        m.add(sp);
        e.waitForStep(2, 15000);
        // and remove it again (this should not affect the consumer yet)
        m.remove(sp);
        // now add the second provider
        m.add(sp2);
        e.step(3);
        e.waitForStep(4, 15000);
        // and remove it again
        m.remove(sp2);
        // finally remove the consumer
        m.remove(sc);
        // ensure we executed all steps inside the component instance
        e.step(6);
    }

    static interface TemporalServiceInterface {
        public void invoke();
    }

    static class TemporalServiceProvider implements TemporalServiceInterface {
        private final Ensure m_ensure;
        public TemporalServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void invoke() {
            m_ensure.step(2);
        }
    }

    static class TemporalServiceProvider2 implements TemporalServiceInterface {
        private final Ensure m_ensure;
        public TemporalServiceProvider2(Ensure e) {
            m_ensure = e;
        }
        public void invoke() {
            m_ensure.step(4);
        }
    }

    static class TemporalServiceConsumer implements Runnable {
        private volatile TemporalServiceInterface m_service;
        private final Ensure m_ensure;

        public TemporalServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void init() {
            m_ensure.step(1);
            Thread t = new Thread(this);
            t.start();
        }
        
        public void run() {
            m_service.invoke();
            m_ensure.waitForStep(3, 15000);
            m_service.invoke();
        }
        
        public void destroy() {
            m_ensure.step(5);
        }
    }
}
