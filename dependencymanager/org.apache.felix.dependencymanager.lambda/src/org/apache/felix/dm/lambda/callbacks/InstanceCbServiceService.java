package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

/**
 * Represents a swap callback(Service, Service) on an Object instance.
 * 
 * <p> The type of the service passed in argument to the callback is defined by the "S" generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbServiceService<S> extends SerializableLambda {
    /**
     * Handles the given argument
     * @param old a Service
     * @param replace a Service
     */
    void accept(S old, S replace);

    default InstanceCbServiceService<S> andThen(InstanceCbServiceService<S> after) {
        Objects.requireNonNull(after);
        return (S old, S replace) -> {
            accept(old, replace);
            after.accept(old, replace);
        };
    }
}
