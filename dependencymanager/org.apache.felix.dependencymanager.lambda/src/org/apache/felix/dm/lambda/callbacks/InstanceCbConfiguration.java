package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

/**
 * Represents a reference to a callback on an Object instance that takes Configuration type as argument.
 * For more informations about configuration type, please refer to {@link CbConfiguration}.
 * 
 * <p> The T generic parameter represents the type of the configuration class passed to the callback argument. 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbConfiguration<T> extends SerializableLambda {
    /**
     * Handles the given argument.
     * @param configType the configuration type
     */
    void accept(T configType);

    default InstanceCbConfiguration<T> andThen(InstanceCbConfiguration<T> after) {
        Objects.requireNonNull(after);
        return (T configProxy) -> {
            accept(configProxy);
            after.accept(configProxy);
        };
    }
}
