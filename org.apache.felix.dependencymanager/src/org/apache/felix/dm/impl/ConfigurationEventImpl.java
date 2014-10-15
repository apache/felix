package org.apache.felix.dm.impl;

import java.util.Dictionary;

import org.apache.felix.dm.context.Event;

public class ConfigurationEventImpl implements Event {
    private final Dictionary<String, Object> m_conf;
    private final String m_pid;
    
    public ConfigurationEventImpl(String pid, Dictionary<String, Object> conf) {
        m_pid = pid;
        m_conf = conf;
    }
    
    public String getPid() {
        return m_pid;
    }
        
    @Override
    public Object getEvent() {
        return m_conf;
    }

    @Override
    public int compareTo(Event other) {
        return m_pid.compareTo(((ConfigurationEventImpl) other).m_pid);
    }

    @Override
    public Dictionary<String, Object> getProperties() {
        return m_conf;
    }

    @Override
    public void close() {
    }
}
