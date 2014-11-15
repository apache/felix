package org.apache.felix.dm.benchmark.controller.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * This activator triggers the scenario controller thread, which will do some microbenchmarks for a given
 * set of scenario bundles. The controller thread is fired only once the framework is started.
 */
public class Activator implements BundleActivator {
	ScenarioControllerImpl m_controller;
	
	@Override
	public void start(BundleContext context) throws Exception {
        m_controller = new ScenarioControllerImpl(context);
        m_controller.start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		m_controller.stop();
	}
}
