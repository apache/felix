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

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency.Any;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.itest.util.Ensure;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

/**
 * This test validates that all services can be discovered using annotation and using "(objectClass=*)" filter.
 */
@Component
public class FELIX5337 implements FrameworkListener {
	/**
	 * We wait for an Ensure object which has a name matching this constant.
	 */
	public final static String ENSURE = "Felix5337";
	
	/**
	 * We need the bundle context because we register a framework listener in order to detect
	 * when all bundles have been started.
	 */
	@Inject
	volatile BundleContext m_bctx;
	
	/**
	 * We use DM api in order to count all available services. The services count will be compared to the
	 * number of services found using annotation.
	 */
	@Inject
	DependencyManager m_dm;
	
	/**
	 * The Ensure service is registered by the FELIX5337Test test.
	 */
	@ServiceDependency(filter = "(name=" + ENSURE + ")")
	volatile Ensure m_sequencer;
	
	/**
	 * Number of services found using annotations.
	 */
	volatile int m_services;
	
	/**
	 * Number of services found using DM api.
	 */
	volatile int m_servicesAPI;
	
	/**
	 * Component used to track all services (using DM API).
	 */
	volatile org.apache.felix.dm.Component m_component;
	
	/**
	 * The component used to track all available services (using DM API).
	 */
	public class ApiCallback {
		void bindServiceAPI(Object service) {
			m_servicesAPI++;
		}
	}

	/**
	 * Track all available services using annotation.
	 */
	@ServiceDependency(service=Any.class)
	void bindService(Object service) {
		m_services++; // thread safe, in DM, service callbacks are always thread safe.
	}

	@Start
	void start() {
		m_sequencer.step(1);
		// use DM api to count all available services.
		m_component = m_dm.createComponent().setImplementation(new ApiCallback())
						  .add(m_dm.createServiceDependency().setService("(objectClass=*)").setCallbacks("bindServiceAPI", null));
		m_dm.add(m_component);

		// Register a framework listener to detect when the framework is started (at this point, all services are registered).
		if (m_bctx.getBundle(0).getState() != Bundle.ACTIVE) {
			m_bctx.addFrameworkListener(this);
		} else {
			frameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, m_bctx.getBundle(), null));
		}
	}

	@Stop
	void stop() {
		m_dm.remove(m_component);
		m_sequencer.step(3);
	}

	@Override
	public void frameworkEvent(FrameworkEvent event) {
		switch (event.getType()) {
		case FrameworkEvent.STARTED:
		    // some services may be registered asynchronously. so, wait 2 seconds to be sure all services are registered (it's dirty, but how to avoid this ?)
		    new Thread(() -> {
		        try
                {
                    Thread.sleep(2000);
                } catch (InterruptedException e)
                {
                }
		        // Make sure all services found using DM api matches the number of services found using annotations.
		        Assert.assertEquals(m_services, m_servicesAPI);
		        m_sequencer.step(2);
		    }).start();
			break;
		}
	}
}
