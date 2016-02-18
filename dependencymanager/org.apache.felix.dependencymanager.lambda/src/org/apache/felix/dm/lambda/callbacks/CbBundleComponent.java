package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;
import org.osgi.framework.Bundle;

/**
 * Represents a callback(Bundle, Component) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbBundleComponent<T> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param bundle the first callback parameter
     * @param component the second callback parameter
     */
    void accept(T instance, Bundle bundle, Component component);

    default CbBundleComponent<T> andThen(CbBundleComponent<? super T> after) {
        Objects.requireNonNull(after);
        return (T instance, Bundle bundle, Component component) -> {
            accept(instance, bundle, component);
            after.accept(instance, bundle, component);
        };
    }
}
