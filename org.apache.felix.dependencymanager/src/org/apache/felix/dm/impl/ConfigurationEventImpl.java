package org.apache.felix.dm.impl;

import java.util.Dictionary;

import org.apache.felix.dm.context.Event;

public class ConfigurationEventImpl extends Event {
    private final String m_pid;
    
    public ConfigurationEventImpl(String pid, Dictionary<String, Object> conf) {
        super(conf);
        m_pid = pid;
    }
    
    public String getPid() {
        return m_pid;
    }
        
    @Override
    public int compareTo(Event other) {
        return m_pid.compareTo(((ConfigurationEventImpl) other).m_pid);
    }

    @Override
    public Dictionary<String, Object> getProperties() {
        return getEvent();
    }
}
