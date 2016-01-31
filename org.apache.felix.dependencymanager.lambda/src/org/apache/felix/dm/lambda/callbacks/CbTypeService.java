package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

/**
 * Represents a callback(Service) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbTypeService<T, S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param service first callback param
     */
    void accept(T instance, S service);

    default CbTypeFuture<T, S> andThen(CbTypeFuture<? super T, S> after) {
        Objects.requireNonNull(after);
        return (T instance, S service) -> {
            accept(instance, service);
            after.accept(instance, service);
        };
    }
}
