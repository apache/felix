package org.apache.felix.dm.itest;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;

public class FELIX2875_ServiceDependencyWithoutServiceNameTest extends TestBase {
    Ensure m_e;
    
    public void testServiceDependencyWithoutName() {
        m_e = new Ensure();
        DependencyManager dm = getDM();
        Component consumer = dm.createComponent()
            .setImplementation(new ConsumeServiceDependencyWithoutName())
            .add(dm.createServiceDependency()
                .setService("(provider=*)").setRequired(true)
                .setCallbacks("add", null))
            .add(dm.createServiceDependency()
                .setService("(|(provider=provider1)(provider=provider2))").setRequired(true)
                .setAutoConfig("m_providers"));
        
        Hashtable<String, String> props = new Hashtable<>();
        props.put("provider", "provider1");
        Component provider1 = dm.createComponent()
            .setImplementation(new Provider1())
            .setInterface(Provider.class.getName(), props);
            
        props = new Hashtable<>();
        props.put("provider", "provider2");
        Component provider2 = dm.createComponent()
            .setImplementation(new Provider2())
            .setInterface(Provider.class.getName(), props);

       dm.add(provider1);
       dm.add(provider2);
       dm.add(consumer);
       m_e.waitForStep(5, 5000);                
    }
    
    private class ConsumeServiceDependencyWithoutName {
        volatile Map<Object, Dictionary<String, String>> m_providers; // autoconfig

        @SuppressWarnings("unused")
        void add(Map<String, Object> props, Object service) {
            if ("provider1".equals(props.get("provider"))) {
                m_e.step();
            } else if ("provider2".equals(props.get("provider"))) {
                m_e.step();
            }
        }
        
        @SuppressWarnings("unused")
        void start() {
            // check if all providers have been injected in our autoconfig field.
            for (Map.Entry<Object, Dictionary<String, String>> e : m_providers.entrySet()) {
                if ("provider1".equals(e.getValue().get("provider"))) {
                    m_e.step();
                } else if ("provider2".equals(e.getValue().get("provider"))) {
                    m_e.step();
                }
            }
            m_e.step(5);
        }
    }
    
    public interface Provider {
    }
    
    public class Provider1 implements Provider {
    }
    
    public class Provider2 implements Provider {
    }
}
