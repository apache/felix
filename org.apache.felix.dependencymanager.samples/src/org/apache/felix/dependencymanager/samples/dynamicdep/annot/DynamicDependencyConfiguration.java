package org.apache.felix.dependencymanager.samples.dynamicdep.annot;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * This interface describes the configuration for our DynamicDependencyComponent component. We are using the bnd metatype
 * annotations, allowing to configure our component  from web console.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@OCD(name = "Dynamic Dependency Configuration (annotation)",
     description = "Declare here the configuration for the DynamicDependency component.")
public interface DynamicDependencyConfiguration {
    
    @AD(description = "Enter the storage type to use", 
        deflt = "mapdb", 
        optionLabels = { "Map DB Storage implementation", "File Storage implementation" },
        optionValues = { "mapdb", "file" })
    String storageType();

    @AD(description = "Specifies here is the storage dependency is required or not (if false, a null object will be used)", deflt = "true")
    boolean storageRequired();

}
