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

import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentState;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;

/**
 * Use case:
 * 
 * - A depends on B (optional) and C (required). A has some "add/change/remove" callbacks on B.
 * - A started
 * - A is stopping because C is lost, so A is called in A.unbind(B), A.stop()
 * - then a ComponentStateListener is called and at this point, the listener is 
 *   changing the service properties for B.
 *   
 * then since A.stop has been called, then A.changed(B b) should never be called.
 */
public class FELIX5426_OptionalChangeCallbackNotCalledTest extends TestBase {

	final Ensure m_ensure = new Ensure();
	
	public void testCleanupDependenciesDuringComponentRemove() {
		DependencyManager m = getDM();
		
		A aObject= new A();
		
		Component a = m.createComponent()
				.setImplementation(aObject)
				.add(m.createServiceDependency().setService(B.class).setRequired(false).setCallbacks("add", "change", "remove"))
				.add(m.createServiceDependency().setService(C.class).setRequired(true).setCallbacks("add", "remove"));

		Component b = m.createComponent()
				.setImplementation(new B())
				.setInterface(B.class.getName(), null);
		
		Component c = m.createComponent()
				.setImplementation(new C())
				.setInterface(C.class.getName(), null);

		ComponentStateListener listenerA = (comp, state) -> {
			if (state == ComponentState.STOPPED) {
				Properties properties = new Properties();
				b.setServiceProperties(properties);
			}
		};
		
		a.add(listenerA);
		
		m.add(a);
		m.add(b);
		m.add(c);
		
		m_ensure.waitForStep(2, 5000); // A started
		
		m.remove(c); // A is stopping, it should be called in A.remove(C), A.remove(B), but never in A.change(B)
		m_ensure.waitForStep(5, 5000);
		
		Assert.assertFalse(aObject.bChanged());
	}
		
	class A {
		boolean m_started;
		boolean m_bChanged;
		
		void start() {
			m_started = true;
		}
		
		void stop() {
			m_started = false;
			m_ensure.step(4);
		}
		
		void add(C c) {
			m_ensure.step(1);
		}		
		
		void add(B b) {
			m_ensure.step(2);
		}
		
		void remove(B b) {
			m_ensure.step(3);
		}
		
		void change(B b, Dictionary<String, Object> properties) {
			m_bChanged = true; // should never be called
			throw new RuntimeException("A.change() method called");
		}
		
		boolean bChanged() {
			return m_bChanged;
		}
		
		void remove(C c) {
			m_ensure.step(5);
		}
	}
	
	class B {
		
	}
	
	class C {
		
	}

}
