package dm.it;

import junit.framework.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Test for FELIX-4334 issue.
 * 
 * Two components: A, B
 * 
 * - A provided.
 * - B has a bundle dependency on the dependency manager shell bundle, which is currently stopped.
 * - B has an instance bound dependency on A.
 * - Now unregister A.
 * - As a result of that, B becomes unavailable and is unbound from A. But B is not destroyed, because A dependency 
 *   is "instance bound". So B is still bound to the bundle dependency.
 * - Now, someone starts the dependency manager shell bundle: B then shall be called in its "changed" callback.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ModifiedBundleDependencyTest extends TestBase {
    public static interface A {
    }
    
    static class AImpl implements A {
    }
        
    public static interface B {
    }
    
    static class BImpl implements B {
        final Ensure m_e;
        
        BImpl(Ensure e) {
            m_e = e;
        }
        
        public void add(Bundle dmTest) {
            m_e.step(1);
        }

        void init(Component c) {
            m_e.step(2);
            DependencyManager dm = c.getDependencyManager();
            c.add(dm.createServiceDependency().setService(A.class).setRequired(true).setCallbacks("add", "remove"));
        }      
        
        public void add(A a) {
            m_e.step(3);
        }
        
        public void start() {
            m_e.step(4);            
        }
        
        public void stop() {
            m_e.step(5);
        }

        public void remove(A a) {
            m_e.step(6);
        }

        public void change(Bundle dmTest) { // called two times: one for STARTING, one for STARTED
            m_e.step(); 
        }
        
        public void destroy() {
            m_e.step(9);
        }

        public void remove(Bundle dmTest) {   
            m_e.step(10);
        }                    
    }
    
    public void testAdapterWithChangedInstanceBoundDependency() {
        DependencyManager m = new DependencyManager(context);
        Ensure e = new Ensure();

        Component a = m.createComponent()
                .setImplementation(new AImpl())
                .setInterface(A.class.getName(), null);
        
        Component b = m.createComponent()
                .setInterface(B.class.getName(), null)
                .setImplementation(new BImpl(e))
                .add(m.createBundleDependency()
                        .setFilter("(Bundle-SymbolicName=org.apache.felix.metatype)")
                        .setStateMask(Bundle.INSTALLED|Bundle.ACTIVE|Bundle.RESOLVED|Bundle.STARTING)
                        .setRequired(true)
                        .setCallbacks("add", "change", "remove"));
                    
        Bundle dmtest = getBundle("org.apache.felix.metatype");
        try {
            dmtest.stop();
        } catch (BundleException e1) {
            Assert.fail("could not find metatype bundle");
        }
        
        m.add(a);
        m.add(b);
        
        e.waitForStep(4, 5000);        
        m.remove(a); // B will loose A and will enter into "waiting for required (instantiated)" state.
        System.out.println("Starting metatype bundle ...");        
        try {
            dmtest.start();
        } catch (BundleException e1) {
            Assert.fail("could not start metatype bundle");
        }
        e.waitForStep(7, 5000);     
        m.remove(b);        
        e.waitForStep(10, 5000);                
    }
}
