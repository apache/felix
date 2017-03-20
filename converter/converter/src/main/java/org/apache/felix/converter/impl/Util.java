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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
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

        return unMangleName(getPrefix(f.getDeclaringClass()), f.getName());
    }

    static Map<String, Set<Method>> getInterfaceKeys(Class<?> intf, Object object) {
        Map<String, Set<Method>> keys = new LinkedHashMap<>();

        String seank = getSingleElementAnnotationKey(intf, object);
        for (Method md : intf.getMethods()) {
            String name = getInterfacePropertyName(md, seank, object);
            if (name != null) {
                Set<Method> set = keys.get(name);
                if (set == null) {
                    set = new LinkedHashSet<>();
                    keys.put(name, set);
                }
                set.add(md);
            }
        }

        for (Iterator<Entry<String, Set<Method>>> it = keys.entrySet().iterator(); it.hasNext(); ) {
            Entry<String, Set<Method>> entry = it.next();
            boolean zeroArgFound = false;
            for (Method md : entry.getValue()) {
                if (md.getParameterCount() == 0) {
                    // OK found the zero-arg param
                    zeroArgFound = true;
                    break;
                }
            }
            if (!zeroArgFound)
                it.remove();
        }
        return keys;
    }

    static String getSingleElementAnnotationKey(Class<?> intf, Object obj) {
        Class<?> ann = getAnnotationType(intf, obj);
        if (ann == null)
            return null;

        boolean valueFound = false;
        for (Method md : ann.getDeclaredMethods()) {
            if ("value".equals(md.getName())) {
                valueFound = true;
                continue;
            }

            if (md.getDefaultValue() == null) {
                // All elements bar value must have a default
                return null;
            }
        }

        if (!valueFound) {
            // Single Element Annotation must have a value element.
            return null;
        }

        return toSingleElementAnnotationKey(ann.getSimpleName());
    }

    private static Class<?> getAnnotationType(Class<?> intf, Object obj) {
        try {
            Method md = intf.getMethod("annotationType");
            Object res = md.invoke(obj);
            if (res instanceof Class)
                return (Class<?>) res;
        } catch (Exception e) {
        }
        return null;
    }

    private static String toSingleElementAnnotationKey(String simpleName) {
        StringBuilder sb = new StringBuilder();

        boolean capitalSeen = true;
        for (char c : simpleName.toCharArray()) {
            if (!capitalSeen) {
                if (Character.isUpperCase(c)) {
                    capitalSeen = true;
                    sb.append('.');
                }
            } else {
                if (Character.isLowerCase(c)) {
                    capitalSeen = false;
                }
            }
            sb.append(Character.toLowerCase(c));
        }

        return sb.toString();
    }

    static String getInterfacePropertyName(Method md, String singleElementAnnotationKey, Object object) {
        if (md.getReturnType().equals(Void.class))
            return null; // not an accessor

        if (md.getParameterTypes().length > 1)
            return null; // not an accessor

        if ("value".equals(md.getName()) && md.getParameterTypes().length == 0 && singleElementAnnotationKey != null)
            return singleElementAnnotationKey;

        if (Object.class.equals(md.getDeclaringClass()) ||
            Annotation.class.equals(md.getDeclaringClass()))
            return null; // do not use any methods on the Object or Annotation class as a accessor

        if ("annotationType".equals(md.getName())) {
            try {
                Object cls = md.invoke(object);
                if (cls instanceof Class && ((Class<?>) cls).isAnnotation())
                    return null;
            } catch (Exception e) {
            }
        }

        if (md.getDeclaringClass().getSimpleName().startsWith("$Proxy")) {
            // TODO is there a better way to do this?
            if (isInheritedMethodInProxy(md, Object.class) ||
                    isInheritedMethodInProxy(md, Annotation.class))
                return null;
        }

        return unMangleName(getPrefix(md.getDeclaringClass()), md.getName());
    }

    private static boolean isInheritedMethodInProxy(Method md, Class<?> cls) {
        for (Method om : cls.getMethods()) {
            if (om.getName().equals(md.getName()) &&
                    Arrays.equals(om.getParameterTypes(), md.getParameterTypes())) {
                return true;
            }
        }
        return false;
    }

    static Object getInterfaceProperty(Object obj, Method md) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (Modifier.isStatic(md.getModifiers()))
            return null;

        if (md.getParameterCount() > 0)
            return null;

        return md.invoke(obj);
    }

    static String getPrefix(Class<?> cls) {
        try {
            Field prefixField = cls.getDeclaredField("PREFIX_");
            if (prefixField.getType().equals(String.class)) {
                if ((prefixField.getModifiers() & (Modifier.PUBLIC | Modifier.FINAL | Modifier.STATIC)) > 0) {
                    return (String) prefixField.get(null);
                }
            }
        } catch (Exception ex) {
            // LOG no prefix field
        }

        if (!cls.isInterface()) {
            for (Class<?> intf : cls.getInterfaces()) {
                String prefix = getPrefix(intf);
                if (prefix.length() > 0)
                    return prefix;
            }
        }

        return "";
    }

    static String mangleName(String prefix, String key) {
        if (!key.startsWith(prefix))
            return null;

        key = key.substring(prefix.length());

        String res = key.replace("_", "__");
        res = res.replace("$", "$$");
        res = res.replaceAll("[.]([._])", "_\\$$1");
        res = res.replace('.', '_');
        // TODO handle Java keywords
        return res;
    }

    static String unMangleName(String prefix, String key) {
        String res = key.replaceAll("_\\$", ".");
        res = res.replace("__", "\f"); // park double underscore as formfeed char
        res = res.replace('_', '.');
        res = res.replace("$$", "\b"); // park double dollar as backspace char
        res = res.replace("$", "");
        res = res.replace('\f', '_');  // convert formfeed char back to single underscore
        res = res.replace('\b', '$');  // convert backspace char back go dollar
        // TODO handle Java keywords
        return prefix + res;
    }
}
