package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a callback(Configuration, Component) which accepts a configuration type for wrapping properties
 * behind a dynamic proxy interface. For more informations about configuration type, please refer to {@link CbConfiguration}.
 * 
 * <p> The T generic parameter represents the type of the class on which the callback is invoked on. 
 * <p> The U generic parameter represents the type of the configuration class passed to the callback argument. 
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
