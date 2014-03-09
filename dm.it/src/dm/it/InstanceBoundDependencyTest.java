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
package dm.it;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import dm.Component;
import dm.DependencyManager;
import junit.framework.Assert;

public class InstanceBoundDependencyTest extends TestBase {
    Ensure m_e;
    
    public void testServiceInjection() {
        DependencyManager m = new DependencyManager(context);
        m_e = new Ensure();
        
        Component myComponent = m.createComponent().setImplementation(new MyComponent());
        myComponent.add(m.createServiceDependency().setService(Service1.class).setRequired(true).setCallbacks("add", "change", "remove"));
        m.add(myComponent);
        
        Component s1_1 = m.createComponent().setImplementation(new Service1Impl()).setInterface(Service1.class.getName(), null);
        m.add(s1_1);
        m_e.waitForStep(2, 5000);
        Component s1_2 = m.createComponent().setImplementation(new Service1Impl()).setInterface(Service1.class.getName(), null);
        m.add(s1_2);
        m_e.waitForStep(3, 5000);
        s1_2.setServiceProperties(new Hashtable() {{ put("foo", "bar");}});
        m_e.waitForStep(4, 5000);
        m.remove(s1_2);
        m_e.waitForStep(5, 5000);
        Component s2_1 = m.createComponent().setImplementation(new Service2Impl()).setInterface(Service2.class.getName(), null);
        m.add(s2_1);
        m_e.waitForStep(6, 5000);
        Component s2_2 = m.createComponent().setImplementation(new Service2Impl()).setInterface(Service2.class.getName(), null);
        m.add(s2_2);
        m_e.waitForStep(7, 5000);
        s2_2.setServiceProperties(new Hashtable() {{ put("foo", "bar");}});
        m_e.waitForStep(8, 5000);
        m.remove(s2_2);
        m_e.waitForStep(9, 5000);
    }
    
    public interface Service1 {        
    }
    
    public class Service1Impl implements Service1 {        
    }
    
    public interface Service2 {        
    }
    
    public class Service2Impl implements Service2 {        
    }
    
    class MyComponent {
        final List<Service1> m_service1List = new ArrayList();
        final List<Service2> m_service2List = new ArrayList();

        void add(Service1 s1) {
            Assert.assertEquals(0, m_service2List.size());
            m_service1List.add(s1);
            if (m_service1List.size() == 1) {
                m_e.step(1);
            } else if (m_service1List.size() == 2) {
                m_e.step(3);
            }
        }
        
        void change(Service1 s1) {
            m_e.step(4);
        }
        
        void remove(Service1 s1) {
            m_service1List.remove(s1);
            Assert.assertEquals(1,  m_service1List.size());
            m_e.step(5);
        }
        
        void init(Component c) {
            DependencyManager m = c.getDependencyManager();
            c.add(m.createServiceDependency().setService(Service2.class).setRequired(true).setCallbacks("add", "change", "remove"));
            m_e.step(2);
        }
        
        void add(Service2 s2) {
            m_service2List.add(s2);
            if (m_service2List.size() == 1) {
                m_e.step(6);
            } else if (m_service2List.size() == 2) {
                m_e.step(7);
            }  
        }
        
        void change(Service2 s2) {
            m_e.step(8);
        }
        
        void remove(Service2 s2) {
            m_service2List.remove(s2);
            Assert.assertEquals(1, m_service2List.size());
            m_e.step(9);
        }
    }
}
