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

import java.util.Hashtable;

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceDependency;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes", "serial"})
public class TemporalServiceDependencyTest extends TestBase {
    public void testServiceConsumptionAndIntermittentAvailability() {
        final DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        TemporalServiceProvider provider = new TemporalServiceProvider(e);
        Component sp = m.createComponent().setImplementation(provider).setInterface(TemporalServiceInterface.class.getName(), null);
        TemporalServiceProvider2 provider2 = new TemporalServiceProvider2(e);
        Component sp2 = m.createComponent().setImplementation(provider2).setInterface(TemporalServiceInterface.class.getName(), null);
        TemporalServiceConsumer consumer = new TemporalServiceConsumer(e);
        Component sc = m.createComponent().setImplementation(consumer)
            .add(m.createTemporalServiceDependency(10000).setService(TemporalServiceInterface.class).setRequired(true));
        // add the service consumer
        m.add(sc);
        // now add the first provider
        m.add(sp);
        e.waitForStep(2, 5000);
        // and remove it again (this should not affect the consumer yet)
        m.remove(sp);
        // now add the second provider
        m.add(sp2);
        e.step(3);
        e.waitForStep(4, 5000);
        // and remove it again
        m.remove(sp2);
        // finally remove the consumer
        m.remove(sc);
        // ensure we executed all steps inside the component instance
        e.step(6);
        m.clear();
    }

    public void testServiceConsumptionWithCallbackAndIntermittentAvailability() {
        final DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        TemporalServiceProvider provider = new TemporalServiceProvider(e);
        Component sp = m.createComponent().setImplementation(provider).setInterface(TemporalServiceInterface.class.getName(), null);
        TemporalServiceProvider2 provider2 = new TemporalServiceProvider2(e);
        Component sp2 = m.createComponent().setImplementation(provider2).setInterface(TemporalServiceInterface.class.getName(), null);
        TemporalServiceConsumerWithCallback consumer = new TemporalServiceConsumerWithCallback(e);
        ServiceDependency temporalDep =  m.createTemporalServiceDependency(10000).setService(TemporalServiceInterface.class).setRequired(true).setCallbacks("add", "remove");
        Component sc = m.createComponent().setImplementation(consumer).add(temporalDep);
            
        // add the service consumer
        m.add(sc);
        // now add the first provider
        m.add(sp);
        e.waitForStep(2, 5000);
        // and remove it again (this should not affect the consumer yet)
        m.remove(sp);
        // now add the second provider
        m.add(sp2);
        e.step(3);
        e.waitForStep(4, 5000);
        // and remove it again
        m.remove(sp2);
        // finally remove the consumer
        m.remove(sc);
        // Wait for the consumer.remove callback
        e.waitForStep(6, 5000);
        // ensure we executed all steps inside the component instance
        e.step(7);
        m.clear();
    }

    // Same test as testServiceConsumptionWithCallbackAndIntermittentAvailability, but the consumer is now
    // an adapter for the Adaptee interface.
    public void testFELIX4858_ServiceAdapterConsumptionWithCallbackAndIntermittentAvailability() {
        final DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        TemporalServiceProvider provider = new TemporalServiceProvider(e);
        Component sp = m.createComponent().setImplementation(provider).setInterface(TemporalServiceInterface.class.getName(), null);
        TemporalServiceProvider2 provider2 = new TemporalServiceProvider2(e);
        Component sp2 = m.createComponent().setImplementation(provider2).setInterface(TemporalServiceInterface.class.getName(), null);
        TemporalServiceConsumerAdapterWithCallback consumer = new TemporalServiceConsumerAdapterWithCallback(e);
        ServiceDependency temporalDep =  m.createTemporalServiceDependency(10000).setService(TemporalServiceInterface.class).setRequired(true).setCallbacks("add", "remove");
        Component sc = m.createAdapterService(Adaptee.class, null).setImplementation(consumer).add(temporalDep);            
        Component adaptee = m.createComponent().setImplementation(new Adaptee()).setInterface(Adaptee.class.getName(), null);
            
        // add the adapter service consumer
        m.add(sc);
        // add the adaptee (the adapter service depends on it)
        m.add(adaptee);
        // now add the first provider
        m.add(sp);
        e.waitForStep(2, 5000);
        // and remove it again (this should not affect the consumer yet)
        m.remove(sp);
        // now add the second provider
        m.add(sp2);
        e.step(3);
        e.waitForStep(4, 5000);
        // and remove it again
        m.remove(sp2);
        // finally remove the consumer
        m.remove(sc);
        // Wait for the consumer.remove callback
        e.waitForStep(6, 5000);
        // ensure we executed all steps inside the component instance
        e.step(7);
        m.clear();
    }

    public void testFelix4602_PropagateServiceInvocationException() {
        final DependencyManager m = getDM();
        final Ensure ensure = new Ensure();
        Runnable provider = new Runnable() {
        	public void run() {
        		throw new UncheckedException();
        	}
        };
        Hashtable props = new Hashtable();
        props.put("target", getClass().getSimpleName());
        Component providerComp = m.createComponent()
        		.setInterface(Runnable.class.getName(), props)
        		.setImplementation(provider);

        Object consumer = new Object() {
        	volatile Runnable m_provider;
        	@SuppressWarnings("unused")
            void start() {
        		try {
        			ensure.step(1);
        			m_provider.run();
        		} catch (UncheckedException e) {
        			ensure.step(2);
        		}
        	}
        };
        Component consumerComp = m.createComponent()
        		.setImplementation(consumer)
        		.add(m.createTemporalServiceDependency(5000)
        				.setService(Runnable.class, "(target=" + getClass().getSimpleName() + ")")
        				.setRequired(true));
        m.add(consumerComp);
        m.add(providerComp);
        ensure.waitForStep(2, 5000);
        m.clear();
    }
    
    static class UncheckedException extends RuntimeException {    	
    }

    static interface TemporalServiceInterface {
        public void invoke();
    }

    static class TemporalServiceProvider implements TemporalServiceInterface {
        private final Ensure m_ensure;
        public TemporalServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void invoke() {
            m_ensure.step(2);
        }
    }

    static class TemporalServiceProvider2 implements TemporalServiceInterface {
        protected final Ensure m_ensure;
        public TemporalServiceProvider2(Ensure e) {
            m_ensure = e;
        }
        public void invoke() {
            m_ensure.step(4);
        }
    }

    static class TemporalServiceConsumer implements Runnable {
        protected volatile TemporalServiceInterface m_service;
        protected final Ensure m_ensure;

        public TemporalServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void init() {
            m_ensure.step(1);
            Thread t = new Thread(this);
            t.start();
        }
        
        public void run() {
            m_service.invoke();
            m_ensure.waitForStep(3, 15000);
            m_service.invoke();
        }
        
        public void destroy() {
            m_ensure.step(5);
        }
    }
    
    static class TemporalServiceConsumerWithCallback extends TemporalServiceConsumer {
        public TemporalServiceConsumerWithCallback(Ensure e) {
            super(e);
        }
        
        public void add(TemporalServiceInterface service) {
            m_service = service;
        }
        
        public void remove(TemporalServiceInterface service) {
            Assert.assertTrue(m_service == service);
            m_ensure.step(6);
        }
    }
    
    public static class Adaptee {       
    }
       
    static class TemporalServiceConsumerAdapterWithCallback extends TemporalServiceConsumer {
        volatile Adaptee m_adaptee;
        
        public TemporalServiceConsumerAdapterWithCallback(Ensure e) {
            super(e);
        }
        
        public void start() {
            Assert.assertTrue(m_adaptee != null);
        }
        
        public void add(TemporalServiceInterface service) {
            m_service = service;
        }
        
        public void remove(TemporalServiceInterface service) {
            Assert.assertTrue(m_service == service);
            m_ensure.step(6);
        }
    }
}
