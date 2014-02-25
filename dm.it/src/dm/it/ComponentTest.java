package dm.it;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import dm.Component;
import dm.DependencyManager;

public class ComponentTest extends TestCase {
    private final Ensure m_ensure = new Ensure();
    private final BundleContext m_context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    public void testSimple() throws Exception {
        DependencyManager dm = new DependencyManager(m_context);
        Component consumer = dm.createComponent();
        consumer
            .setImplementation(new Consumer())
            .add(dm.createServiceDependency()
                .setService(Provider.class)
                .setRequired(true)
                .setCallbacks("add", "remove"))
            .add(dm.createServiceDependency()
                .setService(Provider.class)
                .setRequired(true)
                .setAutoConfig("m_autoConfiguredProvider"));
        ServiceRegistration sr = m_context.registerService(Provider.class.getName(), new ProviderImpl(), null);
        dm.add(consumer);
        m_ensure.waitForStep(2, 5000);
        sr.unregister();    
        m_ensure.waitForStep(5, 5000);
    }
    
    public static interface Provider {        
    }
    
    public class ProviderImpl implements Provider {}
    
    public class Consumer {
        Provider m_provider;
        Provider m_autoConfiguredProvider;
        
        void add(Provider provider) {
            Assert.assertNotNull(provider);
            m_provider = provider;
            m_ensure.step(1);
        }
        
        void start() {
            Assert.assertNotNull(m_autoConfiguredProvider);
            m_ensure.step(2);
        }
        
        void stop() {
            m_ensure.step(3);
        }
        
        void destroy() {
            m_ensure.step(4);
        }
        
        void remove(Provider provider) {
            Assert.assertEquals(m_provider, provider);
            m_ensure.step(5);
        }
    }
}
