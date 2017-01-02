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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentState;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;

/**
 * Verifies if a concurrent deactivation of two components depending on each other does not produce a deadlock.
 */
public class FELIX5471_CyclicDependencyTest extends TestBase {

	volatile Ensure m_ensure;

	public void cyclicDependencyTest() throws InterruptedException {
		DependencyManager m = getDM();
		ExecutorService tpool = Executors.newFixedThreadPool(2);
		try {
			for (int count = 0; count < 1000; count++) {
				m_ensure = new Ensure();

				Component a = m.createComponent()
						.setImplementation(new A())
						.setInterface(A.class.getName(), null)
						.add(m.createServiceDependency().setService(B.class).setRequired(true).setCallbacks("add", "remove"));

				Component b = m.createComponent()
						.setImplementation(new B())
						.setInterface(B.class.getName(), null)
						.add(m.createServiceDependency().setService(A.class).setRequired(false).setCallbacks("add", "remove"));
				
				m.add(a);
				m.add(b);			

				ComponentStateListener l = (c, s) -> {
					if (s == ComponentState.INACTIVE) {
						m_ensure.step();
					}
				};
				a.add(l);
				b.add(l);
				
				m_ensure.waitForStep(4, 50000); // A started, B started

				tpool.execute(() -> m.remove(a));
				tpool.execute(() -> m.remove(b));
				
				m_ensure.waitForStep(10, 50000); // A unbound from  B, stopped and inactive, B unbound from A, stopped and inactive
			}
		} finally {
			tpool.shutdown();
		}
	}

	public class Base {
		void start() {
			m_ensure.step();
		}

		void stop() {
			m_ensure.step();
		}

	}

	public class A extends Base {
		public void add(B b) {
			m_ensure.step();
		}

		public void remove(B b) {
			m_ensure.step();
		}
	}

	public class B extends Base {
		public void add(A a) {
			m_ensure.step();
		}

		public void remove(A a) {
			m_ensure.step();
		}
	}
}
