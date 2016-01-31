package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.osgi.framework.ServiceReference;

/**
 * Represents a callback(ServiceReference, Service, ServiceReference, Service) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbRefServiceRefService<S> {
    /**
     * Handles the given arguments
     * @param oldRef a service reference
     * @param old a service
     * @param replaceRef a service reference
     * @param replace a service
     */
    void accept(ServiceReference<S> oldRef, S old, ServiceReference<S> replaceRef, S replace);

    default CbRefServiceRefService<S> andThen(CbRefServiceRefService<S> after) {
        Objects.requireNonNull(after);
        return (ServiceReference<S> oldRef, S old, ServiceReference<S> replaceRef, S replace) -> {
            accept(oldRef, old, replaceRef, replace);
            after.accept(oldRef, old, replaceRef, replace);
        };
    }
}
