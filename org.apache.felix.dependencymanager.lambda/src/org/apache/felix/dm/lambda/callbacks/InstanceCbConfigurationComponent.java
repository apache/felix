package org.apache.felix.dm.lambda.callbacks;

import java.util.Dictionary;
import java.util.Objects;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.lambda.ConfigurationDependencyBuilder;

/**
 * Represents a callback(Configuration, Component) invoked on an Object instance.
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
public interface InstanceCbConfigurationComponent<T> extends SerializableLambda {
    /**
     * Handles the given arguments
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param component the callback Component 
     */
    void accept(T configType, Component component);

    default InstanceCbConfigurationComponent<T> andThen(InstanceCbConfigurationComponent<T> after) {
        Objects.requireNonNull(after);
        return (T instance, Component component) -> {
            accept(instance, component);
            after.accept(instance, component);
        };
    }
}
