package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;
import org.osgi.framework.ServiceReference;

/**
 * Represents a callback(Component, ServiceReference) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbComponentRef<S> {
    /**
     * Handles the given arguments.
     * @param c a Component
     * @param ref the service reference
     */
    void accept(Component c, ServiceReference<S> ref);

    default CbComponentRef<S> andThen(CbComponentRef<S> after) {
        Objects.requireNonNull(after);
        return (Component c, ServiceReference<S> ref) -> {
            accept(c, ref);
            after.accept(c, ref);
        };
    }
}
