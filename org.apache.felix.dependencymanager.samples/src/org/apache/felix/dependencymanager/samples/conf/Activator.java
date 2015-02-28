package org.apache.felix.dependencymanager.samples.conf;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext context, DependencyManager dm) throws Exception {        
        dm.add(createComponent()
            .setImplementation(Configurator.class)
            .add(createServiceDependency().setService(LogService.class).setRequired(true))
            .add(createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true)));
    }
}
