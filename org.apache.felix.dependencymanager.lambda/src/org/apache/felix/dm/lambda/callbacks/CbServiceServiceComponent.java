package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a swap callback(Service, Service, Component) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbServiceServiceComponent<T, S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param old first callback arg
     * @param replace second callback arg
     * @param c third callback arg
     */ 
    void accept(T instance, S old, S replace, Component c);

    default CbServiceServiceComponent<T, S> andThen(CbServiceServiceComponent<? super T, S> after) {
        Objects.requireNonNull(after);
        return (T instance, S old, S replace, Component c) -> {
            accept(instance, old, replace, c);
            after.accept(instance, old, replace, c);
        };
    }
}
