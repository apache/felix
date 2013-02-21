package org.apache.felix.ipojo.runtime.core.test.components.proxy;

import org.apache.felix.ipojo.runtime.core.test.services.CheckService;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

public class Helper implements CheckService {


    private FooService fs;
    private BundleContext context;
    private ServiceRegistration reg;

    public Helper(BundleContext bc, FooService svc) {
        fs = svc;
        context = bc;
    }

    public void publish() {
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, "Helper");
        context.registerService(CheckService.class, this, props);
        reg = context.registerService(CheckService.class.getName(), this, props);
    }

    public void unpublish() {
        if (reg != null) {
            reg.unregister();
        }
        reg = null;
    }

    public boolean check() {
        return fs.foo();
    }

    public Properties getProps() {
        Properties props = new Properties();
        fs.getBoolean();
        props.put("helper.fs", fs);
        return props;
    }

}
