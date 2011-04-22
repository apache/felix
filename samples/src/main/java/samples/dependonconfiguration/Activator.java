package samples.dependonconfiguration;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

public class Activator extends DependencyActivatorBase {
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        manager.add(createComponent()
            .setImplementation(Task.class)
            .add(createConfigurationDependency()
                .setPid("config.pid")
                // The following is optional and allows to display our configuration from webconsole
                .setHeading("Task Configuration") 
                .setDescription("Configuration for the Task Service")
                .add(createPropertyMetaData()
                     .setCardinality(0)
                     .setType(String.class)
                     .setHeading("Task Interval")
                     .setDescription("Declare here the interval used to trigger the Task")
                     .setDefaults(new String[] {"10"})
                     .setId("interval"))));
    }
    
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {}
}
