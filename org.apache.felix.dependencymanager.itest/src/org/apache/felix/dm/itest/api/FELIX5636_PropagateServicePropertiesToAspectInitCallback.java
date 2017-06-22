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

import java.util.Map;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX5636_PropagateServicePropertiesToAspectInitCallback extends TestBase {
	
	final static Ensure m_e = new Ensure();
	
	public void testAbstractClassDependency() {
		DependencyManager manager = getDM();
		
		// Create a service X
		Properties properties = new Properties();
		properties.put("PropKey", "PropValue");
		Component aComponent = manager.createComponent().setInterface(X.class.getName(), properties)
				.setImplementation(new A());
		manager.add(aComponent);

		// Create a client of X
		manager.add(manager.createComponent()
				.setImplementation(C.class)
				.add(manager.createServiceDependency().setService(X.class).setCallbacks("bind", null).setRequired(true)));
		
		// Create an aspect of service X: the init method should be passed the aspect Component which must has the original service 
		// properties
		manager.add(manager.createAspectService(X.class, null, 100).setImplementation(B.class));	
		
		// Check if the aspect of service X see the original service properties from its init callback.
		m_e.waitForStep(1, 3000);		
	}

	public interface X {

	}

	public static class A implements X {
		public void init(Component component) {
			System.out.println("Service properties in A: " + component.getServiceProperties());
		}
	}

	public static class B implements X {
		public void init(Component component) {
			System.out.println("Service properties in B: " + component.getServiceProperties());
			Assert.assertNotNull(component.getServiceProperties());
			Assert.assertEquals("PropValue", component.getServiceProperties().get("PropKey"));
			m_e.step(1);
		}
	}
	
	public static class C {
		void bind(X x, Map<String, Object> props) {
			System.out.println("C.bind(" + x + ", " + props);
		}
	}

}