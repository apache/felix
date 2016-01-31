package org.apache.felix.dm.lambda.callbacks;

import java.util.Dictionary;
import java.util.Objects;

/**
 * Represents a callback(Service, Dictionary) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbServiceDict<S> {
    /**
     * Handles the given arguments.
     * @param service a Service
     * @param properties a Dictionary
     */
    void accept(S service, Dictionary<String, Object> properties);

    default CbServiceDict<S> andThen(CbServiceDict<S> after) {
        Objects.requireNonNull(after);
        return (S service, Dictionary<String, Object> properties) -> {
            accept(service, properties);
            after.accept(service, properties);
        };
    }
}
