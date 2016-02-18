package org.apache.felix.dm.lambda.samples.compositefactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class Configurator {
    volatile ConfigurationAdmin m_cm; // injected by reflection.
    
    void start() throws IOException {
        // Configure the ServiceConsumer component
        Configuration c = m_cm.getConfiguration(MyConfig.class.getName(), null);
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("foo", "bar");
        c.update(props);
    }
}
