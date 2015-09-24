package org.apache.felix.dm.itest.api;

import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ResourceHandler;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX5054_CleanupInstanceBoundDependenciesInDestroy extends TestBase {
	Ensure m_ensure;
	
	public void testInstanceBoundDependencyNotReAddedTwice() {
		DependencyManager m = getDM();
		m_ensure = new Ensure();
		
		Component a = m.createComponent()
				.setImplementation(new A())
				.add(m.createServiceDependency().setService(B.class).setRequired(true).setCallbacks("bindB", "unbindB"));
		
		Component b = m.createComponent()
				.setImplementation(new B())
				.setInterface(B.class.getName(), null);
		
		Component c = m.createComponent()
				.setImplementation(new C())
				.setInterface(C.class.getName(), null);
		
		m.add(b);
		m.add(a);		
		m.add(c);
		
		m.remove(b);		
		m_ensure.waitForStep(5, 3000);
		m.add(b);
		m_ensure.waitForStep(8, 3000);
	}
	
	public class A {
        private Ensure.Steps m_stepsBindB = new Ensure.Steps(1, 6);
        private Ensure.Steps m_stepsUnbindB = new Ensure.Steps(5);
        private Ensure.Steps m_stepsBindC = new Ensure.Steps(3, 8);
        private Ensure.Steps m_stepsUnbindC = new Ensure.Steps(4);
        private Ensure.Steps m_stepsInit = new Ensure.Steps(2, 7);
        private Dependency m_depC;

		void bindB(B b) {
			m_ensure.steps(m_stepsBindB);
		}
		
		void unbindB(B b) {
			m_ensure.steps(m_stepsUnbindB);
		}

		void init(Component component) {
			DependencyManager dm = component.getDependencyManager();
			m_depC = dm.createServiceDependency().setService(C.class).setRequired(true).setCallbacks("bindC", "unbindC");
			m_ensure.steps(m_stepsInit);
			component.add(m_depC);
		}
		
		void bindC(C c) {
			m_ensure.steps(m_stepsBindC);
		}
		
		void unbindC(C c) {
			m_ensure.steps(m_stepsUnbindC);
		}
	}
	
	public static class B {
		
	}
	
	public static class C {
		
	}	
}
