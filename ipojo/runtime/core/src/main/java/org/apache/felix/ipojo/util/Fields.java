/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
* Fluent API to retrieve fields of a given type.
*/
public class Fields<T> {

    private Class<? extends T> type;
    private Class<?> clazz;
    private Object object;
    private Map<String, Field> fields;

    public Collection<T> get() {
        return map().values();
    }

    public Map<Field, T> map(Object o) {
        this.object = o;
        return map();
    }

    public Map<Field, T> map() {
        Collection<Field> set = retrieve();
        Map<Field, T> results = new LinkedHashMap<Field, T>();
        for (Field field : set) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            try {
                results.put(field, (T) field.get(object));
            } catch (IllegalAccessException e) {
                // Hopefully should not happen.
            }
        }
        return results;
    }

    public Collection<T> get(Object o) {
        this.object = o;
        return get();
    }

    public Fields ofType(Class<? extends T> clazz) {
        this.type = clazz;
        return this;
    }

    public Fields in(Object o) {
        this.object = o;
        this.clazz = o.getClass();
        return this;
    }

    public Fields in(Class<?> c) {
        this.clazz = c;
        return this;
    }

    private Collection<Field> retrieve() {
        if (fields != null) {
            return fields.values();
        }

        if (clazz == null) {
            throw new NullPointerException("Cannot retrieve field, class not set");
        }


        fields = new LinkedHashMap<String, Field>();

        // First the class itself
        Field[] list = clazz.getDeclaredFields();
        for (Field field : list) {
            if (type == null || type.isAssignableFrom(field.getType())) {
                fields.put(field.getName(), field);
            }
        }

        // Traverse class hierarchy
        if (clazz.getSuperclass() != null) {
            traverse(fields, clazz.getSuperclass());
        }

        return fields.values();
    }

    private void traverse(Map<String, Field> fields, Class<?> clazz) {
        // First the given class
        Field[] list = clazz.getDeclaredFields();
        for (Field field : list) {
            if (type == null || type.isAssignableFrom(field.getType())) {
                // Already defined by a daughter class
                if (!fields.containsKey(field.getName())) {
                    // Check visibility if must be either  public or protected.
                    if (Modifier.isPublic(field.getModifiers()) || Modifier.isProtected(field.getModifiers())) {
                        fields.put(field.getName(), field);
                    }
                }
            }
        }

        // If we have a parent class, traverse it
        if (clazz.getSuperclass() != null) {
            traverse(fields, clazz.getSuperclass());
        }
    }

}
