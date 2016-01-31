package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

/**
 * Represents a callback that accepts a the result of a CompletableFuture. The callback is invoked on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbFuture<F> {
    /**
     * Handles the result of a CompletableFuture operation.
     * @param future the result of a CompletableFuture operation.
     */
    void accept(F future);

    default CbFuture<F> andThen(CbFuture<? super F> after) {
        Objects.requireNonNull(after);
        return (F f) -> {
            accept(f);
            after.accept(f);
        };
    }
}
