package org.apache.felix.dm.lambda.callbacks;

import java.util.Dictionary;
import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a callback(Component, Dictionary) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbTypeComponentDictionary<T> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param component the first callback parameter
     * @param conf the second callback parameter
     */
    void accept(T instance, Component component, Dictionary<String, Object> conf);

    default CbTypeComponentDictionary<T> andThen(CbTypeComponentDictionary<? super T> after) {
        Objects.requireNonNull(after);
        return (T instance, Component component, Dictionary<String, Object> conf) -> {
            accept(instance, component, conf);
            after.accept(instance, component, conf);
        };
    }
}
