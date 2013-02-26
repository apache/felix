package org.apache.felix.ipojo.runtime.core.components;

import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.Service;

import java.util.Properties;

public class ServiceConsumer implements CheckService {

    private Service service;
    private Properties props = new Properties();

    public ServiceConsumer() {
        props.put("1", new Integer(service.count()));
        props.put("2", new Integer(service.count()));
        props.put("3", new Integer(service.count()));
    }

    public boolean check() {
        return service.count() > 0;
    }

    public Properties getProps() {
        return props;
    }

}
