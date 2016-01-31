package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a callback(Component, Service, Service) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbTypeComponentServiceService<T, S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param c first callback param
     * @param old second callback param
     * @param replace third callback param
     */ 
    void accept(T instance, Component c, S old, S replace);

    default CbTypeComponentServiceService<T, S> andThen(CbTypeComponentServiceService<? super T, S> after) {
        Objects.requireNonNull(after);
        return (T instance, Component c, S old, S replace) -> {
            accept(instance, c, old, replace);
            after.accept(instance, c, old, replace);
        };
    }
}
