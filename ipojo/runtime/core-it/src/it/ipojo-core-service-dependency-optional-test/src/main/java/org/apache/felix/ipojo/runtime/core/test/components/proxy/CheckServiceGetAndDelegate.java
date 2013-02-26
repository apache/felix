package org.apache.felix.ipojo.runtime.core.test.components.proxy;

import org.apache.felix.ipojo.runtime.core.test.services.CheckService;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;
import org.osgi.framework.BundleContext;

import java.util.Properties;

public class CheckServiceGetAndDelegate implements CheckService {

    private FooService fs;

    private Helper helper;

    public CheckServiceGetAndDelegate(BundleContext bc) {
        helper = new Helper(bc, fs);
    }

    public boolean check() {
        fs.foo();
        return helper.check();
    }

    public Properties getProps() {
        fs.getBoolean();
        return helper.getProps();
    }

}
