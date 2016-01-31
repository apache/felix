package org.apache.felix.dm.lambda.callbacks;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a callback(Service, Map) on an Object instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbServiceMap<S> {
    /**
     * Handles the given arguments.
     * @param service a Service 
     * @param properties a Map
     */
    void accept(S service, Map<String, Object> properties);

    default CbServiceMap<S> andThen(CbServiceMap<S> after) {
        Objects.requireNonNull(after);
        return (S service, Map<String, Object> properties) -> {
            accept(service, properties);
            after.accept(service, properties);
        };
    }
}
