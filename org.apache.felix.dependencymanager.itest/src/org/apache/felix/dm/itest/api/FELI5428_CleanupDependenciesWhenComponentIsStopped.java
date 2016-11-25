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

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;

/**
 * When a component is removed using DependencyManager.remove method, then this
 * test checks if the component removes from its internal datastructure the dependencies
 * which are unbound while the component is being removed.
 */
public class FELI5428_CleanupDependenciesWhenComponentIsStopped extends TestBase {

	final Ensure m_ensure = new Ensure();
	
	public void testCleanupDependenciesDuringComponentRemove() {
		DependencyManager m = getDM();
		
		Component p1 = m.createComponent()
				.setImplementation(new ProviderImpl()).setInterface(Provider.class.getName(), null);
		Component p2 = m.createComponent()
				.setImplementation(new ProviderImpl()).setInterface(Provider.class.getName(), null);
		Consumer c = new Consumer();
		Component consumer = m.createComponent()
				.setImplementation(c)
				.add(m.createServiceDependency().setService(Provider.class).setCallbacks("providerAdded", "providerRemoved"));
				
		m.add(p1);
		m.add(p2);
		m.add(consumer);
	
		Assert.assertEquals(2, c.getProvidersCount());
		
		// remove and re-add the consumer. When the consumer is removed, the internal collection holding the list of producers should be cleared,
		// else, when we re-add the consumer, it would be injected with 4 providers !
		
		m.remove(consumer);
		m.remove(p1);
		m.remove(p2);
		m.add(consumer);
		m.add(p1);
		m.add(p2);
		Assert.assertEquals(2, c.getProvidersCount());
	}
	
	
	interface Provider {}
	
	class ProviderImpl implements Provider {
		
	}
	
	class Consumer {
		final List<Provider> m_providers = new ArrayList<>();
		
		private void providerAdded(Provider provider) {
			m_providers.add(provider);
		}
		
		private void  providerRemoved(Provider provider)  {
			m_providers.remove(provider);
		}
		
		public int getProvidersCount() {
			return m_providers.size();
		}
	}
	

}
