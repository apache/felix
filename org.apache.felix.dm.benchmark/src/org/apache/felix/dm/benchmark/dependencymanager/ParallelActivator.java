package org.apache.felix.dm.benchmark.dependencymanager;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.benchmark.scenario.Helper;
import org.osgi.framework.BundleContext;

/**
 * Parallel version of our default Activator.
 */
public class ParallelActivator extends Activator {
    public void init(BundleContext context, DependencyManager mgr) throws Exception {  
        mgr.setThreadPool(Helper.getThreadPool());
        super.init(context, mgr);
    }
}
