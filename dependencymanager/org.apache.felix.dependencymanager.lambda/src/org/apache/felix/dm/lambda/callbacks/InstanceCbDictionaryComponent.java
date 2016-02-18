package org.apache.felix.dm.lambda.callbacks;

import java.util.Dictionary;
import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a callback(Dictionary, Component) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbDictionaryComponent {
    /**
     * Handles the given arguments.
     * @param properties some service properties
     * @param component a Component
     */
    void accept(Dictionary<String, Object> properties, Component component);

    default InstanceCbDictionaryComponent andThen(InstanceCbDictionaryComponent after) {
        Objects.requireNonNull(after);
        return (Dictionary<String, Object> properties, Component component) -> {
            accept(properties, component);
            after.accept(properties, component);
        };
    }
}
