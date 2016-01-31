package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;
import org.osgi.framework.ServiceReference;

/**
 * Represents a callback(Component, ServiceReference, Service) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbComponentRefService<S> {
    /**
     * Handles the given arguments.
     * @param c a Component
     * @param ref the service reference
     * @param service the service
     */
    void accept(Component c, ServiceReference<S> ref, S service);

    default CbComponentRefService<S> andThen(CbComponentRefService<S> after) {
        Objects.requireNonNull(after);
        return (Component c, ServiceReference<S> ref, S service) -> {
            accept(c, ref, service);
            after.accept(c, ref, service);
        };
    }
}
