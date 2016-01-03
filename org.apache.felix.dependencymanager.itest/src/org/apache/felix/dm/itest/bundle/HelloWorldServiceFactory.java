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
package org.apache.felix.dm.itest.bundle;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class HelloWorldServiceFactory implements ServiceFactory {
	private final Map<ServiceRegistration, HelloWorldService> m_services = new HashMap<>();

	public void stop() {
		synchronized (m_services) {
			if (!m_services.isEmpty()) {
				throw new IllegalStateException("All services should be closed");
			}
		}
	}

	@Override
	public HelloWorld getService(Bundle bundle, ServiceRegistration registration) {
		HelloWorldService service = new HelloWorldService();
		synchronized (m_services) {
			HelloWorldService old = m_services.put(registration, service);
			assert old == null;
		}
		return service;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
		synchronized (m_services) {
			HelloWorldService old = m_services.remove(registration);
			HelloWorldService serv = (HelloWorldService) service;
			assert serv == old;
		}
	}
}
