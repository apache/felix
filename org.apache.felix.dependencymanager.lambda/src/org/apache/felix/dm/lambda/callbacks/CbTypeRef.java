package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.osgi.framework.ServiceReference;

/**
 * Represents a callback(ServiceReference) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbTypeRef<T, S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param service first callback param
     */
    void accept(T instance, ServiceReference<S> service);

    default CbTypeRef<T, S> andThen(CbTypeRef<? super T, S> after) {
        Objects.requireNonNull(after);
        return (T instance, ServiceReference<S> ref) -> {
            accept(instance, ref);
            after.accept(instance, ref);
        };
    }
}
