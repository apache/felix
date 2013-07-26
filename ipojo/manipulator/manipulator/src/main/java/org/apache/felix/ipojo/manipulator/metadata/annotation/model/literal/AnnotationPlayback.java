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

package org.apache.felix.ipojo.manipulator.metadata.annotation.model.literal;

import static java.lang.String.format;
import static org.objectweb.asm.Type.getType;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.manipulator.metadata.annotation.model.AnnotationDiscovery;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.Playback;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * User: guillaume
 * Date: 08/07/13
 * Time: 17:15
 */
public class AnnotationPlayback implements Playback {

    public static final List<? extends Class<? extends Serializable>> BOXED_TYPES = Arrays.asList(Byte.class, Long.class, Character.class, Boolean.class, Double.class, Float.class, Integer.class, Short.class);
    private final Annotation m_annotation;
    private final Type m_annotationType;

    public AnnotationPlayback(final Annotation annotation) {
        m_annotation = annotation;
        m_annotationType = Type.getType(annotation.annotationType());
    }

    private Map<String, Object> getValues() {
        Map<String, Object> values = new HashMap<String, Object>();
        for (Method method : m_annotation.annotationType().getDeclaredMethods()) {
            try {
                values.put(method.getName(), method.invoke(m_annotation));
            } catch (Throwable t) {
                throw new IllegalStateException(
                        format("Cannot get value of the %s.%s attribute",
                               m_annotation.annotationType().getSimpleName(),
                               method.getName()),
                        t
                );
            }
        }
        return values;
    }
    public void accept(final FieldVisitor visitor) {
        AnnotationVisitor av = visitor.visitAnnotation(m_annotationType.getDescriptor(),
                                                       true);
        if (av != null) {
            accept(av);
        }
    }

    public void accept(final ClassVisitor visitor) {
        AnnotationVisitor av = visitor.visitAnnotation(m_annotationType.getDescriptor(),
                                                       true);
        if (av != null) {
            accept(av);
        }
    }

    public void accept(final MethodVisitor visitor) {
        AnnotationVisitor av = visitor.visitAnnotation(m_annotationType.getDescriptor(),
                                                       true);
        if (av != null) {
            accept(av);
        }
    }

    public void accept(final MethodVisitor visitor, final int index) {
        AnnotationVisitor av = visitor.visitParameterAnnotation(index,
                                                                m_annotationType.getDescriptor(),
                                                                true);
        if (av != null) {
            accept(av);
        }
    }

    public void accept(final AnnotationDiscovery visitor) {
        AnnotationVisitor av = visitor.visitAnnotation(m_annotationType.getDescriptor());
        if (av != null) {
            accept(av);
        }
    }

    private void accept(final AnnotationVisitor visitor) {
        // As per the ASM doc, visit methods must be called in a given order:
        // 1. visit()
        // 2. visitEnum()
        // 3. visitAnnotation()
        // 4. visitArray()

        // So values must be sorted
        Map<String, Object> values = getValues();
        accept(values, visitor);
        acceptEnum(values, visitor);
        acceptAnnotation(values, visitor);
        acceptArray(values, visitor);

        // Do not forget to visitEnd()
        visitor.visitEnd();

        // TODO This should disappear, only useful for testing
        if (!values.isEmpty()) {
            // We missed something during serialization
            throw new IllegalStateException(
                    format("Attributes of @%s could not be serialized: %s",
                           m_annotation.annotationType().getSimpleName(),
                           values.keySet())
            );
        }
    }

    private void acceptAnnotation(final Map<String, Object> values, final AnnotationVisitor visitor) {
        Map<String, Object> copy = new HashMap<String, Object>(values);
        for (Map.Entry<String, Object> entry : copy.entrySet()) {

            Class<?> type = entry.getValue().getClass();
            if (Annotation.class.isAssignableFrom(type)) {

                Annotation annotation = (Annotation) entry.getValue();
                AnnotationVisitor annotationVisitor = visitor.visitAnnotation(entry.getKey(),
                                                                              getType(annotation.annotationType()).getDescriptor());
                if (annotationVisitor != null) {
                    AnnotationPlayback playback = new AnnotationPlayback(annotation);
                    playback.accept(annotationVisitor);
                }

                values.remove(entry.getKey());
            }
        }
    }

    private void acceptEnum(final Map<String, Object> values, final AnnotationVisitor visitor) {

        Map<String, Object> copy = new HashMap<String, Object>(values);
        for (Map.Entry<String, Object> entry : copy.entrySet()) {

            Class<?> type = entry.getValue().getClass();
            if (type.isEnum()) {
                Enum<?> enumValue = (Enum<?>) entry.getValue();
                visitor.visitEnum(entry.getKey(),
                                  getType(type).getDescriptor(),
                                  enumValue.name());

                values.remove(entry.getKey());
            }
        }
    }

    private void accept(final Map<String, Object> values, final AnnotationVisitor visitor) {

        Map<String, Object> copy = new HashMap<String, Object>(values);
        for (Map.Entry<String, Object> entry : copy.entrySet()) {

            Class<?> type = entry.getValue().getClass();
            if (isSimpleType(type)) {

                // Accept Byte, Boolean, Character, Short, Integer, Long, Float, Double
                // Accept String
                // Accept Array of byte, boolean, char, short, int, long, float, double
                visitor.visit(entry.getKey(), transform(entry.getValue()));

                values.remove(entry.getKey());
            }
        }
    }

    private boolean isSimpleType(final Class<?> type) {
        return isPrimitive(type) ||
                String.class.equals(type) ||
                Class.class.equals(type) ||
                (type.isArray() && isPrimitive(type.getComponentType()));
    }

    private boolean isPrimitive(final Class<?> type) {
        if (type.isPrimitive()) {
            return true;
        }

        if (BOXED_TYPES.contains(type)) {
            return true;
        }

        return false;
    }

    private void acceptArray(final Map<String, Object> values, final AnnotationVisitor visitor) {
        Map<String, Object> copy = new HashMap<String, Object>(values);
        for (Map.Entry<String, Object> entry : copy.entrySet()) {

            Class<?> type = entry.getValue().getClass();
            if (type.isArray()) {

                // Simple arrays have been visited using AnnotationVisitor.visit(String, Object)

                AnnotationVisitor arrayVisitor = visitor.visitArray(entry.getKey());
                if (arrayVisitor != null) {
                    Object[] array = (Object[]) entry.getValue();
                    Class<?> componentType = array.getClass().getComponentType();
                    Type asmType = Type.getType(componentType);

                    if (componentType.isEnum()) {
                        for (Object o : array) {
                            Enum eValue = (Enum) o;
                            arrayVisitor.visitEnum(null, asmType.getDescriptor(), eValue.name());
                        }
                    } else if (componentType.isAnnotation()) {
                        for (Object o : array) {
                            Annotation annotation = (Annotation) o;
                            AnnotationVisitor annotationVisitor = arrayVisitor.visitAnnotation(null, asmType.getDescriptor());
                            if (annotationVisitor != null) {
                                AnnotationPlayback playback = new AnnotationPlayback(annotation);
                                playback.accept(annotationVisitor);
                            }
                        }
                    } else {
                        for (Object o : array) {
                            arrayVisitor.visit(null, transform(o));
                        }
                    }

                    arrayVisitor.visitEnd();
                }

                values.remove(entry.getKey());
            }
        }
    }

    private Object transform(final Object value) {
        if (value instanceof Class) {
            return getType((Class) value);
        }
        return value;
    }
}
