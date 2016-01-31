package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

/**
 * Represents a callback(T param) on an Object instance.
 * 
 * @param T the type of the callback parameter.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbConsumer<T> extends SerializableLambda {
    /**
     * Handles the given argument
     * @param t the argument
     */
    void accept(T t);

    default CbConsumer<T> andThen(CbConsumer<? super T> after) {
        Objects.requireNonNull(after);
        return (T t) -> {
            accept(t);
            after.accept(t);
        };
    }
}
