package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.osgi.framework.ServiceReference;

/**
 * Represents a callback(ServiceReference, Service) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbTypeRefService<T, S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param ref first callback param
     * @param service second callback param
     */
    void accept(T instance, ServiceReference<S> ref, S service);

    default CbTypeRefService<T, S> andThen(CbTypeRefService<? super T, S> after) {
        Objects.requireNonNull(after);
        return (T instance, ServiceReference<S> ref, S service) -> {
            accept(instance, ref, service);
            after.accept(instance, ref, service);
        };
    }
}
