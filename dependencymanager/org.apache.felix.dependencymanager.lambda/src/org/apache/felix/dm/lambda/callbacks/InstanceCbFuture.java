package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

/**
 * Represents a callback that accepts the result of a CompletableFuture. The callback is invoked on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbFuture<F> {
    /**
     * Handles the result of a CompletableFuture operation.
     * @param future the result of a CompletableFuture operation.
     */
    void accept(F future);

    default InstanceCbFuture<F> andThen(InstanceCbFuture<? super F> after) {
        Objects.requireNonNull(after);
        return (F f) -> {
            accept(f);
            after.accept(f);
        };
    }
}
