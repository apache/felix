package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a callback(Component)  on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbComponent {
    /**
     * Handles the given argument.
     * @param component the callback parameter
     */
    void accept(Component component);

    default InstanceCbComponent andThen(InstanceCbComponent after) {
        Objects.requireNonNull(after);
        return (Component component) -> {
            accept(component);
            after.accept(component);
        };
    }
}
