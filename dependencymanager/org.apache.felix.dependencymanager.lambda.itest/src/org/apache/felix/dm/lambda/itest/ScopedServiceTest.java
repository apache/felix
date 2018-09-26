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
import org.apache.felix.dm.ComponentState;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

/**
 * Validates a simple scoped service, which does not add some dynamic dependencies with a component init method.
 */
public class ScopedServiceTest extends TestBase implements ComponentStateListener {
	final Ensure m_e = new Ensure();
	final Ensure.Steps m_listenerSteps = new Ensure.Steps(1, 4, 11);
	final Ensure.Steps m_serviceImplStartSteps = new Ensure.Steps(2, 5, 12);
	final Ensure.Steps m_serviceImplStopSteps = new Ensure.Steps(8, 10, 16);
    final Ensure.Steps m_serviceConsumerBindSteps = new Ensure.Steps(3, 6, 13);
    final Ensure.Steps m_serviceConsumerUnbindSteps = new Ensure.Steps(7, 9, 15);

    public void testPrototypeComponent() {
        DependencyManager m = getDM();     
        
        Component provider = component(m, c -> c.scope(ServiceScope.PROTOTYPE)
        		.factory(this, "createServiceImpl")
        		.provides(Service.class)
        		.listener(this)
        		.autoAdd(false)
        		.withSvc(Service2.class, svc -> svc.required().add(ServiceImpl::bind)));
        
        Component service2 = component(m, c -> c
        		.provides(Service2.class)
        		.impl(new Service2() {})
        		.autoAdd(false));
        
        Component consumer1 = component(m, c -> c
            .factory(this::createServiceConsumer)
            .withSvc(Service.class, svc -> svc.required().add(ServiceConsumer::bind).change(ServiceConsumer::change).remove(ServiceConsumer::unbind)));        
        
        Component consumer2 = component(m, c -> c
            .factory(this::createServiceConsumer)
            .withSvc(Service.class, svc -> svc.required().add(ServiceConsumer::bind).remove(ServiceConsumer::unbind)));
                
        m.add(provider);          // add provider
        m.add(consumer1);         // add first consumer
        m.add(service2);          // add service2 (the provider depends on it)
        m_e.waitForStep(1, 5000); // our listener has seen the first clone starting
        m_e.waitForStep(2, 5000); // first clone started
        m_e.waitForStep(3, 5000); // first consumer bound to the first clone

        m.add(consumer2);         // add second consumer
        m_e.waitForStep(4, 5000); // our listener has seen the second clone starting
        m_e.waitForStep(5, 5000); // second clone started.
        m_e.waitForStep(6, 5000); // second consumer bound to the second clone

        // make sure both consumers have a different provider instances.
        ServiceConsumer consumer1Impl = (ServiceConsumer) consumer1.getInstance();
        Assert.assertNotNull(consumer1Impl.getService());
        ServiceConsumer consumer2Impl = (ServiceConsumer) consumer2.getInstance();
        Assert.assertNotNull(consumer2Impl.getService());
        Assert.assertNotEquals(consumer1Impl.getService(), consumer2Impl.getService());
        
        m.remove(consumer1); // remove consumer1
        m_e.waitForStep(7, 5000); // consumer1 unbound from first clone
        m_e.waitForStep(8, 5000); // first clone stopped
        
        m.remove(provider); // unregister the provider
        m_e.waitForStep(9, 5000); // consumer2 unbound from second clone
        m_e.waitForStep(10, 5000); // second clone stopped
        m.remove(consumer2);
        
        m.add(provider); // re-register the provider
        m.add(consumer1); // re-add the consumer1
        m_e.waitForStep(11, 5000); // our listener has seen the third clone starting
        m_e.waitForStep(12, 5000); // third clone started
        m_e.waitForStep(13, 5000); // consumer1 bound to the first clone
        
        Properties props = new Properties();
        props.put("foo", "bar");
        provider.setServiceProperties(props); // update provider service properties
        m_e.waitForStep(14, 5000); // consumer1 should be called in its change callback
        
        m.remove(service2); // remove the service2 (it will remove the provider
        m_e.waitForStep(15, 5000); // consumer1 stopped
        m_e.waitForStep(16, 5000); // third clone stopped

        m.clear();
    }
    
    @SuppressWarnings("unused")
    private ServiceImpl createServiceImpl() { 
        return new ServiceImpl();
    }
    
    @SuppressWarnings("unused")
    private ServiceConsumer createServiceConsumer() {
        return new ServiceConsumer();
    }
    
	@Override
	public void changed(Component c, ComponentState state) {
		if (state == ComponentState.STARTING) {
			Assert.assertEquals(ServiceImpl.class, c.getInstance().getClass());
			m_e.steps(m_listenerSteps); // will enter in step 1, 4, 11
		}		
	}
    
    public interface Service { 
    }
    
    public interface Service2 { 
    }
        
    public class ServiceImpl implements Service {
        volatile Bundle m_bundle; // bundle requesting the service
        volatile ServiceRegistration<Service> m_registration; // registration of the requested service
		volatile Service2 m_service2;
        		
        void bind(Service2 service2) {
        	m_service2 = service2;
        }

        void start() {
        	Assert.assertNotNull(m_bundle);
        	Assert.assertNotNull(m_registration);
        	Assert.assertNotNull(m_service2);
        	m_e.steps(m_serviceImplStartSteps); // 2, 5, 12
        }
        
        void stop() {
        	m_e.steps(m_serviceImplStopSteps); // 8, 10, 16
        }
    }
    
    public class ServiceConsumer {
        volatile Service m_myService;

        public void bind(Service service) {
            m_myService = service;
        	m_e.steps(m_serviceConsumerBindSteps); // 3, 6, 13
        }
        
        public void change(Service service, Map<String, Object> properties) {
        	Assert.assertEquals("bar", properties.get("foo"));
        	m_e.step(14);
        }
        
        public void unbind(Service service) {
        	Assert.assertEquals(m_myService, service);
        	m_e.steps(m_serviceConsumerUnbindSteps); // 7, 9, 15
        }
        
        public Service getService() {
            return m_myService;
        }
    }
}
