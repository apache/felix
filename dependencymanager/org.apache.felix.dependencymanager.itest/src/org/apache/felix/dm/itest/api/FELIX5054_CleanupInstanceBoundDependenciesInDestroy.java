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
package org.apache.felix.dm.itest.api;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
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
		
		A aObject = new A();
		Component a = m.createComponent()
				.setImplementation(aObject)
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
		aObject.testDone();
		m.clear();
	}
	
	public class A {
        private Ensure.Steps m_stepsBindB = new Ensure.Steps(1, 6);
        private Ensure.Steps m_stepsUnbindB = new Ensure.Steps(5);
        private Ensure.Steps m_stepsBindC = new Ensure.Steps(3, 8);
        private Ensure.Steps m_stepsUnbindC = new Ensure.Steps(4);
        private Ensure.Steps m_stepsInit = new Ensure.Steps(2, 7);
        private Dependency m_depC;
        private boolean m_done;

		void bindB(B b) {
			m_ensure.steps(m_stepsBindB);
		}
		
		public void testDone() {
            m_done = true;           
        }

        void unbindB(B b) {
			if (! m_done) m_ensure.steps(m_stepsUnbindB);
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
		    if (! m_done) m_ensure.steps(m_stepsUnbindC);
		}
	}
	
	public static class B {
		
	}
	
	public static class C {
		
	}	
}
