package org.apache.felix.dm.lambda.callbacks;

import java.util.Dictionary;
import java.util.Objects;

import org.apache.felix.dm.lambda.ConfigurationDependencyBuilder;

/**
 * Represents a reference to a callback on an Object instance that takes Configuration type as argument.
 * 
 * <p> The T generic parameter represents the type of the configuration class passed to the callback argument. 
 * 
 * <p> Using such callback provides a way for creating type-safe configurations from the actual {@link Dictionary} that is
 * normally injected by Dependency Manager.
 * For more information about configuration types, please refer to {@link ConfigurationDependencyBuilder}.
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
