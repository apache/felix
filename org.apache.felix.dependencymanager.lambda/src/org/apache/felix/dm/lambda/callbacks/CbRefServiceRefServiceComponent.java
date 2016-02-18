package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;
import org.osgi.framework.ServiceReference;

/**
 * Represents a callback(ServiceReference, Service, ServiceReference, Service, Component) that is invoked on a Component implementation class. 
 * The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbRefServiceRefServiceComponent<T, S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation instance on which the callback is invoked on. 
     * @param oldRef first callback arg
     * @param old second callback arg
     * @param replaceRef third callback arg
     * @param replace fourth callback arg
     * @param c fifth callback arg
     */
    void accept(T instance, ServiceReference<S> oldRef, S old, ServiceReference<S> replaceRef, S replace, Component c);

    default CbRefServiceRefServiceComponent<T, S> andThen(CbRefServiceRefServiceComponent<? super T, S> after) {
        Objects.requireNonNull(after);
        return (T instance, ServiceReference<S> oldRef, S old, ServiceReference<S> replaceRef, S replace, Component c) -> {
            accept(instance, oldRef, old, replaceRef, replace, c);
            after.accept(instance, oldRef, old, replaceRef, replace, c);
        };
    }
}
