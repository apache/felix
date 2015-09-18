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
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * This test validates a corner case:
 * 
 * We have the following component players: A, BFactory, B.
 * 
 * component A defines from A.init() a required dependency on BFactory, and an optional dependency on B.
 * component A has a "start" lifecycle callback.
 * 
 * when A.bind(BFactory factory) is called, the factory.create() method is then invoked, which triggers a registration of the B Service.
 * At this point B is available, then A.start() should be called before A.bind(B).
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX5045_OptionalDependencyCBCalledBeforeStartTest extends TestBase {
    public void test_A_DependsOnBFactoryFromInit() throws Throwable {
        final DependencyManager m = getDM();
        Ensure e = new Ensure();
        
        Component bFactory = m.createComponent().setImplementation(new BFactory()).setInterface(BFactory.class.getName(), null);
        Component a = m.createComponent().setImplementation(new A(e));            
        
        // Enable first bFactory.
        m.add(bFactory);
        
        // Then enable A.
        m.add(a);
        
        // A should get BFactory, then it should instantiate B, then A.start() should be called, then A.bind(B) should be called.
        e.waitForStep(4, 5000);
        
        // Now, remove BFactory. A.unbind(B b) should be called, then 
        m.remove(bFactory);
        e.waitForStep(6, 5000);
        e.ensure();
    }
             
    public static class A {
        final Ensure m_e;

        public A(Ensure e) {
            m_e = e;
        }

        void init(Component component) {
            m_e.step(1);
        	DependencyManager dm = component.getDependencyManager();
            Dependency depBFactory = dm.createServiceDependency().setService(BFactory.class).setRequired(true).setCallbacks("bind", "unbind");
            Dependency depB = dm.createServiceDependency().setService(B.class).setRequired(false).setCallbacks("bind", "unbind");
            component.add(depBFactory, depB);
        }
        
        void bind(BFactory bFactory) {
            m_e.step(2);
            bFactory.createB();
        }

        void start() {
            m_e.step(3);
        }
            
        void bind(B b) {  
            m_e.step(4);
        }
        
        void unbind(B b) {  
            m_e.step(5);
        }
        
        void unbind(BFactory bFactory) {
        	m_e.step(6);
        }
    }
    
    public static class BFactory {
        volatile BundleContext m_bctx;
        ServiceRegistration m_registraiton;
        
        void createB() {
            m_registraiton = m_bctx.registerService(B.class.getName(), new B(), null);
        }
        
        void deleteB() {
            m_registraiton.unregister();
        }
    }
    
    public static class B {
    }
}
