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

import org.junit.Assert;
import org.osgi.framework.ServiceReference;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;

/**
 * When a dm component is removed, check if the service it was bound to is unget.
 */
public class UngetServiceTest extends TestBase {
	final Ensure m_ensure = new Ensure();

	public void testUngetService() {
		DependencyManager m = getDM();
		
		Component service = m.createComponent()
				.setImplementation(new Service())
				.setInterface(Service.class.getName(), null);
		m.add(service);
		
		Client clientImpl = new Client();
		Component client = m.createComponent()
				.setImplementation(clientImpl)
				.add(m.createServiceDependency().setService(Service.class).setRequired(true).setCallbacks("bind", null));
		
		m.add(client);
		
		m_ensure.waitForStep(1, 5000);
		m.remove(client);
		
		// The client has been removed and the service reference must have been ungotten.
		ServiceReference ref = clientImpl.getServiceRef();
		Assert.assertEquals(false, this.context.ungetService(ref));		

		m.remove(service);
	}

	public class Service {
	}
	
	public class Client {
		ServiceReference m_ref;
		
		void bind(ServiceReference ref) {
			m_ref = ref;
			m_ensure.step(1);
		}
		
		ServiceReference getServiceRef() {
			return m_ref;
		}
	}

}