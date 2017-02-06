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
import org.apache.felix.dm.ServiceDependency;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AspectNotAlwaysInjectedWithRequiredDependencies extends TestBase {
    final Ensure m_e = new Ensure();

    public void testAspectNotAlwaysInjectedWithRequiredDependency() {
        DependencyManager m = getDM();
        
        Component orig = m.createComponent()
        		.setImplementation(new OriginalServiceImpl())
        		.setInterface(OriginalService.class.getName(), null);
        
        Component aspect = m.createAspectService(OriginalService.class, null, 10)
        		.setImplementation(new Aspect());
        
        m.add(orig);
        m.add(aspect);
        
        m_e.waitForStep(2, 5000);
        
        Component aspectDep = m.createComponent()
        		.setImplementation(new AspectDependency())
        		.setInterface(AspectDependency.class.getName(), null);
        m.add(aspectDep);
        m_e.waitForStep(3,  5000);     
        ServiceDependency sd = m.createServiceDependency().setService(AspectDependency.class).setRequired(true).setCallbacks("add", "remove");
        aspect.add(sd);
        m_e.waitForStep(4,  5000);        
        aspect.remove(sd);
        m_e.waitForStep(5,  5000);
        m.remove(aspect);
        m_e.waitForStep(6,  5000);
    }
    
    public interface OriginalService {}    
    
    public class OriginalServiceImpl implements OriginalService {
    	void start() {
    		m_e.step(1);
    	}
    }    
    
    class AspectDependency {
    	void start() {
    		m_e.step(3);
    	}
    }
    
    public class Aspect implements OriginalService {
    	void add(AspectDependency dep) {
    		m_e.step(4);
    	}
    	
    	void remove(AspectDependency dep) {
    		m_e.step(5);
    	}

    	
    	void start() {
    		m_e.step(2);
    	}
    	
    	void stop() {
    		m_e.step(6);
    	}
    }

}


