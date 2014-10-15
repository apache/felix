package org.apache.felix.dm.itest;

import junit.framework.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;

// TODO add more tests
public class BundleAdapterTest extends TestBase {    
    public void testBundleAdapter() {
        DependencyManager m = getDM();
        // create a bundle adapter service (one is created for each bundle)
        Component adapter = m.createBundleAdapterService(Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE, null, false)
                             .setImplementation(BundleAdapter.class)
                             .setInterface(BundleAdapter.class.getName(), null);

        // create a service provider and consumer
        Consumer c = new Consumer();
        Component consumer = m.createComponent().setImplementation(c)
            .add(m.createServiceDependency().setService(BundleAdapter.class).setCallbacks("add", "remove"));
        
        // add the bundle adapter
        m.add(adapter);
        // add the service consumer
        m.add(consumer);
        // check if at least one bundle was found
        c.check();
        // remove the consumer again
        m.remove(consumer);
        // check if all bundles were removed correctly
        c.doubleCheck();
        // remove the bundle adapter
        m.remove(adapter);
    }
        
    public static class BundleAdapter {
        volatile Bundle m_bundle;
        
        Bundle getBundle() {
            return m_bundle;
        }
    }
    
    static class Consumer {
        private volatile int m_count = 0;

        public void add(BundleAdapter ba) {
            Bundle b = ba.getBundle();
            System.out.println("Consumer.add(" + b.getSymbolicName() + ")");
            Assert.assertNotNull("bundle instance must not be null", b);
            m_count++;
        }
        
        public void check() {
            Assert.assertTrue("we should have found at least one bundle", m_count > 0);
        }
        
        public void remove(BundleAdapter ba) {
            Bundle b = ba.getBundle();
            System.out.println("Consumer.remove(" + b.getSymbolicName() + ")");
            m_count--;
        }
        
        public void doubleCheck() {
            Assert.assertEquals("all bundles we found should have been removed again", 0, m_count);
        }
    }
}
