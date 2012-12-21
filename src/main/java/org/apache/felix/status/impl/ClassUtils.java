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
package org.apache.felix.status.impl;

import java.lang.reflect.Method;

/**
 * Utility methods
 */
public class ClassUtils {

    /**
     * Search a method with the given name and signature
     */
    public static Method searchMethod(final Class<?> clazz, final String mName, final Class<?>[] params) {
        try {
            final Method m = clazz.getMethod(mName, params);
            m.setAccessible(true);
            return m;
        } catch (Throwable nsme) {
            // ignore, we catch Throwable above to not only catch NoSuchMethodException
            // but also other ones like ClassDefNotFoundError etc.
        }
        if ( clazz.getSuperclass() != null ) {
            // try super class
            return searchMethod(clazz.getSuperclass(), mName, params);
        }
        return null;
    }

    /**
     * Invoke the method on the printer with the arguments.
     */
    public static Object invoke(final Object obj, final Method m, final Object[] args) {
        try {
            return m.invoke(obj, args);
        } catch (final Throwable e) {
            // ignore
        }
        return null;
    }
}
