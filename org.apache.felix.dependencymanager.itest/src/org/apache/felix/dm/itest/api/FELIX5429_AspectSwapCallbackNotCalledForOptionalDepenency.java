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
import org.apache.felix.dm.ComponentState;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.Ensure.Steps;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;

/**
 * This test case verifies the following (corner case) scenario:
 * 
 * - Component A depends on Required (required=true)
 * - Component A depends on BService (required=false)
 * - There is an aspect for the B service.
 * - A ComponentStateListener on A removes the aspect if it detects that A is stopping.
 * 
 * Now the test verifies the following: if the "Required" service is removed, then
 * A must be :
 * 
 * 1) swapped with the original B service (since the component state listener removes the B aspect while A is stopping),
 * 2) called in A.remove(original B)
 * 3) and called in A.stop()
 */
public class FELIX5429_AspectSwapCallbackNotCalledForOptionalDepenency extends TestBase {
	
	final Ensure m_ensure = new Ensure();
	
	public void testOptionalAspectRemovedWhenComponentIsStopped() {
		DependencyManager m = getDM();
		
		Component requiredComp = m.createComponent()
				.setImplementation(new Required())
				.setInterface(Required.class.getName(), null);

		Component aComp = m.createComponent()
				.setImplementation(new A())
				.add(m.createServiceDependency().setService(BService.class).setRequired(false).setCallbacks("add", null, "remove", "swap"))
				.add(m.createServiceDependency().setService(Required.class).setRequired(true).setCallbacks("add", "remove"));
		
		Component bComp = m.createComponent()
				.setImplementation(new B())
				.setInterface(BService.class.getName(), null);
		
		Component bAspectComp = m.createAspectService(BService.class, null, 1)
				.setImplementation(new BAspect());
		
		aComp.add((comp, state) -> {
			if (state == ComponentState.STOPPING) {
				System.out.println("removing B aspect");
				m.remove(bAspectComp);
			}
		});
		
		m.add(requiredComp);
		m.add(aComp);
		m.add(bComp);
		m.add(bAspectComp);
		
		System.out.println("removing Required");
		m.remove(requiredComp);
		System.out.println("---");
		m_ensure.waitForStep(7, 5000);
	}
	
	class Required {
	}
	
	class A {
		private BService m_b;
		private final Ensure.Steps m_swapSteps = new Steps(4, 5);

		void add(Required required) {
			System.out.println("A.add(" + required.getClass().getSimpleName() + ")");
			m_ensure.step(1);
		}
		
		void remove(Required required) {
			System.out.println("A.remove(" + required.getClass().getSimpleName() + ")");
		}
		
		void start() {
			System.out.println("A.start");
			m_ensure.step(2); 
		}
		
		void stop() {
			System.out.println("A.stop");
			m_ensure.step(7);
		}
		
		void add(BService b) {
			m_b = b;
			System.out.println("A.add(" + b.getClass().getSimpleName() + ")");
			m_ensure.step(3);
		}
		
		void swap(BService oldB, BService newB) {
			Assert.assertEquals(m_b, oldB);
			m_b = newB;
			System.out.println("A.swap(" + oldB.getClass().getSimpleName() + "," + newB.getClass().getSimpleName() + ")");
			m_ensure.steps(m_swapSteps); // 4, 5
		}
		
		void remove(BService b) {
			System.out.println("A.remove(" + b.getClass().getSimpleName() + ")");
			m_ensure.step(6);
		}
	}
	
	interface BService {
	}
	
	class B implements BService {
	}
	
	class BAspect implements BService {
	}

}
