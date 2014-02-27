package dm.it;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

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
                .setService(Provider.class, "(name=provider2)")
                .setRequired(true)
                .setCallbacks("add", "remove"))
            .add(dm.createServiceDependency()
                .setService(Provider.class, "(name=provider1)")
                .setRequired(true)
                .setAutoConfig("m_autoConfiguredProvider"));
        
        Dictionary props = new Hashtable();
        props.put("name", "provider1");
        Component provider1 = dm.createComponent()
        		.setImplementation(new Provider() { public String toString() { return "provider1";}})
        		.setInterface(Provider.class.getName(), props);
        props = new Hashtable();
        props.put("name", "provider2");
        Component provider2 = dm.createComponent()
				   .setImplementation(new Provider() { public String toString() { return "provider2";}})
				   .setInterface(Provider.class.getName(), props);
        dm.add(provider1);
        dm.add(provider2);
        dm.add(consumer);
        m_ensure.waitForStep(2, 5000);
        dm.remove(provider1); 
        dm.remove(provider2);    
        m_ensure.waitForStep(5, 5000);
    }
    
    public static interface Provider {    	
    }
    
    public class Consumer {
        Provider m_provider;
        Provider m_autoConfiguredProvider;
        
        void add(Map props, Provider provider) {
            Assert.assertNotNull(provider);
            Assert.assertEquals("provider2", props.get("name"));
            m_provider = provider;
            m_ensure.step(1);
        }
        
        void start() {
            Assert.assertNotNull(m_autoConfiguredProvider);
            Assert.assertEquals("provider1", m_autoConfiguredProvider.toString());
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
