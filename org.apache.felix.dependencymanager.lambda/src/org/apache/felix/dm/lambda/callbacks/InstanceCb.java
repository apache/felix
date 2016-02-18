package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

/**
 * Represents a method reference to a no-args callback method from an arbitrary Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCb {
    /**
     * Implements the callback method.
     */
    void cb();

    default InstanceCb andThen(InstanceCb after) {
        Objects.requireNonNull(after);
        return () -> {
            cb();
            after.cb();
        };
    }
}
