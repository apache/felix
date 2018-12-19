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

import java.util.Map;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.Component.ServiceScope;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

/**
 * Validates a simple scoped service, which adds a dynamic dependency from init method.
 * Notice the the prototype will add a dynamic dependency from its init method, and the dependency service
 * properties will be propagated.
 */
public class DynamicScopedServiceTest extends TestBase {
	static Ensure m_e;
	static Ensure.Steps m_serviceImplInitSteps;
	static Ensure.Steps m_serviceImplStartSteps;
	static Ensure.Steps m_serviceImplStopSteps;
	static Ensure.Steps m_serviceConsumerBindSteps;
	static Ensure.Steps m_serviceConsumerUnbindSteps;
    		
    public void setUp() throws Exception {
    	super.setUp();
    	m_e = new Ensure();
    	m_serviceImplInitSteps = new Ensure.Steps(1, 2, 5, 12, 13);
    	m_serviceImplStartSteps = new Ensure.Steps(3, 6, 14);
    	m_serviceImplStopSteps = new Ensure.Steps(9, 11, 17);
    	m_serviceConsumerBindSteps = new Ensure.Steps(4, 7, 15);
    	m_serviceConsumerUnbindSteps = new Ensure.Steps(8, 10, 16);
    }
    
    public void testPrototypeComponentWithFactory() {
    	testPrototypeComponent(true);
    }
    
    public void testPrototypeComponentWithoutFactory() {
    	testPrototypeComponent(false);
    }
    
    private void testPrototypeComponent(boolean useFactory) {
        DependencyManager m = getDM();     
        
        Component provider = null;
        
        if (useFactory) {
        	provider = component(m)
        			.factory(this, "createServiceImpl")
        			.scope(ServiceScope.PROTOTYPE)
        			.provides(Service.class)
        			.withSvc(Service3.class, svc -> svc.optional().autoConfig("m_service3"))
        			.build();

        } else {
        	provider = component(m)
        			.impl(ServiceImplWithConstructor.class)
        			.scope(ServiceScope.PROTOTYPE)
        			.provides(Service.class)
        			.withSvc(Service3.class, svc -> svc.optional().autoConfig("m_service3"))
        			.build();
        }
                
        Properties props = new Properties();
        props.put("foo", "bar");
        Component service2 = component(m)
        	.provides(Service2.class.getName(), props)
        	.impl(new Service2() {})
        	.build();
        
        Component service3 = component(m)
            	.provides(Service3.class.getName())
            	.impl(new Service3() {})
            	.build();
        
        Component consumer1 = component(m)
            .impl(new ServiceConsumer())
            .withSvc(Service.class, svc -> svc.required().add("bind").remove("unbind"))
            .build();
        
        Component consumer2 = component(m)
            .impl(new ServiceConsumer())
            .withSvc(Service.class, svc -> svc.required().add("bind").remove("unbind"))
            .build();
                
        m.add(service3); // add service3 (the provider has an optional callback on it)
        m.add(provider); // add provider
        m.add(service2); // add service2 (the prototype depends on it)
        m.add(consumer1); // add first consumer
        m_e.waitForStep(1, 5000); // Service prototype instance called in init
        m_e.waitForStep(2, 5000); // first clone called in init
        m_e.waitForStep(3, 5000); // first clone called in init
        m_e.waitForStep(4, 5000); // first consumer bound to first clone

        m.add(consumer2); // add second consumer
        m_e.waitForStep(5, 5000); // second clone called in init
        m_e.waitForStep(6, 5000); // second clone called in start
        m_e.waitForStep(7, 5000); // second consumer bound to second clone

        // make sure both consumers have a different provider instances.
        ServiceConsumer consumer1Impl = consumer1.getInstance();
        Assert.assertNotNull(consumer1Impl.getService());
        ServiceConsumer consumer2Impl = consumer2.getInstance();
        Assert.assertNotNull(consumer2Impl.getService());
        Assert.assertNotEquals(consumer1Impl.getService(), consumer2Impl.getService());
        
        m.remove(consumer1); // remove consumer1
        m_e.waitForStep(8, 5000); // consumer1 unbound from first clone
        m_e.waitForStep(9, 5000); // first clone stopped
        
        m.remove(provider); // unregister the provider
        m_e.waitForStep(10, 5000); // consumer2 unbound from second clone
        m_e.waitForStep(11, 5000); // second clone stopped
        
        m.add(provider); // re-register the provider
        m_e.waitForStep(12, 5000); // prototype init called
        m_e.waitForStep(13, 5000); // third clone init method called (because consumer2 is active)  
        m_e.waitForStep(14, 5000); // third clone start method called
        m_e.waitForStep(15, 5000); // consumer2 bound to third clone
        
        m.remove(service2); // remove the service2 (it will destroy the clone)
        m_e.waitForStep(16, 5000); // consumer2 unbound
        m_e.waitForStep(17, 5000); // third clone stopped
        
        m.remove(provider);    
        m.remove(service3);
        m.clear();
    }
    
    @SuppressWarnings("unused")
    private ServiceImpl createServiceImpl() { 
    	return new ServiceImpl();
    }

    public interface Service { 
    }
    
    public interface Service2 { 
    }
    
    public interface Service3 { 
    }
        
    public static class ServiceImpl implements Service {
        volatile Bundle m_bundle; // bundle requesting the service, injected by reflection or from constructor
        volatile ServiceRegistration m_registration; // registration of the requested service, injected by reflection or from constructor
		volatile Service2 m_service2;
		private Service3 m_service3;
        		
		void init(Component c) { // only called on prototype instance, not on clones
			component(c, comp ->
			 	comp.withSvc(Service2.class, svc -> svc.required().add("bind").propagate()));
			m_e.steps(m_serviceImplInitSteps); // 1, 2, 5, 12, 13
		}
		
        void bind(Service2 service2, Map<String, Object> properties) {
        	// check if prototype service properties has propagated the Service2 dependency service properties
        	Assert.assertEquals("bar", properties.get("foo"));
        	m_service2 = service2;
        }

        void start() {
        	Assert.assertNotNull(m_bundle);
        	Assert.assertNotNull(m_registration);
        	Assert.assertNotNull(m_service2);
        	Assert.assertNotNull(m_service3);
        	m_e.steps(m_serviceImplStartSteps); // 3, 6, 14
        }
        
        Service3 getService3() {
        	return m_service3;
        }
        
        void stop() {
        	m_e.steps(m_serviceImplStopSteps); // 9, 11, 17
        }
    }
    
    public static class ServiceImplWithConstructor extends ServiceImpl {        
		/**
		 * Inject requesting bundle and service registration using class constructor, NOT using field reflection
		 */
		public ServiceImplWithConstructor(Bundle b, ServiceRegistration reg) {
			m_bundle = b;
			m_registration = reg;
		}
    }
    
    public class ServiceConsumer {
        volatile Service m_myService;

        public void bind(Service service) {
            m_myService = service;
        	m_e.steps(m_serviceConsumerBindSteps); // 4, 7, 15
        }
        
        public void unbind(Service service) {
        	Assert.assertEquals(m_myService, service);
        	m_e.steps(m_serviceConsumerUnbindSteps); // 8, 10, 16
        }
        
        public Service getService() {
            return m_myService;
        }
    }
}
