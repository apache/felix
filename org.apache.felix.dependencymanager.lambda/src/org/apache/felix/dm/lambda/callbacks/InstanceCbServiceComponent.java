package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a callback(Service, Component) on an Object instance.
 * 
 * <p> The type of the service passed in argument to the callback is defined by the "S" generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbServiceComponent<S> {
    /**
     * Handles the given arguments
     * @param c the component
     * @param service the service
     */
    void accept(S service, Component c);

    default InstanceCbServiceComponent<S> andThen(InstanceCbServiceComponent<S> after) {
        Objects.requireNonNull(after);
        return (S service, Component c) -> {
            accept(service, c);
            after.accept(service, c);
        };
    }
}
