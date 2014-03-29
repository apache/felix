package dm.it.testbundle;

import org.osgi.framework.BundleContext;

import dm.DependencyActivatorBase;
import dm.DependencyManager;

public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext context, DependencyManager dm) throws Exception {
        dm.add(createComponent()
                .setInterface(MyService.class.getName(), null)
                .setImplementation(new MyServiceImpl()));
    }
}
