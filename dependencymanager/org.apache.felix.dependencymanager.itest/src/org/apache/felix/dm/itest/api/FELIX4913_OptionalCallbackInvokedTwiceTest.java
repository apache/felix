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
 * component A optionally depends on BFactory.
 * component A optionally depends on B
 * 
 * when A.bind(BFactory factory) is called, the factory.create() method is then invoked, which triggers a registration of the B Service.
 * At this point A.bind(B) should be called back.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX4913_OptionalCallbackInvokedTwiceTest extends TestBase {
	
    public void test_A_Defines_BDependency_FromInitMethod() throws Throwable {
        final DependencyManager m = getDM();
        Ensure e = new Ensure();
        
        Component bFactory = m.createComponent().setImplementation(new BFactory()).setInterface(BFactory.class.getName(), null);
        Dependency dep = m.createServiceDependency().setService(B.class).setRequired(false).setCallbacks("bind", "unbind");
        Component a = m.createComponent()
            .setImplementation(new A(e, dep))
            .add(m.createServiceDependency().setService(BFactory.class).setRequired(false).setCallbacks("bind", "unbind"));
        
        // Enable first bFactory.
        m.add(bFactory);     
        
        // Then Enable A.
        m.add(a);
        
        // A should get BFactory, then it should instantiate B, and B should then be bound to A.
        e.waitForStep(4, 5000);
        
        // Now, remove BFactory. The AComponent should then call bFactory.removeB(), abd A.unbind(B) should be called.
        m.remove(bFactory);
        e.waitForStep(6, 5000);
        e.ensure();
        clearComponents();
    }
             
    public void test_A_Defines_BDependency_BeforeActivation() throws Throwable {
        final DependencyManager m = getDM();
        Ensure e = new Ensure();
        
        Component bFactory = m.createComponent()
            .setImplementation(new BFactory()).setInterface(BFactory.class.getName(), null);
        Component a = m.createComponent()
            .setImplementation(new A(e, null))
            .add(m.createServiceDependency().setService(B.class).setRequired(false).setCallbacks("bind", "unbind"))
            .add(m.createServiceDependency().setService(BFactory.class).setRequired(false).setCallbacks("bind", "unbind"));
        
        // Enable first bFactory.
        m.add(bFactory);     
        
        // Then Enable A.
        m.add(a);
        
        // A should get BFactory, then it should instantiate B, and B should then be bound to A.
        e.waitForStep(4, 5000);
        
        // Now, remove BFactory. The AComponent should then call bFactory.removeB(), abd A.unbind(B) should be called.
        m.remove(bFactory);
        e.waitForStep(6, 5000);
        e.ensure();
        clearComponents();
    }
             
    public static class A {
        final Ensure m_e;
        final Dependency m_BDependency;

        public A(Ensure e, Dependency bfactoryDependency) {
            m_e = e;
            m_BDependency = bfactoryDependency;
        }

        void init(Component component) {
            m_e.step(1);
            if (m_BDependency != null) {
                component.add(m_BDependency);
            }
        }
        
        void start() {
            m_e.step(2);
        }
        
        void bind(BFactory bFactory) {
            m_e.step(3);
            bFactory.createB();
        }
        
        void unbind(BFactory bFactory) {
            m_e.step(5);
            bFactory.deleteB();
        }
        
        void bind(B b) {  
            m_e.step(4);
        }
        
        void unbind(B b) {  
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
