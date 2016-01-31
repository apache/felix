package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.osgi.framework.ServiceReference;

/**
 * Represents a callback(ServiceReference, Service) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbRefService<S> {
    /**
     * Handles the given arguments.
     * @param ref a Service Reference
     * @param service a Service
     */
    void accept(ServiceReference<S> ref, S service);

    default CbRefService<S> andThen(CbRefService<S> after) {
        Objects.requireNonNull(after);
        return (ServiceReference<S> ref, S service) -> {
            accept(ref, service);
            after.accept(ref, service);
        };
    }
}
