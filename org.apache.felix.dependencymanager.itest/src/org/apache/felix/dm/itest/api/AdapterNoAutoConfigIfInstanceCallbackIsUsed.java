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
import org.junit.Assert;

/**
 * This tests validates that autoconfiguration of adapted service on adapter class field(s) is not
 * enabled when an instance callback is used when injected adapted service.
 */
public class AdapterNoAutoConfigIfInstanceCallbackIsUsed extends TestBase {
    final static Ensure m_e = new Ensure();

    public void testNoAutoConfigIfINstanceCallbackIsUsed() {
        DependencyManager m = getDM();
        
        // Declare S1 service
        Component s1 = m.createComponent().setImplementation(S1Impl.class).setInterface(S1.class.getName(), null);
        m.add(s1);
        
        // Declare S1 adapter
        S1AdapterCallback s1AdapterCB = new S1AdapterCallback();
        Component s1Adapter = m.createAdapterService(S1.class, null, null, s1AdapterCB, "set", null, null, null, false)
            .setImplementation(S1Adapter.class);
        m.add(s1Adapter);
        
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
