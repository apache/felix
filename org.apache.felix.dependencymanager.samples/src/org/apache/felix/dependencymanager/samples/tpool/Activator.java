package org.apache.felix.dependencymanager.samples.tpool;

import org.apache.felix.dm.ComponentExecutorFactory;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

/**
 * See README file describing this Activator.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyActivatorBase {  
    @Override
    public void init(BundleContext context, DependencyManager mgr) throws Exception {
        mgr.add(createComponent()
            .setInterface(ComponentExecutorFactory.class.getName(), null)
            .setImplementation(ComponentExecutorFactoryImpl.class));
    }
}
