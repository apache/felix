package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;
import org.osgi.framework.ServiceReference;

/**
 * Represents a callback(Service, Component, ServiceReference) on an Object instance.
 * 
 * <p> The type of the service passed in argument to the callback is defined by the "S" generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbServiceComponentRef<S> {
    /**
     * Handles the given arguments.
     * @param c a Component
     * @param ref the service reference
     * @param service the service
     */
    void accept(S service, Component c, ServiceReference<S> ref);

    default InstanceCbServiceComponentRef<S> andThen(InstanceCbServiceComponentRef<S> after) {
        Objects.requireNonNull(after);
        return (S service, Component c, ServiceReference<S> ref) -> {
            accept(service, c, ref);
            after.accept(service, c, ref);
        };
    }
}
