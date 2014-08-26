package org.apache.felix.dm.benchmark.dependencymanager;

import java.util.Properties;
import java.util.concurrent.Executor;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.benchmark.scenario.Helper;
import org.osgi.framework.BundleContext;

/**
 * Parallel version of our default Activator.
 */
public class ParallelActivator extends Activator {
    public void start(BundleContext context) throws Exception {
        Properties props = new Properties();
        props.put("target", DependencyManager.THREADPOOL);
        context.registerService(Executor.class.getName(), Helper.getThreadPool(), props);
        super.start(context);
    }
}
