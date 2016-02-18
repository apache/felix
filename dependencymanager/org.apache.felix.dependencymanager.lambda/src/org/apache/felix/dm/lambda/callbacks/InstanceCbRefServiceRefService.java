package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.osgi.framework.ServiceReference;

/**
 * Represents a swap callback(ServiceReference, Service, ServiceReference, Service) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbRefServiceRefService<S> {
    /**
     * Handles the given arguments
     * @param oldRef the old service reference
     * @param old the old service
     * @param replaceRef the replace service reference
     * @param replace the replace service
     */
    void accept(ServiceReference<S> oldRef, S old, ServiceReference<S> replaceRef, S replace);

    default InstanceCbRefServiceRefService<S> andThen(InstanceCbRefServiceRefService<S> after) {
        Objects.requireNonNull(after);
        return (ServiceReference<S> oldRef, S old, ServiceReference<S> replaceRef, S replace) -> {
            accept(oldRef, old, replaceRef, replace);
            after.accept(oldRef, old, replaceRef, replace);
        };
    }
}
