package org.apache.felix.dm.benchmark.controller.impl;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

/**
 * This activator triggers the scenario controller thread, which will do some microbenchmarks for a given
 * set of scenario bundles. The controller thread is fired only once the framework is started.
 */
public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext context, DependencyManager m) throws Exception {
        m.add(createComponent().setImplementation(ScenarioControllerImpl.class));
    }
}
