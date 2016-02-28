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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;
import org.osgi.framework.Constants;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings("rawtypes")
public class AutoConfigTest extends TestBase {
    private final Ensure m_ensure = new Ensure();

    public void testField() throws Exception {
        final DependencyManager dm = getDM();
        // Create a consumer, depending on some providers (autoconfig field).
        ConsumeWithProviderField consumer = new ConsumeWithProviderField();
        Component c = createConsumer(dm, consumer);
        // Create two providers
        Component p1 = createProvider(dm, 10, new Provider() {
            public String toString() { return "provider1"; }
            public void run() { m_ensure.step(); }
        });
        Component p2 = createProvider(dm, 20, new Provider() {
            public String toString() { return "provider2"; }
            public void run() { m_ensure.step(); }
        });

        // add the two providers
        dm.add(p2);
        dm.add(p1);
        // add the consumer, which should have been injected with provider2 (highest rank)
        dm.add(c);
        m_ensure.waitForStep(1, 5000);
        // remove the provider2, the consumer should now be injected with provider1
        dm.remove(p2);
        Assert.assertNotNull(consumer.getProvider());
        Assert.assertEquals("provider1", consumer.getProvider().toString());
        // remove the provider1, the consumer should have been stopped
        dm.remove(p1);
        m_ensure.waitForStep(2, 5000);
        dm.clear();
    }
    
    public void testIterableField() throws Exception {
        final DependencyManager dm = getDM();
        ConsumerWithIterableField consumer = new ConsumerWithIterableField();
        Component c = createConsumer(dm, consumer);
        Component p1 = createProvider(dm, 10, new Provider() {
            public void run() { m_ensure.step(); }
            public String toString() { return "provider1"; }
        });
        Component p2 = createProvider(dm, 20, new Provider() {
            public void run() { m_ensure.step();}
            public String toString() { return "provider2"; }
        });

        dm.add(p2);
        dm.add(p1);
        dm.add(c);
        // the consumer should have been injected with all providers.
        m_ensure.waitForStep(3, 5000);
        
        // check if all providers are there
        Assert.assertNotNull(consumer.getProvider("provider1"));
        Assert.assertNotNull(consumer.getProvider("provider2"));
        
        // remove provider1
        dm.remove(p1);
        
        // check if provider1 has been removed and if provider2 is still there
        Assert.assertNull(consumer.getProvider("provider1"));
        Assert.assertNotNull(consumer.getProvider("provider2"));

        // remove provider2, the consumer should be stopped
        dm.remove(p2);
        m_ensure.waitForStep(4, 5000);
        dm.clear();
    }   
    
    public void testMapField() throws Exception {
        final DependencyManager dm = getDM();
        ConsumerWithMapField consumer = new ConsumerWithMapField();
        Component c = createConsumer(dm, consumer);
        Component p1 = createProvider(dm, 10, new Provider() {
            public void run() { m_ensure.step(); }
            public String toString() { return "provider1"; }
        });
        Component p2 = createProvider(dm, 20, new Provider() {
            public void run() { m_ensure.step();}
            public String toString() { return "provider2"; }
        });

        dm.add(p2);
        dm.add(p1);
        dm.add(c);
        // the consumer should have been injected with all providers.
        m_ensure.waitForStep(3, 5000);
        
        // check if all providers are there
        Assert.assertNotNull(consumer.getProvider("provider1"));
        Assert.assertNotNull(consumer.getProvider("provider2"));
        
        // remove provider1
        dm.remove(p1);
        
        // check if provider1 has been removed and if provider2 is still there
        Assert.assertNull(consumer.getProvider("provider1"));
        Assert.assertNotNull(consumer.getProvider("provider2"));

        // remove provider2, the consumer should be stopped
        dm.remove(p2);
        m_ensure.waitForStep(4, 5000);
        dm.clear();
    }

    private Component createProvider(DependencyManager dm, int rank, Provider provider) {
        return component(dm).impl(provider).provides(Provider.class, Constants.SERVICE_RANKING, new Integer(rank)).build();
    }

    private Component createConsumer(DependencyManager dm, Object consumer) {
        return component(dm).impl(consumer).withSvc(Provider.class, true).build();
    }

    public static interface Provider extends Runnable {      
    }
    
    public class ConsumeWithProviderField {
        volatile Provider m_provider;
        
        void start() {
            Assert.assertNotNull(m_provider);
            Assert.assertEquals("provider2", m_provider.toString());
            m_ensure.step(1);
        }
        
        public Provider getProvider() {
            return m_provider;
        }

        void stop() {
            m_ensure.step(2);
        }
    }
    
    public class ConsumerWithIterableField {
        final Iterable<Provider> m_providers = new ConcurrentLinkedQueue<>();
        final List m_notInjectMe = new ArrayList();
        
        void start() {
            Assert.assertNotNull(m_providers);
            int found = 0;
            for (Provider provider : m_providers) {
                provider.run();
                found ++;
            }
            Assert.assertTrue(found == 2);
            // The "m_notInjectMe" should not be injected with anything
            Assert.assertEquals(m_notInjectMe.size(), 0);
            m_ensure.step(3);
        }
        
        public Provider getProvider(String name) {
            System.out.println("getProvider(" + name + ") : proviers=" + m_providers);
            for (Provider provider : m_providers) {
                if (provider.toString().equals(name)) {
                    return provider;
                }
            }
            return null;
        }
        
        void stop() {
            m_ensure.step(4);
        }
    }    
    
    public class ConsumerWithMapField {
        final Map<Provider, Dictionary> m_providers = new ConcurrentHashMap<>();
        final Map m_notInjectMe = new HashMap<>();
        
        void start() {
            Assert.assertNotNull(m_providers);
            System.out.println("ConsumerMap.start: injected providers=" + m_providers);
            Assert.assertTrue(m_providers.size() == 2);
            Assert.assertEquals(0, m_notInjectMe.size());
            for (Map.Entry<Provider, Dictionary> e : m_providers.entrySet()) {
                Provider provider = e.getKey();
                Dictionary props = e.getValue();
                
                provider.run();
                if (provider.toString().equals("provider1")) {
                    Assert.assertEquals(props.get(Constants.SERVICE_RANKING), 10);
                } else if (provider.toString().equals("provider2")) {
                    Assert.assertEquals(props.get(Constants.SERVICE_RANKING), 20);
                } else {
                    Assert.fail("Did not find any properties for provider " + provider);
                }
            }
            
            m_ensure.step(3);
        }
        
        public Provider getProvider(String name) {
            System.out.println("getProvider(" + name + ") : providers=" + m_providers);
            for (Provider provider : m_providers.keySet()) {
                if (provider.toString().equals(name)) {
                    return provider;
                }
            }
            return null;
        }

        Map<Provider, Dictionary> getProviders() {
            return m_providers;
        }
        
        void stop() {
            m_ensure.step(4);
        }
    }    
}
