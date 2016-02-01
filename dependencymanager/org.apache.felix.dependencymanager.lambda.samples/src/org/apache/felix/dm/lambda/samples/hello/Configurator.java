package org.apache.felix.dm.lambda.samples.hello;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class Configurator {
    ConfigurationAdmin m_cm; 
    
    void bind(ConfigurationAdmin cm) {
        m_cm = cm;
    }
    
    void start() throws IOException {
        // Configure the ServiceConsumer component
        Configuration c = m_cm.getConfiguration(ServiceConsumer.class.getName(), null);
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("foo", "bar");
        c.update(props);
    }
}
