package org.apache.felix.dm.lambda.callbacks;

import java.util.Dictionary;
import java.util.Objects;

import org.apache.felix.dm.lambda.ConfigurationDependencyBuilder;

/**
 * Represents a callback(Configuration) that is invoked on a Component implementation class. 
 * The callback accepts a type-safe configuration class for wrapping properties behind a dynamic proxy interface.
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
public interface CbConfiguration<T, U> extends SerializableLambda {
    /**
     * Handles the given arguments
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param configuration the configuration proxy 
     */
    void accept(T instance, U configuration);

    default CbConfiguration<T, U> andThen(CbConfiguration<T, U> after) {
        Objects.requireNonNull(after);
        return (T instance, U configuration) -> {
            accept(instance, configuration);
            after.accept(instance, configuration);
        };
    }
}
