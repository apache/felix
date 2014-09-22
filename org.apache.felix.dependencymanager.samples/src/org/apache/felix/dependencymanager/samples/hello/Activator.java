package org.apache.felix.dependencymanager.samples.hello;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

/**
 * Simple Activator with one service consumer and a service provider.
 * The ServiceConsumer is also depending on a configuration pid 
 * (see org.apache.felix.dependencymanager.samples.conf.Configurator)
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
        dm.add(createComponent()
            .setImplementation(ServiceProviderImpl.class)
            .setInterface(ServiceProvider.class.getName(), null));
        
        dm.add(createComponent()
            .setImplementation(ServiceConsumer.class)            
            .add(createConfigurationDependency()
                .setPid("org.apache.felix.dependencymanager.samples.hello.ServiceConsumer")
                .setCallback("updated"))
            .add(createServiceDependency()
                .setService(ServiceProvider.class)
                .setRequired(true)));
    
    }
}
