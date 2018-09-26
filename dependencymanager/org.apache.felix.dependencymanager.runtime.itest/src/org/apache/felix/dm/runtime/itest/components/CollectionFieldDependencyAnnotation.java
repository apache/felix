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
package org.apache.felix.dm.runtime.itest.components;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.itest.util.Ensure;
import org.junit.Assert;

/**
 * Tests if a consumer can be injected with some services using a field collection.
 */
public class CollectionFieldDependencyAnnotation {
    
    interface Provider {
    }
    
    @Component
    @Property(name="p", value="v1")
    public static class ProviderImpl1 implements Provider { 
        public final static String ENSURE = "CollectionFieldDependencyAnnotation.ProviderImpl1";

        @ServiceDependency(filter="(name=" + ENSURE + ")")
		Ensure m_ensure;

        @Start
        void start() {
        	m_ensure.step();
        }
    }
    
    @Component
    @Property(name="p", value="v2")
    public static class ProviderImpl2 implements Provider {    	
        public final static String ENSURE = "CollectionFieldDependencyAnnotation.ProviderImpl2";

        @ServiceDependency(filter="(name=" + ENSURE + ")")
		Ensure m_ensure;

        @Start
        void start() {
        	m_ensure.step();
        }
    }
    
    @Component
    public static class Consumer {
        public final static String ENSURE = "CollectionFieldDependencyAnnotation.Consumer";

        @ServiceDependency(filter="(name=" + ENSURE + ")")
		Ensure m_ensure;

    	@ServiceDependency
    	volatile Iterable<Provider> m_list1;    
    	    	
    	@ServiceDependency
    	volatile Collection<Provider> m_list2;    

    	@ServiceDependency
    	volatile Map<Provider, Dictionary<String, Object>> m_list3 = new ConcurrentHashMap<>();
    	
    	@ServiceDependency(filter="(p=v1)")
    	volatile Provider m_p1;
    
    	@ServiceDependency(filter="(p=v2)")
    	volatile Provider m_p2;
    	
    	@Start
    	void start() {
    		m_ensure.step(3);
    		
    		Assert.assertNotNull(m_p1);
    		Assert.assertNotNull(m_p2);
    		Assert.assertNotNull(m_list1);
    		int size = 0; 
    		for (Provider p : m_list1) size ++;
    		Assert.assertEquals(2, size);
    		Assert.assertEquals(2,  m_list2.size());
    		Iterator<Provider> it = m_list1.iterator();
    		Provider p1 = it.next();
    		Provider p2 = it.next();
    		if (p1 == m_p1 && p2 == m_p2) {
    			m_ensure.step(4);
    		} else if (p1 == m_p2 && p2 == m_p1) {
    			m_ensure.step(4);
    		}
    			    		
    		Assert.assertEquals(2, m_list3.size());    		
    		Dictionary<String, Object> p1Props = m_list3.get(m_p1);
    		Assert.assertNotNull(p1Props);
    		Assert.assertEquals("v1", p1Props.get("p"));
    		m_ensure.step();
    		
    		Dictionary<String, Object> p2Props = m_list3.get(m_p2);
    		Assert.assertNotNull(p2Props);
    		Assert.assertEquals("v2", p2Props.get("p"));
    		m_ensure.step();    		
    	}
    }

}
