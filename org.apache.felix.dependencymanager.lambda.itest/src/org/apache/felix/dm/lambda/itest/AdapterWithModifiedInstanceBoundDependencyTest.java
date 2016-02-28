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

import java.util.Map;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;

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
    
    public static class BImpl implements B {
        final Ensure m_e;
        volatile A m_a;
        volatile C m_c;
        
        BImpl(Ensure e) {
            m_e = e;
        }
        
        void init(Component comp) {
            m_e.step(2);
            component(comp, c->c.withSvc(C.class, s->s.add("addC").remove("removeC")));
        }      
        
        void addA(A a, Map<String, Object> properties) {
            m_e.step(1);
        }

        public void addC(C c) {
            m_e.step(3);
        }
        
        public void start() {
            m_e.step(4);            
        }
        
        public void stop() { // C becomes unsatisfied when A properties are changed to foo=bar2
            m_e.step(5);
        }

        public void removeC(C c) {
            m_e.step(6);
        }

        public void changeA(A a, Map<String, Object> properties) {
            Assert.assertEquals("bar2", properties.get("foo"));
            m_e.step(7);
        }
        
        public void destroy() {
            m_e.step(8);
        }

        public void removeA(A a, Map<String, Object> properties) {   
            m_e.step(9);
        }                    
    }
    
    public void testAdapterWithChangedInstanceBoundDependency() {
        DependencyManager m = getDM();
        Ensure e = new Ensure();

        Component a = component(m).impl(new AImpl(e)).provides(A.class).properties(foo -> "bar").build();
        Component b = adapter(m, A.class).provides(B.class).impl(new BImpl(e)).add("addA").change("changeA").remove("removeA").build();
        Component c = component(m).impl(new CImpl()).provides(C.class).withSvc(A.class, "(foo=bar)", true).build();
                      
        m.add(a);
        m.add(c);
        m.add(b);
        
        e.waitForStep(4, 5000);
        
        System.out.println("changing A props ...");
        Properties props = new Properties();
        props.put("foo", "bar2");
        a.setServiceProperties(props);
        
        e.waitForStep(7, 5000);                
        
        m.remove(c);
        m.remove(a);
        m.remove(b);
        
        e.waitForStep(9, 5000);                
    }
    
    public void testAdapterWithChangedInstanceBoundDependencyRef() {
        DependencyManager m = getDM();
        Ensure e = new Ensure();

        Component a = component(m).impl(new AImpl(e)).provides(A.class).properties(foo -> "bar").build();        
        Component b = adapter(m, A.class).impl(new BImpl(e)).provides(B.class).add(BImpl::addA).change(BImpl::changeA).remove(BImpl::removeA).build();        
        Component c = component(m).impl(new CImpl()).provides(C.class).withSvc(A.class, s -> s.filter("(foo=bar)")).build();
                      
        m.add(a);
        m.add(c);
        m.add(b);
        
        e.waitForStep(4, 5000);
        
        System.out.println("changing A props ...");
        Properties props = new Properties();
        props.put("foo", "bar2");
        a.setServiceProperties(props);
        
        e.waitForStep(7, 5000);                
        
        m.remove(c);
        m.remove(a);
        m.remove(b);
        
        e.waitForStep(9, 5000);                
    }
}
