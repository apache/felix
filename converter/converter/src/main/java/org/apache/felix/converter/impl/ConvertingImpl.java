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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.dto.DTO;
import org.osgi.util.converter.ConversionException;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converting;
import org.osgi.util.converter.TypeReference;

public class ConvertingImpl implements Converting, InternalConverting {
    private static final Map<Class<?>, Class<?>> interfaceImplementations;
    static {
        Map<Class<?>, Class<?>> m = new HashMap<>();
        m.put(Collection.class, ArrayList.class);
        m.put(List.class, ArrayList.class);
        m.put(Set.class, LinkedHashSet.class); // preserves insertion order
        m.put(Map.class, LinkedHashMap.class); // preserves insertion order
        interfaceImplementations = Collections.unmodifiableMap(m);
    }

    private volatile InternalConverter converter;
    private volatile Object object;
    private volatile Class<?> sourceAsClass;
    private volatile Object defaultValue;
    private volatile boolean hasDefault;
    private volatile Class<?> sourceClass;
    private volatile Class<?> targetActualClass;
    private volatile Class<?> targetAsClass;
    private volatile Type[] typeArguments;
    private List<Object> keys = new ArrayList<>();
    private volatile Object root;
    private volatile boolean sourceAsJavaBean = false;

    ConvertingImpl(InternalConverter c, Object obj) {
        converter = c;
        object = obj;
    }

    @Override
    public Converting sourceAs(Class<?> cls) {
        sourceAsClass = cls;
        return this;
    }

    @Override
    public Converting sourceAsBean() {
        sourceAsJavaBean = true;
        return this;
    }

    @Override
    public Converting sourceAsDTO() {
        // TODO Implement
        return this;
    }

    @Override
    public Converting targetAs(Class<?> cls) {
        targetAsClass = cls;
        return this;
    }

    @Override
    public Converting targetAsBean() {
        // TODO not yet implemented
        return this;
    }

    @Override
    public Converting targetAsDTO() {
        // TODO Implement
        return this;
    }

    @Override
    public Converting copy() {
        // TODO Implement this
        return null;
    }

    @Override
    public Converting defaultValue(Object defVal) {
        defaultValue = defVal;
        hasDefault = true;

        return this;
    }

    @Override
    public InternalConverting key(Object ... ks) {
        for (Object k : ks) {
            keys.add(k);
        }

        return this;
    }

    @Override
    public InternalConverting root(Object rootObject) {
        if (root == null)
            root = rootObject;
        return this;
    }

    @Override
    public void setConverter(Converter c) {
        if (c instanceof InternalConverter)
            converter = (InternalConverter) c;
        else
            throw new IllegalStateException("Incorrect converter used. Should implement " +
                InternalConverter.class + " but was " + c);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T to(Class<T> cls)  {
        Type type = cls;
        return (T) to(type);
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> T to(TypeReference<T> ref)  {
        return (T) to(ref.getType());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object to(Type type) {
        Class<?> cls = null;
        if (type instanceof Class) {
            cls = (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type rt = pt.getRawType();
            typeArguments = pt.getActualTypeArguments();
            if (rt instanceof Class)
                cls = (Class<?>) rt;
        }
        if (cls == null)
            return null;

        if (object == null)
            return handleNull(cls);

        targetActualClass = Util.primitiveToBoxed(cls);
        if (targetAsClass == null)
            targetAsClass = targetActualClass;

        sourceClass = sourceAsClass != null ? sourceAsClass : object.getClass();

        if (!isCopyRequiredType(targetAsClass) && targetAsClass.isAssignableFrom(sourceClass)) {
                return object;
        }

        Object res = trySpecialCases();
        if (res != null)
            return res;

        if (targetAsClass.isArray()) {
            return convertToArray();
        } else if (Collection.class.isAssignableFrom(targetAsClass)) {
            return convertToCollection();
        } else if (isDTOType(targetAsClass)) {
            return convertToDTO();
        } else if (isMapType(targetAsClass)) {
            return convertToMapType();
        }

        // At this point we know that the target is a 'singular' type: not a map, collection or array
        if (Collection.class.isAssignableFrom(sourceClass)) {
            return convertCollectionToSingleValue(cls);
        } else if ((object = asBoxedArray(object)) instanceof Object[]) {
            return convertArrayToSingleValue(cls);
        }

        Object res2 = tryStandardMethods();
        if (res2 != null) {
            return res2;
        } else {
            if (defaultValue != null)
                return converter.convert(defaultValue).sourceAs(sourceAsClass).targetAs(targetAsClass).to(targetActualClass);
            else
                return null;
        }
    }

    private Object convertArrayToSingleValue(Class<?> cls) {
        Object[] arr = (Object[]) object;
        if (arr.length == 0)
            return null;
        else
            return converter.convert(arr[0]).to(cls);
    }

    private Object convertCollectionToSingleValue(Class<?> cls) {
        Collection<?> coll = (Collection<?>) object;
        if (coll.size() == 0)
            return null;
        else
            return converter.convert(coll.iterator().next()).to(cls);
    }

    @SuppressWarnings("unchecked")
    private <T> T convertToArray() {
        Collection<?> collectionView = collectionView(object);
        Iterator<?> itertor = collectionView.iterator();
        try {
            Object array = Array.newInstance(targetAsClass.getComponentType(), collectionView.size());
            for (int i=0; i<collectionView.size() && itertor.hasNext(); i++) {
                Object next = itertor.next();
                Object converted = converter.convert(next).to(targetAsClass.getComponentType());
                Array.set(array, i, converted);
            }
            return (T) array;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> T convertToCollection() {
        Collection<?> cv = collectionView(object);
        Class<?> targetElementType = null;
        if (typeArguments != null && typeArguments.length > 0 && typeArguments[0] instanceof Class) {
            targetElementType = (Class<?>) typeArguments[0];
        }

        Class<?> ctrCls = interfaceImplementations.get(targetAsClass);
        Class<?>targetCls;
        if (ctrCls != null)
            targetCls = ctrCls;
        else
            targetCls = targetAsClass;

        Collection instance = (Collection) createMapOrCollection(targetCls, cv.size());
        if (instance == null)
            return null;

        for (Object o : cv) {
            if (targetElementType != null)
                o = converter.convert(o).to(targetElementType);

            instance.add(o);
        }

        return (T) instance;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> T convertToDTO() {
        Map m = mapView(object, sourceClass, converter);

        try {
            T dto = (T) targetActualClass.newInstance();

            for (Map.Entry entry : (Set<Map.Entry>) m.entrySet()) {
                Field f = null;
                try {
                    f = targetAsClass.getDeclaredField(mangleName(entry.getKey().toString()));
                } catch (NoSuchFieldException e) {
                    try {
                        f = targetAsClass.getField(mangleName(entry.getKey().toString()));
                    } catch (NoSuchFieldException e1) {
                        // There is not field with this name
                    }
                }

                if (f != null) {
                    Object val = entry.getValue();
                    f.set(dto, converter.convert(val).to(f.getType()));
                }
            }

            return dto;
        } catch (Exception e) {
            throw new ConversionException("Cannot create DTO " + targetActualClass, e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map convertToMap() {
        Map m = mapView(object, sourceClass, converter);
        if (m == null)
            return null;
        Type targetKeyType = null, targetValueType = null;
        if (typeArguments != null && typeArguments.length > 1) {
            targetKeyType = typeArguments[0];
            targetValueType = typeArguments[1];
        }

        Class<?> ctrCls = interfaceImplementations.get(targetActualClass);
        if (ctrCls == null)
            ctrCls = targetActualClass;

        Map instance = (Map) createMapOrCollection(ctrCls, m.size());
        if (instance == null)
            return null;

        for (Map.Entry entry : (Set<Entry>) m.entrySet()) {
            List<Object> ks = new ArrayList<>(keys);
            Object key = entry.getKey();
            if (targetKeyType != null)
                key = converter.convert(key).key(ks.toArray()).to(targetKeyType);
            ks.add(key);
            Object[] ka = ks.toArray();

            Object value = entry.getValue();
            if (value != null) {
                if (targetValueType != null) {
                    value = converter.convert(value).key(ka).to(targetValueType);
                } else {
                    Class<?> cls = value.getClass();
                    if (isCopyRequiredType(cls)) {
                        cls = getConstructableType(cls);
                    }
                    value = converter.convert(value).key(ka).to(cls);
                }
            }
            instance.put(key, value);
        }

        return instance;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object convertToMapType() {
        if (Map.class.isAssignableFrom(targetAsClass))
            return convertToMap();
        else if (Dictionary.class.isAssignableFrom(targetAsClass))
            return new Hashtable((Map) converter.convert(object).to(new ParameterizedType() {
                @Override
                public Type getRawType() {
                    return HashMap.class;
                }

                @Override
                public Type getOwnerType() {
                    return null;
                }

                @Override
                public Type[] getActualTypeArguments() {
                    return typeArguments;
                }
            }));
        else if (targetAsClass.isInterface())
            return createProxy(sourceClass, targetAsClass);
        return createJavaBean(sourceClass, targetAsClass);
    }

    private Object createJavaBean(Class<?> sourceCls, Class<?> targetCls) {
        @SuppressWarnings("rawtypes")
        Map m = mapView(object, sourceCls, converter);
        try {
            Object res = targetActualClass.newInstance();
            for (Method setter : getSetters(targetCls)) {
                String setterName = setter.getName();
                StringBuilder propName = new StringBuilder(Character.valueOf(Character.toLowerCase(setterName.charAt(3))).toString());
                if (setterName.length() > 4)
                    propName.append(setterName.substring(4));

                Class<?> setterType = setter.getParameterTypes()[0];
                setter.invoke(res, converter.convert(m.get(propName.toString())).to(setterType));
            }
            return res;
        } catch (Exception e) {
            throw new ConversionException("Cannot convert to class: " + targetCls.getName() +
                    ". Not a JavaBean with a Zero-arg Constructor.", e);
        }
    }

    @SuppressWarnings("rawtypes")
    private Object createProxy(Class<?> sourceCls, Class<?> targetCls) {
        Map m = mapView(object, sourceCls, converter);
        return Proxy.newProxyInstance(targetCls.getClassLoader(), new Class[] {targetCls},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String propName = getInterfacePropertyName(method);
                    if (propName == null)
                        return null;

                    Class<?> targetType = method.getReturnType();

                    Object val = m.get(propName);

                    // If no value is available take the default if specified
                    if (val == null) {
                        if (targetCls.isAnnotation()) {
                            val = method.getDefaultValue();
                        }

                        if (val == null && args != null && args.length == 1)
                            val = args[0];
                    }
                    return converter.convert(val).to(targetType);
                }
            });
    }

    private Object handleNull(Class<?> cls) {
        if (hasDefault)
            return converter.convert(defaultValue).to(cls);

        Class<?> boxed = Util.primitiveToBoxed(cls);
        if (boxed.equals(cls)) {
            // This is not a primitive, just return null
            return null;
        }
        if (cls.equals(boolean.class)) {
            return false;
        } else if (cls.equals(long.class) ) {
            return 0L;
        } else if (cls.equals(double.class) ) {
            return 0.0;
        } else {
            return 0;
        }
    }

    private static boolean isDTOType(Class<?> cls) {
        try {
            cls.getDeclaredConstructor();
        } catch (NoSuchMethodException | SecurityException e) {
            // No zero-arg constructor, not a DTO
            return false;
        }

        if (cls.getDeclaredMethods().length > 0) {
            // should not have any methods
            return false;
        }

        for (Method m : cls.getMethods()) {
            try {
                Object.class.getMethod(m.getName(), m.getParameterTypes());
            } catch (NoSuchMethodException snme) {
                // Not a method defined by Object.class (or override of such method)
                return false;
            }
        }

        for (Field f : cls.getDeclaredFields()) {
            int modifiers = f.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                // ignore static fields
                continue;
            }

            if (!Modifier.isPublic(modifiers)) {
                return false;
            }
        }

        for (Field f : cls.getFields()) {
            int modifiers = f.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                // ignore static fields
                continue;
            }

            if (!Modifier.isPublic(modifiers)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMapType(Class<?> cls) {
        // All interface types that are not Collections are treated as maps
        if (Map.class.isAssignableFrom(cls))
            return true;
        else if (cls.isInterface() && !Collection.class.isAssignableFrom(cls))
            return true;
        else if (isDTOType(cls))
            return true;
        else if (isWriteableJavaBean(cls))
            return true;
        else
            return Dictionary.class.isAssignableFrom(cls);
    }

    private Object trySpecialCases() {
        // TODO some of these can probably be implemented as an adapter

        if (Boolean.class.equals(targetAsClass)) {
            if (object instanceof Number) {
                return ((Number) object).longValue() != 0;
            } else if (object instanceof Collection && ((Collection<?>) object).size() == 0) {
                // TODO What about arrays?
                return Boolean.FALSE;
            }
        } else if (Character.class.equals(targetAsClass)) {
            if (object instanceof Number) {
                return Character.valueOf((char) ((Number) object).intValue());
            }
        } else if (Number.class.isAssignableFrom(targetAsClass)) {
            if (object instanceof Boolean) {
                return ((Boolean) object).booleanValue() ? 1 : 0;
            }
        } else if (Class.class.equals(targetAsClass)) {
            if (object instanceof Collection && ((Collection<?>) object).size() == 0) {
                return null;
            }
        } else if (Enum.class.isAssignableFrom(targetAsClass)) {
            if (object instanceof Boolean) {
                try {
                    Method m = targetAsClass.getMethod("valueOf", String.class);
                    return m.invoke(null, object.toString().toUpperCase());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (object instanceof Number) {
                try {
                    Method m = targetAsClass.getMethod("values");
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
    private <T> T tryStandardMethods() {
        try {
            Method m = targetActualClass.getDeclaredMethod("valueOf", String.class);
            if (m != null) {
                return (T) m.invoke(null, object.toString());
            }
        } catch (Exception e) {
            try {
                Constructor<?> ctr = targetActualClass.getConstructor(String.class);
                return (T) ctr.newInstance(object.toString());
            } catch (Exception e2) {
            }
        }
        return null;
    }

    private static Collection<?> collectionView(Object obj) {
        if (obj == null)
            return null;

        Collection<?> c = asCollection(obj);
        if (c == null)
            return Collections.singleton(obj);
        else
            return c;
    }

    private static Collection<?> asCollection(Object obj) {
        if (obj instanceof Collection)
            return (Collection<?>) obj;
        else if ((obj = asBoxedArray(obj)) instanceof Object[])
            return Arrays.asList((Object[]) obj);
        else
            return null;
    }

    private static Object asBoxedArray(Object obj) {
        Class<?> objClass = obj.getClass();
        if (!objClass.isArray())
            return obj;

        int len = Array.getLength(obj);
        Object arr = Array.newInstance(Util.primitiveToBoxed(objClass.getComponentType()), len);
        for (int i=0; i<len; i++) {
            Object val = Array.get(obj, i);
            Array.set(arr, i, val);
        }
        return arr;
    }

    @SuppressWarnings("rawtypes")
    private static Map createMapFromBeanAccessors(Object obj, Class<?> sourceCls) {
        Set<String> invokedMethods = new HashSet<>();

        Map result = new HashMap();
        for (Method md : sourceCls.getDeclaredMethods()) {
            handleBeanMethod(obj, md, invokedMethods, result);
        }
        for (Method md : sourceCls.getMethods()) {
            handleBeanMethod(obj, md, invokedMethods, result);
        }

        return result;
    }

    @SuppressWarnings("rawtypes")
    private Map createMapFromDTO(Object obj, InternalConverter converter) {
        Set<String> handledFields = new HashSet<>();

        Map result = new HashMap();
        for (Field f : obj.getClass().getDeclaredFields()) {
            handleField(obj, f, handledFields, result, converter);
        }
        for (Field f : obj.getClass().getFields()) {
            handleField(obj, f, handledFields, result, converter);
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    private static Map createMapFromInterface(Object obj) {
        Map result = new HashMap();
        for (Class i : obj.getClass().getInterfaces()) {
            for (Method md : i.getMethods()) {
                handleInterfaceMethod(obj, md, new HashSet<>(), result);
            }
            if (result.size() > 0)
                return result;
        }
        throw new ConversionException("Cannot be converted to map: " + obj);
    }

    private static Object createMapOrCollection(Class<?> cls, int initialSize) {
        try {
            Constructor<?> ctor = cls.getConstructor(int.class);
            return ctor.newInstance(initialSize);
        } catch (Exception e1) {
            try {
                Constructor<?> ctor2 = cls.getConstructor();
                return ctor2.newInstance();
            } catch (Exception e2) {
                // ignore
            }
        }
        return null;
    }

    private static Class<?> getConstructableType(Class<?> targetCls) {
        if (targetCls.isArray())
            return targetCls;

        Class<?> cls = targetCls;
        do {
            try {
                cls.getConstructor(int.class);
                return cls; // If no exception the constructor is there
            } catch (NoSuchMethodException e) {
                try {
                    cls.getConstructor();
                    return cls; // If no exception the constructor is there
                } catch (NoSuchMethodException e1) {
                    // There is no constructor with this name
                }
            }
            for (Class<?> intf : cls.getInterfaces()) {
                Class<?> impl = interfaceImplementations.get(intf);
                if (impl != null)
                    return impl;
            }

            cls = cls.getSuperclass();
        } while (!Object.class.equals(cls));

        return null;
    }

    private static String getAccessorPropertyName(Method md) {
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

    private static String getInterfacePropertyName(Method md) {
        if (md.getReturnType().equals(Void.class))
            return null; // not an accessor

        if (md.getParameterTypes().length > 1)
            return null; // not an accessor

        if (Object.class.equals(md.getDeclaringClass()))
            return null; // do not use any methods on the Object class as a accessor

        return md.getName().replace('_', '.'); // TODO support all the escaping mechanisms.
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void handleField(Object obj, Field field, Set<String> handledFields, Map result,
            InternalConverter converter) {
        if (Modifier.isStatic(field.getModifiers()))
            return;

        String fn = unMangleName(field.getName());
        if (handledFields.contains(fn))
            return; // Field with this name was already handled

        try {
            List<Object> ks = new ArrayList<>(keys);
            ks.add(fn);
            Object[] ka = ks.toArray();

            Object fVal = field.get(obj);
            if (isMapType(field.getType())) {
                fVal = converter.convert(fVal).key(ka).to(Map.class);
            }

            result.put(fn, fVal);
            handledFields.add(fn);
        } catch (Exception e) {
        }
    }

    private String mangleName(String key) {
        String res = key.replace("_", "__");
        res = res.replace("$", "$$");
        res = res.replaceAll("[.]([._])", "_\\$$1");
        res = res.replace('.', '_');
        // TODO handle Java keywords
        return res;
    }

    private String unMangleName(String key) {
        String res = key.replaceAll("_\\$", ".");
        res = res.replace("__", "\f"); // parkl double underscore as formfeed char
        res = res.replace('_', '.');
        res = res.replace("$$", "\b"); // park double dollar as backspace char
        res = res.replace("$", "");
        res = res.replace('\f', '_');  // convert formfeed char back to single underscore
        res = res.replace('\b', '$');  // convert backspace char back go dollar
        return res;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void handleBeanMethod(Object obj, Method md, Set<String> invokedMethods, Map res) {
        if (Modifier.isStatic(md.getModifiers()))
            return;

        String mn = md.getName();
        if (invokedMethods.contains(mn))
            return; // method with this name already invoked

        String propName = getAccessorPropertyName(md);
        if (propName == null)
            return;

        try {
            res.put(propName.toString(), md.invoke(obj));
            invokedMethods.add(mn);
        } catch (Exception e) {
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void handleInterfaceMethod(Object obj, Method md, Set<String> invokedMethods, Map res) {
        if (Modifier.isStatic(md.getModifiers()))
            return;

        if (md.getParameterCount() > 0)
            return;

        String mn = md.getName();
        if (invokedMethods.contains(mn))
            return; // method with this name already invoked

        String propName = getInterfacePropertyName(md);
        if (propName == null)
            return;

        try {
            res.put(propName.toString(), md.invoke(obj));
            invokedMethods.add(mn);
        } catch (Exception e) {
        }
    }

    private Map<?,?> mapView(Object obj, Class<?> sourceCls, InternalConverter converter) {
        if (Map.class.isAssignableFrom(sourceCls))
            return (Map<?,?>) obj;
        else if (Dictionary.class.isAssignableFrom(sourceCls))
            return null; // TODO
        else if (isDTOType(sourceCls))
            return createMapFromDTO(obj, converter);
        else {
            if (sourceAsJavaBean) {
                Map<?,?> m = createMapFromBeanAccessors(obj, sourceCls);
                if (m.size() > 0)
                    return m;
            }
        }
        return createMapFromInterface(obj);
    }

    private static boolean isCopyRequiredType(Class<?> cls) {
        return Map.class.isAssignableFrom(cls) ||
                Collection.class.isAssignableFrom(cls) ||
                DTO.class.isAssignableFrom(cls) ||
                // isJavaBean
                cls.isArray();
    }

    private static boolean isWriteableJavaBean(Class<?> cls) {
        boolean hasNoArgCtor = false;
        for (Constructor<?> ctor : cls.getConstructors()) {
            if (ctor.getParameterTypes().length == 0)
                hasNoArgCtor = true;
        }
        if (!hasNoArgCtor)
            return false; // A JavaBean must have a public no-arg constructor

        return getSetters(cls).size() > 0;
    }

    private static Set<Method> getSetters(Class<?> cls) {
        Set<Method> setters = new HashSet<>();
        while (!Object.class.equals(cls)) {
            Set<Method> methods = new HashSet<>();
            methods.addAll(Arrays.asList(cls.getDeclaredMethods()));
            methods.addAll(Arrays.asList(cls.getMethods()));
            for (Method md : methods) {
                if (md.getParameterTypes().length != 1)
                    continue; // Only setters with a single argument
                String name = md.getName();
                if (name.length() < 4)
                    continue;
                if (name.startsWith("set") &&
                        Character.isUpperCase(name.charAt(3)))
                    setters.add(md);
            }
            cls = cls.getSuperclass();
        }
        return setters;
    }

}
