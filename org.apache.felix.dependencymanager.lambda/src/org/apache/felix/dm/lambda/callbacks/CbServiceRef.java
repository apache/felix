package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.osgi.framework.ServiceReference;

/**
 * Represents a callback(Service, ServiceReference) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbServiceRef<T, S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param service first callback arg
     * @param ref second callback arg
     */
    void accept(T instance, S service, ServiceReference<S> ref);

    default CbServiceRef<T, S> andThen(CbServiceRef<? super T, S> after) {
        Objects.requireNonNull(after);
        return (T instance, S service, ServiceReference<S> ref) -> {
            accept(instance, service, ref);
            after.accept(instance, service, ref);
        };
    }
}
