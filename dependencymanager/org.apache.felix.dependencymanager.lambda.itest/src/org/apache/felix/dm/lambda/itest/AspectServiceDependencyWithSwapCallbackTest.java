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

import static org.apache.felix.dm.lambda.DependencyManagerActivator.aspect;
import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;


/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AspectServiceDependencyWithSwapCallbackTest extends TestBase {
    public void testServiceRegistrationAndConsumption() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component sp = component(m).impl(new ServiceProvider(e)).provides(ServiceInterface.class).build();
        Component sc = component(m).impl(new ServiceConsumer(e)).withSvc(ServiceInterface.class, s->s.add("add").remove("remove").swap("swap")).build();
        Component asp = aspect(m, ServiceInterface.class).rank(100).impl(ServiceProviderAspect.class).build();
        m.add(sp);
        m.add(sc);
        m.add(asp);
        m.remove(asp); 
        m.remove(sc);
        m.remove(sp);
        
        // ensure we executed all steps inside the component instance
        e.step(7);
    }
    
    public void testServiceRegistrationAndConsumptionRef() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        ServiceConsumer scimpl = new ServiceConsumer(e);
        Component sp = component(m).impl(new ServiceProvider(e)).provides(ServiceInterface.class).build();
        Component sc = component(m).impl(scimpl).withSvc(ServiceInterface.class, s->s.add(scimpl::add).remove(scimpl::remove).swap(scimpl::swap)).build();
        Component asp = aspect(m, ServiceInterface.class).rank(100).impl(ServiceProviderAspect.class).build();
        m.add(sp);
        m.add(sc);
        m.add(asp);
        m.remove(asp); 
        m.remove(sc);
        m.remove(sp);
        
        // ensure we executed all steps inside the component instance
        e.step(7);
    }

    static interface ServiceInterface {
        public void invoke(String caller);
    }

    static class ServiceProvider implements ServiceInterface {
        private final Ensure m_ensure;
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void invoke(String caller) {
        	if (caller.equals("consumer.init")) {
        		m_ensure.step(3);
        	} else if (caller.equals("aspect.consumer.add")) {
        		m_ensure.step(5);
        	}
        }
    }
    
    static class ServiceProviderAspect implements ServiceInterface {
    	private volatile ServiceInterface m_service;
    	
    	public ServiceProviderAspect() {
		}

		@Override
		public void invoke(String caller) {
			m_service.invoke("aspect." + caller);
		}
    }

    static class ServiceConsumer {
        private volatile ServiceInterface m_service;
        private final Ensure m_ensure;
        private int swapCount = 0;

        public ServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void init() {
            m_ensure.step(2);
            m_service.invoke("consumer.init");
        }
        
        public void destroy() {
            m_ensure.step(6);
        }
        
        public void add(ServiceInterface service) {
        	m_service = service;
        	m_ensure.step(1);
        }
        
        public void remove(ServiceInterface service) {
        	m_service = null;
        }
        
        public void swap(ServiceInterface previous, ServiceInterface current) {
        	switch (swapCount) {
        		case 0: m_ensure.step(4);
        				break;
        		case 1: m_ensure.step(5);
        				break;
        		default:
        	}
        	m_service = current;
        	swapCount ++;
        }
    }

}
