package org.apache.felix.ipojo.runtime.core.test.components.proxy;

import org.apache.felix.ipojo.runtime.core.test.services.CheckService;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;
import org.osgi.framework.BundleContext;

import java.util.Properties;

public class CheckServiceDelegator implements CheckService {

    private FooService fs;

    private Helper helper;

    public CheckServiceDelegator(BundleContext bc) {
        helper = new Helper(bc, fs);
    }

    public boolean check() {
        // Don't access the service
        // Just delegate
        return helper.check();
    }

    public Properties getProps() {
        // Don't access the service
        // Just delegate
        return helper.getProps();
    }

}
