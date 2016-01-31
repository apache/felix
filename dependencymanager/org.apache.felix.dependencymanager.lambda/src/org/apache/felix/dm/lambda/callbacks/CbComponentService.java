package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a callback(Component, Service) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbComponentService<S> {
    /**
     * Handles the given arguments
     * @param c the component
     * @param service the service
     */
    void accept(Component c, S service);

    default CbComponentService<S> andThen(CbComponentService<S> after) {
        Objects.requireNonNull(after);
        return (Component c, S service) -> {
            accept(c, service);
            after.accept(c, service);
        };
    }
}
