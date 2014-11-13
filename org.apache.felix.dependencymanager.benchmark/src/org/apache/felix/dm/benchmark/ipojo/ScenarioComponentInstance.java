package org.apache.felix.dm.benchmark.ipojo;

import java.util.Dictionary;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.api.PrimitiveComponentType;

/**
 * Holder class for one of the scenario component instance (Artist, ALbum, Track).
 */
public class ScenarioComponentInstance {
    final String m_id;
    final ComponentInstance m_instance;
    
    public ScenarioComponentInstance(String id, PrimitiveComponentType type, Dictionary<?,?> conf) {
        m_id = id;
        try {
            m_instance = type.createInstance(conf);
        } catch (UnacceptableConfiguration | MissingHandlerException | ConfigurationException e) {
            throw new RuntimeException("Could create component instance", e);
        }
    }
    
    public String getId() { return m_id; }
    public ComponentInstance getComponentInstance() { return m_instance; }
}

