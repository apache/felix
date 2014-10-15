package org.apache.felix.dependencymanager.samples.conf;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * This Activator defines a component that is used to inject configuration into the Configuration Admin service, 
 * in order to configure some other components defined in other sample sub-projects.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext context, DependencyManager dm) throws Exception {        
        dm.add(createComponent()
            .setImplementation(Configurator.class)
            .add(createServiceDependency()
                .setService(ConfigurationAdmin.class)
                .setRequired(true)));        
    }
}
