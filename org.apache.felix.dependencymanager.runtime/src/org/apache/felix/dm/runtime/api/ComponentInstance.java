package org.apache.felix.dm.runtime.api;

import java.util.Dictionary;

/**
 * A Component instance created using a {@link ComponentFactory} service
 */
public interface ComponentInstance {
    /**
     * Destroy the component instance.
     */
    void dispose();
    
    /**
     * Updates the component instance.
     * @param conf the properties used to update the component.
     */
    void update(Dictionary<String, ?> conf);
}
