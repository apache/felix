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

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceDependency;
import org.apache.felix.dm.test.components.Ensure;
import org.apache.felix.dm.test.integration.common.TestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
/**
 * This testcase verify that a Service is not started if one of its extra required dependencies
 * is unavailable.
 */
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class FELIX2369_ExtraDependencyTest extends TestBase
{
    @Test
    public void testExtraDependencies() {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service consumer and provider
        Component sp1 = m.createComponent().setInterface(MyService1.class.getName(), null).setImplementation(new MyService1Impl());
        Component sc = m.createComponent().setImplementation(new MyClient(e, 1));
        
        // provides the MyService1 service (but not the MyService2, which is required by MyClient).
        m.add(sp1);
        // add MyClient (it should not be invoked in its start() method because MyService2 is not there
        m.add(sc);
        // remove MyClient (it should not be invoked in its stop() method because it should not be active, since MyService2 is not there.
        m.remove(sc);
        e.waitForStep(2, 5000);
    }
    
    public interface MyService1 {
    }
    
    public interface MyService2 {
    }

    public static class MyService1Impl implements MyService1 {
    }

    public static class MyService2Impl implements MyService2 {
    }

    // This client is not using callbacks, but instead, it uses auto config.
    public static class MyClient {
        MyService1 m_myService2; // required/unavailable      
        private Ensure m_ensure;
        private final int m_startStep;

        public MyClient(Ensure e, int startStep) {
            m_ensure = e;
            m_startStep = startStep;
        }
        
        public void init(Component s) {
            DependencyManager dm = s.getDependencyManager();
            m_ensure.step(m_startStep);
            List extra = Arrays.asList(new ServiceDependency[] {
                    dm.createServiceDependency() // this dependency is available at this point
                      .setInstanceBound(true)
                      .setService(MyService1.class)
                      .setRequired(false)
                      .setCallbacks("bind", null),
                    dm.createServiceDependency() // not available: we should not be started
                      .setInstanceBound(true)
                      .setService(MyService2.class)
                      .setRequired(true)
                      .setAutoConfig("m_myService2")
            });
            s.add(extra);
        }

        public void start() {
            Assert.fail("start should not be called since MyService2 is unavailable");
        }
        
        void bind(MyService1 s1) { // optional/available
            System.out.println("bound MyService1");
        }
        
        public void stop() {
            Assert.fail("stop should not be called since we should not be active at this point");
        }
        
        public void destroy() {
            m_ensure.step(m_startStep+1);
        }
    }
}
