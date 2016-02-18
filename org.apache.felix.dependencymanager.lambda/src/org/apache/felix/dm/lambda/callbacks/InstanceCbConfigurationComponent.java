package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a callback(Configuration, Component) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * For more informations about configuration type, please refer to {@link CbConfiguration}.
 * 
 * <p> The T generic parameter represents the type of the configuration class passed to the callback argument. 
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
    void accept(T instance, Component component);

    default InstanceCbConfigurationComponent<T> andThen(InstanceCbConfigurationComponent<T> after) {
        Objects.requireNonNull(after);
        return (T instance, Component component) -> {
            accept(instance, component);
            after.accept(instance, component);
        };
    }
}
