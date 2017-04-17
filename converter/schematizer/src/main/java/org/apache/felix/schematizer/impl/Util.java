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
package org.apache.felix.schematizer.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.schematizer.AsDTO;
import org.apache.felix.schematizer.Node.CollectionType;
import org.osgi.util.converter.TypeReference;

public class Util {
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

    public static Type primitiveToBoxed(Type type) {
        if (type instanceof Class)
            return primitiveToBoxed((Class<?>) type);
        else
            return null;
    }

    public static Class<?> primitiveToBoxed(Class<?> cls) {
        Class<?> boxed = boxedClasses.get(cls);
        if (boxed != null)
            return boxed;
        else
            return cls;
    }

    public static byte [] readStream(InputStream is) throws IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] bytes = new byte[8192];

            int length = 0;
            int offset = 0;

            while ((length = is.read(bytes, offset, bytes.length - offset)) != -1) {
                offset += length;

                if (offset == bytes.length) {
                    baos.write(bytes, 0, bytes.length);
                    offset = 0;
                }
            }
            if (offset != 0) {
                baos.write(bytes, 0, offset);
            }
            return baos.toByteArray();
        } finally {
            is.close();
        }
    }

    public static Class<?> rawClassOf(Object type) {
        Class<?> rawClass = null;
        if (type instanceof Class) {
            rawClass = (Class<?>)type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Type rawType = paramType.getRawType();
            if (rawType instanceof Class)
                rawClass = (Class<?>)rawType;
        } else if (type instanceof TypeReference) {
            return rawClassOf(((TypeReference<?>)type).getType());
        }

        return rawClass;
    }

    public static TypeReference<?> typeReferenceOf(Object type) {
        TypeReference<?> typeRef = null;
        if (type instanceof TypeReference)
            typeRef = (TypeReference<?>)type;
        return typeRef;
    }

    public static boolean isDTOType(Class<?> cls) {
        try {
            cls.getDeclaredConstructor();
        } catch (NoSuchMethodException | SecurityException e) {
            // No zero-arg constructor, not a DTO
            return false;
        }

        // ATTENTION!! (Note from David Leangen)
        // This may not be according to spec, but without this, it is not possible
        // to use streams in the constructor, which I think is not intended.
        if (cls.getDeclaredMethods().length > 0) {
            return Arrays.stream(cls.getDeclaredMethods())
                    .map(m -> m.getName())
                    .allMatch(n -> n.startsWith( "lambda$"));
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

    public static boolean hasDTOAnnotation(Class<?> clazz) {
        AsDTO asDTOAnnotation = clazz.getAnnotation(AsDTO.class);
        return asDTOAnnotation != null;
    }

    public static boolean asDTO(Class<?> clazz) {
        return hasDTOAnnotation(clazz) || isDTOType(clazz);
    }

    public static boolean hasCollectionTypeAnnotation(Field field) {
        if (field == null)
            return false;

        Annotation[] annotations = field.getAnnotations();
        if (annotations.length == 0)
            return false;

        return Arrays.stream(annotations)
            .map(a -> a.annotationType().getName())
            .anyMatch(a -> "CollectionType".equals(a.substring(a.lastIndexOf(".") + 1) ));
    }

    public static Class<?> collectionTypeOf(Field field) {
        Annotation[] annotations = field.getAnnotations();

        Annotation annotation = Arrays.stream(annotations)
            .filter(a -> "CollectionType".equals(a.annotationType().getName().substring(a.annotationType().getName().lastIndexOf(".") + 1) ))
            .findFirst()
            .get();

        try {
            Method m = annotation.annotationType().getMethod("value");
            Class<?> value = (Class<?>)m.invoke(annotation, (Object[])null);
            return value;            
        } catch ( Exception e ) {
            return null;
        }
    }

    public static Class<?> getCollectionTypeOf(Field field) {
        Class<?> collectionType;
        CollectionType collectionTypeAnnotation = field.getAnnotation(CollectionType.class);
        if (collectionTypeAnnotation != null)
            collectionType = collectionTypeAnnotation.value();
        else if (hasCollectionTypeAnnotation(field))
            collectionType = collectionTypeOf(field);
        else
            collectionType = Object.class;

        return collectionType;
    }

    public static boolean isCollectionType(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz);        
    }

    public static Map<String, NodeImpl> extractChildren(String path, Map<String, NodeImpl> allNodes) {
        final Map<String, NodeImpl> children = new HashMap<>();
        for (String key : allNodes.keySet()) {
            String newKey = key.replace(path, "");
            if (!newKey.substring(1).contains("/"))
                children.put( newKey, allNodes.get(key));
        }

        return children;
    }
}
