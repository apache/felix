package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

/**
 * Represents a callback(Service, Service) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbServiceService<S> extends SerializableLambda {
    /**
     * Handles the given argument
     * @param old a Service
     * @param replace a Service
     */
    void accept(S old, S replace);

    default CbServiceService<S> andThen(CbServiceService<S> after) {
        Objects.requireNonNull(after);
        return (S old, S replace) -> {
            accept(old, replace);
            after.accept(old, replace);
        };
    }
}
