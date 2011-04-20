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
package org.apache.felix.dm.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@RunWith(JUnit4TestRunner.class)
public class AdapterWithAspectTest extends Base {
    @Configuration
    public static Option[] configuration() {
        return options(
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version(Base.OSGI_SPEC_VERSION),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject()
            )
        );
    }
    
    @Test
    public void testAdapterWithAspectMultipleTimes(BundleContext context) {
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
                .setCallbacks("add", "remove")
            );
        Component adapter = m.createAdapterService(OriginalService.class, null)
            .setInterface(AdaptedService.class.getName(), null)
            .setImplementation(ServiceAdapter.class);
        
        Component adapter2 = m.createAdapterService(OriginalService.class, null)
            .setInterface(AdaptedService.class.getName(), null)
            .setImplementation(ServiceAdapter.class);
        
        Component aspect = m.createAspectService(OriginalService.class, null, 10, null)
            .setImplementation(ServiceAspect.class);
        
        m.add(provider);

        int stepsInLoop = 10;
        int loops = 10;
        for (int loop = 0; loop < loops; loop++) {
            int offset = stepsInLoop * loop;
            
            m.add(adapter);
            m.add(consumer);
            e.waitForStep(1 + offset, 5000);
            m.add(aspect);
            // the aspect adapter will appear
            // the original adapter will disappear
            e.waitForStep(3 + offset, 5000);
            m.add(adapter2);
            // another aspect adapter will appear
            e.waitForStep(4 + offset, 5000);
            m.remove(provider);
            // two times:
            // the aspect adapter will disappear
            // the original adapter will (briefly) appear
            // the original adapter will disappear
            
            // TODO the test will fail somewhere here most of the time
            
            e.waitForStep(8 + offset, 5000);
            m.remove(consumer);
            
            // nothing should happen, all consumed services were already gone
            e.step(9 + offset);
            
            m.add(provider);
            // still nothing should happen
            e.step(10 + offset);
            m.remove(adapter);
            m.remove(adapter2);
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
        
        public ServiceAdapter() {
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
            System.out.println("add: " + counter + " " + service + " " + ServiceUtil.toString(ref));
            m_ensure.step();
        }
        public void remove(ServiceReference ref, AdaptedService service) {
            counter--;
            System.out.println("rem: " + counter + " " + service + " " + ServiceUtil.toString(ref));
            m_ensure.step();
        }
        
//        public void run() {
//            m_service.invoke();
//        }
    }
}


