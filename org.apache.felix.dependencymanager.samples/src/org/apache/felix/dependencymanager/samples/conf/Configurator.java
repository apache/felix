package org.apache.felix.dependencymanager.samples.conf;

import java.io.IOException;
import java.util.Hashtable;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;

/**
 * Configurator class used to inject configuration into Configuration Admin Service.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Configurator {
    private volatile ConfigurationAdmin m_ca;
    volatile Configuration m_serviceConsumerConf;
    volatile Configuration m_serviceConsumerAnnotConf;
    volatile LogService m_log;
    
    public void start() {
        try {
            System.out.println("Configuring sample components ... please consult log service messages for each sample you want to play with.");
            System.out.println("For example: \"log info|grep org.apache.felix.dependencymanager.samples.device.api\"");
            // Provide configuration to the hello.ServiceConsumer component
            m_serviceConsumerConf = m_ca.getConfiguration("org.apache.felix.dependencymanager.samples.hello.api.ServiceConsumer", null);
            Hashtable<String, String> props = new Hashtable<>();
            props.put("key", "value");
            m_serviceConsumerConf.update(props);
            
            // Provide configuration to the hello.annot.ServiceConsumer component
            m_serviceConsumerAnnotConf = m_ca.getConfiguration("org.apache.felix.dependencymanager.samples.hello.annot.ServiceConsumer", null);
            props = new Hashtable<>();
            props.put("key", "value");
            m_serviceConsumerAnnotConf.update(props);
            
            // Provide configuration to the composite component
            m_serviceConsumerAnnotConf = m_ca.getConfiguration("org.apache.felix.dependencymanager.samples.composite.ProviderImpl", null);
            props = new Hashtable<>();
            props.put("key", "value");
            m_serviceConsumerAnnotConf.update(props);

            // Provide configuration to the compositefactory component
            m_serviceConsumerAnnotConf = m_ca.getConfiguration("org.apache.felix.dependencymanager.samples.compositefactory.CompositionManager", null);
            props = new Hashtable<>();
            props.put("key", "value");
            m_serviceConsumerAnnotConf.update(props);            
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void destroy() throws IOException {
    	m_serviceConsumerConf.delete();
        m_serviceConsumerAnnotConf.delete();
    }
}
