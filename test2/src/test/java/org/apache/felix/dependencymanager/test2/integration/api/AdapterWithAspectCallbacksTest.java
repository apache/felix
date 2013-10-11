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
package org.apache.felix.dependencymanager.test2.integration.api;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.ops4j.pax.exam.junit.PaxExam;
import org.apache.felix.dependencymanager.test2.components.Ensure;
import org.apache.felix.dependencymanager.test2.integration.common.TestBase;

@RunWith(PaxExam.class)
public class AdapterWithAspectCallbacksTest extends TestBase {
    @Test
    public void testAdapterWithAspectMultipleTimes() {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();

        Component aspect1 = m.createAspectService(OriginalService.class, null, 10, null)
	            .setImplementation(ServiceAspect.class);
	        
	    Component aspect2 = m.createAspectService(OriginalService.class, null, 15, null)
	            .setImplementation(ServiceAspect.class);
	    
        Component adapter = m.createAdapterService(OriginalService.class, null, "add", null, "remove", "swap")
	            .setInterface(AdaptedService.class.getName(), null)
	            .setImplementation(ServiceAdapter.class);
        
        m.add(adapter);
        m.add(aspect1);
        
        List originals = new ArrayList();
        List consumers = new ArrayList();
        int count = 100;
        for (int i = 0; i < count; i++) {
	        // create a service provider and consumer
        	Dictionary props = new Hashtable();
        	props.put("number", "" + i);
	        Component original = m.createComponent()
	            .setImplementation(new ServiceProvider("" + i, e))
	            .setInterface(OriginalService.class.getName(), props);
	        
	        Component consumer = m.createComponent()
	            .setImplementation(new ServiceConsumer(e, "" + i))
	            .add(m.createServiceDependency()
	                .setService(AdaptedService.class, "(number=" + i + ")")
	                .setCallbacks("add", null, "remove", "swap")
	                .setRequired(true)
	            );
	        
	        m.add(original);
	        e.waitForStep(1 + i, 15000);	        
	        originals.add(original);
//	        m.add(consumer);
	        consumers.add(consumer);
        }
        
        m.add(aspect2);
        e.waitForStep(2 + count, 15000);
        // dumpComponents(m)
        
        for (int i = 0; i < 1; i++) {
        	for (Object original : originals) {
        		m.remove((Component)original);
        	}
        	for (Object consumer : consumers) {
        		m.remove((Component)consumer);
        	}
        }
        m.remove(aspect1);
        m.remove(adapter);
        
        e.waitForStep(count * 3, 15000);
        m.remove(aspect2);
        e.step(count * 3 + 1);
        m.clear();
    }
        
    static interface OriginalService {
        public void invoke();

		public String getMessage();

		public Ensure getEnsure();
    }
    
    static interface AdaptedService {
        public void invoke();

		public String getMessage();
    }
    
    static class ServiceProvider implements OriginalService {
    	private final String message;
    	private final Ensure m_ensure;
    	
		public ServiceProvider(String message, Ensure e) {
			this.message = message;
			this.m_ensure = e;
    	}
        public void start() {
            System.out.println("...original starting");
        }
        public void invoke() {
        }
        
        @Override
        public String toString() {
            return "Original " + message;
        }
		public String getMessage() {
			return message;
		}
		public Ensure getEnsure() {
			return m_ensure;
		}
    }
    
    public static class ServiceAdapter implements AdaptedService {
        private volatile OriginalService m_originalService;
		private int m_nr;
        
		public ServiceAdapter() {
			
		}

        public void init() {
        }
        public void start() {
            System.out.println("...adapter starting");
        }
        public void invoke() {
            m_originalService.invoke();
        }
        public void stop() {
        	System.out.println("...adapter stopping");
        }
        
        void add(ServiceReference ref, OriginalService originalService) {
        	m_originalService = originalService;
        	System.out.println("adapter add: " + originalService + " (" + System.identityHashCode(this) + ")");
        	m_originalService.getEnsure().step();
        }
        
        void remove(ServiceReference ref, OriginalService originalService) {
        	System.out.println("adapter rem: " + originalService + " (" + System.identityHashCode(this) + ")");
        	m_originalService = null;
        }
        
        void swap(ServiceReference oldRef, OriginalService oldService, ServiceReference newRef, OriginalService newService) {
        	m_originalService = newService;
        	System.out.println("adapter swp: " + newService + " (" + System.identityHashCode(this) + ")");
        	m_originalService.getEnsure().step();
        	System.out.println("ensure: " + m_originalService.getEnsure());
        }
        
        @Override
        public String toString() {
            return "Adapter on " + m_originalService;
        }
		public String getMessage() {
			return m_originalService.getMessage();
		}
	
    }
    
    
    public static class ServiceAspect implements OriginalService {
        volatile OriginalService m_service;
        
        public void start() {
            System.out.println("...aspect starting: " + toString());
        }
        
        public void invoke() {
            m_service.invoke();
        }
        
        @Override
        public String toString() {
            return "Aspect on " + m_service;
        }

		public String getMessage() {
			return m_service.getMessage();
		}

		public Ensure getEnsure() {
			return m_service.getEnsure();
		}
    }

    public static class ServiceConsumer {
        Ensure m_ensure;
        volatile AdaptedService service;
		private final String expectedMessage;
        
        public ServiceConsumer(Ensure e, String expectedMessage) {
            m_ensure = e;
			this.expectedMessage = expectedMessage;
        }
        
        public void start() {
            String message = service.getMessage();
            Assert.assertEquals(expectedMessage, message);
        }
        
        public void stop() {
        }
        
        public void add(ServiceReference ref, AdaptedService service) {
        	this.service = service;
        }
        public void remove(ServiceReference ref, AdaptedService service) {
        	this.service = null;
        }
        public void swap(ServiceReference oldRef, AdaptedService oldService, ServiceReference newRef, AdaptedService newService) {
        	this.service = newService;
        }
        
    }
}


