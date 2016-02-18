package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.osgi.framework.Bundle;

/**
 * Represents a callback(Bundle) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbBundle<T> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param bundle the callback parameter
     */
    void accept(T instance, Bundle bundle);

    default CbBundle<T> andThen(CbBundle<? super T> after) {
        Objects.requireNonNull(after);
        return (T instance, Bundle bundle) -> {
            accept(instance, bundle);
            after.accept(instance, bundle);
        };
    }
}
