package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;
import org.osgi.framework.ServiceReference;

/**
 * Represents a callback(Service, Component, ServiceReference) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbServiceComponentRef<T, S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param service the first callback parameter
     * @param c the second callback parameter
     * @param ref the third callback parameter
     */
    void accept(T instance, S service, Component c, ServiceReference<S> ref);

    default CbServiceComponentRef<T, S> andThen(CbServiceComponentRef<T, S> after) {
        Objects.requireNonNull(after);
        return (T instance, S service, Component c, ServiceReference<S> ref) -> {
            accept(instance, service, c, ref);
            after.accept(instance, service, c, ref);
        };
    }
}
