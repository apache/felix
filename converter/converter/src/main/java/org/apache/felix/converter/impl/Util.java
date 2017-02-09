/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.converter.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

class Util {
    private static final Map<Class<?>, Class<?>> boxedClasses;
    static {
        Map<Class<?>, Class<?>> m = new HashMap<>();
        m.put(int.class, Integer.class);
        m.put(long.class, Long.class);
        m.put(double.class, Double.class);
        m.put(float.class, Float.class);
        m.put(boolean.class, Boolean.class);
        m.put(char.class, Character.class);
        m.put(byte.class, Byte.class);
        m.put(void.class, Void.class);
        m.put(short.class, Short.class);
        boxedClasses = Collections.unmodifiableMap(m);
    }

    private Util() {} // prevent instantiation

    static Type primitiveToBoxed(Type type) {
        if (type instanceof Class)
            return primitiveToBoxed((Class<?>) type);
        else
            return null;
    }

    static Class<?> primitiveToBoxed(Class<?> cls) {
        Class<?> boxed = boxedClasses.get(cls);
        if (boxed != null)
            return boxed;
        else
            return cls;
    }

    static Map<String, Method> getBeanKeys(Class<?> beanClass) {
        Map<String, Method> keys = new LinkedHashMap<>();
        for (Method md : beanClass.getDeclaredMethods()) {
            String key = getBeanKey(md);
            if (key != null && !keys.containsKey(key))
                keys.put(key, md);
        }
        return keys;
    }

    static String getBeanKey(Method md) {
        if (Modifier.isStatic(md.getModifiers()))
            return null;

        if (!Modifier.isPublic(md.getModifiers()))
            return null;

        return getBeanAccessorPropertyName(md);
    }

    private static String getBeanAccessorPropertyName(Method md) {
        if (md.getReturnType().equals(Void.class))
            return null; // not an accessor

        if (md.getParameterTypes().length > 0)
            return null; // not an accessor

        if (Object.class.equals(md.getDeclaringClass()))
            return null; // do not use any methods on the Object class as a accessor

        String mn = md.getName();
        int prefix;
        if (mn.startsWith("get"))
            prefix = 3;
        else if (mn.startsWith("is"))
            prefix = 2;
        else
            return null; // not an accessor prefix

        if (mn.length() <= prefix)
            return null; // just 'get' or 'is': not an accessor
        String propStr = mn.substring(prefix);
        StringBuilder propName = new StringBuilder(propStr.length());
        char firstChar = propStr.charAt(0);
        if (!Character.isUpperCase(firstChar))
            return null; // no acccessor as no camel casing
        propName.append(Character.toLowerCase(firstChar));
        if (propStr.length() > 1)
            propName.append(propStr.substring(1));

        return propName.toString();
    }


    static Map<String, Field> getDTOKeys(Class<?> dto) {
        Map<String, Field> keys = new LinkedHashMap<>();

        for (Field f : dto.getFields()) {
            String key = getDTOKey(f);
            if (key != null && !keys.containsKey(key))
                keys.put(key, f);
        }
        return keys;
    }

    static String getDTOKey(Field f) {
        if (Modifier.isStatic(f.getModifiers()))
            return null;

        if (!Modifier.isPublic(f.getModifiers()))
            return null;

        return unMangleName(f.getName());
    }

    static Map<String, Set<Method>> getInterfaceKeys(Class<?> intf) {
        Map<String, Set<Method>> keys = new LinkedHashMap<>();

        for (Method md : intf.getMethods()) {
            String name = getInterfacePropertyName(md);
            if (name != null) {
                Set<Method> set = keys.get(name);
                if (set == null) {
                    set = new LinkedHashSet<>();
                    keys.put(name, set);
                }
                set.add(md);
            }
        }
        return keys;
    }

    static String getInterfacePropertyName(Method md) {
        if (md.getReturnType().equals(Void.class))
            return null; // not an accessor

        if (md.getParameterTypes().length > 1)
            return null; // not an accessor

        if (Object.class.equals(md.getDeclaringClass()) ||
            Annotation.class.equals(md.getDeclaringClass()))
            return null; // do not use any methods on the Object or Annotation class as a accessor

        return md.getName().replace('_', '.'); // TODO support all the escaping mechanisms.
    }

    static Object getInterfaceProperty(Object obj, Method md) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (Modifier.isStatic(md.getModifiers()))
            return null;

        if (md.getParameterCount() > 0)
            return null;

        return md.invoke(obj);
    }

    static String mangleName(String key) {
        String res = key.replace("_", "__");
        res = res.replace("$", "$$");
        res = res.replaceAll("[.]([._])", "_\\$$1");
        res = res.replace('.', '_');
        // TODO handle Java keywords
        return res;
    }

    static String unMangleName(String key) {
        String res = key.replaceAll("_\\$", ".");
        res = res.replace("__", "\f"); // park double underscore as formfeed char
        res = res.replace('_', '.');
        res = res.replace("$$", "\b"); // park double dollar as backspace char
        res = res.replace("$", "");
        res = res.replace('\f', '_');  // convert formfeed char back to single underscore
        res = res.replace('\b', '$');  // convert backspace char back go dollar
        // TODO handle Java keywords
        return res;
    }
}
