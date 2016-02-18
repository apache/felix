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
import static org.apache.felix.dm.lambda.DependencyManagerActivator.serviceDependency;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * One consumer, Three providers. The Consumer has two required dependency on provider1, provider2, and one 
 * instance-bound required dependency on provider3.
 * When the three providers are there, the consumer is started.
 * 
 * This test asserts the following correct behaviors:
 *   - when we remove the dependency on provider2, then the consumer is not stopped.
 *   - when we remove the (instance-bound) dependency on provider3, then the consumer os not stopped.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes", "unused"})
public class RemovedDependencyTest extends TestBase {
    public void testRemoveDependencyAndConsumerMustRemainStarted() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // Create two providers
        Hashtable props = new Hashtable();
        props.put("name", "provider1");
        Component sp = component(m).impl(new ServiceProvider(e)).provides(ServiceInterface.class, props).build();
        props = new Properties();
        props.put("name", "provider2");
        Component sp2 = component(m).impl(new ServiceProvider(e)).provides(ServiceInterface.class.getName(), props).build();
        props = new Properties();
        props.put("name", "provider3");
        Component sp3 = component(m).impl(new ServiceProvider(e)).provides(ServiceInterface.class.getName(), props).build();

        // Create the consumer, and start it
        Dependency d3 = m.createServiceDependency().setService(ServiceInterface.class, "(name=provider3)").setRequired(true).setCallbacks("add", "remove");

        ServiceConsumer consumer = new ServiceConsumer(e, d3);
        Component sc = component(m).impl(consumer).build();
        
        Dependency d1 = serviceDependency(sc, ServiceInterface.class).filter("(name=provider1)").add("add").remove("remove").build();
        Dependency d2 = serviceDependency(sc, ServiceInterface.class).filter("(name=provider2)").add("add").remove("remove").build();
        
        sc.add(d1, d2);
            
        // Add the first two providers and the consumer
        m.add(sp);
        m.add(sp2);
        m.add(sp3);
        m.add(sc);
        
        // Check if consumer has been bound to the three providers
        e.waitForStep(3,  5000);
        Assert.assertEquals(3, consumer.getProvidersCount());
        Assert.assertNotNull(consumer.getProvider("provider1"));
        Assert.assertNotNull(consumer.getProvider("provider2"));
        Assert.assertNotNull(consumer.getProvider("provider3"));
        
        // Now remove the provider2, and check if the consumer is still alive
        sc.remove(d2);
        Assert.assertFalse(consumer.isStopped());
        Assert.assertEquals(2, consumer.getProvidersCount());
        Assert.assertNotNull(consumer.getProvider("provider1"));
        Assert.assertNull(consumer.getProvider("provider2"));
        Assert.assertNotNull(consumer.getProvider("provider3"));

        // Now remove the provider3 (the consumer has an instance bound dependency on it), and check if the consumer is still alive
        sc.remove(d3);
        Assert.assertFalse(consumer.isStopped());
        Assert.assertEquals(1, consumer.getProvidersCount());
        Assert.assertNotNull(consumer.getProvider("provider1"));
        Assert.assertNull(consumer.getProvider("provider2"));
        Assert.assertNull(consumer.getProvider("provider3"));
        
        m.clear();
    }
    
    static interface ServiceInterface {
        public void invoke();
    }

    class ServiceProvider implements ServiceInterface {
        final Ensure m_ensure;
        
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void invoke() {
            m_ensure.step();
        }
    }
    
    class ServiceConsumer {
        private final Ensure m_ensure;
        private final List<ServiceReference> m_providers = new ArrayList<>();
        private BundleContext m_bc;
        private boolean m_stopped;
        private final Dependency m_dependency3;

        public ServiceConsumer(Ensure e, Dependency dependency3) {
            m_ensure = e;
            m_dependency3 = dependency3;
        }
                
        public void add(ServiceReference ref) {
            debug("ServiceConsumer.add(%s)", ref);
            m_providers.add(ref);
            ServiceInterface s = (ServiceInterface) m_bc.getService(ref);
            s.invoke();
        }
        
        public void remove(ServiceReference ref) {
            debug("ServiceConsumer.remove(%s)", ref);
            m_providers.remove(ref);
            debug("ServiceConsumer: current providers list=%s", m_providers);
        }
        
        public void init(Component c) {
            c.add(m_dependency3);
        }
        
        public int getProvidersCount() {
            return m_providers.size();
        }
        
        public ServiceInterface getProvider(String name) {
            for (ServiceReference ref : m_providers) {
                Object n = ref.getProperty("name");
                if (n.equals(name)) {
                    return (ServiceInterface) m_bc.getService(ref);
                }
            }
            return null;
        }
        
        public void stop() {
            m_stopped = true;
        }
        
        public boolean isStopped() {
            return m_stopped;
        }
    }
}
