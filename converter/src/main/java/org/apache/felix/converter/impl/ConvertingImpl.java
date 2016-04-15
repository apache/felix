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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

        if (String.class.equals(targetCls)) {
            if (object instanceof Object[]) {
                return (T) ((Object[])object)[0];
            } else if (object instanceof Collection) {
                Collection<?> c = (Collection<?>) object;
                if (c.size() == 0) {
                    return null;
                }
            }
            return (T) object.toString();
        } else if (String[].class.equals(targetCls)) {
            String[] sa = new String[1];
            sa[0] = object.toString();
            return (T) sa;
        }

        res = (T) tryStandardMethods(targetCls);
        if (res != null) {
            return res;
        } else {
            return null;
        }
    }

    private Object handleNull(Class<?> cls) {
        Class<?> boxed = boxedClasses.get(cls);
        if (boxed == null) {
            // This is not a primitive, just return null
            return null;
        }
        if (cls.equals(boolean.class)) {
            return false;
        } else if (cls.equals(Class.class)) {
            return null;
        } else if (Enum.class.isAssignableFrom(cls)) {
            return null;
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
