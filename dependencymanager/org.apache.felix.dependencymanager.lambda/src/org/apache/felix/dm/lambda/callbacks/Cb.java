package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

/**
 * Represents a java8 method reference to a zero-argument method from a given component implementation class. 
 * <p> The type of the class on which the callback is invoked on is represented by the T generic parameter.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface Cb<T> extends SerializableLambda {
    /**
     * Invokes the callback method on the given component implementation instance.
     * @param t the component implementation instance the callback is invoked on.
     */
    void accept(T t);

    default Cb<T> andThen(Cb<? super T> after) {
        Objects.requireNonNull(after);
        return (T t) -> {
            accept(t);
            after.accept(t);
        };
    }
}