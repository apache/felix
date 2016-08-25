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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    static Type primitiveToBoxed(Type type) {
        if (type instanceof Class)
            return primitiveToBoxed((Class<?>) type);
        else
            return null;
    }

    static Class<?> primitiveToBoxed(Class<?> cls) {
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
}
