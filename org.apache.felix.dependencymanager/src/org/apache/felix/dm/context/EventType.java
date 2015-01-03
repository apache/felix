package org.apache.felix.dm.context;

/**
 * Types of dependency events
 */
public enum EventType {
    /**
     * A Dependency service becomes available.
     */
    ADDED, 
    
    /**
     * A Dependency service has changed.    
     */
    CHANGED, 
    
    /**
     * A Dependency service becomes unavailable.
     */
    REMOVED,
    
    /**
     * A Dependency service has been swapped by another one.
     */
    SWAPPED
}
