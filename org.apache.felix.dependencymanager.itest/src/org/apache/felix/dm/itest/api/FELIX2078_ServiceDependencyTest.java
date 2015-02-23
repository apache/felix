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

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX2078_ServiceDependencyTest extends TestBase {
    public void testRequiredServiceRegistrationAndConsumption() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component sp = m.createComponent().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), null);
        Component sp2 = m.createComponent().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), null);
        Component sc = m.createComponent().setImplementation(new ServiceConsumer(e)).add(m.createServiceDependency().setService(ServiceInterface.class).setRequired(true).setCallbacks("add", "remove"));
        m.add(sp);
        m.add(sp2);
        System.out.println("adding client");
        m.add(sc);
        System.out.println("waiting");
        // wait until both services have been added to our consumer
        e.waitForStep(2, 5000);
        m.remove(sc);
        m.remove(sp2);
        m.remove(sp);
        // ensure we executed all steps inside the component instance
    }
    
    public void testOptionalServiceRegistrationAndConsumption() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component sp = m.createComponent().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), null);
        Component sp2 = m.createComponent().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), null);
        Component sc = m.createComponent().setImplementation(new ServiceConsumer(e)).add(m.createServiceDependency().setService(ServiceInterface.class).setRequired(false).setCallbacks("add", "remove"));
        m.add(sp);
        m.add(sp2);
        m.add(sc);
        // wait until both services have been added to our consumer
        e.waitForStep(2, 5000);
        m.remove(sc);
        m.remove(sp2);
        m.remove(sp);
        // ensure we executed all steps inside the component instance
    }
    
    static interface ServiceInterface {
        public void invoke();
    }

    static class ServiceProvider implements ServiceInterface {
        public ServiceProvider(Ensure e) {
        }
        public void invoke() {
        }
    }

    static class ServiceConsumer {
        private final Ensure m_ensure;

        public ServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void add(ServiceInterface i) {
        	System.out.println("add " + i);
            m_ensure.step();
        }
        
        public void remove(ServiceInterface i) {
            
        }
    }
}
