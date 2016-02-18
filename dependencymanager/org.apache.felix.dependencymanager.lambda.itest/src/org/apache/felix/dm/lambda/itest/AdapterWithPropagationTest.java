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

import static org.apache.felix.dm.lambda.DependencyManagerActivator.adapter;
import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;

/**
 * Checks if a service adapter propagates its service properties, if 
 * the adapted service properties are changed:
 * 
 * S1Impl provides S
 * S1Adapter adapts S1Impl(S) to S2
 * S3 depends on S2
 * 
 * So, when S1Impl service properties are changed, S1Adapter shall propagate the changed properties to S3.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AdapterWithPropagationTest extends TestBase {
    public static interface S1 {}
    
    static class S1Impl implements S1 {
        private Ensure m_ensure;
        public S1Impl(Ensure e) {
            m_ensure = e;
        }
        
        public void start() {
            m_ensure.step(1);
        }
    }
    
    public static interface S2 {}

    static class S1Adapter implements S2 {
        private Ensure m_ensure;
        public S1Adapter(Ensure e) {
            m_ensure = e;
        }
        
        public void add(Map properties, S1 s1) {
            Assert.assertTrue("v1".equals(properties.get("p1")));
            Assert.assertTrue("v2overriden".equals(properties.get("p2")));
            m_ensure.step(2);
        }
        
        public void change(Map properties, S1 s1) {   
            Assert.assertTrue("v1modified".equals(properties.get("p1")));
            Assert.assertTrue("v2overriden".equals(properties.get("p2")));
            m_ensure.step(4);
        }
    }

    static class S3 {
        private final Ensure m_ensure;

        public S3(Ensure e) {
            m_ensure = e;
        }
                        
        public void add(Map properties, S2 s2) {
            Assert.assertTrue("v1".equals(properties.get("p1")));
            Assert.assertTrue("v2".equals(properties.get("p2"))); // s1 should not override adapter service properties
            m_ensure.step(3);
        }
        
        public void change(Map properties, S2 s2) {
            Assert.assertTrue("v1modified".equals(properties.get("p1")));
            Assert.assertTrue("v2".equals(properties.get("p2"))); // s1 should not override adapter service properties
            m_ensure.step(5);
        }
    }
    
    public void testAdapterWithPropagation() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure(); 
        
        Component s1 = component(m).impl(new S1Impl(e)).provides(S1.class).properties(p1 -> "v1", p2 -> "v2overriden").build();
        Component s1Adapter = adapter(m, S1.class).add("add").change("change").impl(new S1Adapter(e)).provides(S2.class).properties(p2 -> "v2").build();   
        Component s3 = component(m).impl(new S3(e)).withSvc(S2.class, s -> s.add("add").change("change")).build();
                                          
        m.add(s1);
        m.add(s1Adapter);
        m.add(s3);
        
        e.waitForStep(3, 5000);
        
        Hashtable s1Properties = new Hashtable();
        s1Properties.put("p1", "v1modified");
        s1Properties.put("p2", "v2overriden");
        s1.setServiceProperties(s1Properties);
        
        e.waitForStep(5, 5000);

        m.clear();
    }
}
