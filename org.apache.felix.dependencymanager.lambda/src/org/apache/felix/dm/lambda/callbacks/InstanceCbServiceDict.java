package org.apache.felix.dm.lambda.callbacks;

import java.util.Dictionary;
import java.util.Objects;

/**
 * Represents a callback(Service, Dictionary) on an Object instance.
 * 
 * <p> The type of the service passed in argument to the callback is defined by the "S" generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbServiceDict<S> {
    /**
     * Handles the given arguments.
     * @param service a Service
     * @param properties a Dictionary
     */
    void accept(S service, Dictionary<String, Object> properties);

    default InstanceCbServiceDict<S> andThen(InstanceCbServiceDict<S> after) {
        Objects.requireNonNull(after);
        return (S service, Dictionary<String, Object> properties) -> {
            accept(service, properties);
            after.accept(service, properties);
        };
    }
}
