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

import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;

/**
 * This tests validates that autoconfiguration of adapted service on adapter class field(s) is not
 * enabled when an instance callback is used when injected adapted service.
 */
public class AdapterNoAutoConfigIfInstanceCallbackIsUsed extends TestBase {
    static Ensure m_e;

    public void testNoAutoConfigIfInstanceCallbackIsUsed() {
        m_e = new Ensure();
        DependencyManager m = getDM();
        
        // Declare S1 service
        component(m, c -> c.impl(S1Impl.class).provides(S1.class));
        
        // Declare S1 adapter
        S1AdapterCallback s1AdapterCB = new S1AdapterCallback();
        adapter(m, S1.class, a -> a.impl(S1Adapter.class).callbackInstance(s1AdapterCB).add("set"));
        
        // At this point, the s1AdapterCB.set(S1 s1) method should be called, and s1Adapter.start() method should then be called.
        // but s1 should not be injected on s1Adapter class fields.
        
        m_e.waitForStep(3, 5000);
        m.clear();
    }
    
    public void testNoAutoConfigIfInstanceCallbackIsUsedRef() {
        m_e = new Ensure();
        DependencyManager m = getDM();
        
        // Declare S1 service
        component(m, c -> c.impl(S1Impl.class).provides(S1.class));
        
        // Declare S1 adapter
        S1AdapterCallback s1AdapterCB = new S1AdapterCallback();
        adapter(m, S1.class, a -> a.impl(S1Adapter.class).add(s1AdapterCB::set));
        
        // At this point, the s1AdapterCB.set(S1 s1) method should be called, and s1Adapter.start() method should then be called.
        // but s1 should not be injected on s1Adapter class fields.
        
        m_e.waitForStep(3, 5000);
        m.clear();
    }
    
    public interface S1 {        
    }
    
    public static class S1Impl implements S1 {        
    }
    
    public static class S1Adapter {
        volatile S1 m_s1; // should not be injected by reflection
        
        void start() {
            m_e.step(2);
            Assert.assertNull(m_s1);
            m_e.step(3);
        }
    }
    
    public static class S1AdapterCallback {
        void set(S1 s1) {
            Assert.assertNotNull(s1);
            m_e.step(1);
        }
    }
    
}
