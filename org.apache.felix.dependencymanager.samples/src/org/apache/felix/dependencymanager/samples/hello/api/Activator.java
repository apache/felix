package org.apache.felix.dependencymanager.samples.hello.api;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
        dm.add(createComponent()
            .setImplementation(ServiceProviderImpl.class)
            .add(createServiceDependency().setService(LogService.class).setRequired(true))
            .setInterface(ServiceProvider.class.getName(), null));
        
        dm.add(createComponent()
            .setImplementation(ServiceConsumer.class)            
            .add(createServiceDependency().setService(LogService.class).setRequired(true))
            .add(createConfigurationDependency()
                .setPid(ServiceConsumer.class.getName()).setCallback("updated"))
            .add(createServiceDependency().setService(ServiceProvider.class).setRequired(true)));
    }
}
