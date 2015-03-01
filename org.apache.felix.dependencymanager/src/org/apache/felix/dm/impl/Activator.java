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
package org.apache.felix.dm.impl;

import org.apache.felix.dm.ComponentExecutorFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * DependencyManager Activator used to track a ComponentExecutorFactory service optionally registered by 
 * a management agent bundle.
 * 
 * @see {@link ComponentExecutorFactory}
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer {
    private BundleContext m_context;
    
	@Override
	public void start(BundleContext context) throws Exception {
		m_context = context;
        Filter filter = context.createFilter("(objectClass=" + ComponentExecutorFactory.class.getName() + ")");
        ServiceTracker tracker = new ServiceTracker(context, filter, this);
        tracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

	@Override
	public Object addingService(ServiceReference reference) {
		ComponentExecutorFactory factory = (ComponentExecutorFactory) m_context.getService(reference);
		ComponentScheduler.instance().bind(factory);
		return factory;
	}

	@Override
	public void modifiedService(ServiceReference reference, Object service) {
	}

	@Override
	public void removedService(ServiceReference reference, Object service) {
		ComponentExecutorFactory factory = (ComponentExecutorFactory) service;
		ComponentScheduler.instance().unbind(factory);
	}
}
