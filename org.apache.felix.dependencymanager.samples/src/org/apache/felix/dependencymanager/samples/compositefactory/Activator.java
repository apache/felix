package org.apache.felix.dependencymanager.samples.compositefactory;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext ctx, DependencyManager m) throws Exception {
        CompositionManager compositionMngr = new CompositionManager();
        m.add(createComponent()
            .setFactory(compositionMngr, "create")
            .setComposition(compositionMngr, "getComposition")
            .add(createConfigurationDependency()
                .setPid(CompositionManager.class.getName())
                .setCallback(compositionMngr, "updated"))
            .add(createServiceDependency().setService(LogService.class).setRequired(true)));
    }
}
