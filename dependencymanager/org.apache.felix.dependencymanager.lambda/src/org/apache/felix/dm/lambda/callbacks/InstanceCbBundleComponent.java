package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;
import org.osgi.framework.Bundle;

/**
 * Represents a callback(Bundle, Component) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbBundleComponent {
    /**
     * Handles the given arguments.
     * @param component the callback parameter
     * @param bundle the callback parameter
     */
    void accept(Bundle bundle, Component component);

    default InstanceCbBundleComponent andThen(InstanceCbBundleComponent after) {
        Objects.requireNonNull(after);
        return (Bundle bundle, Component component) -> {
            accept(bundle, component);
            after.accept(bundle, component);
        };
    }
}
