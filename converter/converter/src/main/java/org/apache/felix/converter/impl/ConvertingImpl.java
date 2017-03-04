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

    volatile InternalConverter converter;
    private volatile Object object;
    private volatile Object defaultValue;
    private volatile boolean hasDefault;
    volatile Class<?> sourceClass;
    volatile Class<?> sourceAsClass;
    private volatile Class<?> targetClass;
    private volatile Class<?> targetAsClass;
    volatile Type[] typeArguments;
    private volatile boolean forceCopy = false;
    private volatile boolean sourceAsJavaBean = false;
    @SuppressWarnings( "unused" )
    private volatile boolean targetAsJavaBean = false;
    private volatile boolean sourceAsDTO = false;
    private volatile boolean targetAsDTO = false;

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
        // To avoid ambiguity, reset any instruction to sourceAsDTO
        sourceAsDTO = false;
        sourceAsJavaBean = true;
        return this;
    }

    @Override
    public Converting sourceAsDTO() {
        // To avoid ambiguity, reset any instruction to sourceAsJavaBean
        sourceAsJavaBean = false;
        sourceAsDTO = true;
        return this;
    }

    @Override
    public Converting targetAs(Class<?> cls) {
        targetAsClass = cls;
        return this;
    }

    @Override
    public Converting targetAsBean() {
        // To avoid ambiguity, reset any instruction to targetAsDTO
        targetAsDTO = false;
        targetAsJavaBean = true;
        return this;
    }

    @Override
    public Converting targetAsDTO() {
        // To avoid ambiguity, reset any instruction to targetAsJavaBean
        targetAsJavaBean = false;
        targetAsDTO = true;
        return this;
    }

    @Override
    public Converting copy() {
        forceCopy  = true;

        return null;
    }

    @Override
    public Converting defaultValue(Object defVal) {
        defaultValue = defVal;
        hasDefault = true;

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

        targetClass = Util.primitiveToBoxed(cls);
        if (targetAsClass == null)
            targetAsClass = targetClass;

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
        } else if (isDTOType(targetAsClass) || ((sourceAsDTO || targetAsDTO) && DTO.class.isAssignableFrom(targetClass))) {
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
                return converter.convert(defaultValue).sourceAs(sourceAsClass).targetAs(targetAsClass).to(targetClass);
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

        Class<?> cls = targetAsClass;
        if (targetAsDTO)
            cls = targetClass;
        try {
            T dto = (T) targetClass.newInstance();

            for (Map.Entry entry : (Set<Map.Entry>) m.entrySet()) {
                Field f = null;
                try {
                    f = cls.getDeclaredField(Util.mangleName(entry.getKey().toString()));
                } catch (NoSuchFieldException e) {
                    try {
                        f = cls.getField(Util.mangleName(entry.getKey().toString()));
                    } catch (NoSuchFieldException e1) {
                        // There is not field with this name
                    }
                }

                if (f != null) {
                    Object val = entry.getValue();
                    if (sourceAsDTO && DTO.class.isAssignableFrom(f.getType()))
                        val = converter.convert(val).sourceAsDTO().to(f.getType());
                    else
                        val = converter.convert(val).to(f.getType());
                    f.set(dto, val);
                }
            }

            return dto;
        } catch (Exception e) {
            throw new ConversionException("Cannot create DTO " + targetClass, e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map convertToMap() {
        Map m = mapView(object, sourceClass, converter);
        if (m == null)
            return null;

        Class<?> ctrCls = interfaceImplementations.get(targetClass);
        if (ctrCls == null)
            ctrCls = targetClass;

        Map instance = (Map) createMapOrCollection(ctrCls, m.size());
        if (instance == null)
            return null;

        for (Map.Entry entry : (Set<Entry>) m.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            value = convertMapValue(value);
            instance.put(key, value);
        }

        return instance;
    }

    Object convertMapValue(Object value) {
        Type targetValueType = null;
        if (typeArguments != null && typeArguments.length > 1) {
            targetValueType = typeArguments[1];
        }

        if (value != null) {
            if (targetValueType != null) {
                value = converter.convert(value).to(targetValueType);
            } else {
                Class<?> cls = value.getClass();
                if (isCopyRequiredType(cls)) {
                    cls = getConstructableType(cls);
                }
                if (sourceAsDTO && DTO.class.isAssignableFrom(cls))
                    value = converter.convert(value).sourceAsDTO().to(cls);
                else
                    value = converter.convert(value).to(cls);
            }
        }
        return value;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Map convertToMapDelegate() {
        if (Map.class.isAssignableFrom(sourceClass)) {
            return MapDelegate.forMap((Map) object, this);
        } else if (Dictionary.class.isAssignableFrom(sourceClass)) {
            return MapDelegate.forDictionary((Dictionary) object, this);
        } else if (isDTOType(sourceClass) || sourceAsDTO) {
            return MapDelegate.forDTO(object, this);
        } else if (sourceAsJavaBean) {
            return MapDelegate.forBean(object, this);
        }

        // Assume it's an interface
        return MapDelegate.forInterface(object, this);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object convertToMapType() {
        if (Map.class.equals(targetClass) && !forceCopy) {
            Map res = convertToMapDelegate();
            if (res != null)
                return res;
        }

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
            Object res = targetClass.newInstance();
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
                    String propName = Util.getInterfacePropertyName(method);
                    if (propName == null)
                        return null;

                    Class<?> targetType = method.getReturnType();

                    Object val = m.get(propName);

                    // If no value is available take the default if specified
                    boolean defaultUsed = false; // TODO maybe we don't need this...
                    if (val == null) {
                        if (targetCls.isAnnotation()) {
                            val = method.getDefaultValue();
                            defaultUsed = true;
                        }

                        if (val == null && args != null && args.length == 1) {
                            val = args[0];
                            defaultUsed = true;
                        }
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
            if (object instanceof Number) {
                try {
                    Method m = targetAsClass.getMethod("values");
                    Object[] values = (Object[]) m.invoke(null);
                    return values[((Number) object).intValue()];
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    Method m = targetAsClass.getMethod("valueOf", String.class);
                    return m.invoke(null, object.toString());
                } catch (Exception e) {
                    try {
                        // Case insensitive fallback
                        Method m = targetAsClass.getMethod("values");
                        for (Object v : (Object[]) m.invoke(null)) {
                            if (v.toString().equalsIgnoreCase(object.toString())) {
                                return v;
                            }
                        }
                    } catch (Exception e1) {
                        throw new RuntimeException(e1);
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T tryStandardMethods() {
        try {
            Method m = targetClass.getDeclaredMethod("valueOf", String.class);
            if (m != null) {
                return (T) m.invoke(null, object.toString());
            }
        } catch (Exception e) {
            try {
                Constructor<?> ctr = targetClass.getConstructor(String.class);
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

        return result;
    }

    @SuppressWarnings("rawtypes")
    private Map createMapFromDTO(Object obj, InternalConverter converter) {
        Set<String> handledFields = new HashSet<>();

        Map result = new HashMap();
        // Do we need 'declaredfields'? We only need to look at the public ones...
        for (Field f : obj.getClass().getDeclaredFields()) {
            handleDTOField(obj, f, handledFields, result, converter);
        }
        for (Field f : obj.getClass().getFields()) {
            handleDTOField(obj, f, handledFields, result, converter);
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void handleDTOField(Object obj, Field field, Set<String> handledFields, Map result,
            InternalConverter converter) {
        String fn = Util.getDTOKey(field);
        if (fn == null)
            return;

        if (handledFields.contains(fn))
            return; // Field with this name was already handled

        try {
            Object fVal = field.get(obj);
            result.put(fn, fVal);
            handledFields.add(fn);
        } catch (Exception e) {
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void handleBeanMethod(Object obj, Method md, Set<String> invokedMethods, Map res) {
        String bp = Util.getBeanKey(md);
        if (bp == null)
            return;

        if (invokedMethods.contains(bp))
            return; // method with this name already invoked

        try {
            res.put(bp, md.invoke(obj));
            invokedMethods.add(bp);
        } catch (Exception e) {
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void handleInterfaceMethod(Object obj, Method md, Set<String> invokedMethods, Map res) {
        String mn = md.getName();
        if (invokedMethods.contains(mn))
            return; // method with this name already invoked

        String propName = Util.getInterfacePropertyName(md);
        if (propName == null)
            return;

        try {
            Object r = Util.getInterfaceProperty(obj, md);
            if (r == null)
                return;

            res.put(propName, r);
            invokedMethods.add(mn);
        } catch (Exception e) {
        }
    }

    private Map<?,?> mapView(Object obj, Class<?> sourceCls, InternalConverter converter) {
        if (Map.class.isAssignableFrom(sourceCls) || (DTO.class.isAssignableFrom(sourceCls) && obj instanceof Map))
            return (Map<?,?>) obj;
        else if (Dictionary.class.isAssignableFrom(sourceCls))
            return null; // TODO
        else if (isDTOType(sourceCls) || sourceAsDTO)
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
        if (cls.isEnum())
            return false;
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
