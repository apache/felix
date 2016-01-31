package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;
import org.osgi.framework.ServiceReference;

/**
 * Represents a callback(Component, ServiceReference) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbTypeComponentRef<T, S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param c the first callback parameter
     * @param ref the second callback parameter
     */
    void accept(T instance, Component c, ServiceReference<S> ref);

    default CbTypeComponentRef<T, S> andThen(CbTypeComponentRef<T, S> after) {
        Objects.requireNonNull(after);
        return (T instance, Component c, ServiceReference<S> ref) -> {
            accept(instance, c, ref);
            after.accept(instance, c, ref);
        };
    }
}
