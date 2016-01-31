package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

/**
 * Represents a callback(Service, Service) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbTypeServiceService<T, S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param old first callback param
     * @param replace second callback param
     */
    void accept(T instance, S old, S replace);

    default CbTypeServiceService<T, S> andThen(CbTypeServiceService<? super T, S> after) {
        Objects.requireNonNull(after);
        return (T instance, S old, S replace) -> {
            accept(instance, old, replace);
            after.accept(instance, old, replace);
        };
    }
}
