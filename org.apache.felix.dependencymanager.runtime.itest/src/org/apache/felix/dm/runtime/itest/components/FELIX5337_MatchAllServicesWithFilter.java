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

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency.Any;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.itest.util.Ensure;
import org.junit.Assert;

@Component
public class FELIX5337_MatchAllServicesWithFilter {
	/**
	 * We wait for an Ensure object which has a name matching this constant.
	 */
	public final static String ENSURE = "FELIX5337_MatchAllServicesWithFilter";

	@Component(provides=Service1.class)
	@Property(name="matchall", value="foo")
	public static class Service1 {		
	}
	
	@Component(provides=Service2.class)
	@Property(name="matchall", value="foo")
	public static class Service2 {		
	}
	
	@Component
	public static class MatchAll {
		@ServiceDependency(filter = "(name=" + ENSURE + ")")
		volatile Ensure m_sequencer;
		
		@ServiceDependency(service=Any.class, filter="(matchall=foo)")
		Map<Object, Dictionary<String, Object>> m_services;
		
		@Start
		void start() {
			System.out.println(m_services);
	        Assert.assertEquals(2, m_services.size());
	        
	        AtomicBoolean service1Injected = new AtomicBoolean(false);
	        AtomicBoolean service2Injected = new AtomicBoolean(false);
	        
	        m_services.forEach((k,v) -> { 
		        Assert.assertEquals("foo", v.get("matchall")); 
		        if (k.getClass().equals(Service1.class)) service1Injected.set(true);
	        });
	        
	        m_services.forEach((k,v) -> { 
		        Assert.assertEquals("foo", v.get("matchall")); 
	        	if (k.getClass().equals(Service2.class)) service2Injected.set(true); 
	        });
	        
	        Assert.assertEquals(true, service1Injected.get());
	        Assert.assertEquals(true, service2Injected.get());

	        m_sequencer.step(1);
		}
	}

}
