/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Provides a way for creating type-safe configurations from a {@link Map} or {@link Dictionary}.
 * <p>
 * This class takes a map or dictionary along with a class, the configuration-type, and returns a proxy that converts
 * method calls from the configuration-type to lookups in the map or dictionary. The results of these lookups are then
 * converted to the expected return type of the invoked configuration method.<br>
 * As proxies are returned, no implementations of the desired configuration-type are necessary!
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
 * dot notation using the keys. For example, a dictionary with <tt>"map" => "{key1.value1, key2.value2}"</tt> and a
 * dictionary with <tt>"map.key1" => "value1", "map2.key2" => "value2"</tt> result in the same map being returned.
 * Instead of a map, you could also define an interface with the methods <tt>getKey1()</tt> and <tt>getKey2</tt> and use
 * that interface as return type instead of a {@link Map}.
 * </p>
 * <p>
 * In case a lookup does not yield a value from the underlying map or dictionary, the following rules are applied:
 * <ol>
 * <li>primitive types yield their default value, as defined by the Java Specification;
 * <li>string, {@link Class}es and enum values yield <code>null</code>;
 * <li>for arrays, collections and maps, an empty array/collection/map is returned;
 * <li>for other interface types that are treated as configuration type a null-object is returned.
 * </ol>
 * </p>
 */
public final class Configurable {

	static class ConfigHandler implements InvocationHandler {
        private final ClassLoader m_cl;
        private final Map<?, ?> m_config;

        public ConfigHandler(ClassLoader cl, Map<?, ?> config) {
            m_cl = cl;
            m_config = config;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = getPropertyName(method.getName());

            Object result = convert(method.getGenericReturnType(), name, m_config.get(name), false /* useImplicitDefault */);
            if (result == null) {
                Object defaultValue = getDefaultValue(proxy, args, method, name);
                if (defaultValue != null) {
                    return defaultValue;
                }
            }

            return result;
        }

        @SuppressWarnings("unchecked")
        private Object convert(ParameterizedType type, String key, Object value) throws Exception {
            Class<?> resultType = (Class<?>) type.getRawType();
            if (Class.class.isAssignableFrom(resultType)) {
                if (value == null) {
                    return null;
                }
                return m_cl.loadClass(value.toString());
            }
            else if (Collection.class.isAssignableFrom(resultType)) {
                Collection<?> input = toCollection(key, value);

                if (resultType == Collection.class || resultType == List.class) {
                    resultType = ArrayList.class;
                }
                else if (resultType == Set.class || resultType == SortedSet.class) {
                    resultType = TreeSet.class;
                }
                else if (resultType == Queue.class) {
                    resultType = LinkedList.class;
                }
                else if (resultType.isInterface()) {
                    throw new RuntimeException("Unknown collection interface: " + resultType);
                }

                Collection<Object> result = (Collection<Object>) resultType.newInstance();
                if (input != null) {
                    Type componentType = type.getActualTypeArguments()[0];
                    for (Object i : input) {
                        result.add(convert(componentType, key, i, false /* useImplicitDefault */));
                    }
                }
                return result;
            }
            else if (Map.class.isAssignableFrom(resultType)) {
                Map<?, ?> input = toMap(key, value);

                if (resultType == SortedMap.class) {
                    resultType = TreeMap.class;
                }
                else if (resultType == Map.class) {
                    resultType = LinkedHashMap.class;
                }
                else if (resultType.isInterface()) {
                    throw new RuntimeException("Unknown map interface: " + resultType);
                }

                Map<Object, Object> result = (Map<Object, Object>) resultType.newInstance();
                Type keyType = type.getActualTypeArguments()[0];
                Type valueType = type.getActualTypeArguments()[1];

                for (Map.Entry<?, ?> entry : input.entrySet()) {
                    result.put(convert(keyType, key, entry.getKey(), false /* useImplicitDefault */), convert(valueType, key, entry.getValue(), false /* useImplicitDefault */));
                }
                return result;
            }

            throw new RuntimeException("Unhandled type: " + type);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private Object convert(Type type, String key, Object value, boolean useImplicitDefault) throws Exception {
            if (type instanceof ParameterizedType) {
                return convert((ParameterizedType) type, key, value);
            }
            if (type instanceof GenericArrayType) {
                return convertArray(((GenericArrayType) type).getGenericComponentType(), key, value);
            }
            Class<?> resultType = (Class<?>) type;
            if (resultType.isArray()) {
                return convertArray(resultType.getComponentType(), key, value);
            }
            if (resultType.isInstance(value)) {
                return value;
            }

            if (Boolean.class.equals(resultType) || Boolean.TYPE.equals(resultType)) {
                if (value == null) {
                    return useImplicitDefault && resultType.isPrimitive() ? DEFAULT_BOOLEAN : null;
                }
                return Boolean.valueOf(value.toString());
            }
            else if (Byte.class.equals(resultType) || Byte.TYPE.equals(resultType)) {
                if (value == null) {
                    return useImplicitDefault && resultType.isPrimitive() ? DEFAULT_BYTE : null;
                }
                if (value instanceof Number) {
                    return ((Number) value).byteValue();
                }
                return Byte.valueOf(value.toString());
            }
            else if (Short.class.equals(resultType) || Short.TYPE.equals(resultType)) {
                if (value == null) {
                    return useImplicitDefault && resultType.isPrimitive() ? DEFAULT_SHORT : null;
                }
                if (value instanceof Number) {
                    return ((Number) value).shortValue();
                }
                return Short.valueOf(value.toString());
            }
            else if (Integer.class.equals(resultType) || Integer.TYPE.equals(resultType)) {
                if (value == null) {
                    return useImplicitDefault && resultType.isPrimitive() ? DEFAULT_INT : null;
                }
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                return Integer.valueOf(value.toString());
            }
            else if (Long.class.equals(resultType) || Long.TYPE.equals(resultType)) {
                if (value == null) {
                    return useImplicitDefault && resultType.isPrimitive() ? DEFAULT_LONG : null;
                }
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
                return Long.valueOf(value.toString());
            }
            else if (Float.class.equals(resultType) || Float.TYPE.equals(resultType)) {
                if (value == null) {
                    return useImplicitDefault && resultType.isPrimitive() ? DEFAULT_FLOAT : null;
                }
                if (value instanceof Number) {
                    return ((Number) value).floatValue();
                }
                return Float.valueOf(value.toString());
            }
            else if (Double.class.equals(resultType) || Double.TYPE.equals(resultType)) {
                if (value == null) {
                    return useImplicitDefault && resultType.isPrimitive() ? DEFAULT_DOUBLE : null;
                }
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return Double.valueOf(value.toString());
            }
            else if (Number.class.equals(resultType)) {
                if (value == null) {
                    return null;
                }
                String numStr = value.toString();
                if (numStr.indexOf('.') > 0) {
                    return Double.valueOf(numStr);
                }
                return Long.valueOf(numStr);
            }
            else if (String.class.isAssignableFrom(resultType)) {
                return value == null ? null : value.toString();
            }
            else if (Enum.class.isAssignableFrom(resultType)) {
                if (value == null) {
                    return null;
                }
                Class<Enum> enumType = (Class<Enum>) resultType;
                return Enum.valueOf(enumType, value.toString().toUpperCase());
            }
            else if (resultType.isInterface()) {
                Map<?, ?> map = toMap(key, value);
                return create(resultType, map);
            }

            throw new RuntimeException("Unhandled type: " + type);
        }

        private Object convertArray(Type type, String key, Object value) throws Exception {
            if (value instanceof String) {
                String str = (String) value;
                if (type == Byte.class || type == byte.class) {
                    return str.getBytes("UTF-8");
                }
                if (type == Character.class || type == char.class) {
                    return str.toCharArray();
                }
            }

            Collection<?> input = toCollection(key, value);
            if (input == null) {
                return null;
            }

            Class<?> componentClass = getRawClass(type);
            Object array = Array.newInstance(componentClass, input.size());

            int i = 0;
            for (Object next : input) {
                Array.set(array, i++, convert(type, key, next, false /* useImplicitDefault */));
            }
            return array;
        }

        private Object getDefaultValue(Object proxy, Object[] args, Method method, String key) throws Throwable {
        	Object def = null;
        	// Handle cases where the method is part of an annotation or is a java8 default method.
        	Class<?> methodClass = method.getDeclaringClass();
        	if (methodClass.isAnnotation()) {
        		// the config type is an annotation: simply invoke the default value
        		def = method.getDefaultValue();
        	} else if (method.isDefault()) {
        		// The config type is a java8 interface with a default method, invoke it.
        		// But it's challenging to invoke a default method from a dynamic proxy ... we have to use the MethodHandles.
        		// see https://zeroturnaround.com/rebellabs/recognize-and-conquer-java-proxies-default-methods-and-method-handles
        		
                Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
                constructor.setAccessible(true);
                def = constructor.newInstance(methodClass, MethodHandles.Lookup.PRIVATE)
                		.unreflectSpecial(method, methodClass)
                		.bindTo(proxy)
                		.invokeWithArguments(args);
        	}
            return convert(method.getGenericReturnType(), key, def, true /* useImplicitDefault */);
        }

        private Class<?> getRawClass(Type type) {
            if (type instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) type).getRawType();
            }
            if (type instanceof Class) {
                return (Class<?>) type;
            }
            throw new RuntimeException("Unhandled type: " + type);
        }

        private Collection<?> toCollection(String prefix, Object value) {
            if (value instanceof Collection) {
                return (Collection<?>) value;
            }

            if (value == null) {
                List<Object> result = new ArrayList<>();

                String needle = prefix.concat(".");
                for (Map.Entry<?, ?> entry : m_config.entrySet()) {
                    String key = entry.getKey().toString();
                    if (!key.startsWith(needle)) {
                        continue;
                    }

                    int idx = 0;
                    try {
                        idx = Integer.parseInt(key.substring(needle.length()));
                    }
                    catch (NumberFormatException e) {
                        // Ignore
                    }

                    result.add(Math.min(result.size(), idx), entry.getValue());
                }

                return result;
            }

            if (value.getClass().isArray()) {
                if (value.getClass().getComponentType().isPrimitive()) {
                    int length = Array.getLength(value);
                    List<Object> result = new ArrayList<Object>(length);
                    for (int i = 0; i < length; i++) {
                        result.add(Array.get(value, i));
                    }
                    return result;
                }
                return Arrays.asList((Object[]) value);
            }

            if (value instanceof String) {
                String str = (String) value;
                if (str.startsWith("[") && str.endsWith("]")) {
                    str = str.substring(1, str.length() - 1);
                }
                return Arrays.asList(str.split("\\s*,\\s*"));
            }

            return Arrays.asList(value);
        }

        private Map<?, ?> toMap(String prefix, Object value) {
            if (value instanceof Map) {
                return (Map<?, ?>) value;
            }

            Map<String, Object> result = new HashMap<>();
            if (value == null) {
                String needle = prefix.concat(".");
                for (Map.Entry<?, ?> entry : m_config.entrySet()) {
                    String key = entry.getKey().toString();
                    if (key.startsWith(needle)) {
                        result.put(key.substring(needle.length()), entry.getValue());
                    }
                }
            }
            else if (value instanceof String) {
                String str = (String) value;
                if (str.startsWith("{") && str.endsWith("}")) {
                    str = str.substring(1, str.length() - 1);
                }
                for (String entry : str.split("\\s*,\\s*")) {
                    String[] pair = entry.split("\\s*\\.\\s*", 2);
                    result.put(pair[0], pair[1]);
                }
            }

            return result;
        }

        private String getPropertyName(String id) {
            StringBuilder sb = new StringBuilder(id);
            if (id.startsWith("get")) {
                sb.delete(0, 3);
            }
            else if (id.startsWith("is")) {
                sb.delete(0, 2);
            }
            char c = sb.charAt(0);
            if (Character.isUpperCase(c)) {
                sb.setCharAt(0, Character.toLowerCase(c));
            }
            return sb.toString();
        }
    }

    private static final Boolean DEFAULT_BOOLEAN = Boolean.FALSE;
    private static final Byte DEFAULT_BYTE = new Byte((byte) 0);
    private static final Short DEFAULT_SHORT = new Short((short) 0);
    private static final Integer DEFAULT_INT = new Integer(0);
    private static final Long DEFAULT_LONG = new Long(0);
    private static final Float DEFAULT_FLOAT = new Float(0.0f);
    private static final Double DEFAULT_DOUBLE = new Double(0.0);

    /**
     * Creates a configuration for a given type backed by a given dictionary.
     * 
     * @param type the configuration class, cannot be <code>null</code>;
     * @param config the configuration to wrap, cannot be <code>null</code>.
     * @return an instance of the given type that wraps the given configuration.
     */
    public static <T> T create(Class<T> type, Dictionary<?, ?> config) {
        Map<Object, Object> map = new HashMap<Object, Object>();
        for (Enumeration<?> e = config.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            map.put(key, config.get(key));
        }
        return create(type, map);
    }

    /**
     * Creates a configuration for a given type backed by a given map.
     * 
     * @param type the configuration class, cannot be <code>null</code>;
     * @param config the configuration to wrap, cannot be <code>null</code>.
     * @return an instance of the given type that wraps the given configuration.
     */
    public static <T> T create(Class<T> type, Map<?, ?> config) {
        ClassLoader cl = type.getClassLoader();
        Object result = Proxy.newProxyInstance(cl, new Class<?>[] { type }, new ConfigHandler(cl, config));
        return type.cast(result);
    }
}
