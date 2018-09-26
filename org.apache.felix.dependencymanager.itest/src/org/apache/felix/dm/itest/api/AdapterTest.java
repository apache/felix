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
import org.apache.felix.dm.AdapterComponent;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.api.FELI5155_AdapterCallbackInstanceCalledTwice.S2;
import org.apache.felix.dm.itest.api.FELI5155_AdapterCallbackInstanceCalledTwice.S2Impl;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;

public class AdapterTest extends TestBase {
    static Ensure m_e;
    
    public void testAdapterCallbackInstanceCalledTwice() {
        DependencyManager m = getDM();
        m_e = new Ensure();
        
        S1AdapterImpl adapterImpl = new S1AdapterImpl();
        
        Component s1 = m.createComponent().setImplementation(S1Impl.class).setInterface(S1.class.getName(), null);
        Component s2 = m.createComponent().setImplementation(S2Impl.class).setInterface(S2.class.getName(), null);

        AdapterComponent s1Adapter = m.createAdapterComponent()
            .setImplementation(adapterImpl)
            .setAdaptee(S1.class, null)
        	.setAdapteeCallbacks("setS1", null, null, null)
        	.add(m.createServiceDependency().setService(S2.class).setRequired(true).setCallbacks("setS2", null, null));        

        m.add(s1);
        m.add(s1Adapter);
        m.add(s2);
        
        m_e.waitForStep(3, 5000);
        clearComponents();
    }
    
    
    public interface S1 {
    }
    
    public static class S1Impl implements S1 {
    }
    
    public interface S2 {
    }
    
    public static class S2Impl implements S2 {
    }

    public static class S1AdapterImpl {
        volatile S1 m_s1;
        
        void setS1(S1 s1) {
            m_e.step(1);
            m_s1 = s1;
        }
        
        void setS2(S2 s2) {
        	m_e.step(2);
        }
                
        void start() {
            Assert.assertNotNull("service s1 not injected", m_s1);
            m_e.step(3);
        }
    }    
}
