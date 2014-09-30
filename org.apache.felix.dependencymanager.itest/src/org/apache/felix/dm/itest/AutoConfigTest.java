package org.apache.felix.dm.itest;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import junit.framework.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Constants;

public class AutoConfigTest extends TestBase {
    private final Ensure m_ensure = new Ensure();

    public void testField() throws Exception {
        final DependencyManager dm = getDM();
        // Create a consumer, depending on some providers (autoconfig field).
        ConsumerField consumer = new ConsumerField();
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
    }
    
    public void testCollectionField() throws Exception {
        final DependencyManager dm = getDM();
        ConsumerCollection consumer = new ConsumerCollection();
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
    }   
    
    public void testMapField() throws Exception {
        final DependencyManager dm = getDM();
        ConsumerMap consumer = new ConsumerMap();
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
    }

    private Component createProvider(DependencyManager dm, int rank, Provider provider) {
        Properties props = new Properties();
        props.put(Constants.SERVICE_RANKING, new Integer(rank));
        return dm.createComponent()
            .setImplementation(provider)
            .setInterface(Provider.class.getName(), props);
    }

    private Component createConsumer(DependencyManager dm, Object consumer) {
        return dm.createComponent()
            .setImplementation(consumer)
            .add(dm.createServiceDependency().setService(Provider.class).setRequired(true));
    }

    public static interface Provider extends Runnable {      
    }
    
    public class ConsumerField {
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
    
    public class ConsumerCollection {
        volatile ConcurrentLinkedQueue<Provider> m_providers;
        
        void start() {
            Assert.assertNotNull(m_providers);
            Assert.assertTrue(m_providers.size() == 2);
            for (Provider provider : m_providers) {
                provider.run();
            }
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

        Collection<Provider> getProviders() {
            return m_providers;
        }
        
        void stop() {
            m_ensure.step(4);
        }
    }    
    
    public class ConsumerMap {
        volatile ConcurrentHashMap<Provider, Dictionary> m_providers;
        
        void start() {
            Assert.assertNotNull(m_providers);
            System.out.println("ConsumerMap.start: injected providers=" + m_providers);
            Assert.assertTrue(m_providers.size() == 2);
            for (Provider provider : m_providers.keySet()) {
                provider.run();

                if (provider.toString().equals("provider1")) {
                    Assert.assertEquals(m_providers.get(provider).get(Constants.SERVICE_RANKING), 10);
                } else if (provider.toString().equals("provider2")) {
                    Assert.assertEquals(m_providers.get(provider).get(Constants.SERVICE_RANKING), 20);
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
