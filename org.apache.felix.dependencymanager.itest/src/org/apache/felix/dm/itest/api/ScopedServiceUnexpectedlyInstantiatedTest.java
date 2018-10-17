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
import org.apache.felix.dm.Component.ServiceScope;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;

/**
 * Make sure a scoped service does not instantiate
 */
public class ScopedServiceUnexpectedlyInstantiatedTest extends TestBase {
	final static Ensure m_e = new Ensure();

    public void testPrototypeComponent() {
        DependencyManager m = getDM();     
        
        Component s1 = m.createComponent()
        	.setScope(ServiceScope.PROTOTYPE)
        	.setImplementation(S1Impl.class)
            .setInterface(S1.class.getName(), null)
            .add(m.createServiceDependency().setRequired(true).setService(S2.class).setCallbacks("bind", null));
        
        Component s2 = m.createComponent()
        		.setInterface(S2.class.getName(), null)
            	.setScope(ServiceScope.PROTOTYPE)
        		.setImplementation(S2Impl.class);
        
        Component consumer1 = m.createComponent()
        		.setImplementation(S1Consumer.class)
        		.add(m.createServiceDependency().setService(S1.class).setRequired(true).setCallbacks("bind", null));
                        
        m.add(s1);          
        m.add(s2);      
        m.add(consumer1);          
        m_e.waitForStep(3, 5000); 

        m.clear();
    }
            
    public interface S1 { 
    }
    
    public interface S2 { 
    }
        
    public static class S1Impl implements S1 {
    	void bind(S2 s2) {
    		m_e.step(2);
    	}
    	
        void start() {
        	m_e.step(3);
        }
    }
    
    public static class S2Impl implements S2 {
        void start() {
        	m_e.step(1);
        }
    }
    
    public static class S1Consumer {
        public void bind(S1 service) {
        	m_e.step(4);
        }
    }
}
