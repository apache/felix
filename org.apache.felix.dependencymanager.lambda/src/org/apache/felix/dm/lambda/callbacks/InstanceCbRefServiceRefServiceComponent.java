package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;
import org.osgi.framework.ServiceReference;

/**
 * Represents a swap callback(ServiceReference, Service, ServiceReference, Service, Component) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbRefServiceRefServiceComponent<S> {
    /**
     * Handles the given arguments
     * @param oldRef an old swapped service reference
     * @param old an old swapped service
     * @param replaceRef the new service reference
     * @param replace the new service
     * @param c a Component
     */
    void accept(ServiceReference<S> oldRef, S old, ServiceReference<S> replaceRef, S replace, Component c);

    default InstanceCbRefServiceRefServiceComponent<S> andThen(InstanceCbRefServiceRefServiceComponent<S> after) {
        Objects.requireNonNull(after);
        return (ServiceReference<S> oldRef, S old, ServiceReference<S> replaceRef, S replace, Component c) -> {
            accept(oldRef, old, replaceRef, replace, c);
            after.accept(oldRef, old, replaceRef, replace, c);
        };
    }
}
