package org.apache.felix.dependencymanager.samples.conf;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class Configurator {
    private volatile ConfigurationAdmin m_ca;
    Configuration m_conf;
    
    public void start() {
        try {
            System.out.println("Starting " + this.getClass().getName());
            m_conf = m_ca.getConfiguration(MyComponent.class.getName(), null);
            Hashtable props = new Properties();
            props.put("key", "value");
            m_conf.update(props);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void destroy() throws IOException {
    	m_conf.delete();  
    }
}
