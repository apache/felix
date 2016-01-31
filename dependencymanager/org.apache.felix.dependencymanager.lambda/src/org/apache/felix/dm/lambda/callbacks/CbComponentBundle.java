package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;
import org.osgi.framework.Bundle;

/**
 * Represents a callback(Component, Bundle) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbComponentBundle {
    /**
     * Handles the given arguments.
     * @param component the callback parameter
     * @param bundle the callback parameter
     */
    void accept(Component component, Bundle bundle);

    default CbComponentBundle andThen(CbComponentBundle after) {
        Objects.requireNonNull(after);
        return (Component component, Bundle bundle) -> {
            accept(component, bundle);
            after.accept(component, bundle);
        };
    }
}
