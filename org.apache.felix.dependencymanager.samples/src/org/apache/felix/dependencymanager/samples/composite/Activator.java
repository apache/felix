package org.apache.felix.dependencymanager.samples.composite;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext ctx, DependencyManager m) throws Exception {
        m.add(createComponent()
            .setImplementation(ProviderImpl.class)
            .setComposition("getComposition")
            .add(createConfigurationDependency().setPid(ProviderImpl.class.getName()))
            .add(createServiceDependency().setService(LogService.class).setRequired(true)));
    }
}
