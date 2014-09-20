package org.apache.felix.dependencymanager.samples.tpool;

import java.util.Hashtable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

/**
 * Registers a threadpool in the OSGi service registry to enable parallelism.
 * DependencyManager core will use the threadpool when handling components dependencies and
 * components lifecycle callbacks.
 * 
 * To turn on parallelism, the "org.apache.felix.dependencymanager.parallel" system property must also be set
 * to "true".
 */
public class Activator extends DependencyActivatorBase {  
    @Override
    public void init(BundleContext context, DependencyManager mgr) throws Exception {
        System.out.println("Declaring ThreadPool for DependencyManager.");
        Hashtable props = new Hashtable();
        props.put("target", DependencyManager.THREADPOOL);
        mgr.add(createComponent()
            .setInterface(Executor.class.getName(), props)
            .setImplementation(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())));
    }
}
