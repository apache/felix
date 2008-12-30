package org.apache.felix.ipojo.api;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;

public abstract class ComponentType {
        
    private List m_instances = new ArrayList();
    
    public abstract Factory getFactory();
    
    public abstract void start();
    public abstract void stop();
    
    
    public ComponentInstance createInstance() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        ComponentInstance ci = _getFactory().createComponentInstance(null); 
        m_instances.add(ci);
        return ci;
    }
    
    public ComponentInstance createInstance(String name) throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        Dictionary dict = null;
        if (name != null) {
            dict = new Properties();
            dict.put("instance.name", name);
        } 
        ComponentInstance ci = _getFactory().createComponentInstance(dict); 
        m_instances.add(ci);
        return ci;
    }
    
    public ComponentInstance createInstance(Dictionary conf) throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        ComponentInstance ci = _getFactory().createComponentInstance(conf);
        m_instances.add(ci);
        return ci;
    }
    
    public boolean disposeInstance(ComponentInstance ci) {
        if (m_instances.remove(ci)) {
            ci.dispose();
            return true;
        } else {
            System.err.println("The instance was not created from this component type");
            return false;
        }
    }
    
    public ComponentInstance getInstanceByName(String name) {
        for (int i = 0; i < m_instances.size(); i++) {
            ComponentInstance ci = (ComponentInstance) m_instances.get(i);
            if (ci.getInstanceName().equals(name)) {
                return ci;
            }
        }
        return null;
    }
    
    public boolean disposeInstance(String name) {
        ComponentInstance ci = getInstanceByName(name);
        if (ci == null) {
            System.err.println("The instance was not found in this component type");
            return false;
        } else {
            return disposeInstance(ci);
        }
    }
    
    private Factory _getFactory() {
        ensureFactory();
        return getFactory();
    }
    
    /**
     * Checks if the factory is already created.
     */
    private void ensureFactory() {
        if (getFactory() == null) {
            throw new IllegalStateException("The factory associated with the component type is not created");
        } else {
            if (getFactory().getState() == Factory.INVALID) {
                throw new IllegalStateException("The factory associated with the component type is invalid (not started or missing handlers)");
            }
        }
    }
    
    
    


}
