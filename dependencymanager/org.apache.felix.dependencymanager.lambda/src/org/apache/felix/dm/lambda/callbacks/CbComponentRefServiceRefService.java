package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;
import org.osgi.framework.ServiceReference;

/**
 * Represents a callback(Component, ServiceReference, Service, ServiceReference, Service) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbComponentRefServiceRefService<S> {
    /**
     * Handles the given arguments
     * @param c a Component
     * @param oldRef an old swapped service reference
     * @param old an old swapped service
     * @param replaceRef the new service reference
     * @param replace the new service
     */
    void accept(Component c, ServiceReference<S> oldRef, S old, ServiceReference<S> replaceRef, S replace);

    default CbComponentRefServiceRefService<S> andThen(CbComponentRefServiceRefService<S> after) {
        Objects.requireNonNull(after);
        return (Component c, ServiceReference<S> oldRef, S old, ServiceReference<S> replaceRef, S replace) -> {
            accept(c, oldRef, old, replaceRef, replace);
            after.accept(c, oldRef, old, replaceRef, replace);
        };
    }
}
