package org.apache.felix.dependencymanager.samples.conf;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Configurator class used to inject configuration into Configuration Admin Service.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Configurator {
    private volatile ConfigurationAdmin m_ca;
    volatile Configuration m_serviceConsumerConf;

    public void start() {
        try {
            System.out.println(Thread.currentThread().getName() + ": Starting " + this.getClass().getName());
            m_serviceConsumerConf = m_ca.getConfiguration("org.apache.felix.dependencymanager.samples.hello.ServiceConsumer", null);
            Hashtable props = new Properties();
            props.put("key", "value");
            m_serviceConsumerConf.update(props);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void destroy() throws IOException {
    	m_serviceConsumerConf.delete();
    }
}
