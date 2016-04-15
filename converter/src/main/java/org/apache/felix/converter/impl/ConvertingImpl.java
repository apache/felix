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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.converter.Converter;
import org.osgi.service.converter.Converting;
import org.osgi.service.converter.TypeReference;

public class ConvertingImpl implements Converting {
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
    private static final Map<Class<?>, Class<?>> interfaceImplementations;
    static {
        Map<Class<?>, Class<?>> m = new HashMap<>();
        m.put(Collection.class, ArrayList.class);
        m.put(List.class, ArrayList.class);
        m.put(Set.class, LinkedHashSet.class); // preserves insertion order
        interfaceImplementations = Collections.unmodifiableMap(m);
    }

    private Converter converter;
    private final Object object;

    ConvertingImpl(Converter c, Object obj) {
        converter = c;
        object = obj;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T to(Class<T> cls) {
        Class<?> targetCls = cls;

        if (object == null)
            return (T) handleNull(cls);

        targetCls = primitiveToBoxed(targetCls);

        if (targetCls.isAssignableFrom(object.getClass()))
            return (T) object;

        T res = (T) trySpecialCases(targetCls);
        if (res != null)
            return res;

        if (targetCls.isArray()) {
            return convertToArray(targetCls);
        } else if (Collection.class.isAssignableFrom(targetCls)) {
            return convertToCollection(targetCls);
        }
        // TODO maps

        // At this point we know that the target is a 'singular' type: not a map, collection or array
        if (object instanceof Collection) {
            Collection<?> coll = (Collection<?>) object;
            if (coll.size() == 0)
                return null;
            else
                return converter.convert(coll.iterator().next()).to(cls);
        } else if (object instanceof Object[]) {
            Object[] arr = (Object[]) object;
            if (arr.length == 0)
                return null;
            else
                return converter.convert(arr[0]).to(cls);
        }

        T res2 = (T) tryStandardMethods(targetCls);
        if (res2 != null) {
            return res2;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T convertToArray(Class<?> targetClass) {
        Collection<?> collectionView = collectionView(object);
        Iterator<?> itertor = collectionView.iterator();
        try {
            Object array = Array.newInstance(targetClass.getComponentType(), collectionView.size());
            for (int i=0; i<collectionView.size() && itertor.hasNext(); i++) {
                Object next = itertor.next();
                Object converted = converter.convert(next).to(targetClass.getComponentType());
                Array.set(array, i, converted);
            }
            return (T) array;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> T convertToCollection(Class<?> targetCls) {
        Collection<?> collectionView = collectionView(object);

        Class<?> ctrCls = interfaceImplementations.get(targetCls);
        if (ctrCls != null)
            targetCls = ctrCls;

        Collection instance = createCollection(targetCls, collectionView);
        if (instance == null)
            return null;

        for (Object o : collectionView) {
            instance.add(o);
        }

        return (T) instance;
    }

    private static Collection<?> createCollection(Class<?> targetCls, Collection<?> collectionView) {
        try {
            Constructor<?> ctor = targetCls.getConstructor(int.class);
            return (Collection<?>) ctor.newInstance(collectionView.size());
        } catch (Exception e1) {
            try {
                Constructor<?> ctor2 = targetCls.getConstructor();
                return (Collection<?>) ctor2.newInstance();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return null;
    }

    private static Collection<?> collectionView(Object obj) {
        if (obj instanceof Collection)
            return (Collection<?>) obj;
        else if (obj instanceof Object[])
            return Arrays.asList((Object[]) obj);
        else
            return Collections.singleton(obj);
    }

    private Object handleNull(Class<?> cls) {
        Class<?> boxed = boxedClasses.get(cls);
        if (boxed == null) {
            // This is not a primitive, just return null
            return null;
        }
        if (cls.equals(boolean.class)) {
            return false;
        } else {
            return 0;
        }
    }

    private Class<?> primitiveToBoxed(Class<?> cls) {
        Class<?> boxed = boxedClasses.get(cls);
        if (boxed != null)
            return boxed;
        else
            return cls;
    }

    @Override
    public <T> T to(TypeReference<T> ref) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object to(Type type) {
        // TODO Auto-generated method stub
        return null;
    }

    private Object trySpecialCases(Class<?> targetCls) {
        if (Boolean.class.equals(targetCls)) {
            if (object instanceof Character) {
                return ((Character) object).charValue() != (char) 0;
            } else if (object instanceof Number) {
                return ((Number) object).longValue() != 0;
            } else if (object instanceof Collection && ((Collection<?>) object).size() == 0) {
                return Boolean.FALSE;
            }
        } else if (Character.class.equals(targetCls)) {
            if (object instanceof Boolean) {
                return ((Boolean) object).booleanValue() ? Character.valueOf((char) 1) : Character.valueOf((char) 0);
            }
        } else if (Integer.class.equals(targetCls)) {
            if (object instanceof Boolean) {
                return ((Boolean) object).booleanValue() ? Integer.valueOf(1) : Integer.valueOf(0);
            }
        } else if (Class.class.equals(targetCls)) {
            if (object instanceof Collection && ((Collection<?>) object).size() == 0) {
                return null;
            } else {
                try {
                    return getClass().getClassLoader().loadClass(converter.convert(object).toString());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (Enum.class.isAssignableFrom(targetCls)) {
            if (object instanceof Boolean) {
                try {
                    Method m = targetCls.getMethod("valueOf", String.class);
                    return m.invoke(null, object.toString().toUpperCase());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (object instanceof Number) {
                try {
                    Method m = targetCls.getMethod("values");
                    Object[] values = (Object[]) m.invoke(null);
                    return values[((Number) object).intValue()];
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T tryStandardMethods(Class<T> cls) {
        try {
            Method m = cls.getDeclaredMethod("valueOf", String.class);
            if (m != null) {
                return (T) m.invoke(null, object.toString());
            }
        } catch (Exception e) {
            try {
                Constructor<T> ctr = cls.getConstructor(String.class);
                return ctr.newInstance(object.toString());
            } catch (Exception e2) {
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return to(String.class);
    }
}
