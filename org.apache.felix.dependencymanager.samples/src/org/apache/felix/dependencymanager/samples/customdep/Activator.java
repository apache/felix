package org.apache.felix.dependencymanager.samples.customdep;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext context, DependencyManager m) throws Exception {
        m.add(createComponent()
            .setImplementation(PathTracker.class)
            .add(createServiceDependency().setService(LogService.class).setRequired(true))
            .add(new PathDependency("/tmp").setCallbacks("add", "remove")));
    }
}
