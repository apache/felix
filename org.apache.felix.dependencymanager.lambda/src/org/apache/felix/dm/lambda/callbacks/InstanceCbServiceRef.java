package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.osgi.framework.ServiceReference;

/**
 * Represents a callback(Service, ServiceReference) on an Object instance.
 * 
 * <p> The type of the service passed in argument to the callback is defined by the "S" generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbServiceRef<S> {
    /**
     * Handles the given arguments.
     * @param ref a Service Reference
     * @param service a Service
     */
    void accept(S service, ServiceReference<S> ref);

    default InstanceCbServiceRef<S> andThen(InstanceCbServiceRef<S> after) {
        Objects.requireNonNull(after);
        return (S service, ServiceReference<S> ref) -> {
            accept(service, ref);
            after.accept(service, ref);
        };
    }
}
