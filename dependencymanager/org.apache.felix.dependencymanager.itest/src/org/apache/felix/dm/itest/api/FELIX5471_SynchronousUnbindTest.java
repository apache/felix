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
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.junit.Assert;

/**
 * Verifies the following scenario:
 * 
 * - DM concurrent mode is used (a threadpool is used to activate components)
 * - A depends on M
 * - M depends on X
 * - A, M, X are started concurrently
 * - X is removed. at this point, A should be called in A.unbind(M) while M is still active
 *   and M should be called in M.unbind(X) while X is still active, 
 */
public class FELIX5471_SynchronousUnbindTest extends ServiceRaceTest {
	
	final Ensure m_ensure = new Ensure();
	
    public FELIX5471_SynchronousUnbindTest() {
        setParallel(); // Configure DM to use a threadpool
    }
    
    public void synchronousUnbind() {
    	DependencyManager dm = getDM();
    	
		Component a = dm.createComponent()
				.setImplementation(new A())
				.add(dm.createServiceDependency().setService(M.class).setRequired(true).setCallbacks("add", "remove"));
		
		Component m = dm.createComponent()
				.setImplementation(new M())
				.setInterface(M.class.getName(), null)
				.add(dm.createServiceDependency().setService(X.class).setRequired(true).setCallbacks("add", "remove"));

		Component x = dm.createComponent()
				.setImplementation(new X())
				.setInterface(X.class.getName(), null);
		
		dm.add(a);
		dm.add(m);
		dm.add(x);
		m_ensure.waitForStep(3, 5000);
		
		dm.remove(x);
		m_ensure.waitForStep(6, 5000);
    }
    
    public class A {
    	void add(M m) {
    		System.out.println("[" + Thread.currentThread().getName() + "] A.add(" + m + ")");
    		Assert.assertTrue("A.add(M): M is not started", m.isStarted());
    		m_ensure.step(3);
    	}
    	
    	void remove(M m) {
    		System.out.println("[" + Thread.currentThread().getName() + "] A.remove(" + m + ")");
    		Assert.assertTrue("A.remove(M): M is not started", m.isStarted());
    		m_ensure.step(4);
    	}
    }

    public class M {
    	volatile boolean m_started;
    	
    	boolean isStarted() {
    		return m_started;
    	}
    	
    	void start() {
    		m_started = true;
    		m_ensure.step(2);
    	}
    	
    	void stop() {
    		m_started = false;
    	}

    	void add(X x) {
    		System.out.println("[" + Thread.currentThread().getName() + "] M.add(" + x + ")");
    		Assert.assertTrue("M.add(X): X is not started", x.isStarted());
    	}
    	
    	void remove(X x) {
    		System.out.println("[" + Thread.currentThread().getName() + "] M.remove(" + x + ")");
    		Assert.assertTrue("M.add(X): X is not started", x.isStarted());
    		m_ensure.step(5);
    	}
    	
    	public String toString() {
    		return "M (" + (m_started ? "started" : "stopped") + ")";
    	}
    }

    public class X {
    	volatile boolean m_started;
    	
    	boolean isStarted() {
    		return m_started;
    	}
    	
    	void start() {
    		m_started = true;
    		m_ensure.step(1);
    	}
    	
    	void stop() {
    		m_started = false;
    		m_ensure.step(6);
    	}
    	
    	public String toString() {
    		return "X (" + (m_started ? "started" : "stopped") + ")";
    	}
    }

}