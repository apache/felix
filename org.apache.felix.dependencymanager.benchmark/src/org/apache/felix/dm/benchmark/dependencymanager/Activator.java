package org.apache.felix.dm.benchmark.dependencymanager;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.benchmark.controller.ScenarioController;
import org.osgi.framework.BundleContext;

/**
 * Activator for a scenario based on Dependency Manager 4.0
 * We'll create many Artists, each one is depending on many Albums, and each Album depends on many Tracks.
 */
public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext context, DependencyManager dm) throws Exception {  
        dm.add(createComponent()
            .setImplementation(Benchmark.class)
            .add(createServiceDependency().setService(ScenarioController.class).setRequired(true)));
    }
}
