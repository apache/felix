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
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Validates a simple scoped service, and some service consumers using ServiceReference and ServiceObjects API.
 */
public class ScopedServiceReferenceAndServiceObjectsTest extends TestBase {
	final static Ensure m_e = new Ensure();

	public void testScopedComponent() {
        DependencyManager m = getDM();     
        
        Component provider = m.createComponent()
        	.setScope(ServiceScope.PROTOTYPE)
            .setImplementation(ServiceImpl.class)
            .setInterface(Service.class.getName(), null);            
                
        
        Consumer1 c1 = new Consumer1();
        Component c1Comp = m.createComponent()
            .setImplementation(c1)
            .add(m.createServiceDependency().setService(Service.class).setRequired(true).setCallbacks("set", null).setDereference(false));
                   
        Consumer2 c2 = new Consumer2();
        Component c2Comp = m.createComponent()
                .setImplementation(c2)
                .add(m.createServiceDependency().setService(Service.class).setRequired(true).setCallbacks("set", null).setDereference(false));

        m.add(provider);          
        m.add(c1Comp);   
        m_e.waitForStep(2, 5000);

        m.add(c2Comp);         
        m_e.waitForStep(4, 5000);
        
        Assert.assertNotNull(c1.getService());
        Assert.assertNotNull(c2.getService());
        Assert.assertNotEquals(c1.getService(), c2.getService());

        m.clear();
    }
            
    public interface Service { 
    }
    
    public static class ServiceImpl implements Service {
        volatile Bundle m_bundle; // bundle requesting the service
        volatile ServiceRegistration m_registration; // registration of the requested service
        
        void start() {
        	m_e.step(); // 1, 3
        }        
    }
    
    public static class Consumer1 {
        volatile Service m_service;
        volatile BundleContext m_bc;

        void set(ServiceReference<Service> ref) {
        	ServiceObjects<Service> so = m_bc.getServiceObjects(ref);  
            m_service = so.getService();
            m_e.step(2);
        }

        Service getService() { return m_service; }        
    }
    
    public static class Consumer2 {
        volatile Service m_service;
        volatile BundleContext m_bc;

        void set(ServiceObjects<Service> so) {
            m_service = so.getService();
            m_e.step(4);
        }

        Service getService() { return m_service; }
    }

}
