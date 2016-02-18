package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a callback(Service, Component) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbServiceComponent<T, S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param service the first callback argument
     * @param c the second callback argument
     */
    void accept(T instance, S service, Component c);

    default CbServiceComponent<T, S> andThen(CbServiceComponent<T, S> after) {
        Objects.requireNonNull(after);
        return (T instance, S s, Component c) -> {
            accept(instance, s, c);
            after.accept(instance, s, c);
        };
    }
}
