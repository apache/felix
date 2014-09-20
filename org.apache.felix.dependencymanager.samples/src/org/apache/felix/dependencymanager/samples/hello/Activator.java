package org.apache.felix.dependencymanager.samples.hello;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

/**
 * Simple Activator with one service consumer and a service provider.
 */
public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
        dm.add(createComponent()
            .setImplementation(ServiceImplementation.class)
            .setInterface(ServiceInterface.class.getName(), null));
        
        dm.add(createComponent()
            .setImplementation(ServiceConsumer.class)
            .add(createServiceDependency()
                .setService(ServiceInterface.class)
                .setRequired(true)));
    
    }
}
