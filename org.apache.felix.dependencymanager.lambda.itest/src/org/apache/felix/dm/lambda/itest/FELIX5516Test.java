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

import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Validates that a service dependency is not dereferenced internally by DM.
 * When you use a method reference for the dependency callback, then dm-lambda auto-detect that the
 * service dependency must not be internally dereferenced.
 * But you when you method reflection based callbacks, you have to call the "dereference(false)" method
 * from the ServiceDependencyBuilder, which indicates to DM that the service reference must not be 
 * dereferenced internally (using BundleContext.getServiceReference() method). 
 */
public class FELIX5516Test extends TestBase {
    private final Ensure m_ensure = new Ensure();

    public void testServiceNotDereferencedInternallyUsingMethodReference() throws Exception {
        final DependencyManager dm = getDM();
        Component service = component(dm).impl(new Factory()).provides(Service.class).build();
        Component client = component(dm).impl(new Client()).withSvc(Service.class, svc -> svc.required().add(Client::bind)).build();
        dm.add(service);
        dm.add(client);
        m_ensure.waitForStep(9,  5000);        
        dm.clear();
    }
    
    public void testServiceNotDereferencedInternallyUsingReflectionCallback() throws Exception {
        final DependencyManager dm = getDM();
        Component service = component(dm).impl(new Factory()).provides(Service.class).build();
        Component client = component(dm).impl(new Client()).withSvc(Service.class, svc -> svc.required().dereference(false).add("bind")).build();
        dm.add(service);
        dm.add(client);
        m_ensure.waitForStep(9,  5000);        
        dm.clear();
    }

    public interface Service {}
    
    public class ServiceImpl implements Service {
    	
    }
    
    public class Factory implements PrototypeServiceFactory<Service> {
		@Override
		public Service getService(Bundle bundle, ServiceRegistration<Service> registration) {
			m_ensure.step();
			return new ServiceImpl();
		}

		@Override
		public void ungetService(Bundle bundle, ServiceRegistration<Service> registration, Service service) {					
			m_ensure.step();
		}
    }
    
    public class Client {
    	ServiceReference<Service> m_ref;
        BundleContext m_ctx;
                
        void bind(ServiceReference<Service> ref) {
        	m_ref = ref;
        }
        
        void start() {
        	ServiceObjects<Service> sobjs = m_ctx.getServiceObjects(m_ref);
        	
        	m_ensure.step(1);
        	Service s1 = sobjs.getService();

        	m_ensure.step(3);
        	Service s2 = sobjs.getService();
        	
        	m_ensure.step(5);   
        	sobjs.ungetService(s1);
        	
        	m_ensure.step(7);   
        	sobjs.ungetService(s2);
        	m_ensure.step(9);   
        }
    }
}
