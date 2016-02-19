package org.apache.felix.dm.lambda.callbacks;

import java.util.Dictionary;
import java.util.Objects;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.lambda.ConfigurationDependencyBuilder;

/**
 * Represents a callback(Configuration, Component) which accepts a configuration type for wrapping properties
 * behind a dynamic proxy interface.
 * 
 * <p> The T generic parameter represents the type of the class on which the callback is invoked on. 
 * <p> The U generic parameter represents the type of the configuration class passed to the callback argument. 
 * 
 * <p> Using such callback provides a way for creating type-safe configurations from the actual {@link Dictionary} that is
 * normally injected by Dependency Manager.
 * For more information about configuration types, please refer to {@link ConfigurationDependencyBuilder}.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbConfigurationComponent<T, U> extends SerializableLambda {
    /**
     * Handles the given arguments
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param configuration the configuration proxy 
     * @param component the callback Component 
     */
    void accept(T instance, U configuration, Component component);

    default CbConfigurationComponent<T, U> andThen(CbConfigurationComponent<T, U> after) {
        Objects.requireNonNull(after);
        return (T instance, U configuration, Component component) -> {
            accept(instance, configuration, component);
            after.accept(instance, configuration, component);
        };
    }
}
