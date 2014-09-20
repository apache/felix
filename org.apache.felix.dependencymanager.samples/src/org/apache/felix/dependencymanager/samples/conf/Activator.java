package org.apache.felix.dependencymanager.samples.conf;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;

public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext context, DependencyManager dm) throws Exception {
        dm.add(createComponent()
            .setImplementation(MyComponent.class)
            .add(createConfigurationDependency()
                .setPid(MyComponent.class.getName())
                .setCallback("updated")));
        
        dm.add(createComponent()
            .setImplementation(Configurator.class)
            .add(createServiceDependency()
                .setService(ConfigurationAdmin.class)
                .setRequired(true)));
    }
}
