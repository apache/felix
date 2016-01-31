package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.osgi.framework.Bundle;

/**
 * Represents a callback(Bundle) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbBundle extends SerializableLambda {
    /**
     * Handles the given argument.
     * @param bundle the callback parameter
     */
    void accept(Bundle bundle);

    default CbBundle andThen(CbBundle after) {
        Objects.requireNonNull(after);
        return (Bundle bundle) -> {
            accept(bundle);
            after.accept(bundle);
        };
    }
}
