package org.apache.felix.dm.lambda.callbacks;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a callback(Service, Map) on an Object instance.
 * 
 * <p> The type of the service passed in argument to the callback is defined by the "S" generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbServiceMap<S> {
    /**
     * Handles the given arguments.
     * @param service a Service 
     * @param properties a Map
     */
    void accept(S service, Map<String, Object> properties);

    default InstanceCbServiceMap<S> andThen(InstanceCbServiceMap<S> after) {
        Objects.requireNonNull(after);
        return (S service, Map<String, Object> properties) -> {
            accept(service, properties);
            after.accept(service, properties);
        };
    }
}
