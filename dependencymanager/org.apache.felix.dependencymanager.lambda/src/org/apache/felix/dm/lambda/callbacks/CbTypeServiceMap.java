package org.apache.felix.dm.lambda.callbacks;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a callback(Component, Map) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbTypeServiceMap<T, S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param service first callback param
     * @param properties second callback param
     */
    void accept(T instance, S service, Map<String, Object> properties);

    default CbTypeServiceMap<T, S> andThen(CbTypeServiceMap<? super T, S> after) {
        Objects.requireNonNull(after);
        return (T instance, S service, Map<String, Object> properties) -> {
            accept(instance, service, properties);
            after.accept(instance, service, properties);
        };
    }
}
