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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class DTOUtil {
    private DTOUtil() {
        // Do not instantiate. This is a utility class.
    }

    public static boolean isDTOType(Class<?> cls) {
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
}
