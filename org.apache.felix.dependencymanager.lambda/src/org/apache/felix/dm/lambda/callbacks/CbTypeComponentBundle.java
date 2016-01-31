package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;
import org.osgi.framework.Bundle;

/**
 * Represents a callback(Component, Bundle) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbTypeComponentBundle<T> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param component the first callback parameter
     * @param bundle the second callback parameter
     */
    void accept(T instance, Component component, Bundle bundle);

    default CbTypeComponentBundle<T> andThen(CbTypeComponentBundle<? super T> after) {
        Objects.requireNonNull(after);
        return (T instance, Component component, Bundle bundle) -> {
            accept(instance, component, bundle);
            after.accept(instance, component, bundle);
        };
    }
}
