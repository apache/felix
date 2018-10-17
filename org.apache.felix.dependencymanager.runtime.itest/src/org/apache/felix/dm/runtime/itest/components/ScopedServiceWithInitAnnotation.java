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
package org.apache.felix.dm.runtime.itest.components;

import java.util.Map;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.PropertyType;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.ServiceScope;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.itest.util.Ensure;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceRegistration;

public class ScopedServiceWithInitAnnotation {
	
    public final static String ENSURE = "ScopedServiceWithInitAnnotation.ENSURE";
	
    
	public interface PrototypeService {
		void ensure();
	}
	
	@PropertyType
	public @interface MyConf {
		public String foo() default "bar";
	}
	
	@Component(scope=ServiceScope.PROTOTYPE)
	@MyConf(foo="bar2")
	public static class PrototypeServiceImpl implements PrototypeService {
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected volatile Ensure m_sequencer;
        
        Bundle m_clientBundle;        
        ServiceRegistration m_registration;
        
        volatile ExtraDendency m_extra;
        
        public PrototypeServiceImpl(Bundle b, ServiceRegistration sr) {
        	m_clientBundle = b;
        	m_registration = sr;
        }
        
        @Init
        void init(org.apache.felix.dm.Component c) {
        	DependencyManager dm = c.getDependencyManager();
        	c.add(dm.createServiceDependency().setService(ExtraDendency.class).setRequired(true));
        }
        
		@Start
		void start() {
        	Assert.assertNotNull(m_clientBundle);
        	Assert.assertNotNull(m_registration);
        	Assert.assertNotNull(m_extra);
			m_sequencer.step();
		}
		
		@Stop
		void stop() {
			m_sequencer.step();
		}
		
		public void ensure() {
        	Assert.assertNotNull(m_clientBundle);
        	Assert.assertNotNull(m_registration);
        	Assert.assertNotNull(m_extra);
		}
	}
		
	@Component
	public static class Consumer {	
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected volatile Ensure m_sequencer;

		private ServiceObjects<PrototypeService> m_so;
		private PrototypeService m_service1;
		private PrototypeService m_service2;
		
		@ServiceDependency
		void bind(PrototypeService service, Map<String, Object> props) {	
			m_service1 = service;
        	Assert.assertEquals("bar2", props.get("foo"));
        	m_service1.ensure();
		}

		@ServiceDependency
		void bind2(ServiceObjects<PrototypeService> so) {
			m_so = so;
			m_service2 = m_so.getService();
		}
		
		@Start
		void start() {
        	Assert.assertNotNull(m_service1);
        	Assert.assertNotNull(m_service2);
        	Assert.assertNotEquals(m_service1, m_service2);
        	m_sequencer.step();
		}
		
		@Stop
		void stop() {
			m_so.ungetService(m_service2);
			m_sequencer.step();			
		}
	}
	
    @Component
    public static class ExtraDendency {
    	@Inject
    	BundleContext m_bc;
    	
    	@Start
    	void start() {
    		Thread t = new Thread(() -> {
    			try {
    				Thread.sleep(1000);
    				System.out.println("register ExtraDendency" );
    				m_bc.registerService(ExtraDendency.class, this, null);
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    		});
    		t.start();
    		
    	}
    }


}
