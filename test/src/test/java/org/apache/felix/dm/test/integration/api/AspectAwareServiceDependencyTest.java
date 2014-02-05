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

@RunWith(PaxExam.class)
public class AspectAwareServiceDependencyTest extends TestBase {
    @Test
    public void testServiceRegistrationAndConsumption() {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component sp = m.createComponent().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), null);
        Component sc = m.createComponent().setImplementation(new ServiceConsumerCallbacks(e)).add(m.createServiceDependency().setService(ServiceInterface.class).setRequired(false).setCallbacks("add", "change", "remove", "swap"));
        Component asp = m.createAspectService(ServiceInterface.class, null, 1000, null).setImplementation(ServiceProviderAspect.class);
        m.add(sp);
        m.add(sc);
        m.add(asp);        
        m.remove(sc);
        m.remove(sp);
        // ensure we executed all steps inside the component instance
        e.step(4);
    }
    
    static interface ServiceInterface {
        public void invoke();
    }

    static class ServiceProvider implements ServiceInterface {
        private final Ensure m_ensure;
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void invoke() {
        }
    }

    static class ServiceConsumerCallbacks {
        private final Ensure m_ensure;

        public ServiceConsumerCallbacks(Ensure e) {
            m_ensure = e;
        }
        
        public void add(ServiceInterface service) {
            m_ensure.step(1);
        }
        public void remove(ServiceInterface service) {
            m_ensure.step(3);
        }
        public void swap(ServiceInterface oldService, ServiceInterface newService) {
        	m_ensure.step(2);
        }
    }
    
    static class ServiceProviderAspect implements ServiceInterface {
    	private volatile ServiceProvider serviceProvider;

    	public ServiceProviderAspect() {
    	}
    	
		public void invoke() {
			serviceProvider.invoke();
		}
    }
}
