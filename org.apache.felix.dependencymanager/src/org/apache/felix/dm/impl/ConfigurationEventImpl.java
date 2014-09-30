package org.apache.felix.dm.impl;

import java.util.Dictionary;

public class ConfigurationEventImpl extends EventImpl {
    private final Dictionary<?,?> m_conf;
    private final String m_pid;
    
    public ConfigurationEventImpl(String pid, Dictionary<?,?> conf) {
        m_pid = pid;
        m_conf = conf;
    }
    
    public String getPid() {
        return m_pid;
    }
    
    public Dictionary<?,?> getConf() {
        return m_conf;
    }
    
    @Override
    public Object getEvent() {
        return m_conf;
    }
}
