package dm.it;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import dm.Component;
import dm.Dependency;
import dm.DependencyManager;

/**
 * One consumer, Three providers. The Consumer has two required dependency on provider1, provider2, and one 
 * instance-bound required dependency on provider3.
 * When the three providers are there, the consumer is started.
 * 
 * This test asserts the following correct behaviors:
 *   - when we remove the dependency on provider2, then the consumer is not stopped.
 *   - when we remove the (instance-bound) dependency on provider3, then the consumer os not stopped.
 */
public class RemovedDependencyTest extends TestBase {
    public void testRemoveDependencyAndConsumerMustRemainStarted() {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // Create two providers
        Properties props = new Properties();
        props.put("name", "provider1");
        Component sp = m.createComponent().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), props);
        props = new Properties();
        props.put("name", "provider2");
        Component sp2 = m.createComponent().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), props);
        props = new Properties();
        props.put("name", "provider3");
        Component sp3 = m.createComponent().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), props);

        // Create the consumer, and start it
        Dependency d1 = m.createServiceDependency().setService(ServiceInterface.class, "(name=provider1)").setRequired(true).setCallbacks("add", "remove");
        Dependency d2 = m.createServiceDependency().setService(ServiceInterface.class, "(name=provider2)").setRequired(true).setCallbacks("add", "remove");
        Dependency d3 = m.createServiceDependency().setService(ServiceInterface.class, "(name=provider3)").setRequired(true).setCallbacks("add", "remove");

        ServiceConsumer consumer = new ServiceConsumer(e, d3);
        Component sc = m.createComponent().setImplementation(consumer).add(d1, d2);
            
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
