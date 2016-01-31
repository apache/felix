package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.osgi.framework.ServiceReference;

/**
 * Represents a callback(ServiceReference) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbRef<S> {
    /**
     * Handles the given argument
     * @param ref a service reference
     */
    void accept(ServiceReference<S> ref);

    default CbRef<S> andThen(CbRef<S> after) {
        Objects.requireNonNull(after);
        return (ServiceReference<S> ref) -> {
            after.accept(ref);
        };
    }
}
