package dm.it.testbundle;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext context, DependencyManager dm) throws Exception {
        dm.add(createComponent()
                .setInterface(MyService.class.getName(), null)
                .setImplementation(new MyServiceImpl()));
    }
}
