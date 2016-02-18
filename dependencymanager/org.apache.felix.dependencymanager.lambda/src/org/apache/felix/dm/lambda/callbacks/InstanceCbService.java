package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

/**
 * Represents a callback(Service) on an Object instance.
 * 
 * <p> The type of the service passed in argument to the callback is defined by the "S" generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbService<S> {
    /**
     * Handles the given argument.
     * @param service a Service
     */
    void accept(S service);

    default InstanceCbService<S> andThen(InstanceCbService<S> after) {
        Objects.requireNonNull(after);
        return (S service) -> {
            accept(service);
            after.accept(service);
        };
    }
}
