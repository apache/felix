package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a callback(Component, Service, Service) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbComponentServiceService<S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param c the component
     * @param old the old service
     * @param replace the new service
     */
    void accept(Component c, S old, S replace);

    default CbComponentServiceService<S> andThen(CbComponentServiceService<S> after) {
        Objects.requireNonNull(after);
        return (Component c, S old, S replace) -> {
            accept(c, old, replace);
            after.accept(c, old, replace);
        };
    }
}
