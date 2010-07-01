package org.apache.felix.dm.impl.dependencies;

import org.apache.felix.dm.dependencies.Dependency;
import org.apache.felix.dm.impl.Logger;

public abstract class DependencyBase implements Dependency, DependencyActivation {
    private boolean m_isRequired;
    private boolean m_isInstanceBound;
    protected final Logger m_logger;

    public DependencyBase(Logger logger) {
        m_logger = logger;
    }
    
    public DependencyBase(DependencyBase prototype) {
        m_logger = prototype.m_logger;
        m_isRequired = prototype.isRequired();
        m_isInstanceBound = prototype.m_isInstanceBound;
    }

    public synchronized boolean isRequired() {
        return m_isRequired;
    }
    
    protected synchronized void setIsRequired(boolean isRequired) {
        m_isRequired = isRequired;
    }
    
    public final boolean isInstanceBound() {
        return m_isInstanceBound;
    }

    public final void setIsInstanceBound(boolean isInstanceBound) {
        m_isInstanceBound = isInstanceBound;
    }
}
