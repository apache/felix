package org.apache.felix.dependencymanager.samples.tpool;

import java.util.Hashtable;
import java.util.concurrent.Executor;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

/**
 * Registers a threadpool in the OSGi service registry to enable parallelism.
 * DependencyManager core will use the threadpool when handling components dependencies and
 * components lifecycle callbacks.
 * 
 * Important note: since we are using the DM API to declare our threadpool, we have to disable
 * parallelism for our "org.apache.felix.dependencymanager.samples.tpool.ThreadPool" component.
 * To do so, we use the 
 * "org.apache.felix.dependencymanager.parallelism=!org.apache.felix.dependencymanager.samples.tpool,*"
 * OSGI service property (see the bnd.bnd file).
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyActivatorBase {  
    @Override
    public void init(BundleContext context, DependencyManager mgr) throws Exception {
        Hashtable<String, String> props = new Hashtable<>();
        props.put("target", DependencyManager.THREADPOOL);
        mgr.add(createComponent()
            .setInterface(Executor.class.getName(), props)
            .setImplementation(ThreadPool.class));
    }
}
