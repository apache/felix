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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;

/**
 * Test for FELIX-4334 issue.
 * 
 * Three components: A, B and C
 * 
 * - A provided with property foo=bar
 * - B adapts A, B has no filters on A, and B.init() method adds an instance bound required dependency to C.
 * - C depends on A(foo=bar)
 * - Now someone modifies the service properties of A: foo=bar2
 * - As a result of that, C becomes unavailable and is unbound from B.
 * - Since B has an instance bound required dependency to C: B should not be destroyed: it should be called in B.stop(), B.remove(C), B.change(A, "foo=bar2))
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AdapterWithModifiedInstanceBoundDependencyTest extends TestBase {
    public static interface A {
    }
    
    static class AImpl implements A {
        final Ensure m_e;
        AImpl(Ensure e) {
            m_e = e;
        }        
    }
    
    public static interface C {
    }

    static class CImpl implements C {
        volatile A m_a;
    }
    
    public static interface B {
    }
    
    static class BImpl implements B {
        final Ensure m_e;
        volatile A m_a;
        volatile C m_c;
        
        BImpl(Ensure e) {
            m_e = e;
        }
        
        public void add(A a) {
            m_e.step(1);
        }

        void init(Component c) {
            m_e.step(2);
            DependencyManager dm = c.getDependencyManager();
            c.add(dm.createServiceDependency().setService(C.class).setRequired(true).setCallbacks("add", "remove"));
        }      
        
        public void add(C c) {
            m_e.step(3);
        }
        
        public void start() {
            m_e.step(4);            
        }
        
        public void stop() { // C becomes unsatisfied when A properties are changed to foo=bar2
            m_e.step(5);
        }

        public void remove(C c) {
            m_e.step(6);
        }

        public void change(Map properties, A a) {
            Assert.assertEquals("bar2", properties.get("foo"));
            m_e.step(7);
        }
        
        public void destroy() {
            m_e.step(8);
        }

        public void remove(A a) {   
            m_e.step(9);
        }                    
    }
    
    public void testAdapterWithChangedInstanceBoundDependency() {
        DependencyManager m = getDM();
        Ensure e = new Ensure();

        Dictionary props = new Hashtable();
        props.put("foo", "bar");
        Component a = m.createComponent()
                .setImplementation(new AImpl(e))
                .setInterface(A.class.getName(), props);
        
        Component b = m.createAdapterService(A.class, null, "add", "change", "remove")
                .setInterface(B.class.getName(), null)
                .setImplementation(new BImpl(e));                
        
        Component c = m.createComponent()
                .setImplementation(new CImpl())
                .setInterface(C.class.getName(), null)
                .add(m.createServiceDependency().setService(A.class, "(foo=bar)").setRequired(true));                     
              
        m.add(a);
        m.add(c);
        m.add(b);
        
        e.waitForStep(4, 5000);
        
        System.out.println("changing A props ...");
        props = new Hashtable();
        props.put("foo", "bar2");
        a.setServiceProperties(props);
        
        e.waitForStep(7, 5000);                
        
        m.remove(c);
        m.remove(a);
        m.remove(b);
        
        e.waitForStep(9, 5000);                
    }
}
