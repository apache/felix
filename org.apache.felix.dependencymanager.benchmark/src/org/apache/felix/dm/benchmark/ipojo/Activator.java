package org.apache.felix.dm.benchmark.ipojo;

import org.apache.felix.dm.benchmark.controller.ScenarioController;
import org.apache.felix.dm.benchmark.scenario.Helper;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer {   
	BundleContext m_context;
	IpojoScenario m_scenario;
	
    @Override
    public void start(BundleContext context) throws Exception {
        Helper.debug(() -> Activator.class.getName() + ".start()");
        m_context = context;
        // Wait for the ScenarioController, before starting the actual stress test.               
	    Filter filter = context.createFilter("(objectClass=" + ScenarioController.class.getName() + ")");
	    ServiceTracker tracker = new ServiceTracker(context, filter, this);
	    tracker.open();        
    }

	@Override
	public Object addingService(ServiceReference reference) {
		// At this point, we can create our Component instance
		ScenarioController controller = (ScenarioController) m_context.getService(reference);
		m_scenario = new IpojoScenario(m_context);
		m_scenario.start();
		return controller;
	}

	@Override
	public void modifiedService(ServiceReference reference, Object service) {
	}

	@Override
	public void removedService(ServiceReference reference, Object service) {
	}
	
    @Override
    public void stop(BundleContext context) throws Exception {
        Helper.debug(() -> Activator.class.getName() + ".stop()");
        if (m_scenario != null) {
        	m_scenario.stop();
        }
    }
}
