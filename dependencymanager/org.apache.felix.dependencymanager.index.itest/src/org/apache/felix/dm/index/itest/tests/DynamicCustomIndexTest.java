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
package org.apache.felix.dm.index.itest.tests;
//import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartupFor;
//import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import java.util.function.Consumer;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.FilterIndex;
import org.apache.felix.dm.index.itest.dynamiccustomindex.DynamicCustomFilterIndex;
import org.junit.Assert;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DynamicCustomIndexTest extends TestBase {
	
	/**
	 * This system property is set to true when the DynamicCustomFilterIndex index has been opened.
	 */
	private final static String OPENED = "org.apache.felix.dm.index.itest.dynamiccustomindex.CustomFilterIndex.opened";
	
	private ServiceRegistration m_reg;
	
	private String m_systemConf;
	
    @SuppressWarnings("unchecked")
	public void setUp() throws Exception {
        System.setProperty(OPENED, "false");
        
        // backup currently configured filter index
        BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    	m_systemConf = context.getProperty(DependencyManager.SERVICEREGISTRY_CACHE_INDICES);
    	
    	// Reset filter indices (we must initialize DependencyManager, so its static initializer will register
    	// the reset backdoor.
    	@SuppressWarnings("unused")
		DependencyManager dm = new DependencyManager(context);
    	Consumer<String> reset = (Consumer<String>) System.getProperties().get("org.apache.felix.dependencymanager.filterindex.reset");
        reset.accept(null); // clear filter index
        
        // register our DynamicCustomFilterIndex service before calling super.setUp(). This will make
        // the "getDM()" method return a DependencyManager that is using our DynamicCustomFilterIndex
    	m_reg = context.registerService(FilterIndex.class.getName(), new DynamicCustomFilterIndex("objectClass"), null);
    	super.setUp();   
    }
    
    @SuppressWarnings("unchecked")
	public void tearDown() throws Exception {
        super.tearDown();
    	try {
    		m_reg.unregister();
    	} catch (IllegalStateException e) { // expected, normally we have already unregistered it    		
    	}
        System.getProperties().remove(OPENED);
        Consumer<String> reset = (Consumer<String>) System.getProperties().get("org.apache.felix.dependencymanager.filterindex.reset");
        reset.accept(m_systemConf);
    }
	
    public void testUsingDynamicCustomIndex() throws Exception {
    	doTestUsingDynamicCustomIndex();
        
        // Make sure our static custom index has been used
        Assert.assertTrue(Boolean.getBoolean(OPENED));
        
        // unregister our dynamic filter index
    	m_reg.unregister();

    	// clear the flag
        System.setProperty(OPENED, "false");

        // redo the test
    	doTestUsingDynamicCustomIndex();
        Assert.assertFalse(Boolean.getBoolean(OPENED));
    }
    
    private void doTestUsingDynamicCustomIndex() throws Exception {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a provider
        Provider provider = new Provider();
        // activate it
        Component p = m.createComponent()
            .setInterface(Service.class.getName(), null)
            .setImplementation(provider);
        
        Client consumer = new Client(e);
        Component c = m.createComponent()
            .setImplementation(consumer)
            .add(m.createServiceDependency()
                .setService(Service.class)
                .setRequired(true)
                );
        
        m.add(p);
        m.add(c);
        e.waitForStep(1, 5000);
        m.remove(p);
        e.waitForStep(2, 5000);
        m.remove(c);
        m.clear();
    }

    public static class Client {
        volatile Service m_service;
        private final Ensure m_ensure;
        
        public Client(Ensure e) {
            m_ensure = e;
        }

        public void start() {
            System.out.println("start");
            m_ensure.step(1);
        }
        
        public void stop() {
            System.out.println("stop");
            m_ensure.step(2);
        }
    }
    
    public static interface Service {
    }
    
    public static class Provider implements Service {
    }
}
