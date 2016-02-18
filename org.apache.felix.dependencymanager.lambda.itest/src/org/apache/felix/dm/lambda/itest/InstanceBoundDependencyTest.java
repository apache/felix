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
package org.apache.felix.dm.lambda.itest;

import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * This test does some injection tests on components being in INSTANTIATED_AND_WAITING_FOR_REQUIRED state.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class InstanceBoundDependencyTest extends TestBase {
    Ensure m_e;
    
    public void testServiceInjection() {
        DependencyManager m = getDM();
        m_e = new Ensure();
        
        // Create a "C" component: it depends on some S1 services, and on some S2 instance-bound services (declared from C.init() method)        
        C cimpl = new C();
        Component c = component(m).impl(cimpl)
            .withSvc(S1.class, sb->sb.add("addS1").change("changeS1").remove("removeS1").autoConfig(true)).build();
        m.add(c);
        
        // Add S1 (s1_1): C.add(S1 s1) is called, then init() is called where a dependency is declared on S2
        Hashtable s1_1_props = new Hashtable();
        s1_1_props.put("name", "s1_1");
        s1_1_props.put(Constants.SERVICE_RANKING, new Integer(10));
        S1Impl s1_1_impl = new S1Impl();
        Component s1_1 = component(m).impl(s1_1_impl).provides(S1.class.getName(), s1_1_props).build();
        m.add(s1_1);
        m_e.waitForStep(1, 5000); // wait until C.init called
        ServiceReference ref = cimpl.getS1("s1_1");
        Assert.assertNotNull(ref);
        Assert.assertNotNull(cimpl.getS1());
        Assert.assertEquals(s1_1_impl, cimpl.getS1());
        
        // At this point, MyComponent is in INSTANTIATED_AND_WAITING_FOR_REQUIRED state. 
        // add now add another higher ranked S1 (s1_2) instance. C.add(s1_2) method should be called (the S1 dependency 
        // is not instance bound), and m_s1 autoconfig field should be updated.
        Hashtable s1_2_props = new Hashtable();
        s1_2_props.put(Constants.SERVICE_RANKING, new Integer(20));
        s1_2_props.put("name", "s1_2");
        S1Impl s1_2_impl = new S1Impl();
        Component s1_2 = component(m).impl(s1_2_impl).provides(S1.class.getName(), s1_2_props).build();
        m.add(s1_2);
        ref = cimpl.getS1("s1_2");
        Assert.assertNotNull(ref);
        Assert.assertNotNull(cimpl.getS1()); 
        Assert.assertEquals(s1_2_impl, cimpl.getS1()); // must return s1_2 with ranking = 20

        // Now, change the s1_1 service properties: C.changed(s1_1) should be called, and C.m_s1AutoConfig should be updated
        s1_1_props.put(Constants.SERVICE_RANKING, new Integer(30));
        s1_1.setServiceProperties(s1_1_props);
        ref = cimpl.getS1("s1_1");
        Assert.assertNotNull(ref);
        Assert.assertEquals(new Integer(30), ref.getProperty(Constants.SERVICE_RANKING));
        Assert.assertNotNull(cimpl.getS1());
        Assert.assertEquals(s1_1_impl, cimpl.getS1());
        
        // Now, remove the s1_1: C.remove(s1_1) should be called, and C.m_s1AutoConfig should be updated
        m.remove(s1_1);
        ref = cimpl.getS1("s1_1");
        Assert.assertNull(cimpl.getS1("s1_1"));
        Assert.assertNotNull(cimpl.getS1());
        Assert.assertEquals(s1_2_impl, cimpl.getS1());
        m.clear();
    }
    
    // C component depends on some S1 required services
    public interface S1 {
    }
    
    public class S1Impl implements S1 {
    }
    
    public interface S2 {        
    }
    
    public class S2Impl implements S2 {        
    }
    
    // Our "C" component: it depends on S1 (required) and S2 (required/instance bound)
    // Class tested with reflection based callbacks
    class C {        
        final Map<String, ServiceReference> m_s1Map = new HashMap();
        final Map<String, ServiceReference> m_s2Map = new HashMap();
        volatile S1 m_s1; // auto configured
        
        S1 getS1() {
            return m_s1;
        }

        void addS1(ServiceReference s1) {
            m_s1Map.put((String) s1.getProperty("name"), s1);
        }
        
        void changeS1(ServiceReference s1) {
            m_s1Map.put((String) s1.getProperty("name"), s1);
        }
        
        void removeS1(ServiceReference s1) {
            m_s1Map.remove((String) s1.getProperty("name"));
        }
        
        void addS2(ServiceReference s2) {
            m_s2Map.put((String) s2.getProperty("name"), s2);
        }
        
        void changeS2(ServiceReference s2) {
            m_s2Map.put((String) s2.getProperty("name"), s2);
        }
        
        void removeS2(ServiceReference s2) {
            m_s2Map.remove((String) s2.getProperty("name"));
        }
        
        ServiceReference getS1(String name) {
            return m_s1Map.get(name);
        }
        
        ServiceReference getS2(String name) {
            return m_s2Map.get(name);
        }
        
        void init(Component c) {
            component(c, comp->comp.withSvc(S2.class, srv -> srv.add("addS2").change("changeS2").remove("removeS2")));
            m_e.step(1);
        }
    }
}
