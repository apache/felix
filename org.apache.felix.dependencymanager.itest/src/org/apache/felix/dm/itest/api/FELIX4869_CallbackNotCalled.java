package org.apache.felix.dm.itest.api;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX4869_CallbackNotCalled extends TestBase {
    public void testAbstractClassDependency() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        TestComponent test = new TestComponent(e);
        Component c = m.createComponent().setImplementation(test);
        
        // start the component
        m.add(c);
        e.waitForStep(1, 5000);
        
        // add two other services
        ServiceRegistration sr1 = context.registerService(Object.class.getName(), new Object(), null);
        ServiceRegistration sr2 = context.registerService(Object.class.getName(), new Object(), null);
        
        // now add a dependency to the just previously registered services
        test.addDependency();
        
        // and verify that the add callback has been invoked two times.
        e.waitForStep(3, 5000);
        
        // Unregister the two services
        sr1.unregister();
        sr2.unregister();
        
        // and verify that the remove callbacks have been invoked two times
        e.waitForStep(5, 5000);
        
        // Clear components.
        m.clear();
    }
    
    public static class TestComponent {
        private volatile DependencyManager dm; 
        private volatile Component c;
        private final Ensure e;
        
        public TestComponent(Ensure e) {
            this.e = e;
        }
        
        public void start() {
            e.step(1);
        }
        
        public void addDependency(){
          c.add(dm.createServiceDependency().setService(Object.class).setCallbacks("add", "remove"));
        }
        
        public void add(ServiceReference ref) {
            e.step();
        }
        
        public void remove(ServiceReference ref) {
            e.step();
        }
      } 
}
