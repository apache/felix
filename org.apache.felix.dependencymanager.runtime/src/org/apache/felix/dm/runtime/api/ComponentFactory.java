package org.apache.felix.dm.runtime.api;

import java.util.Dictionary;

/**
 * When a Component is annotated with a DM "Component" annotation with a "factoryName" attribute, a corresponding
 * ComponentFactory is registered in the OSGi service registry with a @link {@link ComponentFactory#FACTORY_NAME} 
 * servie property with the Component "factoryName" value.
 */
public interface ComponentFactory {
    /**
     * Instantiates a Component instance. Any properties starts with a "." are considered as private. Other properties will be
     * published as the component instance service properties (if the component provides a services).
     * @param conf the properties passed to the component "configure" method which is specified with the "configure" attribute
     * of the @Component annotation.  
     * @return the component instance.
     */
    ComponentInstance newInstance(Dictionary<String, ?> conf);
}
