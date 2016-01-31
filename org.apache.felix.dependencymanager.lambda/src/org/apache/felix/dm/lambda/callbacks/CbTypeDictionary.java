package org.apache.felix.dm.lambda.callbacks;

import java.util.Dictionary;
import java.util.Objects;

/**
 * Represents a callback(Dictionary) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbTypeDictionary<T> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param conf first callback param
     */
    void accept(T instance, Dictionary<String, Object> conf);

    default CbTypeDictionary<T> andThen(CbTypeDictionary<? super T> after) {
        Objects.requireNonNull(after);
        return (T instance, Dictionary<String, Object> conf) -> {
            accept(instance, conf);
            after.accept(instance, conf);
        };
    }
}
