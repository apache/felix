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
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX5630_NullObjectTest extends TestBase {

    private final static String BSN = "org.apache.felix.metatype";

    /**
     * scenario:
     * 
     * - A requires B,C.
     * - A,B,C are started
     * - B,A,C are stopped
     * - A is restarted: at this point A has a bug: it is injected with a null object for B !
     */
    public void testRequiredServiceDependency() {
        DependencyManager manager = getDM();
        
		Component aComponent = manager.createComponent().setInterface(A.class.getName(), null).setImplementation(new A());
		aComponent.add(manager.createServiceDependency().setService(B.class).setRequired(true));
		aComponent.add(manager.createServiceDependency().setService(C.class).setRequired(true));

		Component bComponent = manager.createComponent().setInterface(B.class.getName(), null).setImplementation(new B());
		Component cComponent = manager.createComponent().setInterface(C.class.getName(), null).setImplementation(new C());

		manager.add(bComponent);
		manager.add(aComponent);
		manager.add(cComponent);

		manager.remove(aComponent);
		manager.remove(bComponent);
		manager.add(aComponent); //throws Could not create null object for   
		
		manager.clear();
    }
    
   /**
    * - A requires bundle B, which is started
    * - A started
    * - A stopped
    * - B stopped
    * - A is restarted: at this point A has a bug: it is injected with a null object for the bundle dependency over B
    */
   public void testRequiredBundleDependency() {
        DependencyManager m = getDM();
        
        Ensure e = new Ensure();
        BundleConsumer bundleConsumerImpl = new BundleConsumer(e);
        Component bundleConsumer = m.createComponent()
            .setImplementation(bundleConsumerImpl)
            .add(m.createBundleDependency()
                .setRequired(true)
                .setFilter("(Bundle-SymbolicName=" + BSN + ")")
                .setStateMask( Bundle.ACTIVE));
        // add a bundle consumer with a filter
        m.add(bundleConsumer);
        e.waitForStep(1, 5000);
        // remove bundle consumer
        m.remove(bundleConsumer);
        e.waitForStep(2, 5000);
        // stop the metatype bundle
        stopBundle(BSN);
        // add the bundle consumer again, which must *not* be started
        m.add(bundleConsumer);
        Assert.assertFalse(bundleConsumerImpl.isStarted());        
        startBundle(BSN);
    }

    
	private class A {
		@SuppressWarnings("unused")
		public void init() {
			System.out.println("init");
		}

		@SuppressWarnings("unused")
		public void start() {
			System.out.println("start");
		}

		@SuppressWarnings("unused")
		public void stop() {
			System.out.println("stop");
		}

		@SuppressWarnings("unused")
		public void destroy() {
			System.out.println("destroy");
		}
	}

	private class B {

	}

	private class C {

	}
	
    static class BundleConsumer {
        private final Ensure m_ensure;
        volatile Bundle m_bundle;
		private volatile boolean m_started;

        public BundleConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public boolean isStarted() {
        	return m_started;
        }
        
        public void start() {
        	m_started = true;        	
            if (m_bundle != null && m_bundle.getSymbolicName() != null && m_bundle.getSymbolicName().equals(BSN)) {
                m_ensure.step();
            }
        }
        
        public void stop() {
        	m_started = false;
        	if (m_bundle != null && m_bundle.getSymbolicName().equals(BSN)) {
                m_ensure.step();
            }
        }
    }

}
