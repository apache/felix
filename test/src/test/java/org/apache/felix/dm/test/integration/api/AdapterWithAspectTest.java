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

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.test.components.Ensure;
import org.apache.felix.dm.test.integration.common.TestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.ServiceReference;

@RunWith(PaxExam.class)
public class AdapterWithAspectTest extends TestBase {
    
    @Test
    public void testAdapterWithAspectMultipleTimes() {
        // TODO this test is broken, it assumes that the order in which listeners are added to the BundleContext will also
        // be the order in which they're invoked (which from a spec point of view is not true)
        
        
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        
        // create a service provider and consumer
        Component provider = m.createComponent()
            .setImplementation(new ServiceProvider())
            .setInterface(OriginalService.class.getName(), null);

        Component consumer = m.createComponent()
            .setImplementation(new ServiceConsumer(e))
            .add(m.createServiceDependency()
                .setService(AdaptedService.class)
                .setCallbacks("add", null, "remove", "swap")
            );
        Component adapter = m.createAdapterService(OriginalService.class, null, "add", null, "remove", "swap")
            .setInterface(AdaptedService.class.getName(), null)
            .setImplementation(new ServiceAdapter(e,1));
        
        Component adapter2 = m.createAdapterService(OriginalService.class, null, "add", null, "remove", "swap")
            .setInterface(AdaptedService.class.getName(), null)
            .setImplementation(new ServiceAdapter(e,2));
        
        Component aspect = m.createAspectService(OriginalService.class, null, 10, null)
            .setImplementation(ServiceAspect.class);
        
        m.add(provider);

        int stepsInLoop = 10;
        int loops = 10;
        for (int loop = 0; loop < loops; loop++) {
            int offset = stepsInLoop * loop;
            
            System.out.println("add adapter");
            m.add(adapter);
            System.out.println("add consumer");
            m.add(consumer);
            e.waitForStep(1 + offset, 5000);
            System.out.println("add aspect");
            m.add(aspect);
            // a swap is expected on the adapter
            e.waitForStep(2 + offset, 5000);
            System.out.println("add adapter2");
            m.add(adapter2);
            // another aspect adapter will appear
            e.waitForStep(4 + offset, 5000);
            System.out.println("remove provider");
            m.remove(provider);
            // two times:
            // the aspect adapter will disappear
            // the original adapter will (briefly) appear
            // the original adapter will disappear
            
            // TODO the test will fail somewhere here most of the time
            
            e.waitForStep(8 + offset, 5000);
            System.out.println("remove consumer");
            m.remove(consumer);
            
            // nothing should happen, all consumed services were already gone
            System.out.println("add provider");
            m.add(provider);
            // still nothing should happen
            System.out.println("remove adapter");
            m.remove(adapter);
            System.out.println("remove adapter2");
            m.remove(adapter2);
            System.out.println("remove aspect");
            m.remove(aspect);
        }
        m.remove(provider);
        e.waitForStep(stepsInLoop * loops, 5000);
    }
    
    static interface OriginalService {
        public void invoke();
    }
    
    static interface AdaptedService {
        public void invoke();
    }
    
    static class ServiceProvider implements OriginalService {
        public void start() {
            System.out.println("...provider started");
        }
        public void invoke() {
        }
        
        @Override
        public String toString() {
            return "Provider";
        }
    }
    
    public static class ServiceAdapter implements AdaptedService {
        private volatile OriginalService m_originalService;
		private final Ensure m_ensure;
		private final int m_nr;
        
        public ServiceAdapter(Ensure e, int nr) {
			this.m_ensure = e;
			this.m_nr = nr;
        }
        public void init() {
        }
        public void start() {
            System.out.println("...adapter started");
        }
        public void invoke() {
            m_originalService.invoke();
        }
        public void stop() {
        }
        
        void add(ServiceReference ref, OriginalService originalService) {
        	m_originalService = originalService;
        	m_ensure.step();
        	System.out.println("adapter" + m_nr + " add: " + originalService);
        }
        
        void remove(ServiceReference ref, OriginalService originalService) {
        	System.out.println("adapter" + m_nr + " rem: " + originalService);
        	m_originalService = null;
        }
        
        void swap(ServiceReference oldRef, OriginalService oldService, ServiceReference newRef, OriginalService newService) {
        	m_originalService = newService;
        	m_ensure.step();
        	System.out.println("adapter" + m_nr + " swp: " + newService);
        }
        
        @Override
        public String toString() {
            return "Adapter on " + m_originalService;
        }
    }
    
    public static class ServiceAspect implements OriginalService {
        volatile OriginalService m_service;
        
        public void start() {
            System.out.println("...aspect started");
        }
        
        public void invoke() {
            m_service.invoke();
        }
        
        @Override
        public String toString() {
            return "Aspect on " + m_service;
        }
    }

    public static class ServiceConsumer {
        Ensure m_ensure;
        
        public ServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
//        public void init() {
//            Thread t = new Thread(this);
//            t.start();
//        }
        public void start() {
            System.out.println("Consumer starting...");
        }
        
        int counter = 0;
        public void add(ServiceReference ref, AdaptedService service) {
            counter++;
            System.out.println("consumer add: " + counter + " " + service);
            m_ensure.step();
        }
        public void remove(ServiceReference ref, AdaptedService service) {
            counter--;
            System.out.println("consumer rem: " + counter + " " + service);
            m_ensure.step();
        }
        public void swap(ServiceReference oldRef, AdaptedService oldService, ServiceReference newRef, AdaptedService newService) {
        	System.out.println("consumer swp: " + counter + " " + newService);
        	m_ensure.step();
        }
        
//        public void run() {
//            m_service.invoke();
//        }
    }
}


