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
import org.junit.Assert;
import org.osgi.framework.ServiceReference;

/**
 * Use case:
 * 
 * Client depends on Provider and holds the ServiceReference to it 
 * Provider is swapped with a Provider aspect
 * Then Client and Aspect are stopped
 * 
 * This tests verifies that the ServiceReference originally gotten by CLient to Provider is ungotten
 */
public class UngetAspectServiceTest extends TestBase {
	final Ensure m_ensure = new Ensure();

	public void testUngetSwappedService() {
		DependencyManager m = getDM();
		
		Client clientInstance = new Client();
		Component client = m.createComponent()
				.setImplementation(clientInstance)
				.add(m.createServiceDependency().setService(Provider.class).setRequired(true).setCallbacks("bind", null, null, "swap"));
		
		Component provider =  m.createComponent()
				.setImplementation(new ProviderImpl())
				.setInterface(Provider.class.getName(), null);
		
		Aspect aspectInstance = new Aspect();
		Component aspect =  m.createAspectService(Provider.class, null, 1)
				.setImplementation(aspectInstance);				

		// add client, provider
		m.add(client);
		m.add(provider);
		
		// wait for client to be bound to provider
		m_ensure.waitForStep(1, 5000);
		
		// add aspect
		m.add(aspect);
		
		// check for client to be swapped with aspect
		m_ensure.waitForStep(2, 5000);

		// remove client, and aspect
		m.remove(client);
		m.remove(aspect);

		// Now, no more references should point to the provider
		Assert.assertEquals(false, this.context.ungetService(clientInstance.getServiceRef()));		
	}

	public interface Provider {
	}
	
	public class ProviderImpl implements Provider {		
	}
	
	public class Aspect implements Provider {
		private ServiceReference m_ref;

		void bind(ServiceReference provider) {
			m_ref = provider;
		}
		
		ServiceReference getRef() {
			return m_ref;
		}
	}
	
	public class Client {
		ServiceReference m_Aref;
		
		void bind(ServiceReference Aref, Provider provider) {
			m_Aref = Aref;
			m_ensure.step(1);
		}
		
		void swap(Provider old, Provider replace) {
			m_ensure.step(2);
		}
		
		ServiceReference getServiceRef() {
			return m_Aref;
		}
	}
}