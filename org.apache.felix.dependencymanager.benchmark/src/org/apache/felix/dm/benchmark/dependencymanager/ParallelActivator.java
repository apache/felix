package org.apache.felix.dm.benchmark.dependencymanager;

import java.util.Hashtable;
import java.util.concurrent.Executor;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.benchmark.scenario.Helper;
import org.osgi.framework.BundleContext;

/**
 * Parallel version of our default Activator.
 */
public class ParallelActivator extends Activator {
    public void init(BundleContext context, DependencyManager mgr) throws Exception {
        Hashtable<String, String> props = new Hashtable<>();
        props.put("target", DependencyManager.THREADPOOL);
        context.registerService(Executor.class.getName(), Helper.getThreadPool(), props);
        super.init(context, mgr);
    }
}
