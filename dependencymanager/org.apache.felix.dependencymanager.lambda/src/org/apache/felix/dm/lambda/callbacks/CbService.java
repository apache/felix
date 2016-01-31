package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

/**
 * Represents a callback(Service) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbService<S> {
    /**
     * Handles the given argument.
     * @param service a Service
     */
    void accept(S service);

    default CbService<S> andThen(CbService<S> after) {
        Objects.requireNonNull(after);
        return (S service) -> {
            accept(service);
            after.accept(service);
        };
    }
}
