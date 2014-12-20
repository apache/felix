package org.apache.felix.dm.benchmark.dependencymanager;

import java.util.concurrent.Executor;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentExecutorFactory;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.benchmark.scenario.Helper;
import org.osgi.framework.BundleContext;

/**
 * Parallel version of our default Activator.
 */
public class ParallelActivator extends Activator {
    public void init(BundleContext context, DependencyManager mgr) throws Exception {
        context.registerService(ComponentExecutorFactory.class.getName(), new ComponentExecutorFactory() {
            @Override
            public Executor getExecutorFor(Component component) {
                return Helper.getThreadPool(); // Return our thread pool shared for all components
            }
        }, null);
        super.init(context, mgr);
    }
}
