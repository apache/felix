package org.apache.felix.dm.itest;

import junit.framework.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;

public class BundleDependencyTest extends TestBase {
    private final static String BSN = "org.apache.felix.metatype";
    
    public void testBundleDependencies() {
        DependencyManager m = new DependencyManager(context);
        // create a service provider and consumer
        Consumer c = new Consumer();
        Component consumer = m.createComponent().setImplementation(c).add(m.createBundleDependency().setCallbacks("add", "remove"));
        // add the service consumer
        m.add(consumer);
        // check if at least one bundle was found
        c.check();
        // remove the consumer again
        m.remove(consumer);
        // check if all bundles were removed correctly
        c.doubleCheck();
        
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        Component consumerWithFilter = m.createComponent().setImplementation(new FilteredConsumer(e)).add(m.createBundleDependency().setFilter("(Bundle-SymbolicName=" + BSN + ")").setCallbacks("add", "remove"));
        // add a consumer with a filter
        m.add(consumerWithFilter);
        e.step(2);
        // remove the consumer again
        m.remove(consumerWithFilter);
        e.step(4);
    }
    
    public void testRequiredBundleDependency() {
        DependencyManager m = new DependencyManager(context);
        // create a service provider and consumer
        Consumer c = new Consumer();
        
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        Component consumerWithFilter = m.createComponent()
            .setImplementation(new FilteredConsumerRequired(e))
            .add(m.createBundleDependency()
                .setRequired(true)
                .setFilter("(Bundle-SymbolicName=" + BSN + ")")
                .setCallbacks("add", "remove")
                );
        // add a consumer with a filter
        m.add(consumerWithFilter);
        e.waitForStep(1, 5000);
        // remove the consumer again
        m.remove(consumerWithFilter);
        e.waitForStep(2, 5000);
    }
    
    static class Consumer {
        private volatile int m_count = 0;

        public void add(Bundle b) {
            System.out.println("Consumer.add(" + b.getSymbolicName() + ")");
            Assert.assertNotNull("bundle instance must not be null", b);
            m_count++;
        }
        
        public void check() {
            Assert.assertTrue("we should have found at least one bundle", m_count > 0);
        }
        
        public void remove(Bundle b) {
            System.out.println("Consumer.remove(" + b.getSymbolicName() + ")");
            m_count--;
        }
        
        public void doubleCheck() {
            Assert.assertEquals("all bundles we found should have been removed again", 0, m_count);
        }
    }
    
    static class FilteredConsumer {
        private final Ensure m_ensure;

        public FilteredConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void add(Bundle b) {
            m_ensure.step(1);
        }
        
        public void remove(Bundle b) {
            m_ensure.step(3);
        }
    }
    
    static class FilteredConsumerRequired {
        private final Ensure m_ensure;

        public FilteredConsumerRequired(Ensure e) {
            m_ensure = e;
        }
        
        public void add(Bundle b) {
            System.out.println("Bundle is " + b);
//            Assert.assertNotNull(b);
            if (b.getSymbolicName().equals(BSN)) {
                m_ensure.step(1);
            }
        }
        
        public void remove(Bundle b) {
            Assert.assertNotNull(b);
            if (b.getSymbolicName().equals(BSN)) {
                m_ensure.step(2);
            }
        }
    }
}
