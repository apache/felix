package org.apache.felix.dm.lambda.callbacks;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a callback(Configuration) that is invoked on a Component implementation class. 
 * The callback which accepts a type-safe configuration class for wrapping properties behind a dynamic proxy interface.
 * 
 * <p> The T generic parameter represents the type of the class on which the callback is invoked on. 
 * <p> The U generic parameter represents the type of the configuration class passed to the callback argument. 
 * 
 * <p> Using such callback provides a way for creating type-safe configurations from a actual {@link Map} or {@link Dictionary} that is
 * normally injected by Dependency Manager.
 * The callback accepts in argument an interface that you have to provide, and DM will inject a proxy that converts
 * method calls from your configuration-type to lookups in the actual map or dictionary. The results of these lookups are then
 * converted to the expected return type of the invoked configuration method.<br>
 * As proxies are injected, no implementations of the desired configuration-type are necessary!
 * </p>
 * <p>
 * The lookups performed are based on the name of the method called on the configuration type. The method names are
 * "mangled" to the following form: <tt>[lower case letter] [any valid character]*</tt>. Method names starting with
 * <tt>get</tt> or <tt>is</tt> (JavaBean convention) are stripped from these prefixes. For example: given a dictionary
 * with the key <tt>"foo"</tt> can be accessed from a configuration-type using the following method names:
 * <tt>foo()</tt>, <tt>getFoo()</tt> and <tt>isFoo()</tt>.
 * </p>
 * <p>
 * The return values supported are: primitive types (or their object wrappers), strings, enums, arrays of
 * primitives/strings, {@link Collection} types, {@link Map} types, {@link Class}es and interfaces. When an interface is
 * returned, it is treated equally to a configuration type, that is, it is returned as a proxy.
 * </p>
 * <p>
 * Arrays can be represented either as comma-separated values, optionally enclosed in square brackets. For example:
 * <tt>[ a, b, c ]</tt> and <tt>a, b,c</tt> are both considered an array of length 3 with the values "a", "b" and "c".
 * Alternatively, you can append the array index to the key in the dictionary to obtain the same: a dictionary with
 * "arr.0" =&gt; "a", "arr.1" =&gt; "b", "arr.2" =&gt; "c" would result in the same array as the earlier examples.
 * </p>
 * <p>
 * Maps can be represented as single string values similarly as arrays, each value consisting of both the key and value
 * separated by a dot. Optionally, the value can be enclosed in curly brackets. Similar to array, you can use the same
 * dot notation using the keys. For example, a dictionary with 
 * 
 * <pre>{@code "map" => "{key1.value1, key2.value2}"}</pre> 
 * 
 * and a dictionary with <p>
 * 
 * <pre>{@code "map.key1" => "value1", "map2.key2" => "value2"}</pre> 
 * 
 * result in the same map being returned.
 * Instead of a map, you could also define an interface with the methods <tt>getKey1()</tt> and <tt>getKey2</tt> and use
 * that interface as return type instead of a {@link Map}.
 * 
 * <p>
 * In case a lookup does not yield a value from the underlying map or dictionary, the following rules are applied:
 * <ol>
 * <li>primitive types yield their default value, as defined by the Java Specification;
 * <li>string, {@link Class}es and enum values yield <code>null</code>;
 * <li>for arrays, collections and maps, an empty array/collection/map is returned;
 * <li>for other interface types that are treated as configuration type a null-object is returned.
 * </ol>
 * </p>
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
