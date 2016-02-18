package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a swap callback(Service, Service, Component) on an Object instance.
 * 
 * <p> The type of the service passed in argument to the callback is defined by the "S" generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbServiceServiceComponent<S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param c the component
     * @param old the old service
     * @param replace the new service
     */
    void accept(S old, S replace, Component c);

    default InstanceCbServiceServiceComponent<S> andThen(InstanceCbServiceServiceComponent<S> after) {
        Objects.requireNonNull(after);
        return (S old, S replace, Component c) -> {
            accept(old, replace, c);
            after.accept(old, replace, c);
        };
    }
}
