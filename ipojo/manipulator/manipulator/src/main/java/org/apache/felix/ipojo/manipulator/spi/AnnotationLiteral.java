/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.manipulator.spi;

import static java.lang.String.format;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Base annotation literal class to be extended when declaring stereotype bindings in a programmatic way.
 * Usage example:
 * <pre>
 *     public class InstantiateLiteral extends AnnotationLiteral<Instantiate> implements Instantiate {
 *         public String name() {
 *             return "";
 *         }
 *     }
 * </pre>
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class AnnotationLiteral<T extends Annotation> implements Annotation {

    private Class<? extends Annotation> annotationType;

    public Class<? extends Annotation> annotationType() {
        if (annotationType == null) {
            annotationType = findAnnotationType(getClass());
            if (annotationType == null) {
                throw new IllegalStateException(
                        format("Annotation %s does not specify its annotation type (T) in AnnotationLiteral<T>",
                               getClass().getName())
                );
            }
        }
        return annotationType;
    }

    public org.objectweb.asm.Type getType() {
        return org.objectweb.asm.Type.getType(annotationType());
    }

    private static Class<Annotation> findAnnotationType(final Class<? extends AnnotationLiteral> type) {
        Class<?> implementer = findImplementer(type);
        return findTypeParameter(implementer);
    }

    private static Class<Annotation> findTypeParameter(final Class<?> clazz) {
        // Get the T of AnnotationLiteral<T>
        Type type = clazz.getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            return (Class<Annotation>) pType.getActualTypeArguments()[0];
        }
        return null;
    }

    private static Class<? extends AnnotationLiteral> findImplementer(final Class<? extends AnnotationLiteral> type) {
        Class<? extends AnnotationLiteral> superClass = type.getSuperclass().asSubclass(AnnotationLiteral.class);
        if (AnnotationLiteral.class.equals(superClass)) {
            return type;
        } else {
            return findImplementer(superClass);
        }
    }
}
