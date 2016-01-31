package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a callback(Component) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbTypeComponent<T> extends SerializableLambda {
    /**
     * Handles the given arguments
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param component the callback parameter 
     */
    void accept(T instance, Component component);

    default CbTypeComponent<T> andThen(CbTypeComponent<T> after) {
        Objects.requireNonNull(after);
        return (T instance, Component component) -> {
            accept(instance, component);
            after.accept(instance, component);
        };
    }
}
