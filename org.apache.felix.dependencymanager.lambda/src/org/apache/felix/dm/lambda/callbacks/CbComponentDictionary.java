package org.apache.felix.dm.lambda.callbacks;

import java.util.Dictionary;
import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a callback(Component, Dictionary) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbComponentDictionary {
    /**
     * Handles the given arguments.
     * @param component a Component
     * @param properties some service properties
     */
    void accept(Component component, Dictionary<String, Object> properties);

    default CbComponentDictionary andThen(CbComponentDictionary after) {
        Objects.requireNonNull(after);
        return (Component component, Dictionary<String, Object> properties) -> {
            accept(component, properties);
            after.accept(component, properties);
        };
    }
}
