package org.apache.felix.dm.lambda.callbacks;

import java.util.Dictionary;
import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a callback(Component, ServiceReference, Dictionary) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbTypeComponentServiceDict<T, S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param c first callback param
     * @param service second callback param
     * @param props third callback param
     */
    void accept(T instance, Component c, S service, Dictionary<String, Object> props);

    default CbTypeComponentServiceDict<T, S> andThen(CbTypeComponentServiceDict<T, S> after) {
        Objects.requireNonNull(after);
        return (T instance, Component c, S s, Dictionary<String, Object> props) -> {
            accept(instance, c, s, props);
            after.accept(instance, c, s, props);
        };
    }
}
