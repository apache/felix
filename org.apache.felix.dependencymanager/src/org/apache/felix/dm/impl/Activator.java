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
import org.apache.felix.dm.FilterIndex;
import org.apache.felix.dm.impl.index.ServiceRegistryCacheManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * DependencyManager Activator used to track a ComponentExecutorFactory service
 * optionally registered by a management agent bundle.
 * 
 * @see {@link ComponentExecutorFactory}
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator implements BundleActivator {
	private BundleContext m_context;
	private ServiceTracker<ComponentExecutorFactory, ComponentExecutorFactory> m_execTracker;
	private ServiceTracker<FilterIndex, FilterIndex> m_indexTracker;

	@Override
	public void start(BundleContext context) throws Exception {		
		// make sure the backdoor is set in system properties (needed by tests)
		ServiceRegistryCacheManager.init(); 

		m_context = context;
		Filter execFilter = context.createFilter("(objectClass=" + ComponentExecutorFactory.class.getName() + ")");
		m_execTracker = new ServiceTracker<>(context, execFilter, new ExecutorFactoryCustomizer());
		m_execTracker.open();
		
		Filter indexFilter = context.createFilter("(objectClass=" + FilterIndex.class.getName() + ")");
		m_indexTracker = new ServiceTracker<>(context, indexFilter, new FilterIndexCustomizer());
		m_indexTracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (m_execTracker != null) {
			m_execTracker.close();
		}
		if (m_indexTracker != null) {
			m_indexTracker.close();
		}
	}

	private class ExecutorFactoryCustomizer implements ServiceTrackerCustomizer<ComponentExecutorFactory, ComponentExecutorFactory> {
		@Override
		public ComponentExecutorFactory addingService(ServiceReference<ComponentExecutorFactory> reference) {
			ComponentExecutorFactory factory = (ComponentExecutorFactory) m_context.getService(reference);
			ComponentScheduler.instance().bind(factory);
			return factory;
		}

		@Override
		public void modifiedService(ServiceReference<ComponentExecutorFactory> reference, ComponentExecutorFactory service) {
		}

		@Override
		public void removedService(ServiceReference<ComponentExecutorFactory> reference, ComponentExecutorFactory factory) {
			ComponentScheduler.instance().unbind(factory);
		}
	}
	
	private class FilterIndexCustomizer implements ServiceTrackerCustomizer<FilterIndex, FilterIndex> {
		@Override
		public FilterIndex addingService(ServiceReference<FilterIndex> reference) {
			FilterIndex index = m_context.getService(reference);
			ServiceRegistryCacheManager.registerFilterIndex(index, m_context);
			return index;
		}

		@Override
		public void modifiedService(ServiceReference<FilterIndex> reference, FilterIndex service) {				
		}

		@Override
		public void removedService(ServiceReference<FilterIndex> reference, FilterIndex index) {
			ServiceRegistryCacheManager.unregisterFilterIndex(index);
		}				
	}
}
