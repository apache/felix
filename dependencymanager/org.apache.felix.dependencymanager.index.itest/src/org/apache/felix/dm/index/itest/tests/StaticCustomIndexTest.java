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
import org.junit.Assert;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class StaticCustomIndexTest extends TestBase {
	
	/**
	 * This system property is set to true when the StaticCustomFilterIndex index has been opened.
	 */
	private final static String OPENED = "org.apache.felix.dm.index.itest.staticcustomindex.StaticCustomFilterIndex.opened";	
	
	private String m_systemConf;

    @SuppressWarnings("unchecked")
	public void setUp() throws Exception {    	  
    	System.setProperty(OPENED, "false");
    	
        // backup currently configured filter index
        BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        m_systemConf = context.getProperty(DependencyManager.SERVICEREGISTRY_CACHE_INDICES);
    	
    	// configure our filter index and use the special DM backdoor in order to reinitialize filter indices
        Consumer<String> reset = (Consumer<String>) System.getProperties().get("org.apache.felix.dependencymanager.filterindex.reset");
        reset.accept("org.apache.felix.dm.index.itest.staticcustomindex.StaticCustomFilterIndex:objectClass");
        
        // now call super.setUp() method: the getDM() method will return a DependencyManager that will use the filter index.
        super.setUp();    	
    }
    	
    @SuppressWarnings("unchecked")
	public void tearDown() throws Exception {
        super.tearDown();
        System.getProperties().remove(OPENED);
        Consumer<String> reset = (Consumer<String>) System.getProperties().get("org.apache.felix.dependencymanager.filterindex.reset");
        reset.accept(m_systemConf);
    }

    public void testUsingStaticCustomIndex() throws Exception {
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
        
        // Make sure our static custom index has been used
        Assert.assertTrue(Boolean.getBoolean(OPENED));
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
