package org.apache.felix.dm.lambda.callbacks;

import java.util.Dictionary;
import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a callback(Dictionary, Component) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbDictionaryComponent<T> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param conf the first callback parameter
     * @param component the second callback parameter
     */
    void accept(T instance, Dictionary<String, Object> conf, Component component);

    default CbDictionaryComponent<T> andThen(CbDictionaryComponent<? super T> after) {
        Objects.requireNonNull(after);
        return (T instance, Dictionary<String, Object> conf, Component component) -> {
            accept(instance, conf, component);
            after.accept(instance, conf, component);
        };
    }
}
