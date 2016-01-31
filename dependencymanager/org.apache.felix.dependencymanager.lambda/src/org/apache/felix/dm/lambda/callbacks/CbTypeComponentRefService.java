package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;
import org.osgi.framework.ServiceReference;

/**
 * Represents a callback(Component, ServiceReference, Service) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbTypeComponentRefService<T, S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param c the first callback parameter
     * @param ref the second callback parameter
     * @param service the third callback parameter
     */
    void accept(T instance, Component c, ServiceReference<S> ref, S service);

    default CbTypeComponentRefService<T, S> andThen(CbTypeComponentRefService<T, S> after) {
        Objects.requireNonNull(after);
        return (T instance, Component c, ServiceReference<S> ref, S service) -> {
            accept(instance, c, ref, service);
            after.accept(instance, c, ref, service);
        };
    }
}
