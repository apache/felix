package org.apache.felix.dm.lambda.callbacks;

import java.util.Map;
import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a callback(Component, ServiceReference, Service, Service Reference, Service) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbTypeComponentServiceMap<T, S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param c first callback param
     * @param service second callback param
     * @param props third callback param
     */
    void accept(T instance, Component c, S service, Map<String, Object> props);

    default CbTypeComponentServiceMap<T, S> andThen(CbTypeComponentServiceMap<T, S> after) {
        Objects.requireNonNull(after);
        return (T instance, Component c, S s, Map<String, Object> props) -> {
            accept(instance, c, s, props);
            after.accept(instance, c, s, props);
        };
    }
}
